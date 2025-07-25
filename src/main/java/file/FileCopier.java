package file;

import java.io.IOException;
import java.nio.file.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.FileUtils;

import static ui.CopyFileScannerSwingUI.COPIED;

public class FileCopier implements IFileAction {
    private static final Logger logger = LogManager.getLogger(FileCopier.class);
    private final Path sourceRoot;
    private final Path destRoot;

    public FileCopier(String sourceDir, String destDir) {
        this.sourceRoot = Paths.get(sourceDir);
        this.destRoot = Paths.get(destDir);
    }

    @Override
    public String getActionName() {
        return "COPY";
    }

    @Override
    public String getProceededName() {
        return COPIED;
    }

    public boolean doAction(String relativePath) throws IOException {
        Path sourceFile = sourceRoot.resolve(relativePath);
        Path destFile = destRoot.resolve(relativePath);
        boolean result = false;
        try {
            result = FileUtils.copySingleFile(sourceFile, destFile);
            logger.info("Copied: {} -> {}", sourceFile, destFile);
        } catch (IOException e) {
            logger.error("Error copying file: {} -> {}", sourceFile, destFile, e);
            throw new IOException(e);
        }
        return result;
    }
}