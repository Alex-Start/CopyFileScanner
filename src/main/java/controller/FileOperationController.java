package controller;

import common.ActionTab;
import file.CopyFileScanner;
import file.DuplicateFileScanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.FileActionService;
import ui.*;
import ui.table.FileTableModel;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static ui.CopyFileScannerSwingUI.*;

public class FileOperationController {
    private static final Logger logger = LogManager.getLogger(FileOperationController.class);

    private final JTabbedPane tabbedPane;
    private final FileTableCellEditor fileTableCellEditor;
    private final StatusBarPanel statusBarPanel;
    private final ActionTab ACTION_TAB;
    private final ButtonsManager buttonsManager;
    private final MessageDialog messageDialog;

    private static FileOperationController fileOperationController;

    private static final Map<ActionTab.Tab, TabPanel> tabPanel = new HashMap<>();

    public static FileOperationController getFileOperationController(JTabbedPane tabbedPane, ButtonsManager buttonsManager, StatusBarPanel statusBarPanel, ActionTab actionTab, CopyFileScannerSwingUI copyFileScannerSwingUI) {
        if (fileOperationController == null) {
            FileTableCellEditor fileTableCellEditor = new FileTableCellEditor(actionTab, buttonsManager, statusBarPanel);
            Preferences prefs = Preferences.userNodeForPackage(CopyFileScannerSwingUI.class);
            MessageDialog messageDialog = new MessageDialog(copyFileScannerSwingUI);
            fileOperationController = new FileOperationController(tabbedPane, fileTableCellEditor, statusBarPanel, actionTab, messageDialog);
            buttonsManager.addActionListener(fileOperationController);

            // Create tabs
            tabPanel.put(ActionTab.Tab.COPY, new FindCopyPanel(prefs, fileTableCellEditor, statusBarPanel, fileOperationController));
            tabPanel.put(ActionTab.Tab.DUPLICATE, new FindDuplicatePanel(prefs, fileTableCellEditor, statusBarPanel, fileOperationController));
        }

        return fileOperationController;
    }

    public FileOperationController(JTabbedPane tabbedPane, FileTableCellEditor fileTableCellEditor, StatusBarPanel statusBarPanel, ActionTab actionTab, MessageDialog messageDialog) {
        this.tabbedPane = tabbedPane;
        this.fileTableCellEditor = fileTableCellEditor;
        this.statusBarPanel = statusBarPanel;
        ACTION_TAB = actionTab;
        this.buttonsManager = fileTableCellEditor.getButtonsManager();
        this.messageDialog = messageDialog;
    }

    public static JTable getTable() {
        return fileOperationController.getActiveTabPanel().getJTable();
    }

    public static FileTableModel getTableModel() {
        return (FileTableModel) getTable().getModel();
    }

    public static String getFullFilePath(int columnIndex, int modelRowIndex) {
        String relativePath = getTableModel().getListPaths().get(modelRowIndex);
        if (columnIndex < 0) {
            return relativePath;
        }
        return Path.of(fileOperationController.getActiveTabPanel().getRootPath(columnIndex), relativePath).toString(); // Retrieve full file path
    }

    public void scanDifferences(ActionEvent e) {
        String sourceDir = fileOperationController.getCopyPanel().getSourceField().getText();
        String destDir = fileOperationController.getCopyPanel().getDestField().getText();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            messageDialog.showMessageDialog("Please select both directories.");
            return;
        }

