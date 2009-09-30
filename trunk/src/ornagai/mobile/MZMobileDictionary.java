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
    private Form dictionaryForm;
    private Form optionsForm;
    private Form splashForm;
    private Form resultForm;
    private Resources resourceObject;
    private TextField searchField;
    private RoundButton searchBtn;
    private Label ornagaiLabel;
    private Label mysteryZillionLabel;
    private Label logo;
    private Label smileLabel;
    private Label startTimeLabel;
    private List resultList;
    private Container resPanel;
    private ZawgyiComponent resultDisplay;
    private ZawgyiComponent msgNotFound;
    private final int cluster_prefix_length = 2;
    private final String file_suffix = "_mz.txt";
    private final String window_title = "Ornagai Mobile";
    private boolean debug = false;

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
                System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
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

        // Init the Forms
        splashForm = new Form(window_title);
        dictionaryForm = new Form(window_title);
        optionsForm = new Form(window_title);
        resultForm = new Form(window_title);

        /**
         * Initialize Splash Form
         * */
        Container topContainer = new Container();
        topContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        ornagaiLabel = new Label("Ornagai.com");
        ornagaiLabel.setAlignment(Label.CENTER);
        mysteryZillionLabel = new Label("Mysteryzillion.org");
        mysteryZillionLabel.setAlignment(Label.CENTER);
        logo = new Label(resourceObject.getImage("splashgif"));
        logo.setAlignment(Label.CENTER);
        topContainer.addComponent(ornagaiLabel);
        topContainer.addComponent(mysteryZillionLabel);

        splashForm.setLayout(new BorderLayout());
        splashForm.addComponent(BorderLayout.NORTH, topContainer);
        splashForm.addComponent(BorderLayout.CENTER, logo);


        splashForm.setTransitionOutAnimator(
                Transition3D.createCube(300, false));

        // Start Command is added under Thread method.
        //splashForm.addCommand(exitCommand);
        splashForm.addCommand(optionsCommand);
        splashForm.setCommandListener((ActionListener) this);



        /**
         * Prepare Dictionary Form related objects
         * */
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

        // Prepare Dictionary Form layout
        dictionaryForm.setLayout(new BorderLayout());

        Container searchPanel = new Container(new BorderLayout());
        searchPanel.addComponent(BorderLayout.NORTH, searchField);
        searchPanel.addComponent(BorderLayout.EAST, searchBtn);
        dictionaryForm.addComponent(BorderLayout.NORTH, searchPanel);


        dictionaryForm.addComponent(BorderLayout.CENTER, smileLabel);
        if (debug)
            dictionaryForm.addComponent(BorderLayout.SOUTH, startTimeLabel);
        dictionaryForm.setScrollable(false);

        // Add commands and listener
        dictionaryForm.addCommand(exitCommand);
        dictionaryForm.addCommand(searchCommand);
        dictionaryForm.setCommandListener((ActionListener) this);

        dictionaryForm.setTransitionOutAnimator(
                Transition3D.createCube(300, false));

        //Prepare options form layout
        optionsForm.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        optionsForm.getStyle().setPadding(10, 10, 10, 10);

        optionsForm.addCommand(cancelCommand);
        optionsForm.addCommand(saveCommand);
        optionsForm.setCommandListener((ActionListener) this);

        //Add our first option panel
        Container extDictionaryPanel = new Container(new BorderLayout());
        extDictionaryPanel.getStyle().setBorder(Border.createRoundBorder(10, 10, 0x333333));
        extDictionaryPanel.getStyle().setBgColor(0xDDDDFF);
        extDictionaryPanel.getStyle().setBgTransparency(255);
        extDictionaryPanel.getStyle().setPadding(5, 5, 5, 5);
        optionsForm.addComponent(extDictionaryPanel);

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
            currExternalPath = new TextField();
            currExternalPath.getStyle().setBgSelectionColor(0x233136);
            currExternalPath.getStyle().setFgSelectionColor(0xffffff);
            extDictionaryPanel.addComponent(BorderLayout.CENTER, currExternalPath);

            //Button to clear, button to set
            Container bottomRow = new Container(new FlowLayout(Container.RIGHT));
            browseBtn = new RoundButton("Browse...");
            browseBtn.getStyle().setBgSelectionColor(0x233136);
            browseBtn.getStyle().setFgSelectionColor(0xffffff);
            browseBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    FileChooser.browseForFile(optionsForm, currExternalPath.getText(), new String[]{"mzdict.zip"}, new Image[]{fcDictionaryIcon}, fcFolderIconFull, fcFolderIconEmpty, fcRootIcon, fcBackIcon, new ActionListener() {
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


        /**
         * Prepare Result Form
         */
        resultDisplay = new ZawgyiComponent();
        resultDisplay.setFocusable(false);

        resultForm.setLayout(new BorderLayout());
        //resultForm.addComponent(BorderLayout.CENTER, resultDisplay);
        resultForm.setScrollable(false);

        resultForm.addCommand(exitCommand);
        resultForm.addCommand(backCommand);
        resultForm.setCommandListener((ActionListener) this);
        resultForm.setTransitionOutAnimator(
                Transition3D.createCube(300, true));

        setTheme();
        splashForm.show();
        splashForm.addCommand(startCommand);

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


    private void setTheme() {

        Style headerStyle = new Style();
        headerStyle.setFgColor(0xffffff);
        headerStyle.setBgImage(resourceObject.getImage("ornagai"));
        headerStyle.setScaleImage(false, true);

        Style menuStyle = new Style();
        menuStyle.setBgColor(0x37464A);
        menuStyle.setFgColor(0xffffff);
        
        Style basicFormStyle = new Style();
        basicFormStyle.setBgColor(0xE5FFC5);

        dictionaryForm.setTitleStyle(headerStyle);
        optionsForm.setTitleStyle(headerStyle);
        splashForm.setTitleStyle(headerStyle);
        resultForm.setTitleStyle(headerStyle);
        dictionaryForm.setMenuStyle(menuStyle);
        optionsForm.setMenuStyle(menuStyle);
        splashForm.setMenuStyle(menuStyle);
        resultForm.setMenuStyle(menuStyle);

        dictionaryForm.setStyle(basicFormStyle);
        optionsForm.setStyle(basicFormStyle);
        splashForm.setStyle(basicFormStyle);
        resultForm.setStyle(basicFormStyle);

        mysteryZillionLabel.setStyle(basicFormStyle);
        ornagaiLabel.setStyle(basicFormStyle);
        smileLabel.setStyle(basicFormStyle);
        startTimeLabel.setStyle(basicFormStyle);
        startTimeLabel.getStyle().setFgColor(0xFF0000);
        logo.setStyle(basicFormStyle);

        Style searchFieldStyle = new Style();
        searchFieldStyle.setBgColor(0xffffff);
        searchFieldStyle.setFgColor(0x000000);
        searchFieldStyle.setBgSelectionColor(0x666666);
        searchFieldStyle.setFgSelectionColor(0xffffff);
        searchFieldStyle.setBorder(Border.createRoundBorder(6, 6));
        searchField.setStyle(searchFieldStyle);
    }

    private void setListStyle() {

        /*Style listCellStyle = new Style();
        listCellStyle.setBgColor(0xFFFFFF, false);
        listCellStyle.setBgTransparency(0, false);
        listCellStyle.setFgColor(0x000000);
        listCellStyle.setBgSelectionColor(0xDD1111);
        listCellStyle.setFgSelectionColor(0xffffff);*/

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

            optionsForm.show();
        }

        if (ae.getCommand() == startCommand) {
            dictionaryForm.show();
        }

        if (ae.getCommand() == backCommand) {
            dictionaryForm.show();
            //If we come back from result page
            //surely this list will be available.
            resultList.requestFocus();
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
            splashForm.show();
        }

        if (ae.getCommand() == cancelCommand) {
            //Go back
            splashForm.show();
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

                    //Show a new panel in the same form. This permits more reasonable
                    // tabbing back and forth between results.
                    dictionaryForm.removeComponent(smileLabel);
                    dictionaryForm.removeComponent(startTimeLabel);
                    if (msgNotFound != null) {
                        dictionaryForm.removeComponent(msgNotFound);
                    }
                    if (resPanel != null) {
                        dictionaryForm.removeComponent(resPanel);
                    }
                    dictionaryForm.addComponent(BorderLayout.CENTER, resultDisplay);
                    dictionaryForm.repaint();

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
            dictionaryForm.removeComponent(smileLabel);//If smile is in place
            dictionaryForm.removeComponent(startTimeLabel);
            if (resultList != null) {
                dictionaryForm.removeComponent(resultList);//If result list is in place
            }
            dictionaryForm.addComponent(BorderLayout.CENTER, msgNotFound);
            dictionaryForm.invalidate();
            dictionaryForm.repaint();
            return;
        }

        //Now, re-load and show our dictionary list
        resultList = new List(dictionary);
        resultList.setNumericKeyActions(false);
        resultList.setFixedSelection(List.FIXED_CENTER);
        setListStyle();
        resultList.addActionListener((ActionListener) this);

        //Remove un-needed components
        dictionaryForm.removeComponent(smileLabel);
        dictionaryForm.removeComponent(startTimeLabel);
        if (msgNotFound != null) {
            dictionaryForm.removeComponent(msgNotFound);
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
        dictionaryForm.addComponent(BorderLayout.CENTER, resPanel);

        resultList.requestFocus();

        dictionaryForm.invalidate();
        dictionaryForm.repaint();
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

