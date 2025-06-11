import common.ActionHelper;
import file.*;
import model.FileEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import settings.SettingsManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static serializable.SerializeManager.*;

public class CopyFileScannerGUI extends JFrame {
    private static final Logger logger = LogManager.getLogger(CopyFileScannerGUI.class);
    public static final String SOURCE_DIR_DUPL = "sourceDirDupl";

    private final JTabbedPane tabbedPane;

    private JTextField sourceField, destField, sourceFieldDuplicate;
    private JButton sourceButton, destButton, sourceButtonDuplicate;
    private JButton scanButton, duplicateButton;
    private final JButton copyButton, deleteSourceButton, deleteDestButton;
    private JTable fileTableCopy, fileTableDupl;
    //private DefaultTableModel tableModel;
    private final List<String> filePathsCopy = new ArrayList<>(); // contains relative path for source and dest.
    private final List<String> filePathsDupl = new ArrayList<>(); // contains full path
    private JCheckBox selectAllCheckbox; // 'Select All' checkbox
    private JCheckBox isCheckSource;
    private boolean isFinishedSelectAllAction = true;
    private boolean isCheckBoxActionInTable = false;
    private final JProgressBar progressBar;
    public static final int CHECKBOX_INDEX_COLUMN = 0;
    public static final int FILE_INDEX_COLUMN = 2; // show source file and open source file
    public static final int COMMENT_INDEX_COLUMN = 3; // show comment and open dest. file
    // Message status
    private static final String COPIED = "Copied";
    private static final String DELETED = "Deleted";
    // Button name
    private static final String COPY_SELECTED = "Copy Selected";
    private static final String DELETE_SOURCE = "Delete Source";
    private static final String DELETE_DESTINATION = "Delete Destination";

    private final ActionHelper actionHelper = new ActionHelper(ActionHelper.ActionEnum.COPY);

    private final JLabel selectedFilesLabel;
    private final JLabel totalRowsLabel;
    private final JLabel durationLabel;
    private final JLabel messageLabel;
    private long startTime;
    // List(select, total, duration, progress bar)
    private final Map<ActionHelper.ActionEnum, List<Long>> statusBarData = new HashMap<>();

    private final Preferences prefs = Preferences.userNodeForPackage(CopyFileScannerGUI.class);

