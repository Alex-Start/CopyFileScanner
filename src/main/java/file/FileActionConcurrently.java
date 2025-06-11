package file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FileActionConcurrently {
    private static final Logger logger = LogManager.getLogger(FileActionConcurrently.class);
    private final IFileAction fileAction;

    private final ExecutorService executorService;
    private final AtomicInteger proceededFiles = new AtomicInteger(0);
    private List<String> passedFiles = new ArrayList<>();
    private final Object lock = new Object();
    private volatile boolean isFinished = false;

    public FileActionConcurrently(IFileAction fileAction) {
        this(fileAction, 4);
    }

    public FileActionConcurrently(IFileAction fileAction, int countThreads) {
        this.fileAction = fileAction;
        this.executorService = Executors.newFixedThreadPool(countThreads); // Adjust thread count as needed
    }

    public boolean doActionFiles(Collection<String> filesToDoAction) {
        if (filesToDoAction.isEmpty()) {
            logger.info("No files selected for '{}'.", fileAction.getActionName());
            isFinished = true;
            return false;
        }

        isFinished = false;
        passedFiles = new ArrayList<>();
        logger.info("Starting file '{}' with {} files...", fileAction.getActionName(), filesToDoAction.size());

        AtomicBoolean result = new AtomicBoolean(true);
        for (String relativePath : filesToDoAction) {
            executorService.submit(() -> result.set(result.get() & doActionFile(relativePath)));
        }

        executorService.submit(() -> {
            while(filesToDoAction.size() > getCountProceededFiles()) {
                try {
                    Thread.sleep(1000); // Prevent CPU overuse
                } catch (InterruptedException ignored) {
                }
            }
        });

        shutdownAndAwaitTermination();
        logger.info("'{}' process completed!", fileAction.getActionName());
        isFinished = true;
        return result.get();
    }

    private boolean doActionFile(String relativePath) {
        try {
            fileAction.doAction(relativePath);
            synchronized (lock) {
                passedFiles.add(relativePath);
            }
            return true;
        } catch (IOException e) {
            // error logged in doAction(...)
            return false;
        } finally {
            proceededFiles.incrementAndGet();
        }
    }

    /*public void publish() {
        int total = selectedRows.size();
        while (isWorking()) {
            int proceeded = getCountProceededFiles();
            publish((int) ((proceeded / (double) total) * 100));
            updateStatusPanel(-proceeded);

                        *//*selectedRows.entrySet().stream().parallel()
                                .forEach(entry -> {
                                    if (fileAction.reducePassedList(entry.getValue())) {
                                        markFileAsPassed(entry.getKey(), COPIED);
                                    }
                                });*//*
            List<String> list = fileAction.reducePassedList();
            selectedRows.entrySet().stream().parallel()
                    .filter(entry -> list.contains(entry.getValue()))
                    .forEach(entry -> markFileAsPassed(entry.getKey(), COPIED));
            selectedRows.entrySet().removeIf(entry -> list.contains(entry.getValue()));

            try {
                Thread.sleep(200); // Prevent CPU overuse
            } catch (InterruptedException ignored) {
            }
        }
    }*/

    public boolean isWorking() {
        return !isFinished() || getCountPassedFiles() > 0;
    }
    public boolean isFinished() {
        return isFinished;
    }

    public int getCountPassedFiles() {
        return passedFiles.size();
    }

    public List<String> getPassedFiles() {
        return List.copyOf(passedFiles);
    }

    public int getCountProceededFiles() {
        return proceededFiles.get();
    }

    public List<String> reducePassedList() {
        if(passedFiles.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> passedFilesCopy = List.copyOf(passedFiles);
        passedFilesCopy.forEach(this::reducePassedList);
        return passedFilesCopy;
    }

    public boolean reducePassedList(String relativePath) {
        synchronized (lock) {
            return passedFiles.remove(relativePath);
        }
    }

    private void shutdownAndAwaitTermination() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
