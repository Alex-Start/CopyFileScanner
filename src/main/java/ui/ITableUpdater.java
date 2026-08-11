package ui;

import common.ActionTabWrap;
import model.FileEntry;
import ui.table.FileTableModel;

import java.util.List;
import java.util.Map;

public interface ITableUpdater {
    void updateCopyTable(List<FileEntry> data);
    void updateDuplicateTable(List<FileEntry> data);
    void markTableRowProcessed(ActionTabWrap.ActionTab tab, int rowIndex, String status);
    Map<Integer, String> getSelectedRows(); // To get selected rows from the UI table
    void toggleAllCheckboxes(ActionTabWrap.ActionTab tab, boolean selected);
    void disableButtonsAndClearTable();
    void enablePanelAndButtons();
    void disablePanelAndButtons();
}
