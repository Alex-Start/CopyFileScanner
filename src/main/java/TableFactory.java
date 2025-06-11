import common.ActionHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

// Helper to create JTable
public class TableFactory {
    private final CopyFileScannerGUI GUI;

    public TableFactory(CopyFileScannerGUI gui) {
        this.GUI = gui;
    }

    public JTable createTable() {
        final String SELECT_FIELD_HEADER = "Select";
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{SELECT_FIELD_HEADER, "Folder", "File (Source)", "Comment"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (column == CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN) {
                    return !CopyFileScannerGUI.isSkipCheckBoxCondition(this, row); // Disable checkbox if file is copied
                }
                return false;
            }
        };
        // Add table listener to uncheck 'Select All' if a row is manually unchecked
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN && GUI.isFinishedSelectAllAction()) { // Checkbox column
                GUI.setCheckBoxActionInTable(true);
                GUI.checkCheckBoxes();
                GUI.refreshStatusBar();
                GUI.setCheckBoxActionInTable(false);
            }
        });

        JTable fileTable = new JTable(tableModel);

        // **Set TableRowSorter for multi-column sorting**
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setSortKeys(null);

        // Ensure case-insensitive sorting for Folder and File columns
        sorter.setComparator(1, String.CASE_INSENSITIVE_ORDER); // Folder column
        sorter.setComparator(CopyFileScannerGUI.FILE_INDEX_COLUMN, String.CASE_INSENSITIVE_ORDER); // File column

        fileTable.setRowSorter(sorter);

        fileTable.getColumnModel().getColumn(CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN).setCellRenderer(new CopyFileScannerGUI.CenteredCheckboxRenderer());

        fileTable.getTableHeader().setToolTipText("Click '"+ SELECT_FIELD_HEADER +"' header to clear sorting");
        fileTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = fileTable.columnAtPoint(e.getPoint());
                int modelColumn = fileTable.convertColumnIndexToModel(viewColumn);

                if (modelColumn == CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN) {
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
                        boolean currentValue = (boolean) tableModel.getValueAt(modelRow, CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN);

                        // Only allow toggle if editable
                        if (tableModel.isCellEditable(modelRow, CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN)) {
                            tableModel.setValueAt(!currentValue, modelRow, CopyFileScannerGUI.CHECKBOX_INDEX_COLUMN);
                        }
                    }
                    e.consume(); // Prevent default behavior (if any)
                }
            }
        });

        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JTable fileTable = GUI.getTable();

                if (e.getClickCount() == 2) { // Detect double-click
                    int row = fileTable.rowAtPoint(e.getPoint()); // Get row index
                    int column = fileTable.columnAtPoint(e.getPoint()); // Get column index

                    if (column == fileTable.convertColumnIndexToView(CopyFileScannerGUI.FILE_INDEX_COLUMN) || column == fileTable.convertColumnIndexToView(CopyFileScannerGUI.COMMENT_INDEX_COLUMN)) { // "File" or "Comment" column index (adjust if needed)
                        int modelRow = fileTable.convertRowIndexToModel(row); // Convert view index to model index
                        if (column == fileTable.convertColumnIndexToView(CopyFileScannerGUI.COMMENT_INDEX_COLUMN) && ActionHelper.ActionEnum.DELETE.equals(GUI.getActionHelper().getActionName())) {
                            return;// skip open dest. file for duplicate
                        }
                        int columnIndex = column == fileTable.convertColumnIndexToView(CopyFileScannerGUI.FILE_INDEX_COLUMN) ? CopyFileScannerGUI.FILE_INDEX_COLUMN : CopyFileScannerGUI.COMMENT_INDEX_COLUMN;
                        String filePath = GUI.getFullFilePath(columnIndex, modelRow);
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
