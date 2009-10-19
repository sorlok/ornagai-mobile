package ornagai.mobile;

import ornagai.mobile.io.AbstractFile;
import ornagai.mobile.dictionary.MMDictionary;
import java.io.*;
import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.animations.CommonTransitions;
import com.sun.lwuit.animations.Transition;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.util.*;
import com.sun.lwuit.plaf.*;
import javax.microedition.rms.*;
import ornagai.mobile.filebrowser.ErrorDialog;
import ornagai.mobile.gui.*;
import ornagai.mobile.io.JarredFile;
import ornagai.mobile.io.ZippedFile;

/**
 * @author Thar Htet
 * @author Seth N. Hetu
 */
public class MZMobileDictionary extends MIDlet implements FormController {
    //Shared model
    public static String pathToCustomDict;

    //Optimizations
    public static final boolean OPTIMIZE_AS_UNSIGNED = true;

    //Debug options
    public static final boolean debug = false;
    public static final boolean debug_test_dictloadfailure = false;
    public static final boolean debug_out_of_memory_binary = false;
    public static final boolean debug_out_of_memory_text = false;
    public static final boolean debug_outside_bmp = false;
    public static final boolean debug_neither_text_nor_binary = false;

    //Forms
    private Form splashForm;
    private Form optionsForm;
    private Form dictionaryForm;
    
    //General stuff
    private Image splashImage;
    private Image smileImage;
    private Resources resourceObject;
    private static final String window_title = "Ornagai Mobile";
    private boolean doneInit = false;

    //Some properties
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

    //For generalization purposes (and slower phones)
    public static final Transition GetTransitionRight() {
        if (OPTIMIZE_AS_UNSIGNED)
            return CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 300);
        else
            return Transition3D.createCube(300, false);
    }
    public static final Transition GetTransitionLeft() {
        if (OPTIMIZE_AS_UNSIGNED)
            return CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 300);
        else
            return Transition3D.createCube(300, true);
    }
    

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
        synchronized(MZMobileDictionary.this) {
            splashForm = splashForm!=null ? splashForm : new SplashForm(window_title, splashImage, this);
            splashForm.show();
        }
    }

    public boolean reloadDictionary() {
        if (dictLoader!=null) {
            dictLoader.interrupt();
            dictLoader = null;
        }
        dictionary.freeMostData();
        dictionary = null;
        dictionaryForm = dictionaryForm!=null ? dictionaryForm : new DictionaryForm(window_title, smileImage, dictionary, this);
        ((DictionaryForm)dictionaryForm).setModel(null);
        System.gc();
        System.out.println("Dictionary cleared: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //Load our dictionary, in the background
        if (!loadDictionaryFile())
            return false;
        try {
            dictionary = MMDictionary.createDictionary(dictionaryFile);
        } catch (IllegalArgumentException ex) {
            ErrorDialog.showErrorMessage("Error loading dictionary: \n " + ex.getMessage(), null, this, splashForm.getWidth()-10);
            return false;
        }
        dictLoader = new Thread(new Runnable() {
            public void run() {
                System.out.println("Reloading dictionary: " + dictionaryFile.getClass().getName());
                try {
                    dictionary.loadLookupTree();
                } catch (IllegalArgumentException ex) {
                    ErrorDialog.showErrorMessage("The following error occurred: \n " + ex.getMessage() + " \nPlease try to load your dictionary again. \nIf the problem persists, post an error report on the web site. \n \n ", ex, MZMobileDictionary.this, splashForm.getWidth()-10);
                } catch (OutOfMemoryError err) {
                    ErrorDialog.showErrorMessage("Out of memory. \n \nYour dictionary file is too big. \n \n", null, MZMobileDictionary.this, splashForm.getWidth()-10);
                }
                System.out.println("Reloading -done");

                //TEMP:
                System.gc();
                System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used of " + (Runtime.getRuntime().totalMemory()/1024) + " kb");
            }
        });
        dictLoader.start();
        ((DictionaryForm)dictionaryForm).setModel(dictionary);

        return true;
    }

    public void closeProgram() {
        notifyDestroyed();
    }

    public static final String getFirstWord(String compoundWord) {
        StringBuffer word = new StringBuffer();
        boolean atChar = false;
        for (int i=0; i<compoundWord.length(); i++) {
            char c = Character.toLowerCase(compoundWord.charAt(i));
            if (!atChar) {
                if (c>='a' && c<='z')
                    atChar = true;
                else
                    continue;
            }

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
        if (!loadDictionaryFile())
            return;

        //Load our dictionary, in the background
        Exception thrown = null;
        try {
            dictionary = MMDictionary.createDictionary(dictionaryFile);
            dictLoader = new Thread(new Runnable() {
                public void run() {
                    System.out.println("loadLookupTree() -start");
                    try {
                        dictionary.loadLookupTree();
                    } catch (OutOfMemoryError err) {
                        while (!doneInit) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                break;
                            }
                        }
                        ErrorDialog.showErrorMessage("Out of memory. \n \nYour dictionary file is too big. \n \n", null, MZMobileDictionary.this, splashForm.getWidth()-10);
                    }
                    System.out.println("loadLookupTree() -done");

                    //TEMP:
                    System.gc();
                    System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used,   " + Runtime.getRuntime().freeMemory()/1024 + " kb free.");
                }
            });
            dictLoader.start();
        } catch (IllegalArgumentException ex) {
            thrown = ex;
        }

        /**
         *  Initialize LWUIT Display
         * */
        Display.init(this);

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
        if (debug && thrown==null) {
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

        if (thrown!=null)
            ErrorDialog.showErrorMessage("Error loading dictionary: \n " + thrown.getMessage(), null, this, splashForm.getWidth()-10);

        doneInit = true;
    }


    private boolean loadDictionaryFile() {
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
            if (!((ZippedFile)dictionaryFile).isValid() || debug_test_dictloadfailure) {
                System.out.println("Error: External dictionary file is invalid.");
                dictionaryFile = null;

                //Clear the saved dictionary string, to avoid constant errors on startup
                try {
                    RecordStore properties = RecordStore.openRecordStore(RECORD_STORE_ID, true);
                    byte[] emptyPath = "".getBytes();
                    properties.addRecord(emptyPath, 0, emptyPath.length);
                    properties.closeRecordStore();
                } catch (RecordStoreException ex) {
                    System.out.println("Error saving record record: " + ex.toString());
                }

                //Prompt the user
                try {
                    System.gc();
                    System.out.println("Preparing error dialog: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
                    ErrorDialog.showErrorMessage("Your dictionary file failed to load. It may be invalid. \n\nPlease try to load it again; the jazzlib library we use occasionally glitches.\n\nIf your dictionary file still fails to load, please post an issue on the web site.", null, this, splashForm.getWidth()-10);
                    System.out.println("Error prompt shown.");
                } catch (OutOfMemoryError err) {
                    System.gc();
                    System.out.println("Memory error: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
                }
                return false;
            }
        }

        //Fall back to the default installed dictionary
        if (dictionaryFile==null)
            dictionaryFile = new JarredFile("/dict");
        return true;
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
}

