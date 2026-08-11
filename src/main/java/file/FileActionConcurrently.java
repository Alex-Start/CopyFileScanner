package file;

import common.ActionTabWrap;
import common.ActionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.IFileActionProgressCallback;
import settings.SettingsManager;
import ui.table.FileTableModel;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FileActionConcurrently {
    private static final Logger logger = LogManager.getLogger(FileActionConcurrently.class);

    private static final int MAX_THREADS = 2; // Or use Runtime.getRuntime().availableProcessors()
    private final IFileActionProgressCallback callback;
    private int countThreads;
    private IFileAction fileAction;
    private Map<Integer, String> selectedRows;

    /*public FileActionConcurrently(ActionHelper.Action actionType, String sourceDir, String destDir, Collection<String> selectedRows, ActionTabWrap.ActionTab tab, IFileActionProgressCallback callback) {
        this.tab = tab;
        this.callback = callback;
        this.selectedRows = new HashMap<>();
        AtomicInteger atomicInteger = new AtomicInteger(-1);
        selectedRows.forEach(x-> this.selectedRows.put(atomicInteger.incrementAndGet(), x));

        switch (actionType) {

            case COPY -> {
                countThreads = SettingsManager.getThreadCountCopy(destDir);
                fileAction = new FileCopier(sourceDir, destDir);

            }
            case DELETE_SOURCE -> {
                countThreads = SettingsManager.getThreadCountDelete(selectedRows.toArray(new String[0]));
                fileAction = new FileDeleter();

            }
            case DELETE_DEST -> {
                countThreads = SettingsManager.getThreadCountDelete(selectedRows.toArray(new String[0]));
                fileAction = new FileDeleter();

            }
            default -> {
                throw new IllegalArgumentException("Unsupported action: "+ actionType);
            }
        }
    }

    public FileActionConcurrently(ActionHelper.Action actionType, String sourceDir, String destDir, FileTableModel fileTableModel, ActionTabWrap.ActionTab tab, IFileActionProgressCallback callback) {
        this.tab = tab;
        this.callback = callback;

        switch (actionType) {

            case COPY -> {
                selectedRows = fileTableModel.getSelectedRows();
                countThreads = SettingsManager.getThreadCountCopy(destDir);
                fileAction = new FileCopier(sourceDir, destDir);

            }
            case DELETE_SOURCE -> {
                selectedRows = fileTableModel.getSelectedRows(sourceDir);
                countThreads = SettingsManager.getThreadCountDelete(selectedRows.values().toArray(new String[0]));
                fileAction = new FileDeleter();

            }
            case DELETE_DEST -> {
                selectedRows = fileTableModel.getSelectedRows(destDir);
                countThreads = SettingsManager.getThreadCountDelete(selectedRows.values().toArray(new String[0]));
                fileAction = new FileDeleter();

            }
            default -> {
                throw new IllegalArgumentException("Unsupported action: "+ actionType);
            }
        }
    }*/

    // Constructor for selected rows as collection
    public FileActionConcurrently(ActionHelper.Action actionType, String sourceDir, String destDir,
                                  Collection<String> selectedRows,
                                  ActionTabWrap.ActionTab tab,
                                  IFileActionProgressCallback callback) {
        this.tab = tab;
        this.callback = callback;
        this.selectedRows = buildSelectedRowsMap(selectedRows);
        initAction(actionType, sourceDir, destDir, this.selectedRows);
    }

    // Constructor for selected rows from FileTableModel
    public FileActionConcurrently(ActionHelper.Action actionType, String sourceDir, String destDir,
                                  FileTableModel fileTableModel,
                                  ActionTabWrap.ActionTab tab,
                                  IFileActionProgressCallback callback) {
        this.tab = tab;
        this.callback = callback;
        this.selectedRows = switch (actionType) {
            case COPY -> fileTableModel.getSelectedRows();
            case DELETE_SOURCE -> fileTableModel.getSelectedRows(sourceDir);
            case DELETE_DEST -> fileTableModel.getSelectedRows(destDir);
            default -> throw new IllegalArgumentException("Unsupported action: " + actionType);
        };
        initAction(actionType, sourceDir, destDir, this.selectedRows);
    }

    // Extracted helper method to build indexed map from selected rows
    private Map<Integer, String> buildSelectedRowsMap(Collection<String> rows) {
        Map<Integer, String> map = new HashMap<>();
        AtomicInteger index = new AtomicInteger(-1);
        rows.forEach(x -> map.put(index.incrementAndGet(), x));
        return map;
    }

    // Central method for initializing threads and action based on type
    private void initAction(ActionHelper.Action actionType, String sourceDir, String destDir, Map<Integer, String> rows) {
        switch (actionType) {
            case COPY -> {
                this.countThreads = SettingsManager.getThreadCountCopy(destDir);
                this.fileAction = new FileCopier(sourceDir, destDir);
            }
            case DELETE_SOURCE, DELETE_DEST -> {
                String[] paths = rows.values().toArray(new String[0]);
                this.countThreads = SettingsManager.getThreadCountDelete(paths);
                this.fileAction = new FileDeleter();
            }
            default -> throw new IllegalArgumentException("Unsupported action: " + actionType);
        }
    }

    private ExecutorService executorService;
    private final AtomicInteger proceededFiles = new AtomicInteger(0);
    private List<String> passedFiles = new ArrayList<>();
    private final Object lock = new Object();
    private volatile boolean isFinished = false;
    private int totalFiles;
    private final ActionTabWrap.ActionTab tab;

    public void performFileAction() {

        if (selectedRows.isEmpty()) {
            callback.onActionCompleted("No files selected for action.", null, tab);
            return;
        }

        callback.onActionStarted("Starting file action...", tab);

        totalFiles = selectedRows.size();

        boolean result = doActionFiles(selectedRows);
    }

    public boolean doActionFiles() {
        return doActionFiles(selectedRows);
    }

    public boolean doActionFiles(Map<Integer, String> filesToDoAction) {
        return doActionFiles(filesToDoAction.values());
    }

    public boolean doActionFiles(Collection<String> filesToDoAction) {
        if (filesToDoAction.isEmpty()) {
            logger.info("No files selected for '{}'.", fileAction.getActionName());
            isFinished = true;
            callback.onActionError("No files selected for "+ fileAction.getActionName(), tab);
            return false;
        }

        isFinished = false;
        passedFiles = new ArrayList<>();
        logger.info("Starting file '{}' with {} files...", fileAction.getActionName(), filesToDoAction.size());

        executorService = Executors.newFixedThreadPool(countThreads); // Adjust thread count as needed

        AtomicBoolean result = new AtomicBoolean(true);
        for (String relativePath : filesToDoAction) {
            executorService.submit(() -> result.set(result.get() & doActionFile(relativePath)));
        }

        executorService.submit(() -> {
            while(filesToDoAction.size() > getCountProceededFiles()) {
                try {
                    Thread.sleep(1000); // Prevent CPU overuse
                } catch (InterruptedException ignored) {
                }
            }
        });

        shutdownAndAwaitTermination();
        logger.info("'{}' process completed!", fileAction.getActionName());
        isFinished = true;
        return result.get();
    }

    private boolean doActionFile(String relativePath) {
        boolean status = false;
        try {
            status = fileAction.doAction(relativePath);
            synchronized (lock) {
                passedFiles.add(relativePath);
            }
            return status;
        } catch (IOException e) {
            // error logged in doAction(...)
            // TODO add error message for callback and show it on Completed Action
            return status;
        } finally {
            int currentProcessed = proceededFiles.incrementAndGet();
            int percentage = (int) ((double) currentProcessed / totalFiles * 100);
            if (status) {
                int rowIndex = selectedRows.entrySet().stream()
                        .filter(x -> x.getValue().equals(relativePath))
                        .findFirst().get().getKey();
                callback.onFileProcessed(rowIndex, fileAction.getProceededName(), tab); // Update specific row
            }
            callback.onActionProgress(percentage, tab); // Update overall progress
        }
    }

    public boolean isFinished() {
        return isFinished;
    }

    public int getCountProceededFiles() {
        return proceededFiles.get();
    }


    private void shutdownAndAwaitTermination() {
        executorService.shutdown();
        new Thread(() -> { // Use a separate thread to wait for termination
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                callback.onActionCompleted("File action completed.", null, tab);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                callback.onActionError("File action interrupted.", tab);
                logger.error("File action interrupted: " + e.getMessage(), e);
            }
        }).start();
    }
}
