package ui;

import common.ActionTabWrap;
import controller.FileOperationController;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static serializable.SerializeManager.loadCopyTableFromFile;
import static ui.CopyFileScannerSwingUI.FILE_INDEX_COLUMN;

public class FindCopyPanel implements ITabPanel {
    private JTextField sourceField, destField;
    private JButton sourceButton, destButton;
    private JButton scanButton;
    private JCheckBox selectAllCheckbox; // 'Select All' checkbox
    private JCheckBox isMissingInSource;
    private static TableFactory fileTableCopy;
    private final Preferences prefs;
    private final ITableUpdater fileTableCellEditor;
    private final IStatusBarUpdater statusBarPanel;
    private final FileOperationController fileOperationController;

    public FindCopyPanel(Preferences prefs, ButtonsManager buttonsManager, IStatusBarUpdater statusBarPanel, FileOperationController fileOperationController) {
        this.prefs = prefs;
        this.statusBarPanel = statusBarPanel;
        this.fileOperationController = fileOperationController;
        fileTableCellEditor = new FileTableCellEditor(fileOperationController.getActionTabHelper(), buttonsManager, statusBarPanel);
    }

    public Preferences getPrefs() {
        return prefs;
    }

    public String getSourceFieldText() {
        return getSourceField().getText().trim();
    }

    public String getDestFieldText() {
        return getDestField().getText().trim();
    }

    public JTextField getSourceField() {
        return sourceField;
    }

    public JTextField getDestField() {
        return destField;
    }

    public TableFactory getTableFactory() {
        return fileTableCopy;
    }

    public JTable getJTable() {
        return fileTableCopy.getJTable();
    }

    public String getRootPath(int columnIndex) {
        return columnIndex == FILE_INDEX_COLUMN ? sourceField.getText().trim() : destField.getText().trim();
    }

    public FileTableCellEditor getFileTableCellEditor() {
        return (FileTableCellEditor)fileTableCellEditor;
    }

    public ITableUpdater getTableUpdater() {
        return fileTableCellEditor;
    }

    public void setEnabledButton() {
        scanButton.setEnabled(!sourceField.getText().trim().isEmpty() && !destField.getText().trim().isEmpty());
    }

    public boolean isSelectedCheckSource() {
        return isMissingInSource.isSelected();
    }

    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // TOP Panel ----
        JPanel topPanel = new JPanel(new BorderLayout());

        sourceField = new JTextField(20);
        sourceField.setText(prefs.get("sourceDir", "").trim());

        destField = new JTextField(20);
        destField.setText(prefs.get("destDir", "").trim());

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
        isMissingInSource = new JCheckBox("Missing in Source");
        isMissingInSource.setEnabled(true);

        // 'Select All' Checkbox
        selectAllCheckbox = new JCheckBox("Select All");
        selectAllCheckbox.setEnabled(false); // Disabled, enabled after scanning

        ((FileTableCellEditor)fileTableCellEditor).addMissingInSourceCheckbox(isMissingInSource);
        ((FileTableCellEditor)fileTableCellEditor).addSelectAllCheckbox(selectAllCheckbox);

        fileTableCopy = new TableFactory(ActionTabWrap.ActionTab.COPY);
        ((FileTableCellEditor)fileTableCellEditor).setTablePanel(this);
        fileTableCopy.addTableListener((FileTableCellEditor)fileTableCellEditor, statusBarPanel);

        loadCopyTableFromFile(fileTableCopy.getJTable());
        ((StatusBarPanel)statusBarPanel).forCopy().updateStatusPanel(fileTableCopy.getTableModel().getListPaths().size());
        selectAllCheckbox.setEnabled(!fileTableCopy.getTableModel().getListPaths().isEmpty());

        // Add event listener for 'Select All' for FileCopy tab only
        selectAllCheckbox.addItemListener(((FileTableCellEditor)fileTableCellEditor)::selectAllCheckBoxes);

        scanButton = new JButton("Scan Differences");
        scanButton.setEnabled(! sourceField.getText().isEmpty() && ! destField.getText().isEmpty());

        scanPanel.add(scanButton, BorderLayout.NORTH);
        scanPanel.add(selectAllCheckbox, BorderLayout.CENTER);
        scanPanel.add(isMissingInSource, BorderLayout.EAST);

        bottomPanel.add(scanPanel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(fileTableCopy.getJTable()), BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.CENTER);

        scanButton.addActionListener(fileOperationController::scanDifferences);

        return panel;
    }

    public void enablePanelAndButtons(boolean enableSelectAllCheckbox) {
        scanButton.setEnabled(true);
        scanButton.repaint();
        selectAllCheckbox.setEnabled(enableSelectAllCheckbox);
        isMissingInSource.setEnabled(true);
        isMissingInSource.repaint();
    }

    public void disablePanelAndButtons() {
        // Disable scan button while scanning
        scanButton.setEnabled(false);
        scanButton.repaint();
        selectAllCheckbox.setEnabled(false);
        selectAllCheckbox.repaint();
        isMissingInSource.setEnabled(false);
        isMissingInSource.repaint();
    }

    public void setSelectAllCheckbox(boolean value) {
        selectAllCheckbox.setSelected(value);
    }
}
