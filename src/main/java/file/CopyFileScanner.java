package file;

import common.ActionHelper;
import common.ActionTabWrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.IScanProgressCallback;
import settings.SettingsManager;
import utils.FileUtils;
import utils.ThreadManager;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scan and compare files for source and dest dirs
 */
public class CopyFileScanner implements IFileScanner {
    private static final Logger logger = LogManager.getLogger(CopyFileScanner.class);

    private final IScanProgressCallback callback;
    private ActionTabWrap.ActionTab tab;

    public CopyFileScanner(IScanProgressCallback callback) {
        this.callback = callback;
    }

    public Map<String, String> scanAndCompare(String sourceDirStr, String destDirStr, boolean checkSource, ActionTabWrap.ActionTab tab) {
        this.tab = tab;
        Map<String, String> differences = new ConcurrentHashMap<>();
        File sourceDir = new File(sourceDirStr);
        File destDir = new File(destDirStr);

        if (!sourceDir.isDirectory()) {
            callback.onScanError("Source directory does not exist: " + sourceDirStr, tab);
            return differences;
        }

        callback.onScanStarted("Scanning differences...", tab);

        FileComparator fileComparator = new FileComparator();
        AtomicInteger filesScanned = new AtomicInteger(0);

        String[] arrDirs = new String[]{sourceDirStr};
        if (checkSource) {
            arrDirs = new String[]{sourceDirStr, destDirStr};
        }
        Map<String, List<FileMetadata>> mapFiles = new ThreadManager(arrDirs, false, new SettingsManager(ActionHelper.Action.SCAN))
                .createDynamicThreads(this);
        long totalFiles = mapFiles.values().stream()
                .mapToLong(List::size)
                .sum();

        for (Map.Entry<String, List<FileMetadata>> entry : mapFiles.entrySet()) {
            String rootPath = entry.getKey();
            boolean isSourceDir = rootPath.equalsIgnoreCase(sourceDirStr);

            for(FileMetadata fileMetadata : entry.getValue()) {
                try {

                    if (isSourceDir) {
                        Path relativePath = fileMetadata.getRelativePath();
                        File destFile = new File(destDir, relativePath.toString());

                        if (!fileComparator.compare(fileMetadata, FileMetadata.getFileMetadata(destFile.toPath()))) {
                            differences.put(relativePath.toString(), fileComparator.getMessage());
                        }
                    } else {
                        if (checkSource) {
                            Path relativePath = fileMetadata.getRelativePath();
                            File sourceFile = new File(sourceDir, relativePath.toString());

                            if (!fileComparator.compare(FileMetadata.getFileMetadata(sourceFile.toPath()), fileMetadata)) {
                                differences.put(relativePath.toString(), fileComparator.getMessage());
                            }
                        }
                    }

                    int scanned = filesScanned.incrementAndGet();
                    callback.onScanProgress((int) ((double) scanned / totalFiles * 100), tab);
                } catch (Exception e) {
                    logger.error("Error comparing file {}: {}", fileMetadata.getAbsolutePath(), e.getMessage(), e);
                }
            }
        }

        callback.onScanCompletedCopy(differences, tab, "Differences scan completed: " + totalFiles + " files found");
        logger.info("Scan differences is finished.");

        return differences;
    }

    public void scanFiles(String folderPath, Map<String, List<FileMetadata>> filesByName) {
        AtomicInteger filesScanned = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(100);
        FileUtils.listAllFilesInto(new File(folderPath), metadata -> {
            filesByName.computeIfAbsent(folderPath, k -> new ArrayList<>()).add(metadata);

            updateScanProgress(callback, filesScanned, totalFiles, tab);
        });
    }
    public void scanFiles(File folderPath, Map<String, List<FileMetadata>> filesByName) {
        AtomicInteger filesScanned = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(100);
        FileUtils.listAllFilesInto(folderPath, metadata -> {
            filesByName.computeIfAbsent(folderPath.toPath().toString(), k -> new ArrayList<>()).add(metadata);

            updateScanProgress(callback, filesScanned, totalFiles, tab);
        });
    }
}