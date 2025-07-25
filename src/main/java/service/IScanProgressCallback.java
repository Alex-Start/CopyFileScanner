package service;

import common.ActionTabWrap;

import java.util.List;
import java.util.Map;

// Interface for scan services to report progress to the controller
public interface IScanProgressCallback {
    void onScanStarted(String message, ActionTabWrap.ActionTab tab);
    void onScanProgress(int percentage, ActionTabWrap.ActionTab tab);
    void onScanCompletedCopy(Map<String, String> differences, ActionTabWrap.ActionTab tab, String message); // For copy
    void onScanCompletedDuplicates(Map<String, List<String>> duplicates, ActionTabWrap.ActionTab tab, String message); // For duplicates
    void onScanError(String errorMessage, ActionTabWrap.ActionTab tab);
}
