package ornagai.mobile;

import java.io.InputStream;

/**
 * @author Seth N. Hetu
 */
public abstract class AbstractFile {
    protected abstract InputStream getFileAsInputStream(String resourceName);
    public abstract boolean exists(String resourceName);
    protected abstract void closeFile();

    public void openProcessClose(String resourceName, ProcessAction p) {
        p.processFile(getFileAsInputStream(resourceName));
        closeFile();
    }
}

