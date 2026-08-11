package ui.table;

// Columns for the table. Using enum for clarity.
enum FileTableColumn {
    CHECKBOX(Boolean.class, "Select", 0),
    FOLDER(String.class, "Folder", 1),
    FILENAME(String.class, "Filename (Source)", 2),
    COMMENT(String.class, "Comment", 3);

    private final Class<?> columnClass;
    private final String columnName;
    private final int columnIndex;

    FileTableColumn(Class<?> columnClass, String columnName, int columnIndex) {
        this.columnClass = columnClass;
        this.columnName = columnName;
        this.columnIndex = columnIndex;
    }

    public Class<?> getColumnClass() {
        return columnClass;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public static FileTableColumn fromIndex(int index) {
        for (FileTableColumn col : values()) {
            if (col.getColumnIndex() == index) {
                return col;
            }
        }
        throw new IllegalArgumentException("Invalid column index: " + index);
    }
}
