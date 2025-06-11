import file.CopyFileScanner;
import file.FileActionConcurrently;
import file.FileCopier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class CopyFileScannerConsole {
    private static final Logger logger = LogManager.getLogger(CopyFileScannerConsole.class);

    // TODO there is only copy action
    public static void main(String[] args) {
        String usageMsg = "Usage: java CopyFileScannerConsole source=<source_directory> dest=<destination_directory>";
        if (args.length < 2) {
            logger.error(usageMsg);
            return;
        }

        String sourceDir = null;
        String destDir = null;
        for(String arg : args) {
            if (arg.startsWith("source=")) {
                sourceDir = arg.split("=")[1];
            } else if (arg.startsWith("dest=")) {
                destDir = arg.split("=")[1];
            }
        }

        if(sourceDir == null || destDir == null) {
            logger.error(usageMsg);
            return;
        }

        Map<String, String> differences = CopyFileScanner.scanAndCompare(sourceDir, destDir);

        if (differences.isEmpty()) {
            logger.info("No differences found. Everything is up to date.");
            return;
        }

        // Display differences
        logger.info("Differences found:");
        List<String> diffList = new ArrayList<>(differences.keySet());
        for (int i = 0; i < diffList.size(); i++) {
            logger.info("{}. {} -> {}", (i + 1), diffList.get(i), differences.get(diffList.get(i)));
        }

        // User input
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nEnter the numbers of files you want to copy (comma-separated, or 'all' to copy all): ");
        String input = scanner.nextLine();

        Set<String> selectedFiles = new HashSet<>();
        if (input.trim().equalsIgnoreCase("all")) {
            selectedFiles.addAll(diffList);
        } else {
            for (String index : input.split(",")) {
                try {
                    int idx = Integer.parseInt(index.trim()) - 1;
                    if (idx >= 0 && idx < diffList.size()) {
                        selectedFiles.add(diffList.get(idx));
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Invalid input: {}. Skipping...", index);
                }
            }
        }

        // Start copying
        new FileActionConcurrently(new FileCopier(sourceDir, destDir)).doActionFiles(selectedFiles);
    }
}