        // Disable scan button while scanning
        fileOperationController.disableButtonsAndClearTable();

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> statusBarPanel.getMessageLabel().setText("Start scan differences..."));
            AtomicBoolean scanningFinished = new AtomicBoolean(false);
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            AtomicReference<Map<String, String>> differences = new AtomicReference<>();
            executorService.submit(() -> {
                logger.debug("Started scanning...");
                differences.set(CopyFileScanner.scanAndCompare(sourceDir, destDir, ((FindCopyPanel) fileOperationController.getActiveTabPanel()).isSelectedCheckSource()));
                logger.debug("Scan is finished.");
                scanningFinished.set(true);
            });
            executorService.submit(() -> statusBarPanel.upDownProgressBar(scanningFinished));
            executorService.submit(() -> {
                while(!scanningFinished.get()) {
                    statusBarPanel.updateDuration();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            executorService.shutdown();

            // Wait until scanningFinished becomes true
            while (!scanningFinished.get()) {
                try {
                    Thread.sleep(500); // or shorter if needed
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for scanning to finish.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SwingUtilities.invokeLater(() -> {
                logger.debug("Add {} differences in table...", differences.get().size());
                statusBarPanel.getMessageLabel().setText("Add "+ differences.get().size() +" differences in table...");
                statusBarPanel.getProgressBar().setValue(0);
                JTable fileTableCopy = fileOperationController.getCopyPanel().getJTable();
                FileTableModel tableModel = (FileTableModel)fileTableCopy.getModel();
                tableModel.addDifferences(differences.get());

                statusBarPanel.getProgressBar().setValue((int) ((tableModel.getRowCount() / (double) differences.get().size()) * 100));

                // **Reset sorting to avoid index mismatches**
                TableRowSorter<?> sorter = (TableRowSorter<?>) fileTableCopy.getRowSorter();
                sorter.setSortKeys(null); // **Reset sorting to natural order**

                statusBarPanel.getMessageLabel().setText("Added "+ differences.get().size() +" differences in table (duration: "+ statusBarPanel.getDurationString() +")");

                // **Re-enable buttons after scan**
                fileOperationController.enablePanelAndButtons(!tableModel.getListPaths().isEmpty());
                statusBarPanel.updateStatusPanel(tableModel.getListPaths().size());
            });
            statusBarPanel.setAndRefreshProgressBar(100);
        }).start();

        SwingUtilities.invokeLater(() -> statusBarPanel.getMessageLabel().setText("End scan differences..."));
    }

    public void scanDuplicates(ActionEvent e) {
        String sourceDir = fileOperationController.getDuplicatePanel().getSourceField().getText();
        if (sourceDir.isEmpty()) {
            messageDialog.showMessageDialog("Please select source directory.");
            return;
        }

        // Disable scan button while scanning
        fileOperationController.disableButtonsAndClearTable();

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> statusBarPanel.getMessageLabel().setText("Start scan duplicates..."));
            AtomicBoolean scanningFinished = new AtomicBoolean(false);
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            AtomicReference<Map<String, List<String>>> duplicates = new AtomicReference<>();
            executorService.submit(() -> {
//                long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
//                logger.debug("Used memory: {} MB", usedMem);
                logger.debug("Started scanning...");
                duplicates.set(DuplicateFileScanner.findDuplicateFiles(sourceDir));
                logger.debug("Scan is finished.");
//                usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
//                logger.debug("Used memory: {} MB", usedMem);
                scanningFinished.set(true);
            });
            executorService.submit(() -> statusBarPanel.upDownProgressBar(scanningFinished));
            executorService.submit(() -> {
                while(!scanningFinished.get()) {
                    statusBarPanel.updateDuration();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            executorService.shutdown();

            // Wait until scanningFinished becomes true
            while (!scanningFinished.get()) {
                try {
                    Thread.sleep(500); // or shorter if needed
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for scanning to finish.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SwingUtilities.invokeLater(() -> {
                logger.debug("Add {} duplicates in table...", duplicates.get().size());
                statusBarPanel.getMessageLabel().setText("Add "+ duplicates.get().size() +" duplicates in table...");
                statusBarPanel.getProgressBar().setValue(0);
                JTable fileTableDupl = fileOperationController.getCopyPanel().getJTable();
                FileTableModel tableModel = (FileTableModel) fileTableDupl.getModel();
                tableModel.addDuplicates(duplicates.get());

                statusBarPanel.getProgressBar().setValue((int) ((tableModel.getRowCount() / (double) duplicates.get().size()) * 100));

                // **Reset sorting to avoid index mismatches**
                TableRowSorter<?> sorter = (TableRowSorter<?>) fileTableDupl.getRowSorter();
                sorter.setSortKeys(null); // **Reset sorting to natural order**

                statusBarPanel.getMessageLabel().setText("Added "+ duplicates.get().size() +" duplicates in table (duration: "+ statusBarPanel.getDurationString()+")");

                // **Re-enable buttons after scan**
                fileOperationController.enablePanelAndButtons(false);
                statusBarPanel.updateStatusPanel(((FileTableModel)fileTableDupl.getModel()).getListPaths().size());

            });
            statusBarPanel.setAndRefreshProgressBar(100);
        }).start();

        SwingUtilities.invokeLater(() -> statusBarPanel.getMessageLabel().setText("End scan duplicates..."));
    }

    public void copyFiles(ActionEvent e) {
        String sourceDir = fileOperationController.getCopyPanel().getSourceField().getText();
        String destDir = fileOperationController.getCopyPanel().getDestField().getText();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            messageDialog.showMessageDialog("Please select both directories.");
            return;
        }

        Map<Integer, String> selectedRows  = fileOperationController.getSelectedRows();
        if (selectedRows.isEmpty()) {
            messageDialog.showMessageDialog("No files selected for copying.");
            return;
        }

        fileOperationController.disableButtonsCleanUpProgressBar();

        new FileActionService(statusBarPanel, messageDialog)
                .executeFileCopier(sourceDir, destDir, selectedRows);
    }

    public void deleteSourceFiles(ActionEvent e) {
        deleteFiles(FILE_INDEX_COLUMN);
    }

    public void deleteDestFiles(ActionEvent e) {
        deleteFiles(COMMENT_INDEX_COLUMN);
    }

    private void deleteFiles(int columnIndex) {
        Map<Integer, String> selectedRows  = fileOperationController.getSelectedRows(columnIndex);
        if (selectedRows.isEmpty()) {
            messageDialog.showMessageDialog("No files selected for deleting.");
            return;
        }

        fileOperationController.disableButtonsCleanUpProgressBar();

        new FileActionService(statusBarPanel, messageDialog)
                .executeFileDeleter(columnIndex, selectedRows);
    }

    public void disableButtonsAndClearTable() {
        FileTableModel tableModel = getTableModel();
        tableModel.setRowCount(0); // **Clear old data**
        if (fileOperationController.getActionTabHelper().isEqual(ActionTab.Tab.COPY)) {
            ((FindCopyPanel) fileOperationController.getActiveTabPanel()).setSelectAllCheckbox(false);
        }
        getTableModel().getListPaths().clear();// **Clear old file paths**

        disableButtonsCleanUpProgressBar();
        statusBarPanel.updateTotalLabel(0);
    }

    public void disableButtonsCleanUpProgressBar() {
        disablePanelAndButtons();
        statusBarPanel.setAndRefreshProgressBar(0);
        statusBarPanel.cleanupStartTime();
    }

    public void enablePanelAndButtons() {
        enablePanelAndButtons(true);
    }

    public void enablePanelAndButtons(boolean enableSelectAllCheckbox) {
        tabbedPane.setEnabled(true);
        fileOperationController.getActiveTabPanel().enablePanelAndButtons(enableSelectAllCheckbox);
        checkCheckBoxes();
        statusBarPanel.setAndRefreshProgressBar(100);
    }

    private void disablePanelAndButtons() {
        tabbedPane.setEnabled(false);
        fileOperationController.getActiveTabPanel().disablePanelAndButtons();
        buttonsManager.disablePanelAndButtons();
    }

    public TabPanel getActiveTabPanel() {
        return tabPanel.get(getActionTabHelper().getActionName());
    }

    public ActionTab getActionTabHelper() {
        return ACTION_TAB;
    }

    public FindCopyPanel getCopyPanel() {
        return ((FindCopyPanel)tabPanel.get(ActionTab.Tab.COPY));
    }

    public FindDuplicatePanel getDuplicatePanel() {
        return ((FindDuplicatePanel)tabPanel.get(ActionTab.Tab.DUPLICATE));
    }

    public void checkCheckBoxes() {
        boolean allChecked = true;
        boolean anyChecked = false;
        int count = 0;
        FileTableModel tableModel = getTableModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.isSkipCheckBoxCondition(i)) {
                continue;
            }
            Object checkBoxValue = tableModel.getValueAt(i, CHECKBOX_INDEX_COLUMN);
            if (checkBoxValue == null) continue;
            boolean checked = (Boolean) checkBoxValue;
            if (checked) {
                count++;
            }
            anyChecked |= checked;
            allChecked &= checked;
        }

        if (ACTION_TAB.isEqual(ActionTab.Tab.COPY)) {
            fileTableCellEditor.getButtonsManager().getCopyButton().setEnabled(anyChecked);
            fileTableCellEditor.getButtonsManager().getDeleteDestButton().setEnabled(anyChecked);
            ((FindCopyPanel) fileOperationController.getActiveTabPanel()).setSelectAllCheckbox(allChecked);
        } else {
            fileTableCellEditor.getButtonsManager().getCopyButton().setEnabled(false);
            fileTableCellEditor.getButtonsManager().getDeleteDestButton().setEnabled(false);
        }
        fileTableCellEditor.getButtonsManager().getDeleteSourceButton().setEnabled(anyChecked);
        statusBarPanel.setAndRefreshSelect(count);
    }

    /**
     * Delete checkboxes for copied/deleted
     * @param rowIndex
     * @param value - COPIED/DELETED values for the comment
     */
    public void markFileAsPassed(int rowIndex, String value) {
        SwingUtilities.invokeLater(() -> {
            JTable table = getActiveTabPanel().getJTable();
            FileTableModel tableModel = (FileTableModel) table.getModel();
            if (rowIndex < tableModel.getRowCount()) {
                tableModel.setValueAt(null, rowIndex, CHECKBOX_INDEX_COLUMN); // Remove checkbox
                String newValue = value;
                if (fileOperationController.getActionTabHelper().isEqual(ActionTab.Tab.DUPLICATE)) {
                    newValue = tableModel.getValueAt(rowIndex, COMMENT_INDEX_COLUMN) + " - "+ newValue;
                }
                tableModel.setValueAt(newValue, rowIndex, COMMENT_INDEX_COLUMN); // Mark as passed
                checkCheckBoxes(); // Update 'Select All' and 'Copy/Delete' button state
                table.repaint(); // Refresh table display
            }
        });
    }

    /**
     * Get map of selected rows
     * @return - rowIndex, path - short relative path for COPY, and full path for DUPLICATE.
     */
    public Map<Integer, String> getSelectedRows() {
        return getSelectedRows(-1);
    }
    public Map<Integer, String> getSelectedRows(int columnIndex) {
        Map<Integer, String> selectedRows  = new HashMap<>();
        JTable table = getActiveTabPanel().getJTable();
        FileTableModel tableModel = (FileTableModel) table.getModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int modelRowIndex = table.convertRowIndexToModel(i); // Convert to model index
            Object checkBoxValue = tableModel.getValueAt(modelRowIndex, CHECKBOX_INDEX_COLUMN);
            if (checkBoxValue != null && (Boolean) checkBoxValue) {
                selectedRows.put(modelRowIndex, getFullFilePath(columnIndex, modelRowIndex));
            }
        }

        return selectedRows;
    }
}
