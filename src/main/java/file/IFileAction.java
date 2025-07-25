package file;

import java.io.IOException;

public interface IFileAction {
    String getActionName();
    String getProceededName();
    boolean doAction(String relativePath) throws IOException;
}
