package ui;

import common.ActionTabWrap;
import controller.FileOperationController;
import model.FileEntry;
import ui.table.FileTableModel;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Map;

import static common.ActionTabWrap.ActionTab.DUPLICATE;
import static ui.CopyFileScannerSwingUI.*;

/**
 * Manage buttons: copy, delete and 'Select All' checkbox
 */
public class FileTableCellEditor implements ITableUpdater {
    private boolean isFinishedSelectAllAction = true;
    private boolean isCheckBoxActionInTable = false;

    private final ActionTabWrap actionTabWrap;
    private final ButtonsManager buttonsManager;
    private final IStatusBarUpdater statusBarPanel;

    // For Copy table
    private JCheckBox selectAllCheckbox; // 'Select All' checkbox
    private JCheckBox isMissingInSource;
    private ITabPanel tabPanel;

    public FileTableCellEditor(ActionTabWrap actionTabWrap, ButtonsManager buttonsManager, IStatusBarUpdater statusBarPanel) {
        this.actionTabWrap = actionTabWrap;
        this.buttonsManager = buttonsManager;
        this.statusBarPanel = statusBarPanel;
    }

    public ButtonsManager getButtonsManager() {
        return buttonsManager;
    }

    public JCheckBox getSelectAllCheckbox() {
        return selectAllCheckbox;
    }

    public void addSelectAllCheckbox(JCheckBox selectAllCheckbox) {
        this.selectAllCheckbox = selectAllCheckbox;
    }

    public JCheckBox getMissingInSourceCheckbox() {
        return isMissingInSource;
    }

    public void addMissingInSourceCheckbox(JCheckBox missingInSourceCheckbox) {
        this.isMissingInSource = missingInSourceCheckbox;
    }

    public ITabPanel getTablePanel() {
        return tabPanel;
    }

    public void setTablePanel(ITabPanel tabPanel) {
        this.tabPanel = tabPanel;
    }

    @Override
    public void updateCopyTable(List<FileEntry> data) {
        JTable jTable = tabPanel.getTableFactory().getJTable();
        FileTableModel tableModel = (FileTableModel)jTable.getModel();
        tableModel.addDifferences(data);
        //**Reset sorting to avoid index mismatches**
        TableRowSorter<?> sorter = (TableRowSorter<?>) jTable.getRowSorter();
        sorter.setSortKeys(null); // **Reset sorting to natural order**
    }

    @Override
    public void updateDuplicateTable(List<FileEntry> data) {
        JTable jTable = tabPanel.getTableFactory().getJTable();
        FileTableModel tableModel = (FileTableModel)jTable.getModel();
        tableModel.addDuplicates(data);
        //**Reset sorting to avoid index mismatches**
        TableRowSorter<?> sorter = (TableRowSorter<?>) jTable.getRowSorter();
        sorter.setSortKeys(null); // **Reset sorting to natural order**
    }

    /**
     * Delete checkboxes for copied/deleted
     * @param rowIndex
     * @param status - COPIED/DELETED values for the comment
     */
    @Override
    public void markTableRowProcessed(ActionTabWrap.ActionTab tab, int rowIndex, String status) {
        //TODO tab is extra param
        SwingUtilities.invokeLater(() -> {
            JTable table = tabPanel.getJTable();
            FileTableModel tableModel = (FileTableModel) table.getModel();
            if (rowIndex < tableModel.getRowCount()) {
                tableModel.setValueAt(null, rowIndex, CHECKBOX_INDEX_COLUMN); // Remove checkbox
                String newValue = status;
                if (actionTabWrap.isEqual(DUPLICATE)) {
                    newValue = tableModel.getValueAt(rowIndex, COMMENT_INDEX_COLUMN) + " - "+ newValue;
                }
                tableModel.setValueAt(newValue, rowIndex, COMMENT_INDEX_COLUMN); // Mark as passed
                checkCheckBoxesAndEnableButtons(); // Update 'Select All' and 'Copy/Delete' button state
                table.repaint(); // Refresh table display
            }
        });
    }

    @Override
    public Map<Integer, String> getSelectedRows() {
        return getTablePanel().getTableFactory().getTableModel().getSelectedRows();
    }

    @Override
    public void toggleAllCheckboxes(ActionTabWrap.ActionTab tab, boolean selected) {
        //TODO
    }

    public void disableButtonsAndClearTable() {
        FileTableModel tableModel = tabPanel.getTableFactory().getTableModel();
        tableModel.setRowCount(0); // **Clear old data**
        if (actionTabWrap.isEqual(ActionTabWrap.ActionTab.COPY)) {
            selectAllCheckbox.setSelected(false);
        }
        tabPanel.getTableFactory().getTableModel().getListPaths().clear();// **Clear old file paths**

        disableButtonsCleanUpProgressBar();
        statusBarPanel.updateTotalRows(0);
    }

    public void disableButtonsCleanUpProgressBar() {
        disablePanelAndButtons();
        statusBarPanel.cleanupProgressBar();
        statusBarPanel.cleanupStartTime();
    }

    public void enablePanelAndButtons() {
        tabPanel.enablePanelAndButtons(true);
    }

    public void enablePanelAndButtons(boolean enableSelectAllCheckbox) {
//        tabbedPane.setEnabled(true);
        tabPanel.enablePanelAndButtons(enableSelectAllCheckbox);
        checkCheckBoxesAndEnableButtons();
        statusBarPanel.updateProgressBar(100);
    }

    public void disablePanelAndButtons() {
//        tabbedPane.setEnabled(false);
        tabPanel.disablePanelAndButtons();
        getButtonsManager().disablePanelAndButtons();
    }

    public boolean isFinishedSelectAllAction() {
        return isFinishedSelectAllAction;
    }

    public boolean isCheckBoxActionInTable() {
        return isCheckBoxActionInTable;
    }

    public void setCheckBoxActionInTable(boolean checkBoxActionInTable) {
        isCheckBoxActionInTable = checkBoxActionInTable;
    }

    public void selectAllCheckBoxes(ItemEvent e) {
        //related only for Copy action
        if (!actionTabWrap.isEqual(ActionTabWrap.ActionTab.COPY)) {
            return;
        }
        if (isCheckBoxActionInTable) {
            return;
        }
        isFinishedSelectAllAction = false;
        boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
        int count = 0;
        FileTableModel tableModel = FileOperationController.getTableModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.isSkipCheckBoxCondition(i)) {
                continue;
            }
            if (isSelected) {
                count++;
            }
            tableModel.setValueAt(isSelected, i, CHECKBOX_INDEX_COLUMN); // Check/uncheck all rows
        }
        buttonsManager.setEnabled(isSelected);
        isFinishedSelectAllAction = true;
        statusBarPanel.updateSelectedFiles(count);
    }

    public void checkCheckBoxesAndEnableButtons() {
        boolean allChecked = true;
        boolean anyChecked = false;
        int count = 0;
        FileTableModel tableModel = tabPanel.getTableFactory().getTableModel();
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

        if (actionTabWrap.isEqual(ActionTabWrap.ActionTab.COPY)) {
            selectAllCheckbox.setSelected(allChecked);
        }
        getButtonsManager().updateButtonsForTab(actionTabWrap.getActionName(), anyChecked);
        statusBarPanel.updateSelectedFiles(count);
    }
}
