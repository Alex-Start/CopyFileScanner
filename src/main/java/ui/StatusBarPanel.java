package ui;

import common.ActionTab;

import javax.swing.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatusBarPanel {
    private final JProgressBar progressBar;
    private final JLabel selectedFilesLabel;
    private final JLabel totalRowsLabel;
    private final JLabel durationLabel;
    private final JLabel messageLabel;
    private long startTime = System.currentTimeMillis();
    // List(select, total, duration, progress bar)
    private final Map<ActionTab.Tab, List<Long>> statusBarData = new HashMap<>();
    private final ActionTab actionTab;
    private ActionTab.Tab setForTab;//null - set for current active tab

    public StatusBarPanel(ActionTab actionTab) {
        this.actionTab = actionTab;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        statusBarData.put(ActionTab.Tab.COPY, Arrays.asList(0L,0L,0L,0L));
        statusBarData.put(ActionTab.Tab.DUPLICATE, Arrays.asList(0L,0L,0L,0L));
        totalRowsLabel = new JLabel("Total: 0");
        selectedFilesLabel = new JLabel("Selected: 0");
        durationLabel = new JLabel("Duration: 0s");
        messageLabel = new JLabel("");
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public JLabel getSelectedFilesLabel() {
        return selectedFilesLabel;
    }

    public JLabel getTotalRowsLabel() {
        return totalRowsLabel;
    }

    public JLabel getDurationLabel() {
        return durationLabel;
    }

    public JLabel getMessageLabel() {
        return messageLabel;
    }

    public void cleanupStartTime() {
        startStartTime(); // Record start time
        setAndRefreshDuration(0);
    }

    public void startStartTime() {
        startTime = System.currentTimeMillis();
    }

    public StatusBarPanel forCopy() {
        setForTab = ActionTab.Tab.COPY;
        return this;
    }

    public StatusBarPanel forDuplicate() {
        setForTab = ActionTab.Tab.DUPLICATE;
        return this;
    }

    public void updateStatusPanel(int proceeded) {
        updateDuration();
        if (proceeded != 0) {
            updateTotalLabel(proceeded);
        }
        setForTab = null;
    }

    public void updateDuration() {
        setAndRefreshDuration(getDurationSec());
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

    public void updateTotalLabel(int value) {
        if (value < 0) {
            // TODO index as const/enum/etc.
            value = (int) (getStatusBarValue(1)+value);
        }
        if (value < 0) {
            value = 0;
        }
        setAndRefreshTotal(value);
    }

    void refreshStatusBar() {
        // TODO index as const/enum/etc.
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
            }//TODO add messageLabel
        });
    }

    public void setAndRefreshSelect(long value) {
        setStatusBarData(0, value);
        refreshStatusBar(0, value);
    }

    private void setAndRefreshTotal(long value) {
        setStatusBarData(1, value);
        refreshStatusBar(1, value);
    }

    private void setAndRefreshDuration(long value) {
        setStatusBarData(2, value);
        refreshStatusBar(2, value);
    }

    public void setAndRefreshProgressBar(long value) {
        setStatusBarData(3, value);
        refreshStatusBar(3, value);
    }

    private long getStatusBarValue(int index) {
        List<Long> list = statusBarData.get(actionTab.getActionName());
        return list.get(index);
    }

    private void setStatusBarData(int index, long value) {
        ActionTab.Tab tab = actionTab.getActionName();
        if (ActionTab.Tab.COPY.equals(setForTab)) {
            tab = ActionTab.Tab.COPY;
        }
        if (ActionTab.Tab.DUPLICATE.equals(setForTab)) {
            tab = ActionTab.Tab.DUPLICATE;
        }
        List<Long> list = statusBarData.get(tab);
        list.set(index, value);
    }

    public void upDownProgressBar(AtomicBoolean isFinished) {
        int i = 0;
        boolean plus = true;
        while (!isFinished.get()) {
            progressBar.setValue(i);
            if (plus) {
                i++;
            } else {
                i--;
            }
            if (i >= 100 || i <= 0) {
                plus = ! plus;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
