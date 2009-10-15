package ornagai.mobile;

import ornagai.mobile.dictionary.MMDictionary;
import java.io.*;
import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.util.*;
import com.sun.lwuit.plaf.*;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;
import ornagai.mobile.gui.*;

/**
 * @author Thar Htet
 * @author Seth N. Hetu
 */
public class MZMobileDictionary extends MIDlet implements FormController {
    //Shared model
    public static String pathToCustomDict;

    //Forms
    private Form splashForm;
    private Form optionsForm;
    private Form dictionaryForm;
    
    //General stuff
    private Image splashImage;
    private Image smileImage;
    private Resources resourceObject;
    private static final String window_title = "Ornagai Mobile";
    public static final boolean debug = false;

    //Some properties
    public static boolean fileConnectSupported = false;
    public static boolean fileConnectEnabled = false;
    public static final String RECORD_STORE_ID = "properties";
    public static final int RECORD_DICT_PATH = 1;

    //Our dictionary
    private AbstractFile dictionaryFile;
    private MMDictionary dictionary;
    private Thread dictLoader;

    //We're trying to show how long it takes to load the
    // dictionary, but this will probably require fiddling with the
    // debug flag. 
    private long startTimeMS;
    

    //Good for general re-use
    private static Style i_head_style;
    private static Image headerBGImage;
    public static final Style GetHeaderStyle() {
        if (i_head_style==null) {
            i_head_style = new Style();
            i_head_style.setFgColor(0xffffff);
            i_head_style.setBgImage(headerBGImage);
            i_head_style.setScaleImage(false, true);
        }

        return i_head_style;
    }
    private static Style i_menu_style;
    public static final Style GetMenuStyle() {
        if (i_menu_style==null) {
            i_menu_style = new Style();
            i_menu_style.setBgColor(0x37464A);
            i_menu_style.setFgColor(0xffffff);
        }

        return i_menu_style;
    }
    private static Style i_basic_form_style;
    public static final Style GetBasicFormStyle() {
        if (i_basic_form_style==null) {
            i_basic_form_style = new Style();
            i_basic_form_style.setBgColor(0xE5FFC5);
        }

        return i_basic_form_style;
    }



    //Simple form switcher
    public void switchToOptionsForm() {
        optionsForm = optionsForm!=null ? optionsForm : new OptionsForm(window_title, resourceObject, this); //I miss Ruby's "if" syntax
        optionsForm.show();
    }

    public void switchToDictionaryForm() {
        dictionaryForm = dictionaryForm!=null ? dictionaryForm : new DictionaryForm(window_title, smileImage, dictionary, this);
        dictionaryForm.show();
    }

    public void switchToSplashForm() {
        splashForm = splashForm!=null ? splashForm : new SplashForm(window_title, splashImage, this);
        splashForm.show();
    }

