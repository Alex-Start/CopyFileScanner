package file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMetadata {
    private static final Logger logger = LogManager.getLogger(FileMetadata.class);

    private final String rootPath;      // Store the root folder path
    private final String relativePath;  // Store the relative path
    private final long size;
    private final long lastModified;
    private String comment;
    private byte[] content;  // Lazily loaded content
    private String checksum;  // New field for SHA-256 hash

    public FileMetadata(String rootPath, String filePath, long size, long lastModified) {
        this(rootPath, filePath, size, lastModified, "");
    }

    public FileMetadata(String rootPath, String filePath, long size, long lastModified, String comment) {
        this.rootPath = rootPath;
        this.relativePath = FileUtils.getRelativePath(rootPath, filePath);
        this.size = size;
        this.lastModified = lastModified;
        this.comment = comment;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getComment() {
        return comment;
    }

    public byte[] getContent() {
        if (content == null) {
            try {
                content = Files.readAllBytes(Path.of(rootPath, relativePath));
            } catch (IOException e) {
                logger.error("Error reading file content: {}{}{}", rootPath, File.separator, relativePath);
                content = new byte[0];
            }
        }
        return content;
    }

    public String getChecksum() {
        if (checksum == null) {
            checksum = computeChecksum();
        }
        return checksum;
    }

    private String computeChecksum() {
        try {
            return FileUtils.calculateFileHash(Path.of(rootPath, relativePath).toString());
        } catch (IOException /*| NoSuchAlgorithmException */e) {
            logger.error("Error computing checksum for: {}", Path.of(rootPath, relativePath).toString());
            comment = (comment + " Error computing checksum for: {}" + Path.of(rootPath, relativePath)).trim();
            return "";
        }
    }

    @Override
    public String toString() {
        return "file.FileMetadata{" +
                "relativePath='" + relativePath + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                '}';
    }
}