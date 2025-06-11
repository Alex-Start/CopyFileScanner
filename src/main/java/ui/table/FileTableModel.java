package ui.table;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class FileTableModel extends DefaultTableModel {
    private final List<String> listPaths;

    public FileTableModel(List<String> listPaths, List<String> listPaths1) {
        this.listPaths = listPaths1;
    }



}
