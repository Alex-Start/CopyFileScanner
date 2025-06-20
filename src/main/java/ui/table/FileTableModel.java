package ui.table;

import model.FileEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ui.CopyFileScannerSwingUI.*;

public class FileTableModel extends DefaultTableModel {
    // Copy: contains relative path for source and dest.
    // Duplicate: contains full path
    private List<String> listPaths = new ArrayList<>();

    public FileTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    public List<String> getListPaths() {
        return listPaths;
    }

    public FileTableModel setListPath(List<String> listPaths) {
        this.listPaths = listPaths;
        return this;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == CHECKBOX_INDEX_COLUMN ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column == CHECKBOX_INDEX_COLUMN) {
            return !isSkipCheckBoxCondition(this, row); // Disable checkbox if file is copied
        }
        return false;
    }

    public static boolean isSkipCheckBoxCondition(JTable table, int row) {
        return isSkipCheckBoxCondition(table.getModel(), table.convertRowIndexToModel(row));
    }

    public static boolean isSkipCheckBoxCondition(TableModel table, int row) {
        Object checkboxValue = table.getValueAt(row, COMMENT_INDEX_COLUMN);
        if (COPIED.equals(checkboxValue)) {
            return true;
        }
        if (checkboxValue.toString().endsWith(DELETED)) {
            return true;
        }

        return false;
    }

    public boolean isSkipCheckBoxCondition(int row) {
        return isSkipCheckBoxCondition(this, row);
    }

    public void addDifferences(Map<String, String> differences) {
        this.setRowCount(0);
        listPaths.clear();
        for (Map.Entry<String, String> entry : differences.entrySet()) {
            Path path = Path.of(entry.getKey());
            String folder = path.getParent() == null ? "" : path.getParent().toString();
            this.addRow(new FileEntry(false, folder, path.getFileName().toString(), entry.getValue()).getRowData());
            listPaths.add(entry.getKey());
        }
    }

    public void addDuplicates(Map<String, List<String>> duplicates) {
        this.setRowCount(0);
        listPaths.clear();

        int i = 0;
        for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
            i++;
            for (String destPath : entry.getValue()) {
                Path path = Path.of(destPath);
                String folder = path.getParent() == null ? "" : path.getParent().toString();
                this.addRow(new FileEntry(false, folder, path.getFileName().toString(), "# "+ i).getRowData());
                listPaths.add(destPath);
            }
        }
    }
}
