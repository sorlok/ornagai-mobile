/*
 * This code is licensed under the terms of the MIT License. 
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile;

import java.io.*;
import javax.microedition.midlet.*;
import com.sun.lwuit.*;
import com.sun.lwuit.util.*;
import com.sun.lwuit.plaf.*;
import javax.microedition.rms.*;
import ornagai.mobile.gui.*;
import ornagai.mobile.io.*;
import ornagai.mobile.dictionary.MMDictionary;
import ornagai.mobile.filebrowser.ErrorDialog;

//NOTE: Do NOT change this to *; we cannot load the 3DTransitions (for Blackberry & Sony Ericsson phones)
import com.sun.lwuit.animations.Transition;
import com.sun.lwuit.animations.CommonTransitions;



/**
 * This class provides the main MIDlet for the ornagai-mobile phone dictionary.
 *  It also controls switching between the various LWUIT Forms that make up the
 *  dictionary's primary interface.
 *
 * Like all MIDlets, it is best to read the startApp() function to begin
 *  understanding how the program works. 
 *
 * @author Thar Htet
 * @author Seth N. Hetu
 */
public class MZMobileDictionary extends MIDlet implements FormController {
    //Shared between various forms
    public static String pathToCustomDict;

    //Optimizations
    public static final boolean OPTIMIZE_AS_UNSIGNED = true;

    //Debug options; set the first one to true for a memory test; set the others
    // to true to cause fake failures and to test program recovery.
    public static final boolean debug = false;
    public static final boolean debug_test_dictloadfailure = false;
    public static final boolean debug_out_of_memory_binary = false;
    public static final boolean debug_out_of_memory_text = false;
    public static final boolean debug_outside_bmp = false;
    public static final boolean debug_neither_text_nor_binary = false;

    //Used by the debug test to show how long it took to start the program
    // and perform a search
    private long startTimeMS;

    //Forms - keep null until required to reduce running memory usage.
    // Presumably, we could null out one form when another is loaded, but
    // I see no need for that at the moment.
    private Form splashForm;
    private Form optionsForm;
    private Form dictionaryForm;
    
    //General resources, window title, resource bundle
    private Image splashImage;
    private Image smileImage;
    private Resources resourceObject;
    private static final String window_title = "Ornagai Mobile";

    //Centralize properties related to record store access
    public static final String RECORD_STORE_ID = "properties";
    public static final int RECORD_DICT_PATH = 1;

    //Our dictionary, and some files related to loading it
    private MMDictionary dictionary;
    private AbstractFile dictionaryFile;
    private Thread dictLoader;

    //Used to ensure that our main form never overrides an error dialog
    private boolean doneInit = false;

