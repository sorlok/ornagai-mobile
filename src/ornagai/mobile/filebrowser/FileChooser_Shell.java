package ornagai.mobile.filebrowser;


/**
 * NOTE: The java class "FileChooser" is generated from either "FileChooser_Main.java" or "FileChooser_Shell.java"
 *       This is because some phones are weird, and won't load any class that has the "javax.microedition.io.*"
 *       library linked in. So, we configure this in build.xml with the target "build-without-fc".
 *       So, make sure you're NOT editing FileChooser.java; edit one of the other two files instead.
 *
 * This file was generated from FileChooser_Shell.java. It is a no-functionality class which serves only as an ABI link.
 * @author Seth N. Hetu
 */
public class FileChooser {
    //Keep it internal
    protected FileChooser(){}

    public static final boolean IsFileConnectSupported() {
        return false;
    }

    //Not supported
    public static void browseForFile(Object previousPage, String defaultPath, String[] fileSuffixes, Object[] fileIcons, Object folderIcon, Object emptyFolderIcon, Object rootIcon, Object backIcon, Object badFolderIcon, Object onClose) {
        throw new RuntimeException("FileChooser_Shell does not support actual file browsing.");
    }

    //Not supported
    public static final Object GetZipFile(Object[] fcObj, String pathName) {
        return null;
    }

    //Not supported
    public static final void CloseFC(Object[] fcObj) {
        
    }
}


