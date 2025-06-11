package file;

import java.io.IOException;

public interface IFileAction {
    String getActionName();
    void doAction(String relativePath) throws IOException;
}
