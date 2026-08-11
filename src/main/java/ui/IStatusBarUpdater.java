package ui;

public interface IStatusBarUpdater {
    void updateSelectedFiles(long count);
    void updateTotalRows(long count);
    void startStartTime();
    void cleanupStartTime();
    void updateDuration(long seconds);
    void updateDuration();
    String getDurationString();
    void updateMessage(String message);
    void updateProgressBar(int percentage);
    void cleanupProgressBar();
    void updateStatusPanel(int proceeded);
}
