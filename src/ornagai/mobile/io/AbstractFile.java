/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile.io;

import ornagai.mobile.*;
import java.io.InputStream;

/**
 * A simple file abstraction that allows a ProcessAction to be performed on
 *  a single InputStream. Files are opened and closed automatically to avoid
 *  potential dangling handles.
 *
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

