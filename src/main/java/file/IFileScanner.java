package file;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface IFileScanner {
    void scanFiles(String folderPath, Map<String, List<String>> filesByName);
    void scanFiles(File folder, Map<String, List<String>> filesByName);
}
