package ornagai.mobile;

import java.io.*;
import javax.microedition.midlet.*;

import com.sun.lwuit.*;
import com.sun.lwuit.animations.Transition3D;
import com.sun.lwuit.util.*;
import com.sun.lwuit.events.*;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.layouts.BoxLayout;
import com.sun.lwuit.layouts.FlowLayout;
import com.sun.lwuit.list.DefaultListCellRenderer;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.Style;
import java.util.Vector;
import javax.microedition.amms.control.PanControl;

/**
 * @author Thar Htet
 * @author Seth N. Hetu
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
    private Vector dictionaryData;
    private Vector wordListData;
    private final int cluster_prefix_length = 2;
    private final String file_suffix = "_mz.txt";
    private final String window_title = "Ornagai Mobile";
    private boolean debug = false;


    //Some properties
    private boolean fileConnectSupported = false;
    private boolean fileConnectEnabled = false;
    private String fs = "/";

    //We're trying to show how long it takes to load the
    // dictionary, but this will probably require fiddling with the
    // debug flag. 
    private long startTimeMS;


    public void startApp() {
        //Count
        startTimeMS = System.currentTimeMillis();

        /**
         *  Initialize LWUIT Display
         * */
        Display.init(this);

        //Load properties
        this.fileConnectSupported = (System.getProperty("microedition.io.file.FileConnection.version")!=null);
        this.fs = System.getProperty("file.separator");
        if (this.fs==null)
            this.fs = "/";

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
            getSearchIndex(kindaBigSearch);
        }
        startTimeMS = System.currentTimeMillis() - startTimeMS;
        startTimeLabel.setText("Time to load: " + startTimeMS/1000.0F + " s");
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

        Style listCellStyle = new Style();
        listCellStyle.setBgColor(0xFFFFFF, false);
        listCellStyle.setBgTransparency(0, false);
        listCellStyle.setFgColor(0x000000);
        listCellStyle.setBgSelectionColor(0xDD1111);
        listCellStyle.setFgSelectionColor(0xffffff);

        DefaultListCellRenderer dlcr = new DefaultListCellRenderer(false);
        dlcr.setStyle(listCellStyle);
        resultList.getStyle().setBorder(Border.createEmpty());
        resultList.getStyle().setMargin(0, 5, 5, 5);
        resultList.getStyle().setBgColor(0xFFFFFF, false);
        resultList.getStyle().setBgSelectionColor(0xFFFFFF, false);
        resultList.setListCellRenderer(dlcr);
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    private String[] parseDictionaryEntry(String entry) {
        int firstPipe = entry.indexOf('|');
        if (firstPipe==-1)
            return null;
        String firstWord = entry.substring(0, firstPipe++);

        int secondPipe = entry.indexOf('|', firstPipe);
        if (secondPipe==-1)
            return null;
        String secondWord = entry.substring(firstPipe, secondPipe++);

        String thirdWord = entry.substring(secondPipe, entry.length());

        return new String[]{firstWord, secondWord, thirdWord};
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
            //Break into substrings, if possible. Else, just show what we have
            String entry = (String)dictionaryData.elementAt(resultList.getSelectedIndex());
            String[] entries = parseDictionaryEntry(entry);
            if (entries!=null)
                resultDisplay.setText(entries);
            else
                resultDisplay.setText(entry);
            

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

            System.gc();
            System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
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


    private int getSearchIndex(String query) {
        //Get the prefix string
        //System.out.println("QUERY:" + query);
        String prefix = query.substring(0, Math.min(cluster_prefix_length, query.trim().length()));
        //System.out.println("PREFIX:" + prefix);

        //Detect if the file exist
        InputStream is;
        try {
            is = this.getClass().getResourceAsStream("/txt/" + prefix.toLowerCase() + file_suffix);
            if (debug) {
                System.out.println("AOK : " + "txt/" + prefix.toLowerCase() + file_suffix);
            }
        } catch (Exception e) {
            is = this.getClass().getResourceAsStream("/txt/exception" + file_suffix);
            if (debug) {
                System.out.println("BOK");
            }
        }

        //Load and Search
        return extractAndSearch(read_dict(is), query);
    }


    private void searchAndDisplayResult(String query) {
        int indexToSelect = getSearchIndex(query);

        //When there are optional words available
        if (wordListData.size() > 0) {
            resultList = new List(wordListData);
            resultList.setNumericKeyActions(false);
            resultList.setFixedSelection(List.FIXED_LEAD);
            setListStyle();
            resultList.addActionListener((ActionListener) this);
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
            dictionaryForm.removeComponent(startTimeLabel);
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
