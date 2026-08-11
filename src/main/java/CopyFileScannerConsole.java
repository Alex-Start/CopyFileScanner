import common.ActionHelper;
import common.ActionTabWrap;
import file.CopyFileScanner;
import file.FileActionConcurrently;
import file.FileCopier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.IFileActionProgressCallback;
import service.IScanProgressCallback;
import ui.FindCopyPanel;

import javax.swing.*;
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

        Map<String, String> differences = new HashMap<>();
        IScanProgressCallback callbackScan = new IScanProgressCallback() {
            @Override
            public void onScanStarted(String message, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onScanProgress(int percentage, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onScanCompletedCopy(Map<String, String> diff, ActionTabWrap.ActionTab tab, String message) {
                SwingUtilities.invokeLater(() -> {
                    differences.putAll(diff);
                });
            }

            @Override
            public void onScanCompletedDuplicates(Map<String, List<String>> duplicates, ActionTabWrap.ActionTab tab, String message) {

            }

            @Override
            public void onScanError(String errorMessage, ActionTabWrap.ActionTab tab) {

            }
        };
        new CopyFileScanner(callbackScan).scanAndCompare(sourceDir, destDir, false, ActionTabWrap.ActionTab.COPY);

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

        IFileActionProgressCallback callback = new IFileActionProgressCallback() {
            @Override
            public void onActionStarted(String message, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onActionProgress(int percentage, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onFileProcessed(int rowIndex, String status, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onActionCompleted(String message, String warning, ActionTabWrap.ActionTab tab) {

            }

            @Override
            public void onActionError(String errorMessage, ActionTabWrap.ActionTab tab) {

            }
        };
        // Start copying
        new FileActionConcurrently(ActionHelper.Action.COPY, sourceDir, destDir, selectedFiles, ActionTabWrap.ActionTab.COPY, callback).performFileAction();
    }
}
