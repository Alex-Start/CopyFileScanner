package ui;

import file.DuplicateFileScanner;
import utils.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static ui.CopyFileScannerSwingUI.SOURCE_DIR_DUPL;

public interface ITabPanel {
    JTable getJTable();
    String getRootPath(int columnIndex);
    JPanel createPanel();
    Preferences getPrefs();
    void setEnabledButton();
    void enablePanelAndButtons(boolean enableSelectAllCheckbox);
    void disablePanelAndButtons();
    void setSelectAllCheckbox(boolean value);
    TableFactory getTableFactory();
    FileTableCellEditor getFileTableCellEditor();
    ITableUpdater getTableUpdater();
    String getSourceFieldText();
    String getDestFieldText();

    default void selectFolder(Component rootPanel, JTextField field, String key, boolean multiple) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(multiple); // Allow multiple selection

        int option = chooser.showOpenDialog(rootPanel);
        if (option == JFileChooser.APPROVE_OPTION) {
            String selectedPath = "";
            if (multiple) {
                File[] selectedFolders = chooser.getSelectedFiles();
                List<String> folderPaths = new ArrayList<>();

                for (File folder : selectedFolders) {
                    folderPaths.add(folder.getAbsolutePath());
                }

                selectedPath = String.join(DuplicateFileScanner.DELIM, folderPaths);
            } else {
                selectedPath = chooser.getSelectedFile().getAbsolutePath();
            }
            field.setText(selectedPath);
            getPrefs().put(key, selectedPath); // Save choice
        }
        setEnabledButton();
    }

    default void addTextFieldListener(Component component, JTextField textField, String key) {
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                checkPathAndShowError(component, textField.getText(), key);
                getPrefs().put(key, textField.getText());
            }
        });

        textField.addActionListener(e -> {
            // Triggered when Enter is pressed
            checkPathAndShowError(component, textField.getText(), key);
            getPrefs().put(key, textField.getText());
        });
    }

    private void checkPathAndShowError(Component component, String path, String key) {
        String[] paths = new String[]{path};
        //multiple folders only for duplicates case
        if(SOURCE_DIR_DUPL.equals(key)) {
            paths = path.split(DuplicateFileScanner.DELIM);
        }
        if(! FileUtils.checkDirPath(paths)) {
            JOptionPane.showMessageDialog(component, "Invalid folder path: "+ path);
        }
    }
}
