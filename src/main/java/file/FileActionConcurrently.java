package file;

import common.ActionHelper;
import common.ActionTabWrap;
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
    private static final long THREAD_SHUTDOWN_TIMEOUT_MINUTES = 5;

    private final IFileAction fileAction;
    private final int countThreads;
    private final Map<Integer, String> selectedRows;
    private final IFileActionProgressCallback callback;

    // Constructor for selected rows as collection
    public FileActionConcurrently(ActionHelper.Action actionType, String sourceDir, String destDir,
                                  Collection<String> selectedRows,
                                  ActionTabWrap.ActionTab tab,
                                  IFileActionProgressCallback callback) {
        this.tab = tab;
        this.callback = callback;
        this.selectedRows = buildSelectedRowsMap(selectedRows);
        this.fileAction = createAction(actionType, sourceDir, destDir);
        this.countThreads = calculateThreadCount(actionType, destDir, this.selectedRows);
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
        this.fileAction = createAction(actionType, sourceDir, destDir);
        this.countThreads = calculateThreadCount(actionType, destDir, this.selectedRows);
    }

    // Extracted helper method to build indexed map from selected rows
    private Map<Integer, String> buildSelectedRowsMap(Collection<String> rows) {
        Map<Integer, String> map = new HashMap<>();
        AtomicInteger index = new AtomicInteger(-1);
        rows.forEach(x -> map.put(index.incrementAndGet(), x));
        return map;
    }

    private IFileAction createAction(ActionHelper.Action actionType, String sourceDir, String destDir) {
        return switch (actionType) {
            case COPY -> new FileCopier(sourceDir, destDir);
            case DELETE_SOURCE, DELETE_DEST -> new FileDeleter();
            default -> throw new IllegalArgumentException("Unsupported action: " + actionType);
        };
    }

    private int calculateThreadCount(ActionHelper.Action actionType, String destDir, Map<Integer, String> rows) {
        return switch (actionType) {
            case COPY -> SettingsManager.getThreadCountCopy(destDir);
            case DELETE_SOURCE, DELETE_DEST -> {
                String[] paths = rows.values().toArray(new String[0]);
                yield SettingsManager.getThreadCountDelete(paths);
            }
            default -> throw new IllegalArgumentException("Unsupported action: " + actionType);
        };
    }

    private ExecutorService executorService;
    private final AtomicInteger proceededFiles = new AtomicInteger(0);
    private List<String> passedFiles = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean isFinished = false;
    private int totalFiles;
    private final ActionTabWrap.ActionTab tab;

    public void performFileAction() {
        if (selectedRows.isEmpty()) {
            if (callback != null) {
                callback.onActionCompleted("No files selected for action.", null, tab);
            }
            return;
        }
        if (callback != null) {
            callback.onActionStarted("Starting file action...", tab);
        }
        totalFiles = selectedRows.size();
        doActionFiles(selectedRows);
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
            if (callback != null) {
                callback.onActionError("No files selected for " + fileAction.getActionName(), tab);
            }
            return false;
        }
        isFinished = false;
        passedFiles = Collections.synchronizedList(new ArrayList<>());
        logger.info("Starting file '{}' with {} files...", fileAction.getActionName(), filesToDoAction.size());
        executorService = Executors.newFixedThreadPool(countThreads); // Adjust thread count as needed
        AtomicBoolean overallSuccess = new AtomicBoolean(true);

        for (String relativePath : filesToDoAction) {
            executorService.submit(() -> {
                boolean success = doActionFile(relativePath);
                if (!success) {
                    overallSuccess.set(false);
                }
            });
        }

        shutdownAndAwaitTermination();
        logger.info("'{}' process completed!", fileAction.getActionName());
        isFinished = true;
        return overallSuccess.get();
    }

    private boolean doActionFile(String relativePath) {
        boolean status = false;
        try {
            status = fileAction.doAction(relativePath);
            if (status) {
                passedFiles.add(relativePath);
            }
            return status;
        } catch (IOException e) {
            // error logged in doAction(...)
            // TODO add error message for callback and show it on Completed Action
            logger.error("Error doing action on file {}: {}", relativePath, e.getMessage());
            return false;
        } finally {
            int currentProcessed = proceededFiles.incrementAndGet();
            int percentage = totalFiles > 0 ? (int) ((double) currentProcessed / totalFiles * 100) : 100;
            if (status && callback != null) {
                Optional<Map.Entry<Integer, String>> entry = selectedRows.entrySet().stream()
                        .filter(x -> x.getValue().equals(relativePath))
                        .findFirst();
                entry.ifPresent(e -> callback.onFileProcessed(e.getKey(), fileAction.getProceededName(), tab)); // Update specific row
            }
            if (callback != null) {
                callback.onActionProgress(percentage, tab); // Update overall progress
            }
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
                if (!executorService.awaitTermination(THREAD_SHUTDOWN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
                if (callback != null) {
                    callback.onActionCompleted("File action completed.", null, tab);
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                if (callback != null) {
                    callback.onActionError("File action interrupted.", tab);
                }
                logger.error("File action interrupted: " + e.getMessage(), e);
            }
        }).start();
    }
}
