/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile.filebrowser;

import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.*;
import com.sun.lwuit.plaf.*;
import java.util.Vector;
import ornagai.mobile.RoundButton;
import ornagai.mobile.gui.FormController;

/**
 * A simple dialog for error messages that prompts the user to
 *  exit the program.
 *
 * @author Seth N. Hetu
 */
public class ErrorDialog {
    //Control
    private static FormController controller;
    private static int screenwidth;

    //Show
    private static Form errorForm;
    private static Container detailedErrorMsg;
    private static Label[] labelPool = new Label[10];
    static {
        for (int i=0; i<labelPool.length; i++) {
            labelPool[i] = new Label();
            labelPool[i].getStyle().setBgTransparency(0);
            labelPool[i].getStyle().setFgColor(0xDDDDDD);
            labelPool[i].getStyle().setPadding(0, 0, 0, 0);
            labelPool[i].getStyle().setMargin(0, 0, 0, 0);
        }
    }
    private static Label errorTitle;
    private static RoundButton exitBtn;
    private static Label exceptionTxt;

    //Keep it internal
    protected ErrorDialog(){}


    //Simple call and return, then wait for the user to kill the app
    //  Throwable can be null; errorMsg cannot
    //  String can have newlines
    public static void showErrorMessage(String errorMsg, Throwable specificExcept, FormController controller, int screenwidth) {
        //Init
        if (ErrorDialog.errorForm == null)
            ErrorDialog.createErrorForm();

        //Save
        ErrorDialog.controller = controller;
        ErrorDialog.screenwidth = screenwidth;

        //We have to wrap this manually... LWUIT is giving us a hard time
        //First segment
        Vector segments = new Vector();
        StringBuffer currLine = new StringBuffer();
        for (int i=0; i<errorMsg.length(); i++) {
            char c = errorMsg.charAt(i);
            currLine.append(c);
            if (c==' ' || c=='\t' || c=='\n' || i==errorMsg.length()-1) {
                segments.addElement(currLine.toString());
                currLine.delete(0, currLine.length());
            }
        }

        //Next, add
        detailedErrorMsg.removeAll();
        int currIndex = 0;
        currLine.delete(0, currLine.length());
        for (int i=0; i<segments.size(); i++) {
            String seg = (String)segments.elementAt(i);
            int testLength = detailedErrorMsg.getStyle().getFont().stringWidth(currLine.toString() + seg);
            boolean isNL = false; //Not perfect, but should work fairly well with our segmentation algorithm.
            if (seg.charAt(seg.length()-1)=='\n') {
                isNL = true;
                seg = seg.substring(0, seg.length()-1);
            }

            //Append, and avoid looping forever on empty lines.
            if ((testLength<=screenwidth || currLine.length()==0) && !isNL) {
                currLine.append(seg);
                seg = "";
            }

            //Break?
            if (testLength>screenwidth || i==segments.size()-1 || isNL) {
                //Add
                Label lbl = labelPool[currIndex++];
                lbl.setText(currLine.toString().replace('\n', ' ').trim());
                detailedErrorMsg.addComponent(lbl);

                //Too many lines?
                if (currIndex>=labelPool.length)
                    break;

                //Clear, add
                currLine.delete(0, currLine.length());
                currLine.append(seg);
            }
        }

        //Set text
        exceptionTxt.setText(" ");
        if (specificExcept != null)
            exceptionTxt.setText(specificExcept.getClass().getName());

        //Show
        ErrorDialog.errorForm.show();
    }

    private static void createErrorForm() {
        //Top-level form
        errorForm = new Form();
        errorForm.setLayout(new BorderLayout());
        errorForm.getStyle().setBgColor(0x000000);

        //Add some labels
        errorTitle = new Label("Error!");
        errorTitle.getStyle().setBgTransparency(0);
        errorTitle.getStyle().setFgColor(0xCC0000);
        errorTitle.getStyle().setFont(Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE));
        errorForm.addComponent(BorderLayout.NORTH, errorTitle);
        exceptionTxt = new Label();
        exceptionTxt.getStyle().setBgTransparency(0);
        exceptionTxt.getStyle().setFgColor(0xBBBBBB);
        exceptionTxt.getStyle().setBorder(Border.createEmpty());
        errorForm.addComponent(BorderLayout.SOUTH, exceptionTxt);

        //Prepare our detailed error message panel
        detailedErrorMsg = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        detailedErrorMsg.getStyle().setBgTransparency(0);
        detailedErrorMsg.getStyle().setBorder(Border.createEmpty());

        //Prepare our "exit" button
        exitBtn = new RoundButton("Exit Program");
        exitBtn.getStyle().setBorder(Border.createLineBorder(1, 0xFFFFFF));
        exitBtn.getStyle().setBgColor(0x666666);
        exitBtn.getStyle().setBgSelectionColor(0x666666);
        exitBtn.getStyle().setFgColor(0xDDDDDD);
        exitBtn.getStyle().setFgSelectionColor(0xDDDDDD);
        exitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                controller.closeProgram();
            }
        });

        //Add our main panel
        Container mainPnl = new Container(new BorderLayout());
        mainPnl.addComponent(BorderLayout.CENTER, detailedErrorMsg);
        Container smallPnl = new Container(new FlowLayout(Component.CENTER));
        smallPnl.addComponent(exitBtn);
        mainPnl.addComponent(BorderLayout.SOUTH, smallPnl);
        errorForm.addComponent(BorderLayout.CENTER, mainPnl);
    }
}


