package ornagai.mobile.gui;

import com.sun.lwuit.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.*;
import com.sun.lwuit.plaf.*;
import java.io.IOException;
import ornagai.mobile.*;
import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;
import ornagai.mobile.dictionary.MMDictionary;

/**
 *
 * @author Seth N. Hetu
 */
public class DictionaryForm extends Form implements ActionListener {
    //Components
    private Label startTimeLabel;
    private Label smileLabel;
    private Container searchPanel;
    private TextField searchField;
    private RoundButton searchBtn;
    private ZawgyiComponent resultDisplay;
    private ZawgyiComponent msgNotFound;
    private List resultList;
    private Container resPanel;
    private Label resLbl;

    //Commands
    private Command exitCommand;
    private Command searchCommand;

    //Styles
    private Style searchFieldStyle;

    //Model
    private MMDictionary dictionary;

    //Control
    private FormController formSwitcher;

    //Debug
    private boolean oneTimeMemoryMsg;

    public DictionaryForm(String title, Image smileImage, MMDictionary dictionary, FormController formSwitcher)  {
        super(title);
        this.addComponents(smileImage);
        this.setModel(dictionary);
        this.formSwitcher = formSwitcher;
        System.out.println("DICTIONARY FORM CREATED"); //Make sure it's only done once!
    }

