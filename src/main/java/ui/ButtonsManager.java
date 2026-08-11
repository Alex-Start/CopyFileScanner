package ui;

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

    public void setEnabled(boolean isSelected) {
        copyButton.setEnabled(isSelected);
        deleteSourceButton.setEnabled(isSelected);
        deleteDestButton.setEnabled(isSelected);
    }

    public JButton getDeleteSourceButton() {
        return deleteSourceButton;
    }

    public JButton getDeleteDestButton() {
        return deleteDestButton;
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
