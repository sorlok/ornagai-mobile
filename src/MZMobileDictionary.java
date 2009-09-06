
import java.io.*;
import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.util.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.list.DefaultListCellRenderer;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import java.util.Vector;

/**
 * @author Thar Htet
 */
public class MZMobileDictionary extends MIDlet implements ActionListener {

    private Command exitCommand;
    private Command searchCommand;
    private Command startCommand;
    private Command backCommand;
    private Form dictionaryForm;
    private Form splashForm;
    private Form resultForm;
    private Resources resourceObject;
    private TextField searchField;
    private Label ornagaiLabel;
    private Label mysteryZillionLabel;
    private Label logo;
    private Label smileLabel;
    private List resultList;
    private ZawgyiComponent resultDisplay;
    private ZawgyiComponent msgNotFound;
    private Vector dictionaryData;
    private Vector wordListData;
    private final int cluster_prefix_length = 2;
    private final String file_suffix = "_mz.txt";
    private final String window_title = "Ornagai Mobile";
    private boolean debug = false;

    public void startApp() {

        /**
         *  Initialize LWUIT Display
         * */
        Display.init(this);

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
        backCommand = new Command("Back");

        // Init the Forms
        splashForm = new Form(window_title);
        dictionaryForm = new Form(window_title);
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
        splashForm.addCommand(exitCommand);
        splashForm.setCommandListener((ActionListener) this);

        /**
         * Prepare Dictionary Form related objects
         * */
        searchField = new TextField();
        smileLabel = new Label(resourceObject.getImage("smile"));
        smileLabel.setText(" ");
        smileLabel.setAlignment(Label.CENTER);

        // Prepare Dictionary Form layout
        dictionaryForm.setLayout(new BorderLayout());
        dictionaryForm.addComponent(BorderLayout.NORTH, searchField);
        dictionaryForm.addComponent(BorderLayout.CENTER, smileLabel);
        dictionaryForm.setScrollable(false);

        // Add commands and listener
        dictionaryForm.addCommand(exitCommand);
        dictionaryForm.addCommand(searchCommand);
        dictionaryForm.setCommandListener((ActionListener) this);

        dictionaryForm.setTransitionOutAnimator(
                Transition3D.createCube(300, false));

        /**
         * Prepare Result Form
         */
        resultDisplay = new ZawgyiComponent();

        resultForm.setLayout(new BorderLayout());
        resultForm.addComponent(BorderLayout.CENTER, resultDisplay);
        resultForm.setScrollable(false);

        resultForm.addCommand(exitCommand);
        resultForm.addCommand(backCommand);
        resultForm.setCommandListener((ActionListener) this);
        resultForm.setTransitionOutAnimator(
                Transition3D.createCube(300, true));

        setTheme();
        splashForm.show();
        splashForm.addCommand(startCommand);
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
        splashForm.setTitleStyle(headerStyle);
        resultForm.setTitleStyle(headerStyle);
        dictionaryForm.setMenuStyle(menuStyle);
        splashForm.setMenuStyle(menuStyle);
        resultForm.setMenuStyle(menuStyle);

        dictionaryForm.setStyle(basicFormStyle);
        splashForm.setStyle(basicFormStyle);
        resultForm.setStyle(basicFormStyle);

        mysteryZillionLabel.setStyle(basicFormStyle);
        ornagaiLabel.setStyle(basicFormStyle);
        smileLabel.setStyle(basicFormStyle);
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

        Style listCellStyle = new Style();
        listCellStyle.setBgColor(0xE5FFC5);//(0x233136);
        listCellStyle.setFgColor(0x000000);
        listCellStyle.setBgSelectionColor(0x233136);
        listCellStyle.setFgSelectionColor(0xffffff);

        DefaultListCellRenderer dlcr = new DefaultListCellRenderer(false);
        dlcr.setStyle(listCellStyle);
        resultList.setBorderPainted(false);
        resultList.setListCellRenderer(dlcr);
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

        if (ae.getCommand() == startCommand) {
            dictionaryForm.show();
        }

        if (ae.getCommand() == backCommand) {
            dictionaryForm.show();
            //If we come back from result page
            //surely this list will be available.
            resultList.requestFocus();
        }

        if (ae.getSource() == (Object) resultList) {
            resultDisplay.setText(dictionaryData.elementAt(resultList.getSelectedIndex()).toString());
            resultForm.show();
        }
    }

