/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile.io;

import java.io.*;

/**
 * Represents a files located within the JAR of the program itself.
 *
 * @author Seth N. Hetu
 */
public class JarredFile extends AbstractFile {
    private String resRoot;
    private String resourceName;
    private InputStream currFile;
    public JarredFile(String resourceRoot){
        this.resRoot = resourceRoot;
    }

    public boolean exists(String resourceName) {
        InputStream check = this.getClass().getResourceAsStream(resRoot + "/" + resourceName);
        if (check!=null) {
            try {
                check.close();
            } catch (IOException ex) {}
        }
        return (check!=null);
    }

    protected InputStream getFileAsInputStream(String resourceName) {
        this.resourceName = resourceName;
         this.currFile = this.getClass().getResourceAsStream(resRoot + "/" + resourceName);
         return currFile;
    }

    protected void closeFile() {
        try {
            currFile.close();
        } catch (IOException ex) {
            throw new RuntimeException("Error closing file: " + resourceName);
        }
    }
}
