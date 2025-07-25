package controller;

import common.ActionHelper;
import common.ActionTabWrap;
import file.*;
import model.FileEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.IFileActionProgressCallback;
import service.IScanProgressCallback;
import ui.*;
import ui.table.FileTableModel;
import utils.FileUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import static common.ActionHelper.Action.*;
import static common.ActionTabWrap.ActionTab.DUPLICATE;

public class FileOperationController implements IScanProgressCallback, IFileActionProgressCallback {
    private static final Logger logger = LogManager.getLogger(FileOperationController.class);

    private final JTabbedPane tabbedPane;
    private final IStatusBarUpdater statusBarUpdater;
    private final ActionTabWrap actionTab;
    private final MessageDialog messageDialog;

    private static FileOperationController fileOperationController;

    private static final Map<ActionTabWrap.ActionTab, ITabPanel> tabPanel = new HashMap<>();


    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private final AtomicBoolean actionInProgress = new AtomicBoolean(false);

    // Constants for comments
    private static final String COPIED_STATUS = ActionTabWrap.ActionTab.COPY.getValue();
    private static final String DELETED_STATUS_SUFFIX = " Deleted";

//    private final ITableUpdater tableUpdater;
    private final Preferences preferences;
//    private final MessageDialog messageDialog;
//    private final SettingsManager settingsManager;

//    private CopyFileScanner copyFileScanner;
//    private DuplicateFileScanner duplicateFileScanner;
    private FileActionConcurrently fileActionConcurrently;

    /*public FileOperationController(IStatusBarUpdater statusBarUpdater, ITableUpdater tableUpdater,
                                   Preferences preferences, MessageDialog messageDialog,
                                   ActionTabWrap actionTab, SettingsManager settingsManager) {
        this.statusBarUpdater = Objects.requireNonNull(statusBarUpdater);
//        this.tableUpdater = Objects.requireNonNull(tableUpdater);
        this.preferences = Objects.requireNonNull(preferences);
        this.messageDialog = Objects.requireNonNull(messageDialog);
        this.actionTab = Objects.requireNonNull(actionTab);
//        this.settingsManager = Objects.requireNonNull(settingsManager);

        // Inject callbacks into scanners/action classes
        this.copyFileScanner = new CopyFileScanner(this);
        this.duplicateFileScanner = new DuplicateFileScanner(this);
        this.fileActionConcurrently = new FileActionConcurrently(this);
    }*/

    // --- UI Action Handlers ---
    public void handleScanDifferences(String sourceDir, String destDir, boolean checkSource) {
        if (scanInProgress.get() || actionInProgress.get()) {
            messageDialog.showMessageDialog("A scan or file action is already in progress.");
            return;
        }
        if (!FileUtils.checkDirPath(sourceDir) || !FileUtils.checkDirPath(destDir)) {
            messageDialog.showMessageDialog("Please provide valid source and destination directories.");
            return;
        }

        //actionTab.setActionName(ActionTabWrap.ActionTab.COPY);
        statusBarUpdater.cleanupProgressBar();
        tabbedPane.setEnabled(false);
        getTableUpdater().disableButtonsAndClearTable();
        scanInProgress.set(true);

        // Run scanning in a background thread via SwingWorker
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                new CopyFileScanner(FileOperationController.this).scanAndCompare(sourceDir, destDir, checkSource, ActionTabWrap.ActionTab.COPY);
                return null;
            }

