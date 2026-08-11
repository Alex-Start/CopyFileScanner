package utils;

import file.FileMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FileUtils {
    private static final Logger logger = LogManager.getLogger(FileUtils.class);

    public static String calculateFileHash(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());//HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IOException("Error computing hash for file: " + filePath, e);
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static String getRelativePath(String rootPath, String fullFilePath) {
        return Path.of(rootPath).relativize(Path.of(fullFilePath)).toString();
    }

    public static boolean copySingleFile(Path sourceFile, Path destFile) throws IOException {
        Files.createDirectories(destFile.getParent()); // Ensure destination folder exists
        Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
        return true;//TODO check it
    }

    public static void deleteFilesConcurrently(Collection<String> filePaths) {
        deleteFilesConcurrently(filePaths, 4);
    }

    public static void deleteFilesConcurrently(Collection<String> filePaths, int countThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(countThreads);

        for (String filePath : filePaths) {
            executor.submit(() -> deleteFile(filePath));
        }

        // Shutdown the executor after all tasks are submitted
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                logger.warn("Timeout reached before completion.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean deleteFile(String filePath) {
        boolean result = false;
        try {
            File file = new File(filePath);

            if (!file.exists()) {
                logger.warn("File does not exist: {}", filePath);
                return false;
            }

            Path path = file.toPath();
            // Step 1: set writable to file
            if (!file.canWrite()) {
                file.setWritable(true);
            }

            // 2. Remove 'readonly' attribute if on Windows
            if (isWindows()) {
                try {
                    Files.setAttribute(path, "dos:readonly", false);
                } catch (UnsupportedOperationException ignored) {
                    // Skip if attribute doesn't exist
                }
            } //TODO for unix

            // 3. Delete the file
            Files.delete(path);
            result = true;
        } catch (IOException e) {
            logger.warn("Failed to delete file: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static boolean deleteEmptyFolder(String path) {
        File parentFolder = new File(path);
        // Check and delete the folder if empty
        while (parentFolder != null && parentFolder.isDirectory() && isFolderEmpty(parentFolder)) {
            boolean folderDeleted = parentFolder.delete();
            if (!folderDeleted) {
                logger.warn("Failed to delete folder: {}", parentFolder.getAbsolutePath());
                return false;
            }
            parentFolder = parentFolder.getParentFile(); // Move up in hierarchy
        }

        return true;
    }

    private static boolean isFolderEmpty(File folder) {
        File[] files = folder.listFiles();
        return files == null || files.length == 0;
    }

    public static List<FileMetadata> allFilesIntoList(File directory) {
        List<FileMetadata> fileList = new ArrayList<>();
        return allFilesIntoList(directory, fileList);
    }

    public static List<FileMetadata> allFilesIntoList(File directory, List<FileMetadata> fileList) {
        listAllFilesInto(directory, fileList::add);
        return fileList;
    }

    public static void listAllFilesInto(File directory, Consumer<FileMetadata> fileMetadataConsumer) {
        //TODO check new File(null), new File(""), ...
        if (!directory.isDirectory()) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            walk.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(x->{
                        logger.debug(x.getAbsolutePath());
                        FileMetadata fileMetadata = FileMetadata.getFileMetadata(directory.toPath(), x);
                        fileMetadataConsumer.accept(fileMetadata);
                    });
        } catch (Exception e) {
            // Log this error
            logger.error("listAllFilesInto", e);
        }
    }

    public static boolean checkDirPath(String[] paths) {
        for (String path : paths) {
            if (!checkDirPath(path)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkDirPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }

    //TODO alternative to FileUtils.listAllFilesInto - check what is faster
    private void scanDirectory(File directory, Consumer<FileMetadata> fileMetadataConsumer) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file, fileMetadataConsumer);
                } else {
                    logger.debug(file.getAbsolutePath());
                    FileMetadata fileMetadata = FileMetadata.getFileMetadata(directory.toPath(), file);
                    if (fileMetadata != null) {
                        fileMetadataConsumer.accept(fileMetadata);
                    }
                }
            }
        }
    }
}
