package ornagai.mobile.filebrowser;

import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.*;
import com.sun.lwuit.plaf.*;
import ornagai.mobile.MZMobileDictionary;

/**
 *
 * @author Seth N. Hetu
 */
public class AboutDialog {
    //Show
    private static Form aboutForm;

    //Commands
    private static Command okCommand;

    //Return
    private static Form callingForm;

    //Keep it internal
    protected AboutDialog(){}


    //Simple call and return, then wait for the user to kill the app
    //  Throwable can be null; errorMsg cannot
    //  String can have newlines
    public static void showAboutMessage(Form callingForm) {
        //Init
        if (AboutDialog.aboutForm == null)
            AboutDialog.createAboutForm();

        //Save
        AboutDialog.callingForm = callingForm;

        //Show
        AboutDialog.aboutForm.show();
    }

    private static void createAboutForm() {
        //Top-level form
        aboutForm = new Form();
        aboutForm.setLayout(new BorderLayout());
        aboutForm.getStyle().setBgColor(0x000000);
        aboutForm.setTitle("About Ornagai Mobile");

        //Main body label
        Container aboutLbl = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        aboutLbl.getStyle().setPadding(0, 0, 0, 0);
        String[] lines = new String[]{"Version 2.0","License: MIT (Open Source)","Notable features:",
           "  * 30,000 words","  * Small & fast","  * Powerful search","  * Cross-platform",
           "  * Supports custom ","     dictionaries"};
        for (int i=0; i<lines.length; i++) {
            Label newLine = new Label(lines[i]);
            newLine.getStyle().setBgTransparency(0);
            newLine.getStyle().setBorder(Border.createEmpty());
            newLine.getStyle().setPadding(0, 0, 0, 0);
            newLine.getStyle().setMargin(0, 0, 0, 0);
            aboutLbl.addComponent(newLine);
        }
        aboutForm.addComponent(BorderLayout.CENTER, aboutLbl);

        //Bottom part
        Container lowerPart = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        lowerPart.getStyle().setPadding(0, 0, 0, 0);
        Label copyright1 = new Label("Original code by:");
        Label copyright1a = new Label("    Ko Thar Thar (\u00A9 2009)");
        Label copyright2 = new Label("Improvements by:");
        Label copyright2a = new Label("    Seth N. Hetu (\u00A9 2009)");
        copyright1.getStyle().setBgTransparency(0);
        copyright1a.getStyle().setBgTransparency(0);
        copyright1a.getStyle().setFgColor(0x0000FF);
        copyright2.getStyle().setBgTransparency(0);
        copyright2a.getStyle().setBgTransparency(0);
        copyright2a.getStyle().setFgColor(0x0000FF);
        copyright1.getStyle().setPadding(0, 0, 0, 0);
        copyright1.getStyle().setMargin(0, 0, 0, 0);
        copyright1a.getStyle().setPadding(0, 0, 0, 0);
        copyright1a.getStyle().setMargin(0, 0, 0, 0);
        copyright2.getStyle().setPadding(0, 0, 0, 0);
        copyright2.getStyle().setMargin(5, 0, 0, 0);
        copyright2a.getStyle().setPadding(0, 0, 0, 0);
        copyright2a.getStyle().setMargin(0, 0, 0, 0);
        lowerPart.addComponent(copyright1);
        lowerPart.addComponent(copyright1a);
        lowerPart.addComponent(copyright2);
        lowerPart.addComponent(copyright2a);
        aboutForm.addComponent(BorderLayout.SOUTH, lowerPart);

        //Commands and control (and transitions)
        okCommand = new Command("Ok");
        aboutForm.addCommand(okCommand);
        aboutForm.setCommandListener((ActionListener) new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (ae.getCommand() == AboutDialog.okCommand) {
                    AboutDialog.callingForm.show();
                    AboutDialog.callingForm = null;
                }
            }
        });
        aboutForm.setTransitionOutAnimator(MZMobileDictionary.GetTransitionRight());

       //Match styles to the other forms, not to dialogs
       aboutForm.setTitleStyle(MZMobileDictionary.GetHeaderStyle());
       aboutForm.setMenuStyle(MZMobileDictionary.GetMenuStyle());
       aboutForm.setStyle(MZMobileDictionary.GetBasicFormStyle());
    }

}


