package ui.table;

import controller.FileOperationController;
import model.FileEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ui.CopyFileScannerSwingUI.*;

public class FileTableModel extends DefaultTableModel {
    // Copy: contains relative path for source and dest.
    // Duplicate: contains full path
    private List<String> listPaths = new ArrayList<>();

    private final List<FileEntry> data;
    private final JTable jTable; // To distinguish between copy and duplicate tables

    public FileTableModel() {
        super(new String[]{
                FileTableColumn.CHECKBOX.getColumnName(),
                FileTableColumn.FOLDER.getColumnName(),
                FileTableColumn.FILENAME.getColumnName(),
                FileTableColumn.COMMENT.getColumnName()
        }, 0);
        this.data = new ArrayList<>();
        this.jTable = new JTable(this);
    }

    public JTable getJTable() {
        return jTable;
    }

    public void clearData() {
        data.clear();
        fireTableDataChanged();
    }

    public List<FileEntry> getAllEntries() {
        return new ArrayList<>(data); // Return a copy to prevent external modification
    }

    public FileEntry getFileEntry(int rowIndex) {
        return data.get(rowIndex);
    }

    @Override
    public void addRow(Object[] rowData) {
        super.addRow(rowData);
        data.add(new FileEntry((Boolean) rowData[0], (String) rowData[1], (String) rowData[2], (String) rowData[3]));
    }

    //    @Override
//    public int getRowCount() {
//        int count = super.getRowCount();
//        if (data == null) return count;
//        return data.size();
//    }

