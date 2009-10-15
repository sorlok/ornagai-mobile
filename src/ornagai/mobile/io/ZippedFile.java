package ornagai.mobile.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;

/**
 *
 * @author Seth N. Hetu
 */
public class ZippedFile extends AbstractFile {
    private String pathName;
    private boolean valid;
    private InputStream currFile;
    private FileConnection currFC;
    private Vector fileNames = new Vector(); //String

    public ZippedFile(String path) {
        this.pathName = path;
        this.valid = true;
        FileConnection fc = null;
        ZipInputStream zin = null;
        try {
            fc = (FileConnection) Connector.open(pathName, Connector.READ);
            InputStream in = fc.openInputStream();
            zin = new ZipInputStream(in);
        } catch (IOException ex) {
            this.valid = false;
        } catch (SecurityException ex) {
            this.valid = false;
        }

        if (zin != null) {
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
                fc.close();
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
            FileConnection fc = (FileConnection) Connector.open(pathName, Connector.READ);
            InputStream in = fc.openInputStream();
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry ze = null;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().equals(resourceName)) {
                    this.currFile = zin;
                    this.currFC = fc;
                    return currFile;
                }
            }
            zin.close();
            fc.close();
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
                this.currFC.close();
        } catch (IOException ex) {}
        try {
            if (this.currFile!=null)
                this.currFile.close();
        } catch (IOException ex) {}
    }
}
