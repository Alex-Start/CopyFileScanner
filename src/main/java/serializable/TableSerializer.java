package serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TableSerializer {
    private static final Logger logger = LogManager.getLogger(TableSerializer.class);

    public static void saveTableToFile(JTable table, Path pathFile, List<String> filePath) {
        saveTableToFile(table, pathFile.toString(), filePath);
    }

    public static void saveTableToFile(JTable table, String pathFile, List<String> filePath) {
        try {
            saveTableToFile(table, new File(pathFile), filePath);
        } catch (IOException ex) {
            logger.error("Error Table saved to: {}", pathFile);
        }
    }

    public static void saveTableToFile(JTable table, File file, List<String> filePath) throws IOException {
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        List<String> columnNames = new ArrayList<>();
        for (int i = 0; i < model.getColumnCount(); i++) {
            columnNames.add(model.getColumnName(i));
        }

        List<Object[]> rowData = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            Object[] row = new Object[model.getColumnCount()];
            for (int j = 0; j < model.getColumnCount(); j++) {
                row[j] = model.getValueAt(i, j);
            }
            rowData.add(row);
        }

        SerializableTableData data = new SerializableTableData(columnNames, rowData, filePath);

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(data);
        }
    }

    public static void loadTableFromFile(JTable table, String pathFile, List<String> filePath) {
        File file = new File(pathFile);
        if(!file.exists() || !file.isFile()) {
            logger.warn("Not found {} file to load into table", pathFile);
            return;
        }
        try {
            loadTableFromFile(table, file, filePath);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("loadTableFromFile: {}\n{}", pathFile, e.getMessage());
        }
    }

    public static void loadTableFromFile(JTable table, File file, List<String> filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            SerializableTableData data = (SerializableTableData) in.readObject();

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0); // Clear current data
            model.setColumnIdentifiers(data.getColumnNames().toArray());

            for (Object[] row : data.getRowData()) {
                model.addRow(row);
                //Dupl. case:
                //filePath.add(Path.of((String)row[1], (String)row[2]).toString());
            }

            filePath.addAll(data.getFilePath());
        }
    }
}
