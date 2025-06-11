package settings;

import common.ActionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.ThreadManager;

import java.io.File;

public class SettingsManager {
    private static final Logger logger = LogManager.getLogger(SettingsManager.class);

    private static final boolean AUTO_THREADS = PropertyReader.getPropertyAsBoolean("autoThreads", true);
    private static final int THREAD_COUNT_SCAN = PropertyReader.getPropertyAsInteger("threadCountScan", 1);
    private static final int THREAD_COUNT_COPY = PropertyReader.getPropertyAsInteger("threadCountCopy", 1);
    private static final int THREAD_COUNT_DELETE = PropertyReader.getPropertyAsInteger("threadCountDelete", 1);

    public static final String COPY_FILES_SER = PropertyReader.getPropertyAsString("copyFiles", new File(System.getProperty("user.home"), "copyFiles.ser").getAbsolutePath());
    public static final String DUPLICATE_FILES_SER = PropertyReader.getPropertyAsString("duplicateFiles", new File(System.getProperty("user.home"), "duplicateFiles.ser").getAbsolutePath());
    public static final String WORK_SESSION_ARCH_FOLDER = PropertyReader.getPropertyAsString("workSessionArchFolder", "work_table_arch");

    private final ActionHelper.ActionEnum actionEnum;

    public SettingsManager(ActionHelper.ActionEnum actionEnum) {
        this.actionEnum = actionEnum;
    }

    public ActionHelper.ActionEnum getActionHelperEnum() {
        return actionEnum;
    }

    public int getThreadCount(int countCalcThreads) {
        if (getActionHelperEnum() == null) {
            throw new IllegalArgumentException("Action enum is null");
        }
        int res;
        switch(actionEnum) {
            case SCAN -> res = getThreadCountScan(countCalcThreads);
            case COPY -> res = getThreadCountCopy(countCalcThreads);
            case DELETE -> res = getThreadCountDelete(countCalcThreads);
            default -> throw new IllegalArgumentException("Unknown Action enum: " + actionEnum);
        }

        return res;
    }

    public static int getThreadCountScan(int countCalcThreads) {
        int res = AUTO_THREADS ? countCalcThreads : THREAD_COUNT_SCAN;
        logger.info("getThreadCountScan: {} ", res);
        return res;
    }

    public static int getThreadCountCopy(int countCalcThreads) {
        int res = AUTO_THREADS ? countCalcThreads : THREAD_COUNT_COPY;
        logger.info("getThreadCountCopy: {} ", res);
        return res;
    }

    public static int getThreadCountDelete(int countCalcThreads) {
        int res = AUTO_THREADS ? countCalcThreads : THREAD_COUNT_DELETE;
        logger.info("getThreadCountDelete: {} ", res);
        return res;
    }

    public static int getThreadCountCopy(String path) {
        return getThreadCountCopy(new ThreadManager(path, true).getCountThreads());
    }

    public static int getThreadCountDelete(String path) {
        return getThreadCountDelete(new ThreadManager(path, true).getCountThreads());
    }
}
