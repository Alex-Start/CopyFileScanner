package ui;

import common.ActionTabWrap;
import controller.FileOperationController;
import javax.swing.*;

import static ui.CopyFileScannerSwingUI.*;

public class ButtonsManager {
    private final JButton copyButton, deleteSourceButton, deleteDestButton;

    public ButtonsManager() {
        // 2 actions for 1 button depends on active tab
        copyButton = new JButton(COPY_SELECTED);
        copyButton.setEnabled(false);
        deleteSourceButton = new JButton(DELETE_SOURCE);
        deleteSourceButton.setEnabled(false);
        deleteDestButton = new JButton(DELETE_DESTINATION);
        deleteDestButton.setEnabled(false);
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
        if (ActionTabWrap.ActionTab.COPY.equals(tab)) {
            copyButton.setVisible(true);
            copyButton.setEnabled(anyChecked);
            deleteSourceButton.setText(DELETE_SOURCE);
            deleteSourceButton.setVisible(true);
            deleteSourceButton.setEnabled(anyChecked);
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
        copyButton.setEnabled(isSelected);
        deleteSourceButton.setEnabled(isSelected);
        deleteDestButton.setEnabled(isSelected);
    }

    public void disablePanelAndButtons() {
        copyButton.setEnabled(false);
        copyButton.repaint();
        deleteSourceButton.setEnabled(false);
        deleteSourceButton.repaint();
        deleteDestButton.setEnabled(false);
        deleteDestButton.repaint();
    }
}
