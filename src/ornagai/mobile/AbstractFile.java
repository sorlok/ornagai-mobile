package ornagai.mobile;

import java.io.InputStream;

/**
 * @author Seth N. Hetu
 */
public abstract class AbstractFile {
    protected abstract InputStream getFileAsInputStream();
    protected abstract void closeFile();

    public void openProcessClose(ProcessAction p) {
        p.processFile(getFileAsInputStream());
        closeFile();
    }
}

