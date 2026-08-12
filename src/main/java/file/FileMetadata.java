package file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class FileMetadata {
    public static final String ERROR_FILE_ATTRIBUTES = "Error file attributes";
    private static final Logger logger = LogManager.getLogger(FileMetadata.class);

    private final String rootPath;
    private final String relativePath;
    private final String absolutePath;
    private final long size;
    private final long lastModified;
    private String comment;
    private byte[] content;
    private String checksum;

    public FileMetadata(String rootPath, String filePath, long size, long lastModified) {
        this(rootPath, filePath, size, lastModified, "");
    }

    public FileMetadata(String rootPath, String filePath, long size, long lastModified, String comment) {
        this.rootPath = rootPath;
        this.absolutePath = filePath;
        this.relativePath = FileUtils.getRelativePath(rootPath, filePath);
        this.size = size;
        this.lastModified = lastModified;
        this.comment = comment;
    }

    public static FileMetadata getFileMetadata(Path file) {
        return getFileMetadata(file.getParent(), new File(file.toString()));
    }

    public static FileMetadata getFileMetadata(Path rootPath, File file) {
        try {
            Path filePath = file.toPath();
            String comment = "";
            long lastModifiedTime = 0;
            if (file.exists()) {
                try {
                    lastModifiedTime = Files.readAttributes(filePath, BasicFileAttributes.class).lastModifiedTime().toMillis();
                } catch (Exception e) {
                    logger.error("Error reading file attributes: {}\n{}", file.getAbsolutePath(), e.getMessage());
                    comment = ERROR_FILE_ATTRIBUTES;
                }
            } else {
                logger.error("Not exists file {}", file.getAbsolutePath());
                return null;
            }
            return new FileMetadata(
                    rootPath.toString(),
                    filePath.toString(),
                    file.length(),
                    lastModifiedTime,
                    comment
            );
        } catch (Exception e) {
            logger.error("Error reading file parameters: {}\n{}", file.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    public Path getAbsolutePath() {
        return Path.of(absolutePath);
    }

    public Path getRelativePath() {
        return Path.of(relativePath);
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
            Path filePath = Path.of(rootPath, relativePath);
            try {
                if (Files.exists(filePath) && Files.size(filePath) > 50 * 1024 * 1024) {
                    logger.warn("File exceeds 50MB threshold, skipping full content byte array load: {}", filePath);
                    content = new byte[0];
                } else {
                    content = Files.readAllBytes(filePath);
                }
            } catch (IOException e) {
                logger.error("Error reading file content: {}", filePath, e);
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
        Path path = Path.of(rootPath, relativePath);
        try {
            return FileUtils.calculateFileHash(path.toString());
        } catch (IOException e) {
            logger.error("Error computing checksum for: {}", path.toString());
            comment = (comment + " Error computing checksum for: " + path).trim();
            return "";
        }
    }

    @Override
    public String toString() {
        return "file.FileMetadata{" +
                "relativePath='" + relativePath + "'" +
                ", size=" + size +
                ", lastModified=" + lastModified +
                '}';
    }
}
