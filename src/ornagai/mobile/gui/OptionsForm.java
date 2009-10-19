package ornagai.mobile.gui;

import com.sun.lwuit.*;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.*;
import com.sun.lwuit.plaf.*;
import com.sun.lwuit.util.Resources;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import ornagai.mobile.MZMobileDictionary;
import ornagai.mobile.RoundButton;
import ornagai.mobile.filebrowser.AboutDialog;
import ornagai.mobile.filebrowser.FileChooser;

/**
 *
 * @author Seth N. Hetu
 */
public class OptionsForm extends Form implements ActionListener {
    //Static data
    private static boolean checkedFileConnect = false;
    public static boolean fileConnectSupported = false;
    public static boolean fileConnectEnabled = false;

    //Components
    private TextField currExternalPath;
    private RoundButton browseBtn;
    private Label memoryInUse;
    private Container memContainer;

    //Resources
    private Image fcDictionaryIcon;
    private Image fcRootIcon;
    private Image fcFolderIconFull;
    private Image fcFolderIconEmpty;
    private Image fcBackIcon;
    private Image fcBadIcon;
    private Image fcUnknownIcon;

    //Commands
    private Command saveCommand;
    private Command cancelCommand;

    //Control
    private FormController formSwitcher;

    public OptionsForm(String title, Resources resourceObject, FormController formSwitcher)  {
        super(title);

        if (!checkedFileConnect) {
            checkedFileConnect = true;

            //Load properties
            OptionsForm.fileConnectSupported = (System.getProperty("microedition.io.file.FileConnection.version")!=null);
            OptionsForm.fileConnectEnabled = FileChooser.IsFileConnectSupported();
        }

        this.addComponents(resourceObject);
        this.formSwitcher = formSwitcher;

        //Update form components
        System.gc();
        long memTotalKB = Runtime.getRuntime().totalMemory()/1024;
        long memUsedKB = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024;
        memoryInUse.setText(memUsedKB + " kb / " + memTotalKB + " kb  ");
        currExternalPath.setText(MZMobileDictionary.pathToCustomDict);

        System.out.println("OPTIONS FORM CREATED"); //Make sure it's only done once!
    }

