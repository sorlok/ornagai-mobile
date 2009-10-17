package ornagai.mobile.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;
import ornagai.mobile.filebrowser.FileChooser;

/**
 *
 * @author Seth N. Hetu
 */
public class ZippedFile extends AbstractFile {
    private String pathName;
    private boolean valid;
    private InputStream currFile;
    private Object[] currFC = new Object[1];
    private Vector fileNames = new Vector(); //String

    public ZippedFile(String path) {
        this.pathName = path;
        this.valid = true;
        currFC[0] = null;
        ZipInputStream zin = (ZipInputStream)FileChooser.GetZipFile(currFC, pathName);

        if (zin==null)
            valid = false;
        else {
            ZipEntry ze = null;
            try {
                while ((ze = zin.getNextEntry()) != null) {
                    fileNames.addElement(ze.getName());
                    System.out.println("Zip file contains: " + ze.getName());
                }
            } catch (IOException ex) {
                this.valid = false;
                System.out.println("ERROR: " + ex.toString());
                ex.printStackTrace();
            }

            try {
                zin.close();
                FileChooser.CloseFC(currFC);
            } catch (IOException ex) {
                System.out.println("Error: " + ex.toString());
            }
        }
    }

    public boolean isValid() {
        return valid;
    }

    public boolean exists(String resourceName) {
        for (int i=0; i<fileNames.size(); i++) {
            if (((String)fileNames.elementAt(i)).equals(resourceName))
                return true;
        }
        return false;
    }


    protected InputStream getFileAsInputStream(String resourceName) {
        try {
            Object[] fc = new Object[1];
            ZipInputStream zin = (ZipInputStream)FileChooser.GetZipFile(fc, pathName);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().equals(resourceName)) {
                    this.currFile = zin;
                    this.currFC = fc;
                    return currFile;
                }
            }
            zin.close();
            FileChooser.CloseFC(fc);
        } catch (IOException ex) {
            return null;
        } catch (SecurityException ex) {
            return null;
        }
        return null;
    }

    protected void closeFile() {
        try {
            if (this.currFC!=null)
                FileChooser.CloseFC(currFC);
        } catch (Exception ex) {} //Is an IOException, but don't cast it.
        try {
            if (this.currFile!=null)
                this.currFile.close();
        } catch (IOException ex) {}
    }
}
