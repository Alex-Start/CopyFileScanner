package file;

import common.ActionTabWrap;
import service.IScanProgressCallback;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface IFileScanner {
    void scanFiles(String folderPath, Map<String, List<FileMetadata>> filesByName);
    void scanFiles(File folder, Map<String, List<FileMetadata>> filesByName);
    default void updateScanProgress(IScanProgressCallback callback, AtomicInteger filesScanned, AtomicInteger totalFiles, ActionTabWrap.ActionTab tab) {
        int scanned = filesScanned.incrementAndGet();
        if (scanned > totalFiles.get()) {
            totalFiles.addAndGet(100);
        }
        int percent = (int) ((double) scanned / totalFiles.get() * 100);
        callback.onScanProgress(percent, tab);
    }
}
