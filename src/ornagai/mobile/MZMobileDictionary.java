package ornagai.mobile;

import ornagai.mobile.dictionary.MMDictionary;
import java.io.*;
import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.util.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.FlowLayout;
import com.sun.lwuit.layouts.GridLayout;
import com.sun.lwuit.list.DefaultListCellRenderer;
import com.sun.lwuit.list.DefaultListModel;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import com.waitzar.analysis.segment.WZSegment;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.amms.control.PanControl;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;
import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;
import ornagai.mobile.filebrowser.FileChooser;

/**
 * @author Thar Htet
 * @author Seth N. Hetu
 */
public class MZMobileDictionary extends MIDlet implements ActionListener {

    private Command exitCommand;
    private Command searchCommand;
    private Command startCommand;
    private Command optionsCommand;
    private Command saveCommand;
    private Command cancelCommand;
    private Command backCommand;
    private Resources resourceObject;
    private TextField searchField;
    private RoundButton searchBtn;
    private Label startTimeLabel;
    private List resultList;
    private Label smileLabel;
    private Container resPanel;
    private ZawgyiComponent resultDisplay;
    private ZawgyiComponent msgNotFound;
    private final int cluster_prefix_length = 2;
    private static final String file_suffix = "_mz.txt";
    private static final String window_title = "Ornagai Mobile";
    private boolean debug = false;

    //We'll make our forms take up less memory, if possible
    //private Form dictionaryForm;
    //private Form optionsForm;
    //private Form splashForm;
    //private Form resultForm;

    //For the options menu
    private RoundButton browseBtn;
    private TextField currExternalPath;
    private Image fcDictionaryIcon;
    private Image fcRootIcon;
    private Image fcFolderIconFull;
    private Image fcFolderIconEmpty;
    private Image fcBackIcon;

    //Some properties
    private boolean fileConnectSupported = false;
    private boolean fileConnectEnabled = false;
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
    private boolean oneTimeMemoryMsg;


    private Form i_dict_form;
    private final Form GetDictionaryForm() {
        //Re-generate?
        if (i_dict_form==null) {
            i_dict_form = new Form(window_title);

            //Set layout
            i_dict_form.setLayout(new BorderLayout());

            //Add components used for searching.
            Container searchPanel = new Container(new BorderLayout());
            searchPanel.addComponent(BorderLayout.NORTH, searchField);
            searchPanel.addComponent(BorderLayout.EAST, searchBtn);
            i_dict_form.addComponent(BorderLayout.NORTH, searchPanel);

            //Add the smile label
            i_dict_form.addComponent(BorderLayout.CENTER, smileLabel);
            if (debug)
                i_dict_form.addComponent(BorderLayout.SOUTH, startTimeLabel);
            i_dict_form.setScrollable(false);

            //Add commands and listener; set transition
           i_dict_form.addCommand(exitCommand);
           i_dict_form.addCommand(searchCommand);
           i_dict_form.setCommandListener((ActionListener) this);
           i_dict_form.setTransitionOutAnimator(Transition3D.createCube(300, false));

           //Set styles
           i_dict_form.setTitleStyle(GetHeaderStyle());
           i_dict_form.setMenuStyle(GetMenuStyle());
           i_dict_form.setStyle(GetBasicFormStyle());

           //One more style
           Style searchFieldStyle = new Style();
           searchFieldStyle.setBgColor(0xffffff);
           searchFieldStyle.setFgColor(0x000000);
           searchFieldStyle.setBgSelectionColor(0x666666);
           searchFieldStyle.setFgSelectionColor(0xffffff);
           searchFieldStyle.setBorder(Border.createRoundBorder(6, 6));
           searchField.setStyle(searchFieldStyle);
        }

        return i_dict_form;
    }

