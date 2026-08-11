package ui;

import common.ActionTabWrap;
import ui.table.FileTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import static ui.CopyFileScannerSwingUI.*;

// Helper to create JTable
public class TableFactory {
    private final FileTableModel tableModel;
    private final JTable table;

    public TableFactory(ActionTabWrap.ActionTab actionTab) {

        final String SELECT_FIELD_HEADER = "Select";
//        FileTableModel tableModel = new FileTableModel(new String[]{SELECT_FIELD_HEADER, "Folder", "File (Source)", "Comment"}, 0);
        tableModel = new FileTableModel();

        tableModel.setListPath(tableModel.getListPaths());

        table = tableModel.getJTable();

        // **Set TableRowSorter for multi-column sorting**
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortKeys(null);

        // Ensure case-insensitive sorting for Folder and File columns
        sorter.setComparator(FOLDER_INDEX_COLUMN, String.CASE_INSENSITIVE_ORDER); // Folder column
        sorter.setComparator(CopyFileScannerSwingUI.FILE_INDEX_COLUMN, String.CASE_INSENSITIVE_ORDER); // File column

        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN).setCellRenderer(new CenteredCheckboxRenderer());

        table.getTableHeader().setToolTipText("Click '"+ SELECT_FIELD_HEADER +"' header to clear sorting");
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = table.columnAtPoint(e.getPoint());
                int modelColumn = table.convertColumnIndexToModel(viewColumn);

                if (modelColumn == CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN) {
                    sorter.setSortKeys(null); // Clear all sorting
                }
            }
        });

        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int[] selectedRows = table.getSelectedRows();
                    if (selectedRows.length == 0) return;

                    for (int viewRow : selectedRows) {
                        int modelRow = table.convertRowIndexToModel(viewRow);

                        // Only allow toggle if editable
                        if (tableModel.isCellEditable(modelRow, CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN)) {
                            boolean currentValue = (boolean) tableModel.getValueAt(modelRow, CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN);
                            tableModel.setValueAt(!currentValue, modelRow, CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN);
                        }
                    }
                    e.consume(); // Prevent default behavior (if any)
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTable fileTable = table;//FileOperationController.getTable();

                if (e.getClickCount() == 2) { // Detect double-click
                    int row = fileTable.rowAtPoint(e.getPoint()); // Get row index
                    int column = fileTable.columnAtPoint(e.getPoint()); // Get column index

                    if (column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.FILE_INDEX_COLUMN) || column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.COMMENT_INDEX_COLUMN)) { // "File" or "Comment" column index (adjust if needed)
                        int modelRow = fileTable.convertRowIndexToModel(row); // Convert view index to model index
                        if (column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.COMMENT_INDEX_COLUMN) && ActionTabWrap.ActionTab.DUPLICATE.equals(getFileOperationController().getActionTabHelper().getActionName())) {
                            return;// skip open dest. file for duplicate
                        }
                        int columnIndex = column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.FILE_INDEX_COLUMN) ? CopyFileScannerSwingUI.FILE_INDEX_COLUMN : CopyFileScannerSwingUI.COMMENT_INDEX_COLUMN;
                        String filePath = tableModel.getFullFilePath(columnIndex, modelRow);
                        try {
                            Desktop.getDesktop().open(new File(filePath)); // Open file with default app
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(fileTable, "Cannot open file: " + ex.getMessage());
                        }
                    }
                }
            }
        });
    }

    public void addTableListener(FileTableCellEditor fileTableCellEditor, IStatusBarUpdater statusBarPanel) {
        // Add table listener to uncheck 'Select All' if a row is manually unchecked
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN && fileTableCellEditor.isFinishedSelectAllAction()) { // Checkbox column
                fileTableCellEditor.setCheckBoxActionInTable(true);
                fileTableCellEditor.checkCheckBoxesAndEnableButtons();
                ((StatusBarPanel)statusBarPanel).refreshStatusBar();
                fileTableCellEditor.setCheckBoxActionInTable(false);
            }
        });
    }

    public FileTableModel getTableModel() {
        return tableModel;
    }

    public JTable getJTable() {
        return table;
    }
}
