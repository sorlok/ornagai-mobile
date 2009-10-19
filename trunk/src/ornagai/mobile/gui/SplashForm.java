package ornagai.mobile.gui;

import com.sun.lwuit.*;
import com.sun.lwuit.events.ActionEvent;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.*;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import ornagai.mobile.MZMobileDictionary;

/**
 *
 * @author Seth N. Hetu
 */
public class SplashForm extends Form implements ActionListener {
    //Components
    private Container topContainer;
    private Label ornagaiLabel;
    private Label mysteryZillionLabel;
    private Label logo;

    //Commands
    private Command optionsCommand;
    private Command startCommand;

    //Control
    private FormController formSwitcher;

    public SplashForm(String title, Image splashImage, FormController formSwitcher)  {
        super(title);
        this.addComponents(splashImage);
        this.formSwitcher = formSwitcher;
        System.out.println("SPLASH FORM CREATED"); //Make sure it's only done once!
    }

    private void addComponents(Image splashImage) {
        //Create commands
        startCommand = new Command("Start");
        optionsCommand = new Command("Options");

        //Create the top part of the container
        topContainer = new Container();
        topContainer.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        ornagaiLabel = new Label("Ornagai.com");
        ornagaiLabel.setAlignment(Label.CENTER);
        mysteryZillionLabel = new Label("Mysteryzillion.org");
        mysteryZillionLabel.setAlignment(Label.CENTER);
        logo = new Label(splashImage);
        logo.setAlignment(Label.CENTER);
        topContainer.addComponent(ornagaiLabel);
        topContainer.addComponent(mysteryZillionLabel);

        //Add the top part and the logo
        this.setLayout(new BorderLayout());
        this.addComponent(BorderLayout.NORTH, topContainer);
        this.addComponent(BorderLayout.CENTER, logo);

        //Set transitions and commands
        this.setTransitionOutAnimator(MZMobileDictionary.GetTransitionRight());
        this.addCommand(optionsCommand);
        this.addCommand(startCommand);
        this.setCommandListener(this);

        //Set style
        this.setTitleStyle(MZMobileDictionary.GetHeaderStyle());
        this.setStyle(MZMobileDictionary.GetBasicFormStyle());
        this.setMenuStyle(MZMobileDictionary.GetMenuStyle());
        mysteryZillionLabel.setStyle(MZMobileDictionary.GetBasicFormStyle());
        ornagaiLabel.setStyle(MZMobileDictionary.GetBasicFormStyle());
        logo.setStyle(MZMobileDictionary.GetBasicFormStyle());
    }

    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getCommand() == optionsCommand) {
            //Switch transition
            this.setTransitionOutAnimator(MZMobileDictionary.GetTransitionLeft());

            //Set text
            try {
                RecordStore properties = RecordStore.openRecordStore(MZMobileDictionary.RECORD_STORE_ID, true);
                byte[] b = properties.getRecord(MZMobileDictionary.RECORD_DICT_PATH);
                String path = b==null ? "" : new String(b);
                MZMobileDictionary.pathToCustomDict = path;
                properties.closeRecordStore();
            } catch (RecordStoreException ex) {
                System.out.println("Error reading record store: " + ex.toString());
            }

            //Show form, highlight the "browse" button
            this.formSwitcher.switchToOptionsForm();
        }

        if (ae.getCommand() == startCommand) {
            //Switch transition
            this.setTransitionOutAnimator(MZMobileDictionary.GetTransitionRight());

            //Show the dictionary form
            this.formSwitcher.switchToDictionaryForm();
        }
    }



}