    private Form i_splash_form;
    private final Form GetSplashForm() {
        if (i_splash_form==null) {
            i_splash_form = new Form(window_title);

            //Create the top part of the container
            Container topContainer = new Container();
            topContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            Label ornagaiLabel = new Label("Ornagai.com");
            ornagaiLabel.setAlignment(Label.CENTER);
            Label mysteryZillionLabel = new Label("Mysteryzillion.org");
            mysteryZillionLabel.setAlignment(Label.CENTER);
            Label logo = new Label(resourceObject.getImage("splashgif"));
            logo.setAlignment(Label.CENTER);
            topContainer.addComponent(ornagaiLabel);
            topContainer.addComponent(mysteryZillionLabel);

            //Add the top part and the logo
            i_splash_form.setLayout(new BorderLayout());
            i_splash_form.addComponent(BorderLayout.NORTH, topContainer);
            i_splash_form.addComponent(BorderLayout.CENTER, logo);

            //Set transitions and commands
            i_splash_form.setTransitionOutAnimator(Transition3D.createCube(300, false));
            i_splash_form.addCommand(optionsCommand);
            i_splash_form.addCommand(startCommand);
            i_splash_form.setCommandListener((ActionListener) this);

            //Set style
            i_splash_form.setTitleStyle(GetHeaderStyle());
            i_splash_form.setStyle(GetBasicFormStyle());
            i_splash_form.setMenuStyle(GetMenuStyle());
            mysteryZillionLabel.setStyle(GetBasicFormStyle());
            ornagaiLabel.setStyle(GetBasicFormStyle());
            smileLabel.setStyle(GetBasicFormStyle());
            startTimeLabel.setStyle(GetBasicFormStyle());
            startTimeLabel.getStyle().setFgColor(0xFF0000);
            logo.setStyle(GetBasicFormStyle());
        }

        return i_splash_form;
    }