            @Override
            protected void done() {
                tabbedPane.setEnabled(true);
                scanInProgress.set(false);
            }
        };
        worker.execute();
    }

    public void handleScanDuplicates(String sourceDir) {
        if (scanInProgress.get() || actionInProgress.get()) {
            messageDialog.showMessageDialog("A scan or file action is already in progress.");
            return;
        }
        if (!FileUtils.checkDirPath(sourceDir)) {
            messageDialog.showMessageDialog("Please provide a valid source directory.");
            return;
        }

        //actionTab.setActionName(DUPLICATE);
        statusBarUpdater.cleanupProgressBar();
        tabbedPane.setEnabled(false);
        getTableUpdater().disableButtonsAndClearTable();
        statusBarUpdater.startStartTime();
        scanInProgress.set(true);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                new DuplicateFileScanner(FileOperationController.this).findDuplicateFiles(sourceDir, DUPLICATE);
                return null;
            }

            @Override
            protected void done() {
                tabbedPane.setEnabled(true);
                scanInProgress.set(false);
            }
        };
        worker.execute();
    }

    public void handleCopySelectedFiles(String sourceDir, String destDir) {
        performFileAction(COPY, sourceDir, destDir, ActionTabWrap.ActionTab.COPY);
    }

    public void handleDeleteSourceFiles(String sourceDir, ActionTabWrap.ActionTab tab) {
        performFileAction(DELETE_SOURCE, sourceDir, null, tab);
    }

    public void handleDeleteDestFiles(String destDir, ActionTabWrap.ActionTab tab) {
        performFileAction(DELETE_DEST, null, destDir, tab);
    }

    private void performFileAction(ActionHelper.Action actionType, String sourceDir, String destDir, ActionTabWrap.ActionTab tab) {
        if (scanInProgress.get() || actionInProgress.get()) {
            messageDialog.showMessageDialog("A scan or file action is already in progress.");
            return;
        }

        //actionTab.setActionName(tab);
        statusBarUpdater.cleanupProgressBar();
        tabbedPane.setEnabled(false);
        getTableUpdater().disablePanelAndButtons(); // Disable relevant panel and its buttons
        statusBarUpdater.cleanupStartTime();
        actionInProgress.set(true);

        FileTableModel currentTableModel = getActiveTabPanel().getTableFactory().getTableModel();
        fileActionConcurrently = new FileActionConcurrently(actionType, sourceDir, destDir, currentTableModel, tab, this);
        fileActionConcurrently.performFileAction();
    }

    public void handleSelectAllToggle(ActionTabWrap.ActionTab tab, boolean selected) {
        getTableUpdater().toggleAllCheckboxes(tab, selected);
        updateStatusBarCounts(tab); // Refresh counts after toggling
    }

    public void handleTableCheckboxChange(ActionTabWrap.ActionTab tab) {
        updateStatusBarCounts(tab);
    }

    public void handleTabChange(/*int selectedTabIndex*/) {
        //actionTab.setActionName(selectedTabIndex == 0 ? ActionTabWrap.ActionTab.COPY : ActionTabWrap.ActionTab.DUPLICATE);//TODO
        getActiveTabPanel().getFileTableCellEditor().checkCheckBoxesAndEnableButtons();
        updateStatusBarCounts(actionTab.getActionName());
        statusBarUpdater.cleanupProgressBar(); // Clear progress bar on tab change
    }

    public void saveApplicationState() {
        preferences.put("selectedTab", String.valueOf(actionTab.getActionName().ordinal()));
        // You can save source/dest paths here from the UI panels
        // Example: preferences.put("sourceDirCopy", findCopyPanel.getSourceDir());
        // For table data, you'd need a SerializationService which interacts with FileTableModel
        //TODO
    }

    public void loadApplicationState() {
        // Load initial selected tab
        int selectedTab = Integer.parseInt(preferences.get("selectedTab", "0"));
        //actionTab.setActionName(selectedTab == 0 ? ActionTabWrap.ActionTab.COPY : ActionTabWrap.ActionTab.DUPLICATE);
        // Load paths into UI panels
        // Example: findCopyPanel.setSourceDir(preferences.get("sourceDirCopy", ""));
        // Load table data
        //TODO
    }


    // --- Internal Helper Methods ---

    private void updateStatusBarCounts(ActionTabWrap.ActionTab tab) {
        FileTableModel currentTableModel = getActiveTabPanel().getTableFactory().getTableModel();
        int totalRows = currentTableModel.getRowCount();
        int selectedRows = currentTableModel.getSelectedRows().size(); // Get actual selected rows from model

        statusBarUpdater.updateTotalRows(totalRows);
        statusBarUpdater.updateSelectedFiles(selectedRows);
        statusBarUpdater.updateDuration();
        //statusBarPanel.updateProgressBar(...);
        //statusBarPanel.refreshStatusBar();
    }

    // --- IScanProgressCallback Implementation (Called by CopyFileScanner/DuplicateFileScanner) ---

    @Override
    public void onScanStarted(String message, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            statusBarUpdater.startStartTime();
            statusBarUpdater.updateMessage(message);
            statusBarUpdater.cleanupProgressBar();
        });
    }

    private long lastUpdate = 0;

    @Override
    public void onScanProgress(int percentage, ActionTabWrap.ActionTab tab) {
        long now = System.currentTimeMillis();
        if (percentage == 100 || now - lastUpdate > 100) { // update at most every 100 ms or for 100%
            lastUpdate = now;

            SwingUtilities.invokeLater(() -> {
                statusBarUpdater.updateProgressBar(percentage);
                statusBarUpdater.updateMessage("Scanning... " + percentage + "% Time: " + statusBarUpdater.getDurationString());
            });
        }
    }

    @Override
    public void onScanCompletedCopy(Map<String, String> differences, ActionTabWrap.ActionTab tab, String message) {
        SwingUtilities.invokeLater(() -> {
            getTableUpdater().updateCopyTable(convertDifferencesToEntries(differences, getSourceDir(tab), getDestDir(tab)));
            updateStatusBarCounts(tab);
            statusBarUpdater.updateDuration();
            statusBarUpdater.updateMessage(message);
            //statusBarUpdater.updateProgressBar(100);
            getTableUpdater().enablePanelAndButtons();
            tabbedPane.setEnabled(true);
            scanInProgress.set(false);
        });
    }

    @Override
    public void onScanCompletedDuplicates(Map<String, List<String>> duplicates, ActionTabWrap.ActionTab tab, String message) {
        SwingUtilities.invokeLater(() -> {
            getTableUpdater().updateDuplicateTable(convertDuplicatesToEntries(duplicates));
            updateStatusBarCounts(tab);
            statusBarUpdater.updateDuration();
            statusBarUpdater.updateMessage(message);
            //statusBarUpdater.updateProgressBar(100);
            getTableUpdater().enablePanelAndButtons();
            tabbedPane.setEnabled(true);
            scanInProgress.set(false);
        });
    }

    @Override
    public void onScanError(String errorMessage, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            messageDialog.showMessageDialog("Scan Error: " + errorMessage);
            statusBarUpdater.updateMessage("Scan Failed: " + errorMessage);
            //statusBarUpdater.cleanupProgressBar(); // Reset progress
            getTableUpdater().enablePanelAndButtons();
            tabbedPane.setEnabled(true);
            scanInProgress.set(false);
        });
    }

    // --- IFileActionProgressCallback Implementation (Called by FileActionConcurrently) ---

    @Override
    public void onActionStarted(String message, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            statusBarUpdater.updateMessage(message);
            statusBarUpdater.updateProgressBar(0);
        });
    }

    @Override
    public void onActionProgress(int percentage, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            statusBarUpdater.updateProgressBar(percentage);
            statusBarUpdater.updateMessage("Processing files... " + percentage + "%");
        });
    }

    @Override
    public void onFileProcessed(int rowIndex, String status, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            getTableUpdater().markTableRowProcessed(tab, rowIndex, status);
            updateStatusBarCounts(tab); // Update counts as items are processed/unselected
        });
    }

    @Override
    public void onActionCompleted(String message, String warning, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            statusBarUpdater.updateDuration();
            statusBarUpdater.updateMessage(message);
            //statusBarUpdater.updateProgressBar(100);
            getTableUpdater().enablePanelAndButtons();
            tabbedPane.setEnabled(true);
            actionInProgress.set(false);
            if (warning != null && !warning.isEmpty()) {
                messageDialog.showMessageDialog(warning);
            }
        });
    }

    @Override
    public void onActionError(String errorMessage, ActionTabWrap.ActionTab tab) {
        SwingUtilities.invokeLater(() -> {
            messageDialog.showMessageDialog("Action Error: " + errorMessage);
            statusBarUpdater.updateMessage("Action Failed: " + errorMessage);
            statusBarUpdater.updateProgressBar(0); // Reset progress
            getTableUpdater().enablePanelAndButtons();
            tabbedPane.setEnabled(true);
            actionInProgress.set(false);
        });
    }

    // --- Conversion Methods (Can be moved to a data converter utility if more complex) ---

    private List<FileEntry> convertDifferencesToEntries(Map<String, String> differences, String sourceDir, String destDir) {
        List<FileEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : differences.entrySet()) {
            // Assuming entry.getKey() is relative path, entry.getValue() is destination path
            Path path = Path.of(entry.getKey());
            // sourceDir = folder
            String folder = path.getParent() == null ? "" : path.getParent().toString();

            entries.add(new FileEntry(false, folder, path.getFileName().toString(), entry.getValue()));
        }
        return entries;
    }

    private List<FileEntry> convertDuplicatesToEntries(Map<String, List<String>> duplicates) {
        List<FileEntry> entries = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
            i++;
            for (String destPath : entry.getValue()) {
                Path path = Path.of(destPath);
                String folder = path.getParent() == null ? "" : path.getParent().toString();
                entries.add(new FileEntry(false, folder, path.getFileName().toString(), "# "+ i));
            }
        }
        return entries;
    }

    // --- Placeholder Methods for getting current directory paths from UI ---
    // In a full implementation, these would be passed from the UI panels or managed by the controller
    // if the controller directly holds references to the text fields.
    // For this example, these are just placeholders.
    private String getSourceDir(ActionTabWrap.ActionTab tab) {
        // This should be retrieved from the respective UI panel (FindCopyPanel/FindDuplicatePanel)
        // For now, return a placeholder or previously saved preference.
        if (tab == ActionTabWrap.ActionTab.COPY) {
            return preferences.get("sourceDirCopy", "").trim();
        } else if (tab == DUPLICATE) {
            return preferences.get("sourceDirDupl", "").trim();
        }
        return "";
    }

    private String getDestDir(ActionTabWrap.ActionTab tab) {
        // This should be retrieved from the FindCopyPanel
        if (tab == ActionTabWrap.ActionTab.COPY) {
            return preferences.get("destDirCopy", "").trim();
        }
        return "";
    }


