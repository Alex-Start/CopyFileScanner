package ui;

import common.ActionTabWrap;
import controller.FileOperationController;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

import static serializable.SerializeManager.loadDuplicateTableFromFile;
import static ui.CopyFileScannerSwingUI.SOURCE_DIR_DUPL;

public class FindDuplicatePanel implements ITabPanel {
    private JTextField sourceFieldDuplicate;
    private JButton sourceButtonDuplicate;
    private JButton duplicateButton;
    private TableFactory fileTableDupl;
    private final Preferences prefs;
    private final ITableUpdater fileTableCellEditor;
    private final IStatusBarUpdater statusBarPanel;
    private final FileOperationController fileOperationController;

    public FindDuplicatePanel(Preferences prefs, ButtonsManager buttonsManager, IStatusBarUpdater statusBarPanel, FileOperationController fileOperationController) {
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
        return "";
    }

    public JTextField getSourceField() {
        return sourceFieldDuplicate;
    }

    public TableFactory getTableFactory() {
        return fileTableDupl;
    }

    @Override
    public JTable getJTable() {
        return fileTableDupl.getJTable();
    }

    @Override
    public String getRootPath(int columnIndex) {
        return "";
    }

    public FileTableCellEditor getFileTableCellEditor() {
        return (FileTableCellEditor)fileTableCellEditor;
    }

    @Override
    public ITableUpdater getTableUpdater() {
        return fileTableCellEditor;
    }

    public void setEnabledButton() {
        duplicateButton.setEnabled(!sourceFieldDuplicate.getText().isEmpty());
    }

    @Override
    public JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // TOP Panel ----
        JPanel topPanel = new JPanel(new BorderLayout());
        // Source Panel -> TOP Panel ----
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourceFieldDuplicate = new JTextField(20);
        sourceFieldDuplicate.setText(prefs.get(SOURCE_DIR_DUPL, ""));

        sourceButtonDuplicate = new JButton("Select Source");

        sourceButtonDuplicate.addActionListener(e -> selectFolder(panel, sourceFieldDuplicate, SOURCE_DIR_DUPL, true));
        addTextFieldListener(panel, sourceFieldDuplicate, SOURCE_DIR_DUPL);

        duplicateButton = new JButton("Find Duplicates");
        duplicateButton.setEnabled(! sourceFieldDuplicate.getText().isEmpty());
        duplicateButton.addActionListener(fileOperationController::scanDuplicates);

        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.X_AXIS)); // Align components in a row
        sourcePanel.add(sourceFieldDuplicate, BorderLayout.CENTER);
        sourcePanel.add(sourceButtonDuplicate, BorderLayout.EAST);

        topPanel.add(sourcePanel, BorderLayout.NORTH);
        topPanel.add(duplicateButton, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        // Bottom Panel ----
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(duplicateButton, BorderLayout.NORTH);

        fileTableDupl = new TableFactory(ActionTabWrap.ActionTab.DUPLICATE);
        ((FileTableCellEditor)fileTableCellEditor).setTablePanel(this);
        fileTableDupl.addTableListener((FileTableCellEditor)fileTableCellEditor, statusBarPanel);

        loadDuplicateTableFromFile(fileTableDupl.getJTable());
        ((StatusBarPanel)statusBarPanel).forDuplicate().updateStatusPanel(fileTableDupl.getTableModel().getListPaths().size());

        bottomPanel.add(new JScrollPane(fileTableDupl.getJTable()), BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.CENTER);

        return panel;
    }

    public void enablePanelAndButtons(boolean enableSelectAllCheckbox) {
        duplicateButton.setEnabled(true);
        duplicateButton.repaint();
    }

    public void disablePanelAndButtons() {
        duplicateButton.setEnabled(false);
        duplicateButton.repaint();
    }

    public void setSelectAllCheckbox(boolean value) {
        //absent for duplicate table
    }
}