    private Form i_opt_form;
    private final Form GetOptionsForm() {
        if (i_opt_form==null) {
            i_opt_form = new Form(window_title);

            //Prepare options form layout
            i_opt_form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
            i_opt_form.getStyle().setPadding(10, 10, 10, 10);

            i_opt_form.addCommand(cancelCommand);
            i_opt_form.addCommand(saveCommand);
            i_opt_form.setCommandListener((ActionListener) this);
            i_opt_form.setTransitionOutAnimator(Transition3D.createCube(300, true));

            //Add our first option panel
            Container extDictionaryPanel = new Container(new BorderLayout());
            extDictionaryPanel.getStyle().setBorder(Border.createRoundBorder(10, 10, 0x333333));
            extDictionaryPanel.getStyle().setBgColor(0xDDDDFF);
            extDictionaryPanel.getStyle().setBgTransparency(255);
            extDictionaryPanel.getStyle().setPadding(5, 5, 5, 5);
            i_opt_form.addComponent(extDictionaryPanel);

            //Label
            Label extDictLbl = new Label("External Dictionary");
            extDictLbl.getStyle().setBgTransparency(0);
            extDictLbl.getStyle().setFgColor(0x444444);
            extDictionaryPanel.addComponent(BorderLayout.NORTH, extDictLbl);

            //Disabled?
            if (!this.fileConnectSupported || !this.fileConnectEnabled) {
                TextArea notSupportedLbl = new TextArea("Disabled: Your phone does not support the file connections API, or it uses a non-standard path naming convention.");
                notSupportedLbl.setEditable(false);
                notSupportedLbl.setRows(5);
                notSupportedLbl.getStyle().setBorder(Border.createEmpty());
                notSupportedLbl.getStyle().setBgTransparency(0);
                notSupportedLbl.getStyle().setFgColor(0xDD0000);
                notSupportedLbl.getStyle().setFgSelectionColor(0xDD0000);
                notSupportedLbl.getStyle().setPadding(0, 0, 10, 5);
                extDictionaryPanel.addComponent(BorderLayout.CENTER, notSupportedLbl);
            } else {
                //Current path
                extDictionaryPanel.addComponent(BorderLayout.CENTER, currExternalPath);

                //Button to clear, button to set
                Container bottomRow = new Container(new FlowLayout(Container.RIGHT));
                browseBtn = new RoundButton("Browse...");
                browseBtn.getStyle().setBgSelectionColor(0x233136);
                browseBtn.getStyle().setFgSelectionColor(0xffffff);
                browseBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        FileChooser.browseForFile(i_opt_form, currExternalPath.getText(), new String[]{"mzdict.zip"}, new Image[]{fcDictionaryIcon}, fcFolderIconFull, fcFolderIconEmpty, fcRootIcon, fcBackIcon, new ActionListener() {
                            public void actionPerformed(ActionEvent result) {
                                String path = (String)result.getSource();
                                setDictionaryPath(path);
                            }
                        });
                    }
                });
                RoundButton clearBtn = new RoundButton("Clear");
                clearBtn.getStyle().setBgSelectionColor(0x233136);
                clearBtn.getStyle().setFgSelectionColor(0xffffff);
                clearBtn.getStyle().setMargin(Container.RIGHT, 5);
                clearBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        currExternalPath.setText("");
                    }
                });
                bottomRow.addComponent(browseBtn);
                bottomRow.addComponent(clearBtn);
                extDictionaryPanel.addComponent(BorderLayout.SOUTH, bottomRow);
            }

            //Set styles
            i_opt_form.setMenuStyle(GetMenuStyle());
            i_opt_form.setStyle(GetBasicFormStyle());
            i_opt_form.setTitleStyle(GetHeaderStyle());
        }

        return i_opt_form;
    }


    private Form i_res_form;
    private final Form GetResultsForm() {
        if (i_res_form==null) {
            i_res_form = new Form(window_title);

            //Initialize form
            i_res_form.setLayout(new BorderLayout());
            i_res_form.setScrollable(false);

            //Add commands and transitions
            i_res_form.addCommand(exitCommand);
            i_res_form.addCommand(backCommand);
            i_res_form.setCommandListener((ActionListener) this);
            i_res_form.setTransitionOutAnimator(Transition3D.createCube(300, true));

            //Set style
            i_res_form.setTitleStyle(GetHeaderStyle());
            i_res_form.setMenuStyle(GetMenuStyle());
            i_res_form.setStyle(GetBasicFormStyle());
        }

        return i_res_form;
    }

    private Style i_head_style;
    private final Style GetHeaderStyle() {
        if (i_head_style==null) {
            i_head_style = new Style();
            i_head_style.setFgColor(0xffffff);
            i_head_style.setBgImage(resourceObject.getImage("ornagai"));
            i_head_style.setScaleImage(false, true);
        }

        return i_head_style;
    }

    private Style i_menu_style;
    private final Style GetMenuStyle() {
        if (i_menu_style==null) {
            i_menu_style = new Style();
            i_menu_style.setBgColor(0x37464A);
            i_menu_style.setFgColor(0xffffff);
        }

        return i_menu_style;
    }

    private Style i_basic_form_style;
    private final Style GetBasicFormStyle() {
        if (i_basic_form_style==null) {
            i_basic_form_style = new Style();
            i_basic_form_style.setBgColor(0xE5FFC5);
        }

        return i_basic_form_style;
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
        this.fileConnectSupported = (System.getProperty("microedition.io.file.FileConnection.version")!=null);
        try {
            Enumeration roots = FileSystemRegistry.listRoots();
            if (roots.hasMoreElements())
                this.fileConnectEnabled = true;
        } catch (SecurityException ex) {
            this.fileConnectEnabled = false;
        } catch (ClassCastException ex) {
            this.fileConnectEnabled = false;
        }

        // Get LWUIT Resources.
        try {
            resourceObject = Resources.open("/MZDictRsc.res");
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        // Init Commands
        exitCommand = new Command("Exit");
        searchCommand = new Command("Search");
        startCommand = new Command("Start");
        optionsCommand = new Command("Options");
        backCommand = new Command("Back");
        saveCommand = new Command("Save");
        cancelCommand = new Command("Cancel");


        //Init our result panel
        resultDisplay = new ZawgyiComponent();
        resultDisplay.setFocusable(false);


        // Prepare Dictionary Form related objects
        searchField = new TextField();
        searchBtn = new RoundButton("Search");
        searchBtn.getStyle().setBgSelectionColor(0x233136);
        searchBtn.getStyle().setFgSelectionColor(0xffffff);
        searchBtn.getStyle().setMargin(Component.BOTTOM, 10);
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (searchField.getText().trim().length() > 0) {
                    searchAndDisplayResult(searchField.getText().trim());
                }
            }
        });

        currExternalPath = new TextField();
        currExternalPath.getStyle().setBgSelectionColor(0x233136);
        currExternalPath.getStyle().setFgSelectionColor(0xffffff);

        smileLabel = new Label(resourceObject.getImage("smile"));
        smileLabel.setText(" ");
        smileLabel.setAlignment(Label.CENTER);

        //Load resources for file browser
        fcDictionaryIcon = resourceObject.getImage("fc_dictionary");
        fcRootIcon = resourceObject.getImage("fc_root");
        fcFolderIconFull = resourceObject.getImage("fc_folder_full");
        fcFolderIconEmpty = resourceObject.getImage("fc_empty_folder");
        fcBackIcon = resourceObject.getImage("fc_back");

        startTimeLabel = new Label("");
        startTimeLabel.setAlignment(Label.CENTER);

        //Sohw the first form
        GetSplashForm().show();

        //Clear all other forms
        i_dict_form = null;
        i_opt_form = null;
        i_res_form = null;
        //i_splash_form = null;

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
        startTimeLabel.setText("Time to load: " + startTimeMS/1000.0F + " s");
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


    private void setListStyle() {
        DictionaryRenderer dlcr = new DictionaryRenderer(0xFFBBBB, 0xDDDDDD);
        dlcr.getStyle().setBgSelectionColor(0x111188);
        dlcr.getStyle().setFgSelectionColor(0xFFFFFF);
        resultList.getStyle().setBorder(Border.createEmpty());
        resultList.getStyle().setMargin(0, 5, 5, 5);
        resultList.getStyle().setBgColor(0xFFFFFF);
        resultList.getStyle().setBgSelectionColor(0xFFFFFF);
        resultList.setItemGap(0);
        resultList.setListCellRenderer(dlcr);
    }



    //Todo:
    private void setDictionaryPath(String path) {
        if (path!=null) {
            currExternalPath.setText(path);
            //TODO: Load it
        }
    }



    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getCommand() == exitCommand) {
            notifyDestroyed();
        }

        if (ae.getCommand() == searchCommand) {
            if (searchField.getText().trim().length() > 0) {
                searchAndDisplayResult(searchField.getText().trim());
            }
        }

        if (ae.getCommand() == optionsCommand) {
            if (browseBtn!=null)
                browseBtn.requestFocus();

            //Set text
            try {
                RecordStore properties = RecordStore.openRecordStore(RECORD_STORE_ID, true);
                byte[] b = properties.getRecord(RECORD_DICT_PATH);
                String path = b==null ? "" : new String(b);
                currExternalPath.setText(path);
                properties.closeRecordStore();
            } catch (RecordStoreException ex) {
                System.out.println("Error reading record store: " + ex.toString());
            }

            GetOptionsForm().show();

            //Clear all other forms
            i_dict_form = null;
            //i_opt_form = null;
            i_res_form = null;
            i_splash_form = null;
        }

        if (ae.getCommand() == startCommand) {
            GetDictionaryForm().show();

            //Clear all other forms
            //i_dict_form = null;
            i_opt_form = null;
            i_res_form = null;
            i_splash_form = null;
        }

        if (ae.getCommand() == backCommand) {
            GetDictionaryForm().show();
            //If we come back from result page
            //surely this list will be available.
            resultList.requestFocus();

            //Clear all other forms
            //i_dict_form = null;
            i_opt_form = null;
            i_res_form = null;
            i_splash_form = null;
        }

        if (ae.getCommand() == saveCommand) {
            //Save path
            try {
                byte[] path = currExternalPath.getText().getBytes();
                RecordStore properties = RecordStore.openRecordStore(RECORD_STORE_ID, true);
                properties.setRecord(RECORD_DICT_PATH, path, 0, path.length);
                properties.closeRecordStore();
            } catch (RecordStoreException ex) {
                System.out.println("Error saving path: " + ex.toString());
            }

            //Reset model
            if (dictLoader!=null) {
                dictLoader.interrupt();
                dictLoader = null;
            }
            dictionary = null;
            System.gc();

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

            //Additionally, re-load and re-set the result list
            /*resPanel.removeComponent(resultList);
            resultList = new List(dictionary);
            resultList.setNumericKeyActions(false);
            resultList.setFixedSelection(List.FIXED_CENTER);
            setListStyle();
            resultList.addActionListener((ActionListener) this);
            resPanel.addComponent(BorderLayout.CENTER, resultList);*/
            
            //Go back
            GetSplashForm().show();

            //Clear all other forms
            i_dict_form = null;
            i_opt_form = null;
            i_res_form = null;
            //i_splash_form = null;
        }

        if (ae.getCommand() == cancelCommand) {
            //Go back
            GetSplashForm().show();

            //Clear other forms
            i_dict_form = null;
            i_opt_form = null;
            i_res_form = null;
            //i_splash_form = null;
        }

        if (ae.getSource() == (Object) resultList) {
            //Get this entry based on its ID
            DictionaryListEntry entry = (DictionaryListEntry)resultList.getSelectedItem();

            //Need to search?
            if (entry.id==-2)
                entry.id = dictionary.findWordIDFromEntry(entry);

            //Invalid? Not found?
            if (entry.id==-1) {
                //Just go back to the entry list
                searchField.requestFocus();
                resultList.setVisible(false);
            } else {
                //Set all parts of the word
                String[] pieces = dictionary.getWordTuple(entry);
                if (pieces!=null)
                    resultDisplay.setTextToDictionaryEntry(pieces[0], pieces[1], pieces[2], dictionary.getFormat());
                else
                    resultDisplay.setText(entry.word, dictionary.getFormat());
                resultDisplay.setVisible(true);

                //TEST conversion code.
                /*String origZawgyi08 = "\u1021\u1001\u103A\u102D\u1033\u1095\n"
                        + "\u1010\u102D\u102F\u1038\u103B\u1019\u103D\u1004\u1039\u1037\n"
                        + "\u107E\u1000\u103C\u1000\u1039\u1031\u1011\u102C\u1004\u1039\u1031\u1001\u103A\u102C\u1000\u1039\u104B\n"
                        + "\u1017\u102F\u1012\u1076\u101D\u102B\u1012\u104B\n"
                        + "\u101C\u1088\u1015\u1039\u101B\u103D\u102C\u1038\u1019\u1088\u104B \u1010\u1015\u1039\u101C\u1088\u1015\u1039\u101B\u103D\u102C\u1038\u1019\u1088\u104A\u1031\u101B\u108A\u1094\u1019\u1088\u104B\n"
                        + "\u1021\u1002\u1064\u101C\u102D\u1015\u1039 \u1018\u102C\u101E\u102C\u1005\u1000\u102C\u1038\u104B\n";
                String origZawgyi09 = "\u1021\u1001\u103A\u102D\u102F\u1037\n"
                        + "\u1010\u102D\u102F\u1038\u103B\u1019\u103D\u1004\u1039\u1037\n"
                        + "\u103B\u1000\u103C\u1000\u1039\u1031\u1011\u102C\u1004\u1039\u1031\u1001\u103A\u102C\u1000\u1039\u104B\n"
                        + "\u1017\u102F\u1012\u103F\u1013\u101D\u102B\u1012\u104B\n"
                        + "\u101C\u103D\u102F\u1015\u1039\u101B\u103D\u102C\u1038\u1019\u103D\u102F\u104B \u1010\u1015\u1039\u101C\u103D\u102F\u1015\u1039\u101B\u103D\u102C\u1038\u1019\u103D\u102F\u104A\u1031\u101B\u103C\u103D\u1037\u1019\u103D\u102F\u104B\n"
                        + "\u1021\u1002\u103F\u1004\u101C\u102D\u1015\u1039 \u1018\u102C\u101E\u102C\u1005\u1000\u102C\u1038\u104B\n";

                //resultDisplay.setTextToDictionaryEntry("Zawgyi 2008", "", origZawgyi08, MMDictionary.FORMAT_ZG2008);
                resultDisplay.setTextToDictionaryEntry("Zawgyi 2009", "", origZawgyi09, MMDictionary.FORMAT_ZG2009);
                //resultDisplay.setTextToDictionaryEntry("Zawgyi 2008 Parsed As '09", "", origZawgyi08, MMDictionary.FORMAT_ZG2009);*/

                    //Show a new panel in the same form. This permits more reasonable
                    // tabbing back and forth between results.
                    GetDictionaryForm().removeComponent(smileLabel);
                    GetDictionaryForm().removeComponent(startTimeLabel);
                    if (msgNotFound != null) {
                        GetDictionaryForm().removeComponent(msgNotFound);
                    }
                    if (resPanel != null) {
                        GetDictionaryForm().removeComponent(resPanel);
                    }
                    GetDictionaryForm().addComponent(BorderLayout.CENTER, resultDisplay);
                    GetDictionaryForm().repaint();

                    if (!oneTimeMemoryMsg) {
                        oneTimeMemoryMsg = true;
                        System.gc();
                        System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
                    }
            }
        }
    }

    private void searchAndDisplayResult(String query) {
        //First, make sure our dictionary is done loading
        synchronized(this) {
            if (dictLoader!=null) {
                try {
                    dictLoader.join();
                } catch (InterruptedException ex) {}
                dictLoader = null;
            }
        }

        //Clear our previous display, to save memory
        resultDisplay.setVisible(false);
        resultDisplay.clearData();

        //For now, we only search for the first word
        StringBuffer word = new StringBuffer();
        for (int i=0; i<query.length(); i++) {
            char c = Character.toLowerCase(query.charAt(i));
            if (c>='a' && c<='z')
                word.append(c);
            else
                break;
        }
        if (word.length()==0)
            return;


        //Select the proper index and secondary data
        try {
            //Nothing to search for?
            dictionary.performSearch(word.toString());
        } catch (IOException ex) {
            //Error message
            String message = query + " " + "\u104F\u0020\u1021\u1013\u102D\u1015\u1078\u102B\u101A\u1039\u1000\u102D\u102F\n" +
                    "\u1024\u1021\u1018\u102D\u1013\u102B\u1014\u1039\u1010\u103C\u1004\u1039\n" +
                    "\u1019\u101E\u103C\u1004\u1039\u1038\u101B\u1031\u101E\u1038\u1015\u102B\u104B";
            msgNotFound = new ZawgyiComponent();
            msgNotFound.setText(message, MMDictionary.FORMAT_ZG2008);
            GetDictionaryForm().removeComponent(smileLabel);//If smile is in place
            GetDictionaryForm().removeComponent(startTimeLabel);
            if (resultList != null) {
                GetDictionaryForm().removeComponent(resultList);//If result list is in place
            }
            GetDictionaryForm().addComponent(BorderLayout.CENTER, msgNotFound);
            GetDictionaryForm().invalidate();
            GetDictionaryForm().repaint();
            return;
        }

        //Now, re-load and show our dictionary list
        resultList = new List(dictionary);
        resultList.setNumericKeyActions(false);
        resultList.setFixedSelection(List.FIXED_CENTER);
        setListStyle();
        resultList.addActionListener((ActionListener) this);

        //Remove un-needed components
        GetDictionaryForm().removeComponent(smileLabel);
        GetDictionaryForm().removeComponent(startTimeLabel);
        if (msgNotFound != null) {
            GetDictionaryForm().removeComponent(msgNotFound);
        }

        resPanel = new Container(new BorderLayout());
        Label resLbl = new Label("Results");
        resLbl.getStyle().setBgColor(0xFFFFFF, false);
        resPanel.addComponent(BorderLayout.NORTH, resLbl);
        resPanel.addComponent(BorderLayout.CENTER, resultList);
        resPanel.getStyle().setBorder(Border.createLineBorder(1));
        resPanel.getStyle().setBgColor(0xFFFFFF, false);
        resPanel.getStyle().setBgSelectionColor(0xFFFFFF, false);
        resPanel.getStyle().setBgTransparency(0, false);
        GetDictionaryForm().addComponent(BorderLayout.CENTER, resPanel);

        resultList.requestFocus();

        GetDictionaryForm().invalidate();
        GetDictionaryForm().repaint();
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

