package ui;

import common.ActionTab;
import controller.FileOperationController;
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
    private final FileTableCellEditor fileTableCellEditor;
    private final StatusBarPanel statusBarPanel;

    public TableFactory(FileTableCellEditor fileTableCellEditor, StatusBarPanel statusBarPanel) {
        this.fileTableCellEditor = fileTableCellEditor;
        this.statusBarPanel = statusBarPanel;
    }

    public JTable createTable() {
        final String SELECT_FIELD_HEADER = "Select";
        FileTableModel tableModel = new FileTableModel(new String[]{SELECT_FIELD_HEADER, "Folder", "File (Source)", "Comment"}, 0);

        tableModel.setListPath(tableModel.getListPaths());

        // Add table listener to uncheck 'Select All' if a row is manually unchecked
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN && fileTableCellEditor.isFinishedSelectAllAction()) { // Checkbox column
                fileTableCellEditor.setCheckBoxActionInTable(true);
                getFileOperationController().checkCheckBoxes();
                statusBarPanel.refreshStatusBar();
                fileTableCellEditor.setCheckBoxActionInTable(false);
            }
        });

        JTable fileTable = new JTable(tableModel);

        // **Set TableRowSorter for multi-column sorting**
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortKeys(null);

        // Ensure case-insensitive sorting for Folder and File columns
        sorter.setComparator(1, String.CASE_INSENSITIVE_ORDER); // Folder column
        sorter.setComparator(CopyFileScannerSwingUI.FILE_INDEX_COLUMN, String.CASE_INSENSITIVE_ORDER); // File column

        fileTable.setRowSorter(sorter);

        fileTable.getColumnModel().getColumn(CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN).setCellRenderer(new CenteredCheckboxRenderer());

        fileTable.getTableHeader().setToolTipText("Click '"+ SELECT_FIELD_HEADER +"' header to clear sorting");
        fileTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = fileTable.columnAtPoint(e.getPoint());
                int modelColumn = fileTable.convertColumnIndexToModel(viewColumn);

                if (modelColumn == CopyFileScannerSwingUI.CHECKBOX_INDEX_COLUMN) {
                    sorter.setSortKeys(null); // Clear all sorting
                }
            }
        });

        fileTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    int[] selectedRows = fileTable.getSelectedRows();
                    if (selectedRows.length == 0) return;

                    for (int viewRow : selectedRows) {
                        int modelRow = fileTable.convertRowIndexToModel(viewRow);

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

        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTable fileTable = FileOperationController.getTable();

                if (e.getClickCount() == 2) { // Detect double-click
                    int row = fileTable.rowAtPoint(e.getPoint()); // Get row index
                    int column = fileTable.columnAtPoint(e.getPoint()); // Get column index

                    if (column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.FILE_INDEX_COLUMN) || column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.COMMENT_INDEX_COLUMN)) { // "File" or "Comment" column index (adjust if needed)
                        int modelRow = fileTable.convertRowIndexToModel(row); // Convert view index to model index
                        if (column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.COMMENT_INDEX_COLUMN) && ActionTab.Tab.DUPLICATE.equals(getFileOperationController().getActionTabHelper().getActionName())) {
                            return;// skip open dest. file for duplicate
                        }
                        int columnIndex = column == fileTable.convertColumnIndexToView(CopyFileScannerSwingUI.FILE_INDEX_COLUMN) ? CopyFileScannerSwingUI.FILE_INDEX_COLUMN : CopyFileScannerSwingUI.COMMENT_INDEX_COLUMN;
                        String filePath = FileOperationController.getFullFilePath(columnIndex, modelRow);
                        try {
                            Desktop.getDesktop().open(new File(filePath)); // Open file with default app
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(fileTable, "Cannot open file: " + ex.getMessage());
                        }
                    }
                }
            }
        });

        return fileTable;
    }
}
