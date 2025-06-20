package ui;

import common.ActionTab;
import controller.FileOperationController;
import ui.table.FileTableModel;

import javax.swing.*;
import java.awt.event.ItemEvent;

import static ui.CopyFileScannerSwingUI.*;

/**
 * Manage buttons: copy, delete and 'Select All' checkbox
 */
public class FileTableCellEditor {
    private boolean isFinishedSelectAllAction = true;
    private boolean isCheckBoxActionInTable = false;

    private final ActionTab actionTab;
    private final ButtonsManager buttonsManager;
    private final JButton copyButton;
    private final JButton deleteSourceButton;
    private final JButton deleteDestButton;
    private final StatusBarPanel statusBarPanel;

    public FileTableCellEditor(ActionTab actionTab, ButtonsManager buttonsManager, StatusBarPanel statusBarPanel) {
        this.actionTab = actionTab;
        this.buttonsManager = buttonsManager;
        this.copyButton = buttonsManager.getCopyButton();
        this.deleteSourceButton = buttonsManager.getDeleteSourceButton();
        this.deleteDestButton = buttonsManager.getDeleteDestButton();
        this.statusBarPanel = statusBarPanel;
    }

    public ButtonsManager getButtonsManager() {
        return buttonsManager;
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
        if (!actionTab.isEqual(ActionTab.Tab.COPY)) {
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
        copyButton.setEnabled(isSelected);
        deleteSourceButton.setEnabled(isSelected);
        deleteDestButton.setEnabled(isSelected);
        isFinishedSelectAllAction = true;
        statusBarPanel.setAndRefreshSelect(count);
    }
}
