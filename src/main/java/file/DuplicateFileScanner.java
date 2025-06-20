package file;

import common.ActionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import settings.SettingsManager;
import utils.FileUtils;
import utils.ThreadManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class DuplicateFileScanner implements IFileScanner {
    private static final Logger logger = LogManager.getLogger(DuplicateFileScanner.class);
    public static final String DELIM = ";";

    public static Map<String, List<String>> findDuplicateFiles(String directoryPath) {
        String[] paths = directoryPath.split(DELIM);

        //Map<String, List<String>> filesByName = ThreadManager.oneThreadPerDevice(paths, new DuplicateFileScanner());
        Map<String, List<String>> filesByName = new ThreadManager(paths, false, new SettingsManager(ActionHelper.Action.SCAN)).createDynamicThreads(new DuplicateFileScanner());

        // Step 2: Compare files with the same name using content hash
        Map<String, List<String>> duplicates = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : filesByName.entrySet()) {
            List<String> filePaths = entry.getValue();
            if (filePaths.size() > 1) {
                logger.debug("Matched: {}", filePaths);
                String key = Path.of(filePaths.get(0)).getFileName().toString();
                String suffix = "";
                if (duplicates.containsKey(key)) {
                    int i = 1;
                    while (duplicates.containsKey(key + " [" + i + "]")) {
                        i++;
                    }
                    suffix = " [" + i + "]";
                }
                duplicates.put(key + suffix, filePaths);
            }
        }

        logger.info("Scan duplicates is finished.");

        return duplicates;
    }

    public void scanFiles(String folderPath, Map<String, List<String>> filesByName) {
        if(folderPath == null || folderPath.isEmpty()) {
            logger.warn("scanFiles: Folder is null or empty");
            return;
        }
        scanFiles(new File(folderPath), filesByName);
    }
    public void scanFiles(File folder, Map<String, List<String>> filesByName) {
        if (folder == null || !folder.exists()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanFiles(file, filesByName);
            } else {
                try {
                    logger.debug(file.getAbsolutePath());
                    String hash = calculateFileHash(file.getAbsolutePath());
                    filesByName.computeIfAbsent(hash, k -> new ArrayList<>()).add(file.getAbsolutePath());
                } catch (IOException e) {
                    logger.error("Error reading file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    private static String calculateFileHash(String filePath) throws IOException {
        return FileUtils.calculateFileHash(filePath);
    }

    public static void main(String[] args) {
        String directory = "C:\\Your\\Folder\\Path";
        Map<String, List<String>> duplicates = findDuplicateFiles(directory);

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