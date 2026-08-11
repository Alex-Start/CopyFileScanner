package service;

import common.ActionTabWrap;

// Interface for file action services to report progress to the controller
public interface IFileActionProgressCallback {
    void onActionStarted(String message, ActionTabWrap.ActionTab tab);
    void onActionProgress(int percentage, ActionTabWrap.ActionTab tab);
    void onFileProcessed(int rowIndex, String status, ActionTabWrap.ActionTab tab);
    void onActionCompleted(String message, String warning, ActionTabWrap.ActionTab tab);
    void onActionError(String errorMessage, ActionTabWrap.ActionTab tab);
}
