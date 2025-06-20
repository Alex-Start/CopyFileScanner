package service;

import file.FileActionConcurrently;
import file.FileCopier;
import file.FileDeleter;
import file.IFileAction;
import settings.SettingsManager;
import ui.MessageDialog;
import ui.StatusBarPanel;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static ui.CopyFileScannerSwingUI.*;

public class FileActionService {
    private final StatusBarPanel statusBarPanel;
    private final MessageDialog messageDialog;

    public FileActionService(StatusBarPanel statusBarPanel, MessageDialog messageDialog) {
        this.statusBarPanel = statusBarPanel;
        this.messageDialog = messageDialog;
    }

    public void executeFileCopier(String sourceDir, String destDir, Map<Integer, String> selectedRows) {
        IFileAction fileCopyAction = new FileCopier(sourceDir, destDir);
        SwingWorker<Void, Integer> worker = createWorker(fileCopyAction, SettingsManager.getThreadCountCopy(destDir)
                , COPIED, selectedRows);

        worker.execute();
    }

    public void executeFileDeleter(int columnIndex, Map<Integer, String> selectedRows) {
        String deleteMsg = (columnIndex == FILE_INDEX_COLUMN ? "Source " : "Dest ") + DELETED;
        IFileAction fileDeleteAction = new FileDeleter();
        // count of threads depends on all values in selectedRows. IF there are various disks from different physical HDD/SDD we can delete it in parallel.
        SwingWorker<Void, Integer> worker = createWorker(fileDeleteAction, SettingsManager.getThreadCountDelete(selectedRows.values().toArray(new String[0]))
                , deleteMsg, selectedRows);

        worker.execute();
    }

    private SwingWorker<Void, Integer> createWorker(IFileAction ifileAction, int countTreads, String actionName, Map<Integer, String> selectedRows) {
        return new SwingWorker<>() {
            private String warnMess = "";
            @Override
            protected Void doInBackground() {
                AtomicBoolean isPassed = new AtomicBoolean(false);
                FileActionConcurrently fileAction = new FileActionConcurrently(ifileAction, countTreads);

                ExecutorService executor = Executors.newFixedThreadPool(2);
                List<Callable<Void>> tasks = List.of(
                        () -> { isPassed.set(fileAction.doActionFiles(selectedRows.values())); return null; },
                        () -> { trackProgress(fileAction, selectedRows); return null; }
                );

                try {
                    executor.invokeAll(tasks);  // Run both tasks concurrently
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executor.shutdown();
                }

                warnMess = !isPassed.get() ? "\nThere are some error(s)" : "";
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                statusBarPanel.getProgressBar().setValue(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                statusBarPanel.setAndRefreshProgressBar(100);
                getFileOperationController().enablePanelAndButtons();
                getFileOperationController().checkCheckBoxes();
                messageDialog.showMessageDialog(actionName +" is completed."+ warnMess);
            }

            private void trackProgress(FileActionConcurrently fileAction, Map<Integer, String> selectedRows) {
                int total = selectedRows.size();

                while (fileAction.isWorking()) {
                    int proceeded = fileAction.getCountProceededFiles();
                    publish((int) ((proceeded / (double) total) * 100));

                    /*selectedRows.entrySet().stream().parallel()
                                .forEach(entry -> {
                                    if (fileAction.reducePassedList(entry.getValue())) {
                                        markFileAsPassed(entry.getKey(), actionName);
                                    }
                                });*/
                    List<String> list = fileAction.reducePassedList();
                    selectedRows.entrySet().stream()
                            .filter(entry -> list.contains(entry.getValue()))
                            .forEach(entry -> getFileOperationController().markFileAsPassed(entry.getKey(), actionName));
                    selectedRows.entrySet().removeIf(entry -> list.contains(entry.getValue()));
                    statusBarPanel.updateStatusPanel(-list.size());

                    try {
                        Thread.sleep(200); // Prevent CPU overuse
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
    }
}
