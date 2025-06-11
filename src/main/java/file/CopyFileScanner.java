package file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

public class CopyFileScanner {
    private static final Logger logger = LogManager.getLogger(CopyFileScanner.class);

    public static Map<String, String> scanAndCompare(String sourceDir, String destDir) {
        return scanAndCompare(sourceDir, destDir, false);
    }

    public static Map<String, String> scanAndCompare(String sourceDir, String destDir, boolean isCheckSource) {
        logger.info("Starting file.CopyFileScanner...\nfrom {} to {}", sourceDir, destDir);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        Map<String, String> differences = new HashMap<>();
        try {
            Future<List<FileMetadata>> sourceFuture = executor.submit(() -> new FileScanner(sourceDir).scan());
            Future<List<FileMetadata>> destFuture = executor.submit(() -> new FileScanner(destDir).scan());

            List<FileMetadata> sourceFiles = sourceFuture.get();
            List<FileMetadata> destFiles = destFuture.get();

            // Validate scan results
            if (sourceFiles == null || sourceFiles.isEmpty()) {
                logger.warn("No files found in source directory: {}", sourceDir);
                return differences;
            }

            if (destFiles == null || destFiles.isEmpty()) {
                logger.warn("No files found in destination directory: {}", destDir);
                return differences;
            }

            Future<Map<String, String>> compareFuture = executor.submit(() -> new FileComparator(isCheckSource).compare(sourceFiles, destFiles));
            differences = compareFuture.get();

            logger.info("Differences found: {}", differences.size());
            differences.forEach((key, value) -> logger.warn("{} -> {}", key, value));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Scanning was interrupted", e);
        } catch (ExecutionException e) {
            logger.error("Error during scanning or comparison", e.getCause());
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    logger.warn("Executor did not terminate in time. Forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                logger.error("Shutdown was interrupted", e);
            }
            logger.info("file.CopyFileScanner execution completed.");
        }

        return differences;
    }
}