//======================== my version =========================== TODO refactor with new above one
    public static FileOperationController getFileOperationController(JTabbedPane tabbedPane, ButtonsManager buttonsManager, IStatusBarUpdater statusBarPanel, ActionTabWrap actionTabWrap, CopyFileScannerSwingUI copyFileScannerSwingUI) {
        if (fileOperationController == null) {
//            FileTableCellEditor fileTableCellEditor = new FileTableCellEditor(actionTabWrap, buttonsManager, statusBarPanel);
            Preferences prefs = Preferences.userNodeForPackage(CopyFileScannerSwingUI.class);
            MessageDialog messageDialog = new MessageDialog(copyFileScannerSwingUI);
            fileOperationController = new FileOperationController(tabbedPane, statusBarPanel, actionTabWrap, messageDialog, prefs);
            buttonsManager.addActionListener(fileOperationController);

            // Create tabs
            tabPanel.put(ActionTabWrap.ActionTab.COPY, new FindCopyPanel(prefs, buttonsManager, statusBarPanel, fileOperationController));
            tabPanel.put(DUPLICATE, new FindDuplicatePanel(prefs, buttonsManager, statusBarPanel, fileOperationController));
        }

        return fileOperationController;
    }

    public FileOperationController(JTabbedPane tabbedPane, IStatusBarUpdater statusBarUpdater, ActionTabWrap actionTabWrap, MessageDialog messageDialog, Preferences preferences) {
        this.tabbedPane = tabbedPane;
        this.statusBarUpdater = statusBarUpdater;
        actionTab = actionTabWrap;
        this.messageDialog = messageDialog;
        this.preferences = preferences;
    }

    public ITableUpdater getTableUpdater() {
        return this.getActiveTabPanel().getTableUpdater();
    }

    public void toggleComponents() {
        getActiveTabPanel().getFileTableCellEditor().checkCheckBoxesAndEnableButtons();
        ((StatusBarPanel)statusBarUpdater).refreshStatusBar();
    }

    public static JTable getTable() {
        return fileOperationController.getActiveTabPanel().getJTable();
    }

    public static FileTableModel getTableModel() {
        return (FileTableModel) getTable().getModel();
    }

    public void scanDifferences(ActionEvent e) {
        String sourceDir = fileOperationController.getCopyPanel().getSourceField().getText().trim();
        String destDir = fileOperationController.getCopyPanel().getDestField().getText().trim();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            messageDialog.showMessageDialog("Please select both directories.");
            return;
        }

        handleScanDifferences(sourceDir, destDir, ((FindCopyPanel) fileOperationController.getActiveTabPanel()).isSelectedCheckSource());
    }

    public void scanDuplicates(ActionEvent e) {
        String sourceDir = fileOperationController.getDuplicatePanel().getSourceField().getText().trim();
        if (sourceDir.isEmpty()) {
            messageDialog.showMessageDialog("Please select source directory.");
            return;
        }

        handleScanDuplicates(sourceDir);
    }

    public void copyFiles(ActionEvent e) {
        String sourceDir = fileOperationController.getCopyPanel().getSourceField().getText();
        String destDir = fileOperationController.getCopyPanel().getDestField().getText();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            messageDialog.showMessageDialog("Please select both directories.");
            return;
        }

