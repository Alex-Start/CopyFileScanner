package model;

import java.lang.reflect.Field;

public record FileEntry(Boolean selected, String folder, String filename, String comment) {
    public Object[] getRowData() {
        return new Object[]{selected, folder, filename, comment};
    }
    // Convenience method to update comment/selected status
    public FileEntry withComment(String newComment) {
        return new FileEntry(this.selected, this.folder, this.filename, newComment);
    }
    public FileEntry withSelected(Boolean newSelected) {
        return new FileEntry(newSelected, this.folder, this.filename, this.comment);
    }
    public Object getValue(int columnIndex) {
        Field[] fields = FileEntry.class.getDeclaredFields();
        if (fields.length <= columnIndex || columnIndex < 0) {
            throw new IllegalArgumentException("FileEntry: Incorrect columnIndex values: "+ columnIndex);
        }
        Field declaredField = fields[columnIndex];
        declaredField.setAccessible(true);
        try {
            return declaredField.get(this);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
