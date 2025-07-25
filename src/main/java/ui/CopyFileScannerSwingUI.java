package ui;

import common.ActionTabWrap;
import controller.FileOperationController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static serializable.SerializeManager.*;

public class CopyFileScannerSwingUI extends JFrame {
//    private static final Logger logger = LogManager.getLogger(CopyFileScannerSwingUI.class);
    public static final String SOURCE_DIR_DUPL = "sourceDirDupl";

    private static final JTabbedPane tabbedPane = new JTabbedPane();

    public static final int CHECKBOX_INDEX_COLUMN = 0;
    public static final int FOLDER_INDEX_COLUMN = 1;
    public static final int FILE_INDEX_COLUMN = 2; // show source file and open source file
    public static final int COMMENT_INDEX_COLUMN = 3; // show comment and open dest. file
    // Message status
    public static final String COPIED = "Copied";
    public static final String DELETED = "Deleted";
    // Button name
    static final String COPY_SELECTED = "Copy Selected";
    static final String DELETE_SOURCE = "Delete Source";
    static final String DELETE_DESTINATION = "Delete Destination";

    private static final ActionTabWrap ACTION_TAB = new ActionTabWrap(tabbedPane);
    private static final StatusBarPanel statusBarPanel = new StatusBarPanel(ACTION_TAB);
    private static final ButtonsManager buttonsManager = new ButtonsManager();
    private static FileOperationController fileOperationController;

    public CopyFileScannerSwingUI() {
        setTitle("Copy File Scanner");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        fileOperationController = FileOperationController.getFileOperationController(tabbedPane, buttonsManager, statusBarPanel, ACTION_TAB, this);

        tabbedPane.addTab("Find Copy", fileOperationController.getCopyPanel().createPanel());
        tabbedPane.addTab("Find Duplicates", fileOperationController.getDuplicatePanel().createPanel());

        add(tabbedPane);

        JPanel buttonDeletePanel = new JPanel(new BorderLayout());
        buttonDeletePanel.add(buttonsManager.getDeleteSourceButton(), BorderLayout.WEST);
        buttonDeletePanel.add(buttonsManager.getDeleteDestButton(), BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(buttonsManager.getCopyButton(), BorderLayout.CENTER);
        buttonPanel.add(buttonDeletePanel, BorderLayout.EAST);

        bottomPanel.add(buttonPanel, BorderLayout.NORTH);

        bottomPanel.add(statusBarPanel.getProgressBar(), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBarPanel.refreshStatusBar();
        statusPanel.add(statusBarPanel.getSelectedFilesLabel());
        statusPanel.add(statusBarPanel.getTotalRowsLabel());
        statusPanel.add(statusBarPanel.getDurationLabel());
        statusPanel.add(statusBarPanel.getMessageLabel());

        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);

        registerListeners();
    }

    private void registerListeners() {
        // Add tab change listener
        tabbedPane.addChangeListener(e -> fileOperationController.toggleComponents());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveCopyTableToFile(fileOperationController.getCopyPanel().getJTable());
                saveDuplicateTableToFile(fileOperationController.getDuplicatePanel().getJTable());
            }
        });
    }

    public static FileOperationController getFileOperationController() {
        return fileOperationController;
    }

}