    private void addComponents(Resources resourceObject) {
        //Create commands
        saveCommand = new Command("Save");
        cancelCommand = new Command("Cancel");

        //Prepare options form layout
        this.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        this.getStyle().setPadding(10, 10, 10, 10);

        this.addCommand(cancelCommand);
        this.addCommand(saveCommand);
        this.setCommandListener((ActionListener) this);
        this.setTransitionOutAnimator(Transition3D.createCube(300, true));

        //Add our first option panel
        Container extDictionaryPanel = new Container(new BorderLayout());
        extDictionaryPanel.getStyle().setBorder(Border.createRoundBorder(10, 10, 0x333333));
        extDictionaryPanel.getStyle().setBgColor(0xDDDDFF);
        extDictionaryPanel.getStyle().setBgTransparency(255);
        extDictionaryPanel.getStyle().setPadding(5, 5, 5, 5);
        this.addComponent(extDictionaryPanel);

        //Path label
        currExternalPath = new TextField();
        Style searchFieldStyle = new Style();
        searchFieldStyle.setBgColor(0xffffff);
        searchFieldStyle.setFgColor(0x000000);
        searchFieldStyle.setBgSelectionColor(0x666666);
        searchFieldStyle.setFgSelectionColor(0xffffff);
        searchFieldStyle.setBorder(Border.createRoundBorder(6, 6));
        currExternalPath.setStyle(searchFieldStyle);

        //Label
        Label extDictLbl = new Label("External Dictionary");
        extDictLbl.getStyle().setBgTransparency(0);
        extDictLbl.getStyle().setFgColor(0x444444);
        extDictionaryPanel.addComponent(BorderLayout.NORTH, extDictLbl);

        //Disabled?
        if (!OptionsForm.fileConnectSupported || !OptionsForm.fileConnectEnabled) {
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

            //Load resources for file browser
            fcDictionaryIcon = resourceObject.getImage("fc_dictionary");
            fcRootIcon = resourceObject.getImage("fc_root");
            fcFolderIconFull = resourceObject.getImage("fc_folder_full");
            fcFolderIconEmpty = resourceObject.getImage("fc_empty_folder");
            fcBackIcon = resourceObject.getImage("fc_back");
            fcBadIcon = resourceObject.getImage("fc_bad");
            fcUnknownIcon = resourceObject.getImage("fc_unknown");

            //Button to clear, button to set
            Container bottomRow = new Container(new FlowLayout(Container.RIGHT));
            browseBtn = new RoundButton("Browse...");
            browseBtn.getStyle().setBgSelectionColor(0x233136);
            browseBtn.getStyle().setFgSelectionColor(0xffffff);
            browseBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    FileChooser.browseForFile(OptionsForm.this, currExternalPath.getText(), new String[]{"mzdict.zip"}, new Image[]{fcDictionaryIcon}, fcFolderIconFull, fcFolderIconEmpty, fcRootIcon, fcBackIcon, fcBadIcon, fcUnknownIcon, new ActionListener() {
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

        //Add our second option panel
        Container aboutPanel = new Container(new BorderLayout());
        aboutPanel.getStyle().setBorder(Border.createRoundBorder(10, 10, 0x333333));
        aboutPanel.getStyle().setBgColor(0xDDDDFF);
        aboutPanel.getStyle().setBgTransparency(255);
        aboutPanel.getStyle().setPadding(5, 10, 5, 5);
        aboutPanel.getStyle().setMargin(Container.TOP, 5);
        this.addComponent(aboutPanel);

        //Label
        Label aboutDictLbl = new Label("About");
        aboutDictLbl.getStyle().setBgTransparency(0);
        aboutDictLbl.getStyle().setFgColor(0x444444);
        aboutPanel.addComponent(BorderLayout.NORTH, aboutDictLbl);

        //About button
        RoundButton aboutDictBtn = new RoundButton("About this Dictionary");
        aboutDictBtn.getStyle().setBgSelectionColor(0x233136);
        aboutDictBtn.getStyle().setFgSelectionColor(0xffffff);
        aboutDictBtn.getStyle().setMargin(Container.RIGHT, 5);
        aboutDictBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showAboutWindow();
            }
        });

        //Panel to hold it
        Container aboutBtnContainer = new Container(new FlowLayout(Container.CENTER));
        aboutBtnContainer.addComponent(aboutDictBtn);
        aboutPanel.addComponent(BorderLayout.CENTER, aboutBtnContainer);

        //Add a debugging label
        memContainer = new Container(new FlowLayout(Container.CENTER));
        memContainer.getStyle().setPadding(Container.TOP, 15);
        memoryInUse = new Label();
        memoryInUse.getStyle().setBgTransparency(0);
        memoryInUse.getStyle().setFgColor(0x009900);
        memoryInUse.getStyle().setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL));
        memContainer.addComponent(memoryInUse);
        this.addComponent(memContainer);

        //Set styles
        this.setMenuStyle(MZMobileDictionary.GetMenuStyle());
        this.setStyle(MZMobileDictionary.GetBasicFormStyle());
        this.setTitleStyle(MZMobileDictionary.GetHeaderStyle());
    }


    private void showAboutWindow() {
        AboutDialog.showAboutMessage(this);
    }


    private void setDictionaryPath(String path) {
        if (path!=null) {
            currExternalPath.setText(path);
            //TODO: Load it
        }
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getCommand() == saveCommand) {
            //Save path
            try {
                byte[] path = currExternalPath.getText().getBytes();
                RecordStore properties = RecordStore.openRecordStore(MZMobileDictionary.RECORD_STORE_ID, true);
                properties.setRecord(MZMobileDictionary.RECORD_DICT_PATH, path, 0, path.length);
                properties.closeRecordStore();
            } catch (RecordStoreException ex) {
                System.out.println("Error saving path: " + ex.toString());
            }

            //Reset model
            boolean loadOK = formSwitcher.reloadDictionary();
            System.out.println("Load ok: " + loadOK);

            //Go back
            if (loadOK)
                formSwitcher.switchToSplashForm();
        }

        if (ae.getCommand() == cancelCommand) {
            //Go back
            formSwitcher.switchToSplashForm();
        }
    }

    public void show() {
        //Always start with the browse button in-focus.
        if (browseBtn!=null)
            browseBtn.requestFocus();

        super.show();
    }

}
