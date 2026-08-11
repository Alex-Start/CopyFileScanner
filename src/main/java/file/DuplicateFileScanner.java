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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Find duplicates files (the same hash contents file)
 */
public class DuplicateFileScanner implements IFileScanner {
    private static final Logger logger = LogManager.getLogger(DuplicateFileScanner.class);
    public static final String DELIM = ";";
    private final IScanProgressCallback callback;
    private ActionTabWrap.ActionTab tab;

    public DuplicateFileScanner(IScanProgressCallback callback) {
        this.callback = callback;
    }

    public Map<String, List<String>> findDuplicateFiles(String directoryPath, ActionTabWrap.ActionTab tab) {
        this.tab = tab;
        String[] paths = directoryPath.split(DELIM);
        AtomicInteger filesScanned = new AtomicInteger(0);

        callback.onScanStarted("Scanning for duplicates...", tab);
        //Map<String, List<FileMetadata>> filesByName = ThreadManager.oneThreadPerDevice(paths, this);
        Map<String, List<FileMetadata>> filesByName = new ThreadManager(paths, false, new SettingsManager(ActionHelper.Action.SCAN)).createDynamicThreads(this);

        int totalFiles = filesByName.size();
        int duplicateCount = 0;

        // Step 2: Compare files with the same name using content hash
        Map<String, List<String>> duplicates = new HashMap<>();
        for (Map.Entry<String, List<FileMetadata>> entry : filesByName.entrySet()) {
            List<FileMetadata> fileMetadata = entry.getValue();
            List<String> filePaths = fileMetadata.stream().map(x->x.getAbsolutePath().toString()).collect(Collectors.toList());
            if (filePaths.size() > 1) {
                logger.debug("Matched: {}", filePaths);
                // get the first to get file name
                String key = Path.of(filePaths.get(0)).getFileName().toString();
                // show the same file names with grouping 'filename [n]'
                String suffix = "";
                if (duplicates.containsKey(key)) {
                    int i = 1;
                    while (duplicates.containsKey(key + " [" + i + "]")) {
                        i++;
                    }
                    suffix = " [" + i + "]";
                }
                duplicates.put(key + suffix, filePaths);
                duplicateCount += entry.getValue().size();
            }
            int scanned = filesScanned.incrementAndGet();
            callback.onScanProgress((int) ((double) scanned / totalFiles * 100), tab);
        }

        callback.onScanCompletedDuplicates(duplicates, tab, "Duplicate scan completed: " + duplicateCount + " duplicate files found in " + duplicates.size() + " groups.");
        logger.info("Scan duplicates is finished.");

        return duplicates;
    }

    public void scanFiles(String folderPath, Map<String, List<FileMetadata>> filesByName) {
        AtomicInteger filesScanned = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(100);
        FileUtils.listAllFilesInto(new File(folderPath), metadata -> {
            String hash = metadata.getChecksum();
            filesByName.computeIfAbsent(hash, k -> new ArrayList<>()).add(metadata);

            updateScanProgress(callback, filesScanned, totalFiles, tab);
        });
    }
    public void scanFiles(File folderPath, Map<String, List<FileMetadata>> filesByName) {
        AtomicInteger filesScanned = new AtomicInteger(0);
        AtomicInteger totalFiles = new AtomicInteger(100);
        FileUtils.listAllFilesInto(folderPath, metadata -> {
            String hash = metadata.getChecksum();
            filesByName.computeIfAbsent(hash, k -> new ArrayList<>()).add(metadata);

            updateScanProgress(callback, filesScanned, totalFiles, tab);
        });
    }

    public static void main(String[] args) {
        String directory = "C:\\Your\\Folder\\Path";

        IScanProgressCallback callback = new IScanProgressCallback() {
            @Override
            public void onScanStarted(String message, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onScanProgress(int percentage, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onScanCompletedCopy(Map<String, String> differences, ActionTabWrap.ActionTab tab, String message) {

            }

            @Override
            public void onScanCompletedDuplicates(Map<String, List<String>> duplicates, ActionTabWrap.ActionTab tab, String message) {

            }

            @Override
            public void onScanError(String errorMessage, ActionTabWrap.ActionTab tab) {

            }
        }
        ;

        Map<String, List<String>> duplicates = new DuplicateFileScanner(callback).findDuplicateFiles(directory, ActionTabWrap.ActionTab.DUPLICATE);

        if (duplicates.isEmpty()) {
            System.out.println("No duplicate files found.");
        } else {
            System.out.println("Duplicate files found:");
            for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
                System.out.println("File: " + entry.getKey());
                for (String path : entry.getValue()) {
                    System.out.println("  -> " + path);
                }
            }
        }
    }
}