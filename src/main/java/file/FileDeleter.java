package file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDeleter implements IFileAction {
    private static final Logger logger = LogManager.getLogger(FileDeleter.class);

    public FileDeleter() {
    }

    @Override
    public String getActionName() {
        return "DELETE";
    }

    @Override
    public void doAction(String relativePath) throws IOException {
        Path sourceFile = Paths.get(relativePath);
        if(FileUtils.deleteFile(sourceFile.toString())) {
            logger.info("Deleted: {}", relativePath);
            FileUtils.deleteEmptyFolder(sourceFile.getParent().toString());
            return;
        }

        throw new IOException("Error for deleting file: "+ relativePath);
    }
}