//        Map<Integer, String> selectedRows  = getTableModel().getSelectedRows();
//        if (selectedRows.isEmpty()) {
//            messageDialog.showMessageDialog("No files selected for copying.");
//            return;
//        }

//        tabbedPane.setEnabled(false);
//        fileOperationController.getActiveTabPanel().getFileTableCellEditor().disableButtonsCleanUpProgressBar();

//        new FileActionService(statusBarUpdater, messageDialog)
//                .executeFileCopier(sourceDir, destDir, selectedRows);
        handleCopySelectedFiles(sourceDir, destDir);
    }

    public void deleteSourceFiles(ActionEvent e) {

//        deleteFiles(FILE_INDEX_COLUMN);
        if (ActionTabWrap.ActionTab.COPY.equals(getActionTabHelper().getActionName())) {
            String sourceDir = fileOperationController.getActiveTabPanel().getSourceFieldText();
            if (sourceDir.isEmpty()) {
                messageDialog.showMessageDialog("Please select Source directories.");
                return;
            }
            handleDeleteSourceFiles(sourceDir, ActionTabWrap.ActionTab.COPY);
        } else {
            // For duplicates, the sourceDir passed to fileActionConcurrently is the root folder.
            // The actual paths to delete are in the table data's 'folder' and 'filename' fields.
            handleDeleteSourceFiles(null, DUPLICATE);
        }
    }

    public void deleteDestFiles(ActionEvent e) {

//        deleteFiles(COMMENT_INDEX_COLUMN);
        if (ActionTabWrap.ActionTab.COPY.equals(getActionTabHelper().getActionName())) {
            String destDir = fileOperationController.getActiveTabPanel().getDestFieldText();
            if (destDir.isEmpty()) {
                messageDialog.showMessageDialog("Please select both directories.");
                return;
            }
            handleDeleteDestFiles(destDir, ActionTabWrap.ActionTab.COPY);
        } else {
            handleDeleteDestFiles(null, DUPLICATE);
        }
    }

//    private void deleteFiles(int columnIndex) {
//        Map<Integer, String> selectedRows  = getTableModel().getSelectedRows();
//        if (selectedRows.isEmpty()) {
//            messageDialog.showMessageDialog("No files selected for copying.");
//            return;
//        }
//
//        tabbedPane.setEnabled(false);
//        fileOperationController.getActiveTabPanel().getFileTableCellEditor().disableButtonsCleanUpProgressBar();
//
//        new FileActionService(statusBarUpdater, messageDialog)
//                .executeFileDeleter(columnIndex, selectedRows);
//    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public ITabPanel getActiveTabPanel() {
        return tabPanel.get(getActionTabHelper().getActionName());
    }

    public ActionTabWrap getActionTabHelper() {
        return actionTab;
    }

    public FindCopyPanel getCopyPanel() {
        return ((FindCopyPanel)tabPanel.get(ActionTabWrap.ActionTab.COPY));
    }

    public FindDuplicatePanel getDuplicatePanel() {
        return ((FindDuplicatePanel)tabPanel.get(DUPLICATE));
    }

}
