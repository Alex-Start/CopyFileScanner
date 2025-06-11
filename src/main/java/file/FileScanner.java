package file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileScanner {
    private static final Logger logger = LogManager.getLogger(FileScanner.class);
    private final Path rootPath;
    public final String ERROR_FILE_ATTRIBUTES = "Error file attributes";

    public FileScanner(String directory) {
        this.rootPath = Path.of(directory);
    }

    public List<FileMetadata> scan() {
        List<FileMetadata> fileList = new ArrayList<>();
        scanDirectory(rootPath.toFile(), fileList);
        return fileList;
    }

    private void scanDirectory(File directory, List<FileMetadata> fileList) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanDirectory(file, fileList);
                    } else {
                        FileMetadata fileMetadata = getFileMetadata(file);
                        if (fileMetadata != null) {
                            fileList.add(fileMetadata);
                        }                    }
                }
            }
        }
    }

    private FileMetadata getFileMetadata(File file) {
        try {
            Path filePath = file.toPath();
            String comment = "";

            long lastModifiedTime = 0;
            try {
                lastModifiedTime = Files.readAttributes(filePath, BasicFileAttributes.class).lastModifiedTime().toMillis();
            } catch (Exception e) {
                logger.error("Error reading file attributes: {}\n{}", file.getAbsolutePath(), e.getMessage());
                comment = ERROR_FILE_ATTRIBUTES;
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
}