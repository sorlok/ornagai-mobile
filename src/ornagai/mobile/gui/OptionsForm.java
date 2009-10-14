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
import ornagai.mobile.filebrowser.FileChooser;

/**
 *
 * @author Seth N. Hetu
 */
public class OptionsForm extends Form implements ActionListener {
    //Components
    private TextField currExternalPath;
    private RoundButton browseBtn;

    //Resources
    private Image fcDictionaryIcon;
    private Image fcRootIcon;
    private Image fcFolderIconFull;
    private Image fcFolderIconEmpty;
    private Image fcBackIcon;

    //Commands
    private Command saveCommand;
    private Command cancelCommand;

    //Control
    private FormController formSwitcher;

    public OptionsForm(String title, Resources resourceObject, FormController formSwitcher)  {
        super(title);
        this.addComponents(resourceObject);
        this.formSwitcher = formSwitcher;
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
        currExternalPath.getStyle().setBgSelectionColor(0x233136);
        currExternalPath.getStyle().setFgSelectionColor(0xffffff);

        //Label
        Label extDictLbl = new Label("External Dictionary");
        extDictLbl.getStyle().setBgTransparency(0);
        extDictLbl.getStyle().setFgColor(0x444444);
        extDictionaryPanel.addComponent(BorderLayout.NORTH, extDictLbl);

        //Disabled?
        if (!MZMobileDictionary.fileConnectSupported || !MZMobileDictionary.fileConnectEnabled) {
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

            //Button to clear, button to set
            Container bottomRow = new Container(new FlowLayout(Container.RIGHT));
            browseBtn = new RoundButton("Browse...");
            browseBtn.getStyle().setBgSelectionColor(0x233136);
            browseBtn.getStyle().setFgSelectionColor(0xffffff);
            browseBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    FileChooser.browseForFile(OptionsForm.this, currExternalPath.getText(), new String[]{"mzdict.zip"}, new Image[]{fcDictionaryIcon}, fcFolderIconFull, fcFolderIconEmpty, fcRootIcon, fcBackIcon, new ActionListener() {
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
        this.setMenuStyle(MZMobileDictionary.GetMenuStyle());
        this.setStyle(MZMobileDictionary.GetBasicFormStyle());
        this.setTitleStyle(MZMobileDictionary.GetHeaderStyle());
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
            formSwitcher.reloadDictionary();

            //Go back
            formSwitcher.switchToSplashForm();
        }

        if (ae.getCommand() == cancelCommand) {
            //Go back
            formSwitcher.switchToSplashForm();
        }
    }

    public void show() {
        //Always start with the browse button in-focus.
        browseBtn.requestFocus();

        super.show();
    }

}