    private String read_dict(InputStream is) {
        String s = "";
        try {

            s = readUnicodeFileUTF8(is);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //result.setText(s);
        return s;
    }

    //REF :http://www.j2meforums.com/forum/index.php?topic=9553.0
    private final String readUnicodeFileUTF8(InputStream is) {
        StringBuffer sb = new StringBuffer(256);
        try {
            int[] surrogatePair = new int[2];
            //InputStream is = this.getClass().getResourceAsStream(filename);

            int val = 0;
            int unicharCount = 0;
            while ((val = readNextCharFromStreamUTF8(is)) != -1) {
                unicharCount++;
                if (val <= 0xFFFF) {
                    // if first value is the Byte Order Mark (BOM), do not add
                    if (!(unicharCount == 1 && val == 0xFEFF)) {
                        sb.append((char) val);
                    }
                } else {
                    supplementCodePointToSurrogatePair(val, surrogatePair);
                    sb.append((char) surrogatePair[0]);
                    sb.append((char) surrogatePair[1]);
                }
            }
            is.close();
        } catch (Exception e) {
        }


        return new String(sb);
    }

    private final static int readNextCharFromStreamUTF8(InputStream is) {
        int c = -1;
        if (is == null) {
            return c;
        }
        boolean complete = false;

        try {
            int byteVal;
            int expecting = 0;
            int composedVal = 0;

            while (!complete && (byteVal = is.read()) != -1) {
                if (expecting > 0 && (byteVal & 0xC0) == 0x80) {  /* 10xxxxxx */
                    expecting--;
                    composedVal = composedVal | ((byteVal & 0x3F) << (expecting * 6));
                    if (expecting == 0) {
                        c = composedVal;
                        complete = true;
                    //System.out.println("appending: U+" + Integer.toHexString(composedVal) );
                    }
                } else {
                    composedVal = 0;
                    expecting = 0;
                    if ((byteVal & 0x80) == 0) {    /* 0xxxxxxx */
                        // one byte character, no extending byte expected
                        c = byteVal;
                        complete = true;
                    //System.out.println("appending: U+" + Integer.toHexString(byteVal) );
                    } else if ((byteVal & 0xE0) == 0xC0) {   /* 110xxxxx */
                        expecting = 1;  // expecting 1 extending byte
                        composedVal = ((byteVal & 0x1F) << 6);
                    } else if ((byteVal & 0xF0) == 0xE0) {   /* 1110xxxx */
                        expecting = 2;  // expecting 2 extending bytes
                        composedVal = ((byteVal & 0x0F) << 12);
                    } else if ((byteVal & 0xF8) == 0xF0) {   /* 11110xxx */
                        expecting = 3;  // expecting 3 extending bytes
                        composedVal = ((byteVal & 0x07) << 18);
                    } else {
                        // non conformant utf-8, ignore or catch error
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return c;
    }

    private final static void supplementCodePointToSurrogatePair(int codePoint, int[] surrogatePair) {
        int high4 = ((codePoint >> 16) & 0x1F) - 1;
        int mid6 = ((codePoint >> 10) & 0x3F);
        int low10 = codePoint & 0x3FF;

        surrogatePair[0] = (0xD800 | (high4 << 6) | (mid6));
        surrogatePair[1] = (0xDC00 | (low10));
    }

    private void searchAndDisplayResult(String query) {
        int indexToSelect;

        //Get the prefix string
        //System.out.println("QUERY:" + query);
        String prefix = query.substring(0, Math.min(cluster_prefix_length, query.trim().length()));
        //System.out.println("PREFIX:" + prefix);

        //Detect if the file exist
        InputStream is;
        try {
            is = this.getClass().getResourceAsStream("txt/" + prefix.toLowerCase() + file_suffix);
            if (debug) {
                System.out.println("AOK : " + "txt/" + prefix.toLowerCase() + file_suffix);
            }
        } catch (Exception e) {
            is = this.getClass().getResourceAsStream("txt/exception" + file_suffix);
            if (debug) {
                System.out.println("BOK");
            }
        }

        //Load and Search
        indexToSelect = extractAndSearch(read_dict(is), query);

        //When there are optional words available
        if (wordListData.size() > 0) {
            resultList = new List(wordListData);
            resultList.setNumericKeyActions(false);
            resultList.setFixedSelection(List.FIXED_LEAD);
            setListStyle();
            resultList.addActionListener((ActionListener) this);
            dictionaryForm.removeComponent(smileLabel);
            if (msgNotFound != null) {
                dictionaryForm.removeComponent(msgNotFound);
            }
            dictionaryForm.addComponent(BorderLayout.CENTER, resultList);
            dictionaryForm.invalidate();
            dictionaryForm.repaint();

            //If the result is found.
            if (indexToSelect != -1) {
                resultList.setSelectedIndex(indexToSelect);
                resultList.requestFocus();
                resultList.setFocus(true);
            } else {
                //No Eaxt word found.
                //Similar item list will still be shown.
            }

        } else {
            //Not even optional words.
            String message = query + " " + "\u104F\u0020\u1021\u1013\u102D\u1015\u1078\u102B\u101A\u1039\u1000\u102D\u102F\n" +
                    "\u1024\u1021\u1018\u102D\u1013\u102B\u1014\u1039\u1010\u103C\u1004\u1039\n" +
                    "\u1019\u101E\u103C\u1004\u1039\u1038\u101B\u1031\u101E\u1038\u1015\u102B\u104B";
            msgNotFound = new ZawgyiComponent();
            msgNotFound.setText(message);
            dictionaryForm.removeComponent(smileLabel);//If smile is in place
            if (resultList != null) {
                dictionaryForm.removeComponent(resultList);//If result list is in place
            }
            dictionaryForm.addComponent(BorderLayout.CENTER, msgNotFound);
            dictionaryForm.invalidate();
            dictionaryForm.repaint();
        }
    }

    /**
     * This method iterates the dictionary text file 
     * (whic is loaded based on prefixed index) and
     * search if the query word is found in the dictionary file.
     *
     * If the word is found, instantly break from loop and return
     * the word definition.
     *
     * If the word is NOT found, prepare the word list for display and
     * prepare the dictionary item list for later display, in case user
     * select one from list item.
     */
    private int extractAndSearch(String s, String query) {
        //Reset the two lists
        dictionaryData = new Vector();
        wordListData = new Vector();

        //Start index for next line.
        int start_index = 0;

        //The first index of item which match with searching word.
        int queryFoundIndex = -1;

        String line;
        //Till the end, split the lines based on ||
        while (start_index < s.trim().length()) {
            line = s.substring(start_index, s.indexOf("||", start_index)).trim();

            if (line.length() > 0) {
                //Only one assignment is allowed
                if (line.trim().toLowerCase().indexOf(query.toLowerCase() + "|") == 0 &&
                        queryFoundIndex == -1) {
                    queryFoundIndex = wordListData.size();
                    if (debug) {
                        System.out.println("Index found : " + queryFoundIndex);
                    }
                }
                dictionaryData.addElement(line);
                wordListData.addElement(line.substring(0, line.indexOf("|")) + " (" +
                        line.substring(line.indexOf("|") + 1, line.indexOf("|", line.indexOf("|") + 1)) + ")");
            }

            start_index += (line.length() + 2);
        }
        if (debug) {
            System.out.println("Number of entry loaded from Dict file : " + dictionaryData.size());
        }
        if (debug) {
            System.out.println("Number of words loaded from Dict file : " + wordListData.size());
        }
        return queryFoundIndex;
    }
}
