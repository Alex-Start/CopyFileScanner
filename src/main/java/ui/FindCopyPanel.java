package ui;

import controller.FileOperationController;
import ui.table.FileTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static serializable.SerializeManager.loadCopyTableFromFile;
import static ui.CopyFileScannerSwingUI.FILE_INDEX_COLUMN;

public class FindCopyPanel implements TabPanel {
    private JTextField sourceField, destField;
    private JButton sourceButton, destButton;
    private JButton scanButton;
    private JCheckBox selectAllCheckbox; // 'Select All' checkbox
    private JCheckBox isCheckSource;
    private static JTable fileTableCopy;
    private final Preferences prefs;
    private final FileTableCellEditor fileTableCellEditor;
    private final StatusBarPanel statusBarPanel;
    private final FileOperationController fileOperationController;

    public FindCopyPanel(Preferences prefs, FileTableCellEditor fileTableCellEditor, StatusBarPanel statusBarPanel, FileOperationController fileOperationController) {
        this.prefs = prefs;
        this.fileTableCellEditor = fileTableCellEditor;
        this.statusBarPanel = statusBarPanel;
        this.fileOperationController = fileOperationController;
    }

    public Preferences getPrefs() {
        return prefs;
    }

    public JTextField getSourceField() {
        return sourceField;
    }

    public JTextField getDestField() {
        return destField;
    }

    public JTable getJTable() {
        return fileTableCopy;
    }

    public String getRootPath(int columnIndex) {
        return columnIndex == FILE_INDEX_COLUMN ? sourceField.getText() : destField.getText();
    }

    public void setEnabledButton() {
        scanButton.setEnabled(!sourceField.getText().isEmpty() && !destField.getText().isEmpty());
    }

    public boolean isSelectedCheckSource() {
        return isCheckSource.isSelected();
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // TOP Panel ----
        JPanel topPanel = new JPanel(new BorderLayout());

        sourceField = new JTextField(20);
        sourceField.setText(prefs.get("sourceDir", ""));

        destField = new JTextField(20);
        destField.setText(prefs.get("destDir", ""));

        sourceButton = new JButton("Select Source");
        destButton = new JButton("Select Destination");

        sourceButton.addActionListener(e -> selectFolder(panel, sourceField, "sourceDir", false));
        destButton.addActionListener(e -> selectFolder(panel, destField, "destDir", false));
        addTextFieldListener(panel, sourceField, "sourceDir");
        addTextFieldListener(panel, destField, "destDir");

        // Source Panel -> TOP Panel ----
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.X_AXIS)); // Align components in a row
        sourcePanel.add(sourceField, BorderLayout.CENTER);
        sourcePanel.add(sourceButton, BorderLayout.EAST);
        // Dest Panel -> TOP Panel ----
        JPanel destPanel = new JPanel(new BorderLayout());
        destPanel.setLayout(new BoxLayout(destPanel, BoxLayout.X_AXIS));
        destPanel.add(destField, BorderLayout.CENTER);
        destPanel.add(destButton, BorderLayout.EAST);

        topPanel.add(sourcePanel, BorderLayout.NORTH);
        topPanel.add(destPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Bottom Panel ----
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // Scan Panel -> Bottom Panel ----
        JPanel scanPanel = new JPanel(new BorderLayout());

        // 'Check Source' Checkbox
        isCheckSource = new JCheckBox("Check Source");
        isCheckSource.setEnabled(true);

        // 'Select All' Checkbox
        selectAllCheckbox = new JCheckBox("Select All");
        selectAllCheckbox.setEnabled(false); // Disabled, enabled after scanning

        fileTableCopy = new TableFactory(fileTableCellEditor, statusBarPanel).createTable();
        loadCopyTableFromFile(fileTableCopy);
        statusBarPanel.forCopy().updateStatusPanel(((FileTableModel)fileTableCopy.getModel()).getListPaths().size());
        selectAllCheckbox.setEnabled(!((FileTableModel)fileTableCopy.getModel()).getListPaths().isEmpty());

        // Add event listener for 'Select All' for FileCopy tab only
        selectAllCheckbox.addItemListener(fileTableCellEditor::selectAllCheckBoxes);

        scanButton = new JButton("Scan Differences");
        scanButton.setEnabled(! sourceField.getText().isEmpty() && ! destField.getText().isEmpty());

        scanPanel.add(scanButton, BorderLayout.NORTH);
        scanPanel.add(selectAllCheckbox, BorderLayout.CENTER);
        scanPanel.add(isCheckSource, BorderLayout.EAST);

        bottomPanel.add(scanPanel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(fileTableCopy), BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.CENTER);

        scanButton.addActionListener(fileOperationController::scanDifferences);

        return panel;
    }

    public void enablePanelAndButtons(boolean enableSelectAllCheckbox) {
        scanButton.setEnabled(true);
        scanButton.repaint();
        selectAllCheckbox.setEnabled(enableSelectAllCheckbox);
        isCheckSource.setEnabled(true);
        isCheckSource.repaint();
    }

    public void disablePanelAndButtons() {
        // Disable scan button while scanning
        scanButton.setEnabled(false);
        scanButton.repaint();
        selectAllCheckbox.setEnabled(false);
        selectAllCheckbox.repaint();
        isCheckSource.setEnabled(false);
        isCheckSource.repaint();
    }

    public void setSelectAllCheckbox(boolean value) {
        selectAllCheckbox.setSelected(value);
    }
}
