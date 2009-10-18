package ornagai.mobile.filebrowser;

import com.sun.lwuit.*;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.events.*;
import com.sun.lwuit.events.ActionListener;
import com.sun.lwuit.layouts.*;
import com.sun.lwuit.plaf.*;
import java.util.Vector;
import ornagai.mobile.RoundButton;
import ornagai.mobile.gui.FormController;

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

        //aboutForm.addComponent(new Label("About..."));


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
        aboutForm.setTransitionOutAnimator(Transition3D.createCube(300, false));
    }

}


