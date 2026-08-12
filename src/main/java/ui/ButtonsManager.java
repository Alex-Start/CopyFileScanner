package ui;

import common.ActionTabWrap;
import controller.FileOperationController;
import javax.swing.*;

import static ui.CopyFileScannerSwingUI.*;

public class ButtonsManager {
    private final JButton copyButton, deleteSourceButton, deleteDestButton;
    private ActionTabWrap.ActionTab currentTab = ActionTabWrap.ActionTab.COPY;

    public ButtonsManager() {
        // 2 actions for 1 button depends on active tab
        copyButton = new JButton(COPY_SELECTED);
        copyButton.setEnabled(false);
        deleteSourceButton = new JButton(DELETE_SOURCE);
        deleteSourceButton.setEnabled(false);
        deleteDestButton = new JButton(DELETE_DESTINATION);
        deleteDestButton.setEnabled(false);
        updateButtonsForTab(ActionTabWrap.ActionTab.COPY, false);
    }

    public void addActionListener(FileOperationController fileOperationController) {
        copyButton.addActionListener(fileOperationController::copyFiles);
        deleteSourceButton.addActionListener(fileOperationController::deleteSourceFiles);
        deleteDestButton.addActionListener(fileOperationController::deleteDestFiles);
    }

    public JButton getCopyButton() {
        return copyButton;
    }

    public JButton getDeleteSourceButton() {
        return deleteSourceButton;
    }

    public JButton getDeleteDestButton() {
        return deleteDestButton;
    }

    public void updateButtonsForTab(ActionTabWrap.ActionTab tab, boolean anyChecked) {
        this.currentTab = tab;
        if (ActionTabWrap.ActionTab.COPY.equals(tab)) {
            copyButton.setVisible(true);
            copyButton.setEnabled(anyChecked);
            deleteSourceButton.setText(DELETE_SOURCE);
            deleteSourceButton.setVisible(true);
            deleteSourceButton.setEnabled(anyChecked);
            deleteDestButton.setText(DELETE_DESTINATION);
            deleteDestButton.setVisible(true);
            deleteDestButton.setEnabled(anyChecked);
        } else {
            copyButton.setVisible(false);
            copyButton.setEnabled(false);
            deleteSourceButton.setText(DELETE_SELECTED);
            deleteSourceButton.setVisible(true);
            deleteSourceButton.setEnabled(anyChecked);
            deleteDestButton.setVisible(false);
            deleteDestButton.setEnabled(false);
        }
    }

    public void setEnabled(boolean isSelected) {
        if (ActionTabWrap.ActionTab.COPY.equals(currentTab)) {
            copyButton.setEnabled(isSelected);
            deleteSourceButton.setEnabled(isSelected);
            deleteDestButton.setEnabled(isSelected);
        } else {
            copyButton.setEnabled(false);
            deleteSourceButton.setEnabled(isSelected);
            deleteDestButton.setEnabled(false);
        }
    }

    public void disablePanelAndButtons() {
        updateButtonsForTab(currentTab, false);
        copyButton.repaint();
        deleteSourceButton.repaint();
        deleteDestButton.repaint();
    }
}