    public CopyFileScannerGUI() {
        setTitle("Copy File Scanner");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        // Create tabs
        JPanel findCopyPanel = createFindCopyPanel();
        JPanel findDuplicatesPanel = createFindDuplicatesPanel();

        tabbedPane.addTab("Find Copy", findCopyPanel);
        tabbedPane.addTab("Find Duplicates", findDuplicatesPanel);

        // Add tab change listener
        tabbedPane.addChangeListener(e -> toggleComponents(tabbedPane.getSelectedIndex()));

        statusBarData.put(ActionHelper.ActionEnum.COPY, Arrays.asList(0L,0L,0L,0L));
        statusBarData.put(ActionHelper.ActionEnum.DELETE, Arrays.asList(0L,0L,0L,0L));

        add(tabbedPane);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        // 2 actions for 1 button depends on active tab
        copyButton = new JButton(COPY_SELECTED);
        copyButton.setEnabled(false);
        copyButton.addActionListener(this::copyFiles);
        deleteSourceButton = new JButton(DELETE_SOURCE);
        deleteSourceButton.setEnabled(false);
        deleteSourceButton.addActionListener(this::deleteSourceFiles);
        deleteDestButton = new JButton(DELETE_DESTINATION);
        deleteDestButton.setEnabled(false);
        deleteDestButton.addActionListener(this::deleteDestFiles);

        JPanel buttonDeletePanel = new JPanel(new BorderLayout());
        buttonDeletePanel.add(deleteSourceButton, BorderLayout.WEST);
        buttonDeletePanel.add(deleteDestButton, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(copyButton, BorderLayout.CENTER);
        buttonPanel.add(buttonDeletePanel, BorderLayout.EAST);

        bottomPanel.add(buttonPanel, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        totalRowsLabel = new JLabel("Total: 0");
        selectedFilesLabel = new JLabel("Selected: 0");
        durationLabel = new JLabel("Duration: 0s");
        messageLabel = new JLabel("");
        refreshStatusBar();
        statusPanel.add(selectedFilesLabel);
        statusPanel.add(totalRowsLabel);
        statusPanel.add(durationLabel);
        statusPanel.add(messageLabel);

        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveCopyTableToFile(fileTableCopy, filePathsCopy);
                saveDuplicateTableToFile(fileTableDupl, filePathsDupl);
            }
        });
    }

    public boolean isFinishedSelectAllAction() {
        return isFinishedSelectAllAction;
    }

    public boolean isCheckBoxActionInTable() {
        return isCheckBoxActionInTable;
    }

    public void setCheckBoxActionInTable(boolean checkBoxActionInTable) {
        isCheckBoxActionInTable = checkBoxActionInTable;
    }

    public ActionHelper getActionHelper() {
        return actionHelper;
    }

    String getFullFilePath(int columnIndex, int modelRowIndex) {
        String relativePath = getFilePaths().get(modelRowIndex);
        if (columnIndex < 0) {
            return relativePath;
        }
        return Path.of(getRootPath(columnIndex), relativePath).toString(); // Retrieve full file path
    }

    private String getRootPath(int columnIndex) {
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            return columnIndex == FILE_INDEX_COLUMN ? sourceField.getText() : destField.getText();
        }
        return "";
    }

    private JPanel createFindCopyPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // TOP Panel ----
        JPanel topPanel = new JPanel(new BorderLayout());

        sourceField = new JTextField(20);
        sourceField.setText(prefs.get("sourceDir", ""));

        destField = new JTextField(20);
        destField.setText(prefs.get("destDir", ""));

        sourceButton = new JButton("Select Source");
        destButton = new JButton("Select Destination");

        sourceButton.addActionListener(e -> selectFolder(sourceField, "sourceDir", false));
        destButton.addActionListener(e -> selectFolder(destField, "destDir", false));
        addTextFieldListener(sourceField, "sourceDir");
        addTextFieldListener(destField, "destDir");

        // Source Panel -> TOP Panel ----
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.X_AXIS)); // Align components in a row
        sourcePanel.add(sourceField, BorderLayout.CENTER);
        sourcePanel.add(sourceButton, BorderLayout.EAST);
        // Dest Panel -> TOP Panel ----
        JPanel destPanel = new JPanel(new BorderLayout());
        destPanel.setLayout(new BoxLayout(destPanel, BoxLayout.X_AXIS));
        destPanel.add(destField, BorderLayout.CENTER);
        destPanel.add(destButton, BorderLayout.EAST);

        topPanel.add(sourcePanel, BorderLayout.NORTH);
        topPanel.add(destPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        // Bottom Panel ----
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // Scan Panel -> Bottom Panel ----
        JPanel scanPanel = new JPanel(new BorderLayout());
        // 'Select All' Checkbox
        selectAllCheckbox = new JCheckBox("Select All");
        selectAllCheckbox.setEnabled(false); // Disabled initially, enabled after scanning
        // 'Check Source' Checkbox
        isCheckSource = new JCheckBox("Check Source");
        isCheckSource.setEnabled(true);

        fileTableCopy = new TableFactory(this).createTable();
        loadCopyTableFromFile(fileTableCopy, filePathsCopy);

        // Add event listener for 'Select All'
        selectAllCheckbox.addItemListener(e -> {
            if (!getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
                return;
            }
            if (isCheckBoxActionInTable) {
                return;
            }
            isFinishedSelectAllAction = false;
            boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
            int count = 0;
            TableModel tableModel = fileTableCopy.getModel();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (isSkipCheckBoxCondition(tableModel, i)) {
                    continue;
                }
                if (isSelected) {
                    count++;
                }
                tableModel.setValueAt(isSelected, i, CHECKBOX_INDEX_COLUMN); // Check/uncheck all rows
            }
            copyButton.setEnabled(isSelected);
            deleteSourceButton.setEnabled(isSelected);
            deleteDestButton.setEnabled(isSelected);
            isFinishedSelectAllAction = true;
            setAndRefreshSelect(count);
        });

        scanButton = new JButton("Scan Differences");
        scanButton.setEnabled(! sourceField.getText().isEmpty() && ! destField.getText().isEmpty());

        scanPanel.add(scanButton, BorderLayout.NORTH);
        scanPanel.add(selectAllCheckbox, BorderLayout.CENTER);
        scanPanel.add(isCheckSource, BorderLayout.EAST);

        bottomPanel.add(scanPanel, BorderLayout.NORTH);
        bottomPanel.add(new JScrollPane(fileTableCopy), BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.CENTER);

        scanButton.addActionListener(this::scanDifferences);

        return panel;
    }

    private JPanel createFindDuplicatesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        // TOP Panel ----
        JPanel topPanel = new JPanel(new BorderLayout());
        // Source Panel -> TOP Panel ----
        JPanel sourcePanel = new JPanel(new BorderLayout());
        sourceFieldDuplicate = new JTextField(20);
        sourceFieldDuplicate.setText(prefs.get(SOURCE_DIR_DUPL, ""));

        sourceButtonDuplicate = new JButton("Select Source");

        sourceButtonDuplicate.addActionListener(e -> selectFolder(sourceFieldDuplicate, SOURCE_DIR_DUPL, true));
        addTextFieldListener(sourceFieldDuplicate, SOURCE_DIR_DUPL);

        duplicateButton = new JButton("Find Duplicates");
        duplicateButton.setEnabled(! sourceFieldDuplicate.getText().isEmpty());
        duplicateButton.addActionListener(this::scanDuplicates);

        sourcePanel.setLayout(new BoxLayout(sourcePanel, BoxLayout.X_AXIS)); // Align components in a row
        sourcePanel.add(sourceFieldDuplicate, BorderLayout.CENTER);
        sourcePanel.add(sourceButtonDuplicate, BorderLayout.EAST);

        topPanel.add(sourcePanel, BorderLayout.NORTH);
        topPanel.add(duplicateButton, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);

        // Bottom Panel ----
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(duplicateButton, BorderLayout.NORTH);
        fileTableDupl = new TableFactory(this).createTable();
        loadDuplicateTableFromFile(fileTableDupl, filePathsDupl);

        bottomPanel.add(new JScrollPane(fileTableDupl), BorderLayout.CENTER);

        panel.add(bottomPanel, BorderLayout.CENTER);

        return panel;
    }

    private void addTextFieldListener(JTextField textField, String key) {
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                checkPathAndShowError(textField.getText(), key);
                prefs.put(key, textField.getText());
            }
        });

        textField.addActionListener(e -> {
            // Triggered when Enter is pressed
            checkPathAndShowError(textField.getText(), key);
            prefs.put(key, textField.getText());
        });
    }

    private void checkPathAndShowError(String path, String key) {
        String[] paths = new String[]{path};
        //multiple folders only for duplicates case
        if(SOURCE_DIR_DUPL.equals(key)) {
            paths = path.split(DuplicateFileScanner.DELIM);
        }
        if(! checkPath(paths)) {
            JOptionPane.showMessageDialog(this, "Invalid folder path: "+ path);
        }
    }

    private boolean checkPath(String[] paths) {
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return false;
            }
        }
        return true;
    }

    private void toggleComponents(int tabIndex) {
        boolean isFindCopy = (tabIndex == 0);

        if (isFindCopy) {
            getActionHelper().setActionName(ActionHelper.ActionEnum.COPY);
        } else {
            getActionHelper().setActionName(ActionHelper.ActionEnum.DELETE);
        }

        checkCheckBoxes();
        refreshStatusBar();
    }

    void checkCheckBoxes() {
        boolean allChecked = true;
        boolean anyChecked = false;
        int count = 0;
        TableModel tableModel = getTableModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (isSkipCheckBoxCondition(tableModel, i)) {
                continue;
            }
            boolean checked = (Boolean) tableModel.getValueAt(i, CHECKBOX_INDEX_COLUMN);
            if (checked) {
                count++;
            }
            anyChecked |= checked;
            allChecked &= checked;
        }
        setSelectAllCheckbox(allChecked);
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            copyButton.setEnabled(anyChecked);
            deleteDestButton.setEnabled(anyChecked);
        } else {
            copyButton.setEnabled(false);
            deleteDestButton.setEnabled(false);
        }
        deleteSourceButton.setEnabled(anyChecked);
        setAndRefreshSelect(count);
    }

    private void setSelectAllCheckbox(boolean value) {
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            selectAllCheckbox.setSelected(value);
        }
    }

    private TableModel getTableModel() {
        return getTable().getModel();
    }

    JTable getTable() {
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            return fileTableCopy;
        }
        return fileTableDupl;
    }

    private void selectFolder(JTextField field, String key, boolean multiple) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(multiple); // Allow multiple selection

        int option = chooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            String selectedPath = "";
            if (multiple) {
                File[] selectedFolders = chooser.getSelectedFiles();
                List<String> folderPaths = new ArrayList<>();

                for (File folder : selectedFolders) {
                    folderPaths.add(folder.getAbsolutePath());
                }

                selectedPath = String.join(DuplicateFileScanner.DELIM, folderPaths);
            } else {
                selectedPath = chooser.getSelectedFile().getAbsolutePath();
            }
            field.setText(selectedPath);
            prefs.put(key, selectedPath); // Save choice
        }
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            scanButton.setEnabled(!sourceField.getText().isEmpty() && !destField.getText().isEmpty());
        } else {
            duplicateButton.setEnabled(!sourceFieldDuplicate.getText().isEmpty());
        }
    }

    private void scanDifferences(ActionEvent e) {
        String sourceDir = sourceField.getText();
        String destDir = destField.getText();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select both directories.");
            return;
        }

        // Disable scan button while scanning
        disableButtonsAndClearTable();

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> messageLabel.setText("Start scan differences..."));
            AtomicBoolean scanningFinished = new AtomicBoolean(false);
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            AtomicReference<Map<String, String>> differences = new AtomicReference<>();
            executorService.submit(() -> {
                logger.debug("Started scanning...");
                differences.set(CopyFileScanner.scanAndCompare(sourceDir, destDir, isCheckSource.isSelected()));
                logger.debug("Scan is finished.");
                scanningFinished.set(true);
            });
            executorService.submit(() -> upDownProgressBar(scanningFinished, progressBar));
            executorService.submit(() -> {
                while(!scanningFinished.get()) {
                    updateDuration();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            executorService.shutdown();

            // Wait until scanningFinished becomes true
            while (!scanningFinished.get()) {
                try {
                    Thread.sleep(500); // or shorter if needed
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for scanning to finish.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SwingUtilities.invokeLater(() -> {
                logger.debug("Add {} differences in table...", differences.get().size());
                messageLabel.setText("Add "+ differences.get().size() +" differences in table...");
                progressBar.setValue(0);
                DefaultTableModel tableModel = (DefaultTableModel) getTableModel();
                tableModel.setRowCount(0);
                filePathsCopy.clear();
                for (Map.Entry<String, String> entry : differences.get().entrySet()) {
                    progressBar.setValue((int) ((tableModel.getRowCount() / (double) differences.get().size()) * 100));
                    Path path = Path.of(entry.getKey());
                    String folder = path.getParent() == null ? "" : path.getParent().toString();
                    tableModel.addRow(new FileEntry(false, folder, path.getFileName().toString(), entry.getValue()).getRowData());
                    filePathsCopy.add(entry.getKey());
                }

                // **Reset sorting to avoid index mismatches**
                TableRowSorter<?> sorter = (TableRowSorter<?>) fileTableCopy.getRowSorter();
                sorter.setSortKeys(null); // **Reset sorting to natural order**

                messageLabel.setText("Added "+ differences.get().size() +" differences in table (duration: "+ getDurationString() +")");

                // **Re-enable buttons after scan**
                enablePanelAndButtons(!filePathsCopy.isEmpty());
                updateStatusPanel(filePathsCopy.size());
            });
            setAndRefreshProgressBar(100);
        }).start();

        SwingUtilities.invokeLater(() -> messageLabel.setText("End scan differences..."));
    }

    private void scanDuplicates(ActionEvent e) {
        String sourceDir = sourceFieldDuplicate.getText();
        if (sourceDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select source directory.");
            return;
        }

        // Disable scan button while scanning
        disableButtonsAndClearTable();

        new Thread(() -> {
            SwingUtilities.invokeLater(() -> messageLabel.setText("Start scan duplicates..."));
            AtomicBoolean scanningFinished = new AtomicBoolean(false);
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            AtomicReference<Map<String, List<String>>> duplicates = new AtomicReference<>();
            executorService.submit(() -> {
//                long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
//                logger.debug("Used memory: {} MB", usedMem);
                logger.debug("Started scanning...");
                duplicates.set(DuplicateFileScanner.findDuplicateFiles(sourceDir));
                logger.debug("Scan is finished.");
//                usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
//                logger.debug("Used memory: {} MB", usedMem);
                scanningFinished.set(true);
            });
            executorService.submit(() -> upDownProgressBar(scanningFinished, progressBar));
            executorService.submit(() -> {
                while(!scanningFinished.get()) {
                    updateDuration();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            executorService.shutdown();

            // Wait until scanningFinished becomes true
            while (!scanningFinished.get()) {
                try {
                    Thread.sleep(500); // or shorter if needed
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for scanning to finish.");
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            SwingUtilities.invokeLater(() -> {
                logger.debug("Add {} duplicates in table...", duplicates.get().size());
                messageLabel.setText("Add "+ duplicates.get().size() +" duplicates in table...");
                progressBar.setValue(0);
                DefaultTableModel tableModel = (DefaultTableModel) getTableModel();
                tableModel.setRowCount(0);
                filePathsDupl.clear();

                int i = 0;
                for (Map.Entry<String, List<String>> entry : duplicates.get().entrySet()) {
                    progressBar.setValue((int) ((tableModel.getRowCount() / (double) duplicates.get().size()) * 100));
                    i++;
                    for (String destPath : entry.getValue()) {
                        Path path = Path.of(destPath);
                        String folder = path.getParent() == null ? "" : path.getParent().toString();
                        tableModel.addRow(new FileEntry(false, folder, path.getFileName().toString(), "# "+ i).getRowData());
                        filePathsDupl.add(destPath);
                    }
                }

                // **Reset sorting to avoid index mismatches**
                TableRowSorter<?> sorter = (TableRowSorter<?>) fileTableDupl.getRowSorter();
                sorter.setSortKeys(null); // **Reset sorting to natural order**

                messageLabel.setText("Added "+ duplicates.get().size() +" duplicates in table (duration: "+ getDurationString()+")");

                // **Re-enable buttons after scan**
                enablePanelAndButtons(false);
                updateStatusPanel(filePathsDupl.size());

            });
            setAndRefreshProgressBar(100);
        }).start();

        SwingUtilities.invokeLater(() -> messageLabel.setText("End scan duplicates..."));
    }

    private void upDownProgressBar(AtomicBoolean isFinished, JProgressBar progressBar) {
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

    private void copyFiles(ActionEvent e) {
        String sourceDir = sourceField.getText();
        String destDir = destField.getText();
        if (sourceDir.isEmpty() || destDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select both directories.");
            return;
        }

        Map<Integer, String> selectedRows  = getSelectedRows();
        if (selectedRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for copying.");
            return;
        }

        disableButtonsCleanUpProgressBar();
        IFileAction fileCopyAction = new FileCopier(sourceDir, destDir);
        SwingWorker<Void, Integer> worker = createWorker(fileCopyAction, SettingsManager.getThreadCountCopy(destDir)
                , COPIED, selectedRows);

        worker.execute();
    }

    private void deleteSourceFiles(ActionEvent e) {
        deleteFiles(FILE_INDEX_COLUMN);
    }

    private void deleteDestFiles(ActionEvent e) {
        deleteFiles(COMMENT_INDEX_COLUMN);
    }

    private void deleteFiles(int columnIndex) {
        String deleteMsg = (columnIndex == FILE_INDEX_COLUMN ? "Source " : "Dest ") + DELETED;
        Map<Integer, String> selectedRows  = getSelectedRows(columnIndex);
        if (selectedRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for deleting.");
            return;
        }

        disableButtonsCleanUpProgressBar();
        IFileAction fileDeleteAction = new FileDeleter();
        // count of threads depends on all values in selectedRows. IF there are various disks from different physical HDD/SDD we can delete it in parallel.
        SwingWorker<Void, Integer> worker = createWorker(fileDeleteAction, SettingsManager.getThreadCountDelete(selectedRows.values().iterator().next())
                , deleteMsg, selectedRows);

        worker.execute();
    }

    private SwingWorker<Void, Integer> createWorker(IFileAction ifileAction, int countTreads, String actionName, Map<Integer, String> selectedRows) {
        return new SwingWorker<>() {
            private String warnMess = "";
            @Override
            protected Void doInBackground() {
                AtomicBoolean isPassed = new AtomicBoolean(false);
                FileActionConcurrently fileAction = new FileActionConcurrently(ifileAction, countTreads);

                ExecutorService executor = Executors.newFixedThreadPool(2);
                List<Callable<Void>> tasks = List.of(
                        () -> { isPassed.set(fileAction.doActionFiles(selectedRows.values())); return null; },
                        () -> { trackProgress(fileAction, selectedRows); return null; }
                );

                try {
                    executor.invokeAll(tasks);  // Run both tasks concurrently
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executor.shutdown();
                }

                warnMess = !isPassed.get() ? "\nThere are some error(s)" : "";
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                progressBar.setValue(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                setAndRefreshProgressBar(100);
                enablePanelAndButtons();
                checkCheckBoxes();
                JOptionPane.showMessageDialog(CopyFileScannerGUI.this, actionName +" is completed."+ warnMess);
            }

            private void trackProgress(FileActionConcurrently fileAction, Map<Integer, String> selectedRows) {
                int total = selectedRows.size();

                while (fileAction.isWorking()) {
                    int proceeded = fileAction.getCountProceededFiles();
                    publish((int) ((proceeded / (double) total) * 100));

                    /*selectedRows.entrySet().stream().parallel()
                                .forEach(entry -> {
                                    if (fileAction.reducePassedList(entry.getValue())) {
                                        markFileAsPassed(entry.getKey(), actionName);
                                    }
                                });*/
                    List<String> list = fileAction.reducePassedList();
                    selectedRows.entrySet().stream()
                            .filter(entry -> list.contains(entry.getValue()))
                            .forEach(entry -> markFileAsPassed(entry.getKey(), actionName));
                    selectedRows.entrySet().removeIf(entry -> list.contains(entry.getValue()));
                    updateStatusPanel(-list.size());

                    try {
                        Thread.sleep(200); // Prevent CPU overuse
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
    }

    private void disableButtonsAndClearTable() {
        DefaultTableModel tableModel = (DefaultTableModel) getTableModel();
        tableModel.setRowCount(0); // **Clear old data**
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            filePathsCopy.clear(); // **Clear old file paths**
            selectAllCheckbox.setSelected(false);
        } else {
            filePathsDupl.clear();
        }

        disableButtonsCleanUpProgressBar();
        updateTotalLabel(0);
    }

    private void disableButtonsCleanUpProgressBar() {
        disablePanelAndButtons();
        setAndRefreshProgressBar(0);
        cleanupStartTime();
    }

    private void cleanupStartTime() {
        startTime = System.currentTimeMillis(); // Record start time
        setAndRefreshDuration(0);
    }

    private void updateStatusPanel(int proceeded) {
        updateDuration();
        if (proceeded != 0) {
            updateTotalLabel(proceeded);
        }
    }

    private void updateDuration() {
        setAndRefreshDuration(getDurationSec());
    }

    private long getDurationSec() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    private String getDurationString() {
        Date date = new Date(System.currentTimeMillis() - startTime);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    private void updateTotalLabel(int value) {
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

    private void setAndRefreshSelect(long value) {
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

    private void setAndRefreshProgressBar(long value) {
        setStatusBarData(3, value);
        refreshStatusBar(3, value);
    }

    private long getStatusBarValue(int index) {
        List<Long> list = statusBarData.get(getActionHelper().getActionName());
        return list.get(index);
    }

    private void setStatusBarData(int index, long value) {
        List<Long> list = statusBarData.get(getActionHelper().getActionName());
        list.set(index, value);
    }

    private void enablePanelAndButtons() {
        enablePanelAndButtons(true);
    }

    private void enablePanelAndButtons(boolean enableSelectAllCheckbox) {
        tabbedPane.setEnabled(true);
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            scanButton.setEnabled(true);
            scanButton.repaint();
            selectAllCheckbox.setEnabled(enableSelectAllCheckbox);
            isCheckSource.setEnabled(true);
            isCheckSource.repaint();
        } else {
            duplicateButton.setEnabled(true);
            duplicateButton.repaint();
        }

        checkCheckBoxes();
        setAndRefreshProgressBar(100);
    }

    private void disablePanelAndButtons() {
        tabbedPane.setEnabled(false);
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            // Disable scan button while scanning
            scanButton.setEnabled(false);
            scanButton.repaint();
            selectAllCheckbox.setEnabled(false);
            selectAllCheckbox.repaint();
            isCheckSource.setEnabled(false);
            isCheckSource.repaint();
        } else {
            duplicateButton.setEnabled(false);
            duplicateButton.repaint();
        }
        copyButton.setEnabled(false);
        copyButton.repaint();
        deleteSourceButton.setEnabled(false);
        deleteSourceButton.repaint();
        deleteDestButton.setEnabled(false);
        deleteDestButton.repaint();
    }

    /**
     * Delete checkboxes for copied/deleted
     * @param rowIndex
     * @param value - COPIED/DELETED values for the comment
     */
    private void markFileAsPassed(int rowIndex, String value) {
        SwingUtilities.invokeLater(() -> {
            JTable table = getTable();
            DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
            if (rowIndex < tableModel.getRowCount()) {
                tableModel.setValueAt(false, rowIndex, CHECKBOX_INDEX_COLUMN); // Remove checkbox
                String newValue = value;
                if (getActionHelper().isEqual(ActionHelper.ActionEnum.DELETE)) {
                    newValue = tableModel.getValueAt(rowIndex, COMMENT_INDEX_COLUMN) + " - "+ newValue;
                }
                tableModel.setValueAt(newValue, rowIndex, COMMENT_INDEX_COLUMN); // Mark as passed
                checkCheckBoxes(); // Update 'Select All' and 'Copy/Delete' button state
                table.repaint(); // Refresh table display
            }
        });
    }

    /**
     * Get map of selected rows
     * @return - rowIndex, path - short relative path for COPY, and full path for DUPLICATE.
     */
    private Map<Integer, String> getSelectedRows() {
        return getSelectedRows(-1);
    }
    private Map<Integer, String> getSelectedRows(int columnIndex) {
        Map<Integer, String> selectedRows  = new HashMap<>();
        JTable table = getTable();
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            int modelRowIndex = table.convertRowIndexToModel(i); // Convert to model index
            if ((Boolean) tableModel.getValueAt(modelRowIndex, CHECKBOX_INDEX_COLUMN)) {
                selectedRows.put(modelRowIndex, getFullFilePath(columnIndex, modelRowIndex));
            }
        }

        return selectedRows;
    }

    List<String> getFilePaths() {
        if (getActionHelper().isEqual(ActionHelper.ActionEnum.COPY)) {
            return filePathsCopy;
        }
        return filePathsDupl;
    }

    static class CenteredCheckboxRenderer extends JCheckBox implements TableCellRenderer {
        public CenteredCheckboxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER); // Center the checkbox
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSkipCheckBoxCondition(table, row)) {
                return new JLabel(""); // Hide checkbox if file is copied
            }
            setSelected(Boolean.TRUE.equals(value)); // Set checkbox state
            return this;
        }
    }

    private static boolean isSkipCheckBoxCondition(JTable table, int row) {
        return isSkipCheckBoxCondition(table.getModel(), table.convertRowIndexToModel(row));
    }

    static boolean isSkipCheckBoxCondition(TableModel table, int row) {
        if (COPIED.equals(table.getValueAt(row, COMMENT_INDEX_COLUMN))) {
            return true;
        }
        if (table.getValueAt(row, COMMENT_INDEX_COLUMN).toString().endsWith(DELETED)) {
            return true;
        }

        return false;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CopyFileScannerGUI::new);
    }
}