    private void addComponents(Image smileImage) {
        //Create commands
        exitCommand = new Command("Exit");
        searchCommand = new Command("Search");

        //Set layout
        this.setLayout(new BorderLayout());

        //Init (but don't add) the result display(s)
        resultDisplay = new ZawgyiComponent();
        resultDisplay.setFocusable(false);
        msgNotFound = new ZawgyiComponent();
        resPanel = new Container(new BorderLayout());
        resLbl = new Label("Results");
        resLbl.getStyle().setBgTransparency(0x0);
        resPanel.addComponent(BorderLayout.NORTH, resLbl);
        resPanel.getStyle().setBorder(Border.createLineBorder(1));
        resPanel.getStyle().setBgColor(0xFFFFFF);
        //resPanel.getStyle().setBgSelectionColor(0xFFFFFF, false);
        resPanel.getStyle().setBgTransparency(0xFF);
        
        //Create the smile and splash labels
        smileLabel = new Label(smileImage);
        smileLabel.setText(" ");
        smileLabel.setAlignment(Label.CENTER);
        startTimeLabel = new Label("");
        startTimeLabel.setAlignment(Label.CENTER);
        smileLabel.setStyle(MZMobileDictionary.GetBasicFormStyle());
        startTimeLabel.setStyle(MZMobileDictionary.GetBasicFormStyle());
        startTimeLabel.getStyle().setFgColor(0xFF0000);

        //Create components used for searching
        searchField = new TextField();
        searchBtn = new RoundButton("Search");
        searchBtn.getStyle().setBgSelectionColor(0x233136);
        searchBtn.getStyle().setFgSelectionColor(0xffffff);
        searchBtn.getStyle().setMargin(Component.BOTTOM, 10);
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (searchField.getText().trim().length() > 0) {
                    searchAndDisplayResults(searchField.getText().trim());
                }
            }
        });

        //Add components used for searching.
        searchPanel = new Container(new BorderLayout());
        searchPanel.addComponent(BorderLayout.NORTH, searchField);
        searchPanel.addComponent(BorderLayout.EAST, searchBtn);
        this.addComponent(BorderLayout.NORTH, searchPanel);

        //Add the smile label
        this.addComponent(BorderLayout.CENTER, smileLabel);
        if (MZMobileDictionary.debug)
            this.addComponent(BorderLayout.SOUTH, startTimeLabel);
        this.setScrollable(false);

        //Add commands and listener; set transition
       this.addCommand(exitCommand);
       this.addCommand(searchCommand);
       this.setCommandListener((ActionListener) this);
       this.setTransitionOutAnimator(MZMobileDictionary.GetTransitionRight());

       //Set styles
       this.setTitleStyle(MZMobileDictionary.GetHeaderStyle());
       this.setMenuStyle(MZMobileDictionary.GetMenuStyle());
       this.setStyle(MZMobileDictionary.GetBasicFormStyle());

       //One more style
       searchFieldStyle = new Style();
       searchFieldStyle.setBgColor(0xffffff);
       searchFieldStyle.setFgColor(0x000000);
       searchFieldStyle.setBgSelectionColor(0x666666);
       searchFieldStyle.setFgSelectionColor(0xffffff);
       searchFieldStyle.setBorder(Border.createRoundBorder(6, 6));
       searchField.setStyle(searchFieldStyle);

    }

    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getCommand() == exitCommand) {
            formSwitcher.closeProgram();
        }

        if (ae.getCommand() == searchCommand) {
            if (searchField.getText().trim().length() > 0) {
                searchAndDisplayResults(searchField.getText().trim());
            }
        }

        if (ae.getSource() == resultList) {
            //Get this entry based on its ID
            DictionaryListEntry entry = (DictionaryListEntry)resultList.getSelectedItem();

            //Need to search?
            if (entry.id==-2) {
                //First, how many words BACK should we search?
                String spelling = entry.word;
                int countBack = 0;
                for (int i=resultList.getSelectedIndex()-1; i>0; i--) {
                    String prevSpelling = ((DictionaryListEntry)dictionary.getItemAt(i)).word;
                    if (prevSpelling.equals(spelling))
                        countBack++;
                    else
                        break;
                }

                entry.id = dictionary.findWordIDFromEntry(entry, countBack);
            }

            //Invalid? Not found?
            if (entry.id==-1) {
                //Just go back to the entry list
                searchField.requestFocus();
                resultList.setVisible(false);
                removeCentralComponent();
                repaint();
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
                this.removeCentralComponent();
                this.addComponent(BorderLayout.CENTER, resultDisplay);
                resultDisplay.setVisible(true);
                this.repaint();

                if (!oneTimeMemoryMsg) {
                    oneTimeMemoryMsg = true;
                    System.gc();
                    System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
                }
            }
        }
    }


    public void setModel(MMDictionary model) {
        this.dictionary = model;

        //Special one-time actions
        if (this.dictionary==null) {
            //Remove our result List
            resPanel.removeComponent(resultList);
            resultList = null;
        } else {
            //Create a new list, since changing the model causes exceptions to be thrown
            resultList = new List(dictionary);
            resultList.setNumericKeyActions(false);
            resultList.setFixedSelection(List.FIXED_CENTER);
            resultList.addActionListener((ActionListener) this);
            resultList.setIsScrollVisible(true);

            //Set this list's style
            DictionaryRenderer dlcr = new DictionaryRenderer(0xFFBBBB, 0xDDDDDD);
            //dlcr.getStyle().setBgSelectionColor(0x111188);
            //dlcr.getStyle().setFgSelectionColor(0xFFFFFF);
            resultList.getStyle().setBorder(Border.createEmpty());
            resultList.getStyle().setMargin(0, 5, 5, 5);
            resultList.setItemGap(0);
            resultList.setListCellRenderer(dlcr);

            //Add it
            resPanel.addComponent(BorderLayout.CENTER, resultList);
        }
    }


    public void setStatusMessage(String msg) {
        startTimeLabel.setText(msg);
    }


    private void searchAndDisplayResults(String query) {
        //First, make sure we're done loading our dictionary
        formSwitcher.waitForDictionaryToLoad();

        //Clear our previous display, to save memory (has very little effect)
        resultDisplay.setVisible(false);
        resultDisplay.clearData();

        //For now, we only search for the first word
        String word = MZMobileDictionary.getFirstWord(query);
        if (word.length()==0)
            return;


        //Select the proper index and secondary data
        String errorMsg = null;
        try {
            //Nothing to search for?
            dictionary.performSearch(word.toString());
        } catch (IOException ex) {
            //Error message
            errorMsg = query + " " + "\u104F\u0020\u1021\u1013\u102D\u1015\u1078\u102B\u101A\u1039\u1000\u102D\u102F\n" +
                    "\u1024\u1021\u1018\u102D\u1013\u102B\u1014\u1039\u1010\u103C\u1004\u1039\n" +
                    "\u1019\u101E\u103C\u1004\u1039\u1038\u101B\u1031\u101E\u1038\u1015\u102B\u104B";
        }

        //Remove un-needed components
        removeCentralComponent();

        //In error?
        if (errorMsg!=null) {
            msgNotFound.setText(errorMsg, MMDictionary.FORMAT_ZG2008);

            this.addComponent(BorderLayout.CENTER, msgNotFound);
            msgNotFound.setVisible(true);
        } else {
            this.addComponent(BorderLayout.CENTER, resPanel);
            resPanel.setVisible(true);
            resultList.setVisible(true);
            resultList.requestFocus();
        }

        //Either way, repaint
        this.invalidate();
        this.repaint();
    }

    private void removeCentralComponent() {
        if (smileLabel.isVisible()) {
            this.removeComponent(smileLabel);//If smile is in place
            smileLabel.setVisible(false);
        }
        if (startTimeLabel.isVisible()) {
            this.removeComponent(startTimeLabel);
            startTimeLabel.setVisible(false);
        }
        if (msgNotFound.isVisible()) {
            this.removeComponent(msgNotFound);
            msgNotFound.setVisible(false);
        }
        if (resPanel!=null && resPanel.isVisible()) {
            this.removeComponent(resPanel);//If result panel is in place
            resPanel.setVisible(false);
        }
        if (resultDisplay.isVisible()) {
            this.removeComponent(resultDisplay);
            resultDisplay.setVisible(false);
        }
    }

}

