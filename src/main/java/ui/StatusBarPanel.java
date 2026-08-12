package ui;

import common.ActionTabWrap;
import javax.swing.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusBarPanel implements IStatusBarUpdater {
    private final JProgressBar progressBar;
    private final JLabel selectedFilesLabel;
    private final JLabel totalRowsLabel;
    private final JLabel durationLabel;
    private final JLabel messageLabel;
    private long startTime = System.currentTimeMillis();

    private final Map<ActionTabWrap.ActionTab, List<Long>> statusBarData = new HashMap<>();
    private final ActionTabWrap actionTabWrap;
    private ActionTabWrap.ActionTab setForActionTab;

    public StatusBarPanel(ActionTabWrap actionTabWrap) {
        this.actionTabWrap = actionTabWrap;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusBarData.put(ActionTabWrap.ActionTab.COPY, Arrays.asList(0L, 0L, 0L, 0L));
        statusBarData.put(ActionTabWrap.ActionTab.DUPLICATE, Arrays.asList(0L, 0L, 0L, 0L));
        totalRowsLabel = new JLabel("Total: 0");
        selectedFilesLabel = new JLabel("Selected: 0");
        durationLabel = new JLabel("Duration: 0s");
        messageLabel = new JLabel("");
    }

    public JProgressBar getProgressBar() { return progressBar; }
    public JLabel getSelectedFilesLabel() { return selectedFilesLabel; }
    public JLabel getTotalRowsLabel() { return totalRowsLabel; }
    public JLabel getDurationLabel() { return durationLabel; }
    public JLabel getMessageLabel() { return messageLabel; }

    public void cleanupStartTime() {
        startStartTime();
        updateDuration(0);
    }

    public void startStartTime() {
        startTime = System.currentTimeMillis();
    }

    public StatusBarPanel forCopy() {
        setForActionTab = ActionTabWrap.ActionTab.COPY;
        return this;
    }

    public StatusBarPanel forDuplicate() {
        setForActionTab = ActionTabWrap.ActionTab.DUPLICATE;
        return this;
    }

    public void updateStatusPanel(int proceeded) {
        updateDuration();
        if (proceeded != 0) {
            updateTotalLabel(proceeded);
        }
        setForActionTab = null;
    }

    public void updateDuration() {
        updateDuration(getDurationSec());
    }

    private long getDurationSec() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public String getDurationString() {
        Date date = new Date(System.currentTimeMillis() - startTime);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    @Override
    public void updateMessage(String message) {
        SwingUtilities.invokeLater(() -> messageLabel.setText(message));
    }

    @Override
    public void updateProgressBar(int percentage) {
        setStatusBarData(3, percentage);
        refreshStatusBar(3, percentage);
    }

    public void updateTotalLabel(int value) {
        if (value < 0) {
            value = (int) (getStatusBarValue(1) + value);
        }
        if (value < 0) {
            value = 0;
        }
        updateTotalRows(value);
    }

    public void refreshStatusBar() {
        refreshStatusBar(0);
        refreshStatusBar(1);
        refreshStatusBar(2);
        refreshStatusBar(3);
    }

    private void refreshStatusBar(int index) {
        refreshStatusBar(index, getStatusBarValue(index));
    }

    private void refreshStatusBar(int index, long value) {
        SwingUtilities.invokeLater(() -> {
            switch (index) {
                case 0 -> {
                    selectedFilesLabel.setText("Selected: " + value);
                    selectedFilesLabel.repaint();
                }
                case 1 -> {
                    totalRowsLabel.setText("Total: " + value);
                    totalRowsLabel.repaint();
                }
                case 2 -> {
                    durationLabel.setText("Duration: " + value + "s");
                    durationLabel.repaint();
                }
                case 3 -> {
                    progressBar.setValue((int) value);
                    progressBar.repaint();
                }
            }
        });
    }

    public void updateSelectedFiles(long value) {
        setStatusBarData(0, value);
        refreshStatusBar(0, value);
    }

    public void updateTotalRows(long value) {
        setStatusBarData(1, value);
        refreshStatusBar(1, value);
    }

    public void updateDuration(long value) {
        setStatusBarData(2, value);
        refreshStatusBar(2, value);
    }

    public void cleanupProgressBar() {
        updateProgressBar(0);
    }

    private long getStatusBarValue(int index) {
        List<Long> list = statusBarData.get(actionTabWrap.getActionName());
        return list != null ? list.get(index) : 0L;
    }

    private void setStatusBarData(int index, long value) {
        ActionTabWrap.ActionTab actionTab = actionTabWrap.getActionName();
        if (ActionTabWrap.ActionTab.COPY.equals(setForActionTab)) {
            actionTab = ActionTabWrap.ActionTab.COPY;
        }
        if (ActionTabWrap.ActionTab.DUPLICATE.equals(setForActionTab)) {
            actionTab = ActionTabWrap.ActionTab.DUPLICATE;
        }
        List<Long> list = statusBarData.get(actionTab);
        if (list != null) {
            list.set(index, value);
        }
    }

    public void upDownProgressBar(AtomicBoolean isFinished) {
        int i = 0;
        boolean plus = true;
        while (!isFinished.get()) {
            final int val = i;
            SwingUtilities.invokeLater(() -> progressBar.setValue(val));
            if (plus) {
                i++;
            } else {
                i--;
            }
            if (i >= 100 || i <= 0) {
                plus = !plus;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