    @Override
    public int getColumnCount() {
        return FileTableColumn.values().length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (row < 0 || row >= data.size()) {
            return null; // Handle out of bounds
        }
        FileEntry entry = data.get(row);
        FileTableColumn column = FileTableColumn.fromIndex(col);
        return switch (column) {
            case CHECKBOX -> entry.selected();
            case FOLDER -> entry.folder();
            case FILENAME -> entry.filename();
            case COMMENT -> entry.comment();
        };
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (row < 0 || row >= data.size()) {
            return; // Handle out of bounds
        }
        FileEntry entry = data.get(row);
        FileTableColumn column = FileTableColumn.fromIndex(col);

        switch (column) {
            case CHECKBOX -> {
                if (aValue == null || aValue instanceof Boolean) {
                    data.set(row, entry.withSelected((Boolean) aValue));
                    fireTableCellUpdated(row, col);
                }
            }
            case COMMENT -> { // Allow updating comment for status
                if (aValue instanceof String) {
                    data.set(row, entry.withComment((String) aValue));
                    fireTableCellUpdated(row, col);
                }
            }
            // Other columns are immutable through direct setValueAt
            default -> {}
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return FileTableColumn.fromIndex(columnIndex).getColumnClass();
    }

    public void updateFileStatus(int modelRowIndex, String newStatus) {
        setValueAt(newStatus, modelRowIndex, FileTableColumn.COMMENT.getColumnIndex());
        // Also unselect the checkbox when status changes to processed
        setValueAt(false, modelRowIndex, FileTableColumn.CHECKBOX.getColumnIndex());//TODO null for checkbox
    }

    public void toggleAllCheckboxes(boolean selected) {
        for (int i = 0; i < data.size(); i++) {
            if (isCellEditable(i, FileTableColumn.CHECKBOX.getColumnIndex())) { // Only toggle editable ones
                setValueAt(selected, i, FileTableColumn.CHECKBOX.getColumnIndex());
            }
        }
    }

    /**
     * Get map of selected rows
     * @return - rowIndex, path - short relative path for COPY, and full path for DUPLICATE.
     */
    public Map<Integer, String> getSelectedRows() {
        return getSelectedRows(-1);
    }

    //TODO delete it
    public Map<Integer, String> getSelectedRows(int columnIndex) {
        String rootDir = "";
        if (columnIndex > 0) {
            rootDir = getFileOperationController().getActiveTabPanel().getRootPath(columnIndex);
        }
        return getSelectedRows(rootDir);
    }

    public Map<Integer, String> getSelectedRows(String rootDir) {
        Map<Integer, String> selectedRows  = new HashMap<>();
        JTable table = getJTable();
        for (int i = 0; i < getRowCount(); i++) {
            int modelRowIndex = table.convertRowIndexToModel(i); // Convert to model index
            if (Boolean.TRUE.equals(getValueAt(modelRowIndex, CHECKBOX_INDEX_COLUMN))) {
                selectedRows.put(modelRowIndex, getFullFilePath(rootDir, modelRowIndex));
            }
        }

        return selectedRows;

        /*List<FileEntry> selectedRows = new ArrayList<>();
        for (int i = 0; i < getRowCount(); i++) {
            if (Boolean.TRUE.equals(getValueAt(i, FileTableColumn.CHECKBOX.getColumnIndex()))) {
                selectedRows.add(data.get(i));
            }
        }
        return selectedRows;*/
    }

    public String getFullFilePath(int columnIndex, int modelRowIndex) {
        FileEntry fileEntry = getFileEntry(modelRowIndex);
        String relativePath = Path.of((String)fileEntry.getValue(FOLDER_INDEX_COLUMN), (String)fileEntry.getValue(FILE_INDEX_COLUMN)).toString();//.getListPaths().get(modelRowIndex);
        if (columnIndex < 0) {
            return relativePath;
        }

        return Path.of(getFileOperationController().getActiveTabPanel().getRootPath(columnIndex), relativePath).toString(); // Retrieve full file path
    }

    public String getFullFilePath(String rootDir, int modelRowIndex) {
        FileEntry fileEntry = FileOperationController.getTableModel().getFileEntry(modelRowIndex);
        if (rootDir == null || rootDir.trim().isEmpty()) {
            rootDir = "";
        }
        return Path.of(rootDir, (String)fileEntry.getValue(FOLDER_INDEX_COLUMN), (String)fileEntry.getValue(FILE_INDEX_COLUMN)).toString(); // Retrieve full file path
    }

    public List<String> getListPaths() {
        return listPaths;
    }

    public FileTableModel setListPath(List<String> listPaths) {
        this.listPaths = listPaths;
        return this;
    }

//    @Override
//    public Class<?> getColumnClass(int columnIndex) {
//        return columnIndex == CHECKBOX_INDEX_COLUMN ? Boolean.class : String.class;
//    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (FileTableColumn.fromIndex(column) == FileTableColumn.CHECKBOX) {
//        if (column == CHECKBOX_INDEX_COLUMN) {
            return !isSkipCheckBoxCondition(this, row); // Disable checkbox if file is copied
        }
        return false;
    }

    public static boolean isSkipCheckBoxCondition(JTable table, int row) {
        return isSkipCheckBoxCondition(table.getModel(), table.convertRowIndexToModel(row));
    }

    public static boolean isSkipCheckBoxCondition(TableModel table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) {
            return false;
        }
        Object commentObj = table.getValueAt(row, FileTableColumn.COMMENT.getColumnIndex());
        if (commentObj instanceof String comment) {
            return comment.equals(COPIED) || comment.endsWith(DELETED);
        }
        return false;
    }

    public boolean isSkipCheckBoxCondition(int row) {
        return isSkipCheckBoxCondition(this, row);
    }

    // Add data for copy differences
    public void addDifferences(Map<String, String> differences) {
        addDifferences(differences, null, null);
    }

    public void addDifferences(Map<String, String> differences, String sourceDir, String destDir) {
        this.setRowCount(0);
        listPaths.clear();
        clearData();

        for (Map.Entry<String, String> entry : differences.entrySet()) {
            // Assuming entry.getKey() is relative path, entry.getValue() is destination path
            Path path = Path.of(entry.getKey());
            // sourceDir = folder
            String folder = path.getParent() == null ? "" : path.getParent().toString();
            this.addRow(new FileEntry(false, folder, path.getFileName().toString(), entry.getValue()).getRowData());
            listPaths.add(entry.getKey());
            //data.add(new FileEntry(false, folder, path.getFileName().toString(), entry.getValue()));
        }
        fireTableDataChanged();
    }

    public void addDifferences(List<FileEntry> fileEntries) {
        this.setRowCount(0);
        listPaths.clear();
        clearData();
        for (FileEntry fileEntry : fileEntries) {
            this.addRow(fileEntry.getRowData());
            listPaths.add(Path.of(fileEntry.folder(), fileEntry.filename()).toString());
        }

        fireTableDataChanged();
    }

    // Add data for duplicate files
    public void addDuplicates(Map<String, List<String>> duplicates) {
        this.setRowCount(0);
        listPaths.clear();
        clearData();

        int i = 0;
        for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
            i++;
            for (String destPath : entry.getValue()) {
                Path path = Path.of(destPath);
                String folder = path.getParent() == null ? "" : path.getParent().toString();
                this.addRow(new FileEntry(false, folder, path.getFileName().toString(), "# "+ i).getRowData());
                listPaths.add(destPath);
                //data.add(new FileEntry(false, folder, path.getFileName().toString(), "# "+ i));
            }
        }
        fireTableDataChanged();
    }

    // Add data for duplicate files
    public void addDuplicates(List<FileEntry> duplicates) {
        this.setRowCount(0);
        listPaths.clear();
        clearData();

        for (FileEntry fileEntry : duplicates) {
            this.addRow(fileEntry.getRowData());
            listPaths.add(Path.of(fileEntry.folder(), fileEntry.filename()).toString());
        }

        fireTableDataChanged();
    }
}