    public void reloadDictionary() {
        if (dictLoader!=null) {
            dictLoader.interrupt();
            dictLoader = null;
        }
        dictionary = null;
        dictionaryForm = dictionaryForm!=null ? dictionaryForm : new DictionaryForm(window_title, smileImage, dictionary, this);
        ((DictionaryForm)dictionaryForm).setModel(null);
        System.gc();
        System.out.println("Dictionary cleared: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //Load our dictionary, in the background
        loadDictionaryFile();
        dictionary = MMDictionary.createDictionary(dictionaryFile);
        dictLoader = new Thread(new Runnable() {
            public void run() {
                System.out.println("Reloading dictionary: " + dictionaryFile.getClass().getName());
                dictionary.loadLookupTree();
                System.out.println("Reloading -done");

                //TEMP:
                System.gc();
                System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
            }
        });
        dictLoader.start();
    }

    public void closeProgram() {
        notifyDestroyed();
    }

    public static final String getFirstWord(String compoundWord) {
        StringBuffer word = new StringBuffer();
        for (int i=0; i<compoundWord.length(); i++) {
            char c = Character.toLowerCase(compoundWord.charAt(i));
            if (c>='a' && c<='z')
                word.append(c);
            else
                break;
        }
        return word.toString();
    }

    public void startApp() {
        //Count
        startTimeMS = System.currentTimeMillis();

        System.gc();
        System.out.println("Memory in use at very beginning: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");


        //Add our record if we don't have it
        try {
            RecordStore properties = RecordStore.openRecordStore(RECORD_STORE_ID, true);
            if (properties.getNumRecords()==0) {
                byte[] emptyPath = "".getBytes();
                properties.addRecord(emptyPath, 0, emptyPath.length);
            }
            properties.closeRecordStore();
        } catch (RecordStoreException ex) {
            System.out.println("Error adding initial record: " + ex.toString());
        }


        //Set the dictionaryFile to be valid
        loadDictionaryFile();

        //Load our dictionary, in the background
        dictionary = MMDictionary.createDictionary(dictionaryFile);
        dictLoader = new Thread(new Runnable() {
            public void run() {
                System.out.println("loadLookupTree() -start");
                dictionary.loadLookupTree();
                System.out.println("loadLookupTree() -done");

                //TEMP:
                System.gc();
                System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used,   " + Runtime.getRuntime().freeMemory()/1024 + " kb free.");
            }
        });
        dictLoader.start();

        /**
         *  Initialize LWUIT Display
         * */
        Display.init(this);

        //Load properties
        MZMobileDictionary.fileConnectSupported = (System.getProperty("microedition.io.file.FileConnection.version")!=null);
        try {
            Enumeration roots = FileSystemRegistry.listRoots();
            if (roots.hasMoreElements())
                MZMobileDictionary.fileConnectEnabled = true;
        } catch (SecurityException ex) {
            MZMobileDictionary.fileConnectEnabled = false;
        } catch (ClassCastException ex) {
            MZMobileDictionary.fileConnectEnabled = false;
        }

        // Get LWUIT Resources.
        try {
            resourceObject = Resources.open("/MZDictRsc.res");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        //Before we forget
        headerBGImage = resourceObject.getImage("ornagai");
        splashImage = resourceObject.getImage("splashgif");
        smileImage = resourceObject.getImage("smile");

        //Show the first form
        splashForm = splashForm!=null ? splashForm : new SplashForm(window_title, splashImage, this);
        splashForm.show();

        //Count how long it took the form (and a dictionary) to load
        if (debug) {
            String kindaBigSearch = "coal";
            try {
                synchronized(this) {
                    if (dictLoader!=null) {
                        try {
                            dictLoader.join();
                        } catch (InterruptedException ex) {}
                        dictLoader = null;
                    }
                }

                dictionary.performSearch(kindaBigSearch);
            } catch (IOException ex) {
                throw new RuntimeException("Unable to search: " + ex.toString());
            }
        }
        startTimeMS = System.currentTimeMillis() - startTimeMS;
        dictionaryForm = dictionaryForm!=null ? dictionaryForm : new DictionaryForm(window_title, smileImage, dictionary, this);
        ((DictionaryForm)dictionaryForm).setStatusMessage("Time to load: " + startTimeMS/1000.0F + " s");
    }


    private void loadDictionaryFile() {
        //Check if our user-supplied dicionary is valid
        String udPath = "";
        dictionaryFile = null;
        try {
            RecordStore properties = RecordStore.openRecordStore(RECORD_STORE_ID, true);
            byte[] b = properties.getRecord(RECORD_DICT_PATH);
            udPath = b==null ? "" : new String(b);
            properties.closeRecordStore();
        } catch (RecordStoreException ex) {
            System.out.println("Error reading record store: " + ex.toString());
        }
        if (udPath.length()>0) {
            dictionaryFile = new ZippedFile(udPath);
            if (!((ZippedFile)dictionaryFile).isValid()) {
                System.out.println("Error: External dictionary file is invalid.");
                dictionaryFile = null;
            }
        }

        //Fall back to the default installed dictionary
        if (dictionaryFile==null)
            dictionaryFile = new JarredFile("/dict");
    }





    //Todo:
    public void pauseApp() {}
    public void destroyApp(boolean unconditional) {}


    public void waitForDictionaryToLoad() {
        //First, make sure our dictionary is done loading
        synchronized(this) {
            if (dictLoader!=null) {
                try {
                    dictLoader.join();
                } catch (InterruptedException ex) {}
                dictLoader = null;
            }
        }
    }


    class ZippedFile extends AbstractFile {
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



    class JarredFile extends AbstractFile {
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
}