    //Generalize transitions to the "right" and "left", and only show the 3D
    // transition on signed applets (which presumably run on more powerful phones)
    //If unsigned users want 3D transitions, we'll have to fork a separate build.
    public static final Transition GetTransitionRight() {
        if (OPTIMIZE_AS_UNSIGNED)
            return CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, false, 300);
        else
            return CommonTransitions.createFade(300);
            //return Transition3D.createCube(300, false);
    }
    public static final Transition GetTransitionLeft() {
        if (OPTIMIZE_AS_UNSIGNED)
            return CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 300);
        else
            return CommonTransitions.createFade(300);
            //return Transition3D.createCube(300, true);
    }
    

    //Form/Menu/Header styles. Singletons, good for general re-use.
    //   Note that singletons are not strictly necessary, as Components
    //   appear to copy Styles by value.
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


    //Safe access to our three forms: create them if they don't already exist
    private final SplashForm GetSplashForm() {
        if (splashForm==null)
            splashForm = new SplashForm(window_title, splashImage, this);
        return (SplashForm)splashForm;
    }
    private final DictionaryForm GetDictionaryForm() {
        if (dictionaryForm==null)
            dictionaryForm = new DictionaryForm(window_title, smileImage, dictionary, this);
        return (DictionaryForm)dictionaryForm;
    }
    private final OptionsForm GetOptionsForm() {
        if (optionsForm==null)
            optionsForm = new OptionsForm(window_title, resourceObject, this);
        return (OptionsForm)optionsForm;
    }


    //Implementation of the form switching elements of the FormController interface
    //  All of these are straightforward; just show the options form
    public void switchToOptionsForm() {
        GetOptionsForm().show();
    }
    public void switchToDictionaryForm() {
        GetDictionaryForm().show();
    }
    public void switchToSplashForm() {
        synchronized(MZMobileDictionary.this) {
            GetSplashForm().show();
        }
    }

    //Implementation of the reloadDictionary() command from FormController
    // Get a valid dictionary file and then load it.
    public boolean reloadDictionary() {
        //Stop loading of the current dictionary. Delete it, and reclaim its memory.
        if (dictLoader!=null) {
            dictLoader.interrupt();
            dictLoader = null;
        }
        dictionary.freeMostData();
        dictionary = null;
        GetDictionaryForm().setModel(null);
        System.gc();
        System.out.println("Dictionary cleared: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //Load our dictionary, in the background
        if (!loadDictionaryFile())
            return false;
        try {
            dictionary = MMDictionary.createDictionary(dictionaryFile);
        } catch (IllegalArgumentException ex) {
            ErrorDialog.showErrorMessage("Error loading dictionary: \n " + ex.getMessage(), null, this, GetSplashForm().getWidth()-10);
            return false;
        }
        dictLoader = new Thread(new Runnable() {
            public void run() {
                System.out.println("Reloading dictionary: " + dictionaryFile.getClass().getName());
                try {
                    dictionary.loadLookupTree();
                } catch (IllegalArgumentException ex) {
                    ErrorDialog.showErrorMessage("The following error occurred: \n " + ex.getMessage() + " \nPlease try to load your dictionary again. \nIf the problem persists, post an error report on the web site. \n \n ", ex, MZMobileDictionary.this, GetSplashForm().getWidth()-10);
                } catch (OutOfMemoryError err) {
                    ErrorDialog.showErrorMessage("Out of memory. \n \nYour dictionary file is too big. \n \n", null, MZMobileDictionary.this, GetSplashForm().getWidth()-10);
                }
                System.out.println("Reloading -done");

                //TEMP:
                System.gc();
                System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used of " + (Runtime.getRuntime().totalMemory()/1024) + " kb");
            }
        });
        dictLoader.start();
        GetDictionaryForm().setModel(dictionary);

        return true;
    }

    public void closeProgram() {
        notifyDestroyed();
    }

    //Utility method: return the first [a-z]+ string in a compound word
    public static final String getFirstWord(String compoundWord) {
        StringBuffer word = new StringBuffer();
        boolean atChar = false;
        for (int i=0; i<compoundWord.length(); i++) {
            char c = Character.toLowerCase(compoundWord.charAt(i));

            //Ignore leading non-alphabetic letters
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


    //Application entry point.
    public void startApp() {
        //Track how long it takes our system to load
        if (debug)
            startTimeMS = System.currentTimeMillis();

        //Get a feel for our basline (optimal) memory usage
        if (debug) {
            System.gc();
            System.out.println("Memory in use at very beginning: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
        }

        //Initialize LWUIT Display
        // Do this early to allow us to show our Error Form at any time.
        Display.init(this);


        //Add our record if we don't have it (not strictly necessary, but I'm leaving this until the next release)
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


        //Load our dictionary file; return if it failed to load.
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
                        ErrorDialog.showErrorMessage("Out of memory. \n \nYour dictionary file is too big. \n \n", null, MZMobileDictionary.this, GetSplashForm().getWidth()-10);
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

        //Get LWUIT Resources.
        try {
            resourceObject = Resources.open("/MZDictRsc.res");
        } catch (IOException ex) {
            System.out.println("Error loading initial resource object: " + ex.getMessage());
        }

        //Create shared resources
        headerBGImage = resourceObject.getImage("ornagai");
        splashImage = resourceObject.getImage("splashgif");
        smileImage = resourceObject.getImage("smile");

        //Create & show the first form
        GetSplashForm().show();

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

        //Show the user how long it took to load the entire dictionary and perform a search
        if (debug) {
            startTimeMS = System.currentTimeMillis() - startTimeMS;
            GetDictionaryForm().setStatusMessage("Time to load: " + startTimeMS/1000.0F + " s");
        }

        //Was there a problem? If so...
        if (thrown!=null)
            ErrorDialog.showErrorMessage("Error loading dictionary: \n " + thrown.getMessage(), null, this, GetSplashForm().getWidth()-10);

        //Allow our dictionary loader to show any additional errors.
        doneInit = true;
    }


    //Load the dictionary file as stored in our record store.
    // Load the embedded file if no dictionary is specified.
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
                    ErrorDialog.showErrorMessage("Your dictionary file failed to load. It may be invalid. \n\nPlease try to load it again; the jazzlib library we use occasionally glitches.\n\nIf your dictionary file still fails to load, please post an issue on the web site.", null, this, GetSplashForm().getWidth()-10);
                    System.out.println("Error prompt shown.");
                } catch (OutOfMemoryError err) {
                    System.gc();
                    System.out.println("Memory error: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
                }
                return false;
            }
        }

        //Fall back to the default installed dictionary
        // NOTE: At the moment, this does not happen. That is because our "ErrorDialog"
        //   always forces the user to quit the application (due to its asynchronous nature).
        //   We should add a "WarningDialog" with a callback, or copy the behavior of LWUIT's
        //   Dialog class (and block). Either way, we need to allow users to fall back on the default
        //   dictionary.
        if (dictionaryFile==null)
            dictionaryFile = new JarredFile("/dict");
        return true;
    }


    //Todo: Standard J2ME MIDlet control. At the moment, LWUIT's structure means
    //  that we don't needlessly consume CPU power (since it's not a game)
    //  but we might consider unloading the dictionary when the application pauses.
    public void pauseApp() {}
    public void destroyApp(boolean unconditional) {}


    //Simple encapsulation of dictLoader.join()
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

