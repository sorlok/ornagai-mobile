/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

import java.io.InputStream;

/**
 * Simple callback interface. Perform an action on an open input stream.
 * @author Seth N. Hetu
 */
public interface ProcessAction {
    public abstract void processFile(InputStream file);
}
