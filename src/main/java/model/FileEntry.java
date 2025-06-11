package model;

public record FileEntry(boolean selected, String folder, String filename, String comment) {
    public Object[] getRowData() {
        return new Object[]{selected, folder, filename, comment};
    }
}
