package serializable;

import ui.table.FileTableModel;

import javax.swing.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static settings.SettingsManager.*;

public class SerializeManager {

    private static final String DATETIME_PATTERN = "yyyyMMdd-HHmmss";
    private static final String ARCHIVE_FOLDER = WORK_SESSION_ARCH_FOLDER;

    public static void loadCopyTableFromFile(JTable table) {
        TableSerializer.loadTableFromFile(table, COPY_FILES_SER, ((FileTableModel)table.getModel()).getListPaths());
    }

    public static void loadDuplicateTableFromFile(JTable table) {
        TableSerializer.loadTableFromFile(table, DUPLICATE_FILES_SER, ((FileTableModel)table.getModel()).getListPaths());
    }

    public static void saveCopyTableToFile(JTable table) {
        TableSerializer.saveTableToFile(table, COPY_FILES_SER, ((FileTableModel)table.getModel()).getListPaths());
        TableSerializer.saveTableToFile(table, getBackUpPath(COPY_FILES_SER), ((FileTableModel)table.getModel()).getListPaths());
    }

    public static void saveDuplicateTableToFile(JTable table) {
        TableSerializer.saveTableToFile(table, DUPLICATE_FILES_SER, ((FileTableModel)table.getModel()).getListPaths());
        TableSerializer.saveTableToFile(table, getBackUpPath(DUPLICATE_FILES_SER), ((FileTableModel)table.getModel()).getListPaths());
    }

    // add date time in the end of the file name
    private static Path getBackUpPath(String path) {
        Path path1 = Path.of(path);
        Path newPath = Path.of(path1.toAbsolutePath().getParent().toString(), ARCHIVE_FOLDER);
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error to create directory path: "+ path, e);
        }
        String fileName = path1.getFileName().toString();
        String[] splitFilename = fileName.split("\\.");
        String ext = splitFilename[splitFilename.length-1];
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATETIME_PATTERN));
        return Path.of(newPath.toString(), fileName.replace(ext, dateTime+"."+ext)).toAbsolutePath();
    }
}
