package serializable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SerializableTableData implements Serializable {
    @Serial
    private static final long serialVersionUID = 7871614391238143686L;

    private final List<String> columnNames;
    private final List<Object[]> rowData;
    private List<String> filePath = new ArrayList<>();

    public SerializableTableData(List<String> columnNames, List<Object[]> rowData, List<String> filePath) {
        this.columnNames = columnNames;
        this.rowData = rowData;
        this.filePath = filePath;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<Object[]> getRowData() {
        return rowData;
    }

    public List<String> getFilePath() {
        return filePath;
    }
}