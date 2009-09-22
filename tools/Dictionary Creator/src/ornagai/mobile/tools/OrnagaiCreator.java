/*
 * OrnagaiCreator.java
 *
 * Created on Sep 10, 2009, 1:10:13 AM
 */

package ornagai.mobile.tools;


import java.awt.Color;
import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import sevenzip.compression.lzma.Encoder;


/**
 *
 * @author Seth N. Hetu
 */
public class OrnagaiCreator extends javax.swing.JApplet {
    //State
    private File dictionaryFile = null;
    private int numColumns = 0;
    private int colidWord = 0;
    private int colidPOS = 0;
    private int colidDefinition = 0;
    private int sampleTextID = -1;

    //State, page 2
    private String newFilePrefix = "mydict";
    private String newFileSuffix = ".mzdict.zip";
    private File newFileDirectory = new File(".");
    private int lumpSizeKb = 200;

    //For display and reordering.
    private ArrayList<String[]> allDictEntries = new ArrayList<String[]>();

    //For general usage
    private Random rand = new Random();


    //In case we want to start this with a "main" entry
    /*public static void main(String[] args) {
        new OrnagaiCreator().setVisible(true);
    }*/


    class DictionaryWord {
        String wordStr;
        String definitionStr;
        int wordID;

        public DictionaryWord(String wordStr, int wordID) {
            this.wordStr = wordStr;
            this.wordID = wordID;
        }
    }

    class LookupNode {
        Hashtable<Character, LookupNode> jumpTable = new Hashtable<Character, LookupNode>();
        ArrayList<DictionaryWord> primaryMatches = new ArrayList<DictionaryWord>();
        ArrayList<DictionaryWord> secondaryMatches = new ArrayList<DictionaryWord>();
        LookupNode parent;
        int id;
        int startBitID;
        int totalSum;

        public LookupNode(LookupNode parent, int id) {
            this.parent = parent;
            this.id = id;
        }
    }


    //Read a file, load up some random entries in the hash table, etc.
    private void readNewDictionaryFile(File newFile) {
        //Set form elements
        dictionaryFile = newFile;
        txtPathToDictionary1.setText(dictionaryFile.getAbsolutePath());
        newFileDirectory = dictionaryFile.getParentFile();
        txtTblRow2Value.setText(newFileDirectory.getAbsolutePath());

        //Don't update
        sampleTextID = -1;

        //Read each entry, make sure all lines have the same number of tabs, etc.
        allDictEntries.clear();
        String errorMsg = "";
        int[] totalEntryLengths = null;
        int demoWord = 0;
        try {
            BufferedReader inFile = new BufferedReader(new InputStreamReader(new FileInputStream(dictionaryFile), Charset.forName("UTF-8")));
            int lineID = 0;
            for (String line = inFile.readLine(); line!=null; line=inFile.readLine()) {
                //Skip empty lines
                line = line.trim();
                lineID++;
                if (line.length()==0)
                    continue;

                //Break
                String[] entries = line.split("\\t");
                if (allDictEntries.size()==0) {
                    numColumns = entries.length;
                    totalEntryLengths = new int[numColumns];
                } else if (numColumns != entries.length) {
                    errorMsg = "Line ["+lineID+"] contains " + entries.length + " entries when " + numColumns + " was expected.";
                    break;
                }

                //Store
                allDictEntries.add(entries);
                for (int i=0; i<numColumns; i++)
                    totalEntryLengths[i] += entries[i].length();

                //Pick a good demo word
                if (demoWord==0) {
                    if (line.contains("scion"))
                        demoWord = allDictEntries.size()-1;
                }
            }
            inFile.close();
        } catch (FileNotFoundException ex) {
            errorMsg = "File not found: " + ex.toString();
        } catch (UnsupportedEncodingException ex) {
            errorMsg = "Unsupported encoding: " + ex.toString();
        } catch (IOException ex) {
            errorMsg = "IO exception: " + ex.toString();
        }

        //Problem?
        if (errorMsg.length()>0) {
            JOptionPane.showMessageDialog(this, errorMsg, "Error Reading Dictionary File", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Add entries to the combo boxes.
        cmbStructWord1.removeAllItems();
        cmbStructPOS1.removeAllItems();
        cmbStructDef1.removeAllItems();
        for (int i=0; i<=numColumns; i++) {
            String newRow = (i<numColumns) ? "Column " + (i+1) : "(Don't include)";
            cmbStructWord1.addItem(newRow);
            cmbStructPOS1.addItem(newRow);
            cmbStructDef1.addItem(newRow);
        }

        //Determine which column is which.
        colidWord = numColumns;
        colidPOS = numColumns;
        colidDefinition = numColumns;
        if (numColumns==1)
            colidWord = 0;
        else if (numColumns==2) {
            colidWord = 0;
            colidDefinition = 1;
        } else {
            //First: the longest column is the definition list
            colidDefinition = 0;
            for (int i=1; i<numColumns; i++) {
                if (totalEntryLengths[i] > totalEntryLengths[colidDefinition])
                    colidDefinition = i;
            }

            //Next: the first non-definition row is the word
            for (colidWord=0; colidWord<numColumns; colidWord++) {
                if (colidWord != colidDefinition)
                    break;
            }

            //Finally: the first non-word, non-definition row is the pos
            for (colidPOS=0; colidPOS<numColumns; colidPOS++) {
                if ((colidPOS!=colidWord) && (colidPOS!=colidDefinition))
                    break;
            }
        }

        //Update our combo boxes.
        cmbStructWord1.setSelectedIndex(colidWord);
        cmbStructPOS1.setSelectedIndex(colidPOS);
        cmbStructDef1.setSelectedIndex(colidDefinition);

        //Finally, pick a word and show it in our Sample label
        sampleTextID = demoWord;
        reloadSampleText();

        //And now that that's all done....
        recheckEnabledComponents();
    }


    //Reload our "sample" panel
    private void reloadSampleText() {
        //Clear, and possibly do nothing
        lblSampleEntry1.setText("");
        if (sampleTextID==-1)
            return;

        //Load our entry by ID
        String[] entry = allDictEntries.get(sampleTextID);
        String word = (colidWord<numColumns) ? entry[colidWord] : "";
        String pos = (colidPOS<numColumns) ? entry[colidPOS] : "";
        String defRaw = (colidDefinition<numColumns) ? entry[colidDefinition] : "";

        //Add some optional hyphens to myanmar words, in obvious places
        StringBuffer def = new StringBuffer();
        for (int i=0; i<defRaw.length(); i++) {
            char c = defRaw.charAt(i);
            def.append(c);
            ZawgyiSegment.SEMANTICS sem = ZawgyiSegment.getSemantics(c);
            if (sem == ZawgyiSegment.SEMANTICS.MY_VIRAMA) {
                //Generally obvious cases
                if (i+1<defRaw.length()) {
                    char nextC = defRaw.charAt(i+1);
                    ZawgyiSegment.SEMANTICS nextSem = ZawgyiSegment.getSemantics(nextC);
                    if (nextSem == ZawgyiSegment.SEMANTICS.MY_BASE || nextSem==ZawgyiSegment.SEMANTICS.MY_LEADING) {
                        def.append("&shy;");
                    }
                }
            }
        }

        //Now, get our entries and build an HTML string
        StringBuffer sb = new StringBuffer();
        sb.append("<html><div style=\"background:#DDDDDD; width:142px; height:87px;\">");
        sb.append("<b>"+word+"</b>");
        sb.append("<br>");
        sb.append("<i>"+pos+"</i>");
        sb.append("<br>");
        sb.append(def.toString());
        sb.append("</div></html>");
        lblSampleEntry1.setText(sb.toString());
    }

    //Which components should and should not be shown?
    private void recheckEnabledComponents() {
        //Show stage 2 dictionary components?
        Component[] dictComponents = new Component[]{cmbStructWord1, cmbStructPOS1, cmbStructDef1, btnRandomDictEntry1, btnNext1};
        for (Component cmp : dictComponents) {
            cmp.setEnabled(dictionaryFile!=null);
        }
    }


    class ProgressBarUpdater implements Runnable {
        private int amount;
        private String text;
        public ProgressBarUpdater(int amount, String text) {
            this.amount = amount;
            this.text = text;
        }
        public void run() {
            progCreation.setValue(amount);
            lblProgressAmt.setText(text);
        }
    }
    private void updateProgressBar(int amount, String text) {
        SwingUtilities.invokeLater(new ProgressBarUpdater(amount, text));
    }
    private void updateProgressBar(String[] messages, int currMsgID) {
        int newVal = (currMsgID*100)/(messages.length-1);
        updateProgressBar(newVal, messages[currMsgID]);
    }


    private void createDictionaryFile() {
        //Signals
        String[] progStates = new String[]{
            "Creating un-compressed files.",
            "LZMA-compressing files.",
            "Zipping all files.",
            "Done"
        };

        //Signal
        updateProgressBar(progStates, 0);

        //Get a list of files to compress (File[] {source, lzma})
        Hashtable<String, File[]> toZipFiles = null;
        if (cmbTblRow3Value.getSelectedIndex()==0) {
            toZipFiles = createTextDictionaryFiles();
        } else {
            toZipFiles = createBinaryOptimizedDictionaryFiles();
        }

        //Signal
        updateProgressBar(progStates, 1);

        //Compress them all via lzma
        if (!compressAllFiles(toZipFiles))
            return;

        //Signal
        updateProgressBar(progStates, 2);

        //Step 1: Make a new zip archive
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(new FileOutputStream(new File(newFileDirectory, newFilePrefix + newFileSuffix)));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Can't create zip file: " + newFilePrefix + newFileSuffix + ": " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Max compression
        zipOut.setLevel(9);

        //Step 2: Add each file:
        byte[] buff = new byte[1024];
        for (String prefix : toZipFiles.keySet()) {
            //Get our file
            //File tempFile = toZipFiles.get(prefix)[0]; //source
            File tempFile = toZipFiles.get(prefix)[0];   //uncompressed
            FileInputStream in = null;
            try {
                in = new FileInputStream(tempFile);
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Can't read tempoprary file: " + tempFile.getName() + ": " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Add a zip header
            try {
                zipOut.putNextEntry(new ZipEntry(prefix+"."+(cmbTblRow3Value.getSelectedIndex()==0?"txt":"bin")));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error adding entry: " + prefix + " :" + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Read all data from this file, and add it to our zip file
            int len = 0;
            for (;;) {
                try {
                    len = in.read(buff);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error reading entry: " + tempFile.getName() + ": " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (len>0) {
                    //Write it
                    try {
                        zipOut.write(buff, 0, len);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(this, "Error writing entry: " + prefix + ".lzma: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else
                    break;
            }

            //Complete the entry
            try {
                zipOut.closeEntry();
                in.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error closing streams for entry: " + tempFile.getName() + ": " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        //Complete the archive
        try {
            zipOut.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error closing dictionary output file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Signal
        updateProgressBar(progStates, 3);

        //Done!
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                lblDone.setVisible(true);
                lblDoneDetails.setVisible(true);
                btnOpenFolder3.setVisible(true);
            }
        });
    }


    //Compresses them in-place, using temporary files
    private boolean compressAllFiles(Hashtable<String, File[]> files) {
        byte[] buff = new byte[1024];
        for (String prefix : files.keySet()) {
            //Get our file, make a new output file.
            File inFile = files.get(prefix)[0]; //Source
            File outFile = null;

            try {
                outFile = File.createTempFile(prefix, "lzma", newFileDirectory);
                outFile.deleteOnExit();
                files.get(prefix)[1] = outFile;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Couldn't make temporary lzma file: " + prefix + ".lzma: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            //Just use the core LZMA source; the streams seem to fail
            BufferedInputStream inStream = null;
            BufferedOutputStream outStream = null;
            try {
                inStream  = new BufferedInputStream(new FileInputStream(inFile));
                outStream = new BufferedOutputStream(new FileOutputStream(outFile));

                //Default values for compression
                boolean eos = false;
		int Algorithm = 2;
		int MatchFinder = 1;
                int DictionarySize = 1 << 23;
		int Fb = 128;
		int Lc = 3;
		int Lp = 0;
		int Pb = 2;

                //Encode
                Encoder encoder = new Encoder();
                if (!encoder.SetAlgorithm(Algorithm))
                    throw new IllegalArgumentException("Incorrect compression mode");
                if (!encoder.SetDictionarySize(DictionarySize))
                    throw new IllegalArgumentException("Incorrect dictionary size");
                if (!encoder.SetNumFastBytes(Fb))
                    throw new IllegalArgumentException("Incorrect -fb value");
                if (!encoder.SetMatchFinder(MatchFinder))
                    throw new IllegalArgumentException("Incorrect -mf value");
                if (!encoder.SetLcLpPb(Lc, Lp, Pb))
                    throw new IllegalArgumentException("Incorrect -lc or -lp or -pb value");
                encoder.SetEndMarkerMode(eos);
                encoder.WriteCoderProperties(outStream);
                long fileSize;
                if (eos)
                    fileSize = -1;
                else
                    fileSize = inFile.length();
                for (int i = 0; i < 8; i++)
                    outStream.write((int)(fileSize >>> (8 * i)) & 0xFF);
                encoder.Code(inStream, outStream, -1, -1, null);

                //Done
                outStream.flush();
                outStream.close();
                inStream.close();
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Error compressing temporary file: " + prefix + ".lzma: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return false;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error compressing temporary file: " + prefix + ".lzma: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return false;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Bad compression arguments for file: " + prefix + ".lzma: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return false;
            } catch (OutOfMemoryError ex) {
                JOptionPane.showMessageDialog(this, "Out of memory for file: " + prefix + ".lzma, some temporary files coulud not be removed.", "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                try {
                    inStream.close();
                } catch (Exception ex2) {}
                try {
                    outStream.close();
                } catch (Exception ex2) {}
                return false;
            }
        }

        //Success
        return true;
    }


    private Hashtable<String, File[]> createTextDictionaryFiles() {
        Hashtable<String, File[]> toZipFiles = new Hashtable<String, File[]>();

        //Make a single UTF-8 file with every entry in WORD/POS/DEF, zg2009 format
        BufferedWriter outFile = null;
        String prefix = "words-tabwpd-zg2009";        
	try {
            File temp = File.createTempFile(prefix, "txt", newFileDirectory);
            temp.deleteOnExit();
            toZipFiles.put(prefix, new File[]{temp, null});
            outFile = new BufferedWriter(new PrintWriter(temp, "UTF-8"));
	} catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Cannot output to file: " + prefix+".txt", "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
	} catch (UnsupportedEncodingException ex) {
            JOptionPane.showMessageDialog(this, "Out file encoding not supported (UTF-8).", "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
	} catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error making temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        //Now, add each row
        for (String[] row : allDictEntries) {
            String word = (colidWord<numColumns) ? row[colidWord] : "";
            String pos = (colidPOS<numColumns) ? row[colidPOS] : "";
            String definition = (colidDefinition<numColumns) ? row[colidDefinition] : "";

            try {
                outFile.write(word + "\t" + pos + "\t" + definition + "\n");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error writing temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        try {
            outFile.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error closing temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return toZipFiles;
    }


    //Orders all words, assigns the "total sum" variable, too
    private int buildInOrderDictionary(LookupNode currNode, ArrayList<DictionaryWord> newDictionary) {
        //First, handle all primary matches here
        for (DictionaryWord word : currNode.primaryMatches)
            newDictionary.add(word);

        //Now, handle all children, from left to right
        int totalSum = currNode.primaryMatches.size();
        for (char c='a'; c<='z'; c++) {
            if (currNode.jumpTable.containsKey(c)) {
                totalSum += buildInOrderDictionary(currNode.jumpTable.get(c), newDictionary);
            }
        }
        currNode.totalSum = totalSum;

        return totalSum;
    }


    private Hashtable<String, File[]> createBinaryOptimizedDictionaryFiles() {
        Hashtable<String, File[]> toZipFiles = new Hashtable<String, File[]>();

        //Necessary for phase 2
        ArrayList<Integer> numDefinitionsPerLump = new ArrayList<Integer>();
        ArrayList<DictionaryWord> wordsInDictionary = new ArrayList<DictionaryWord>();

        //Start with the lump files, since we'll need their sizes later
        ArrayList<Character> lettersAsEncountered = new ArrayList<Character>();
        //Hashtable<String, String> wordToDefinition = new Hashtable<String, String>();
        int bytesPerLump = lumpSizeKb*1024;
        for (int rowID=0; rowID<allDictEntries.size(); rowID++) {
            //Locate data
            String[] row = allDictEntries.get(rowID);
            String word = (colidWord<numColumns) ? row[colidWord] : "";
            DictionaryWord newWord = new DictionaryWord(word, -1);
            String combinedDef = null;
            {
                String pos = (colidPOS<numColumns) ? row[colidPOS] : "";
                String definition = (colidDefinition<numColumns) ? row[colidDefinition] : "";
                combinedDef = pos+"\t"+definition;
            }
            newWord.definitionStr = combinedDef;
            wordsInDictionary.add(newWord);
        }

        //Gather word list data
        ArrayList<Character> lettersInWordlist = new ArrayList<Character>();
        //ArrayList<Integer> sizeOfWords = new ArrayList<Integer>();
        int longestWord = 0;
        for (DictionaryWord entry  : wordsInDictionary) {
            //Get letter semantics
            String word = entry.wordStr;
            int length = 0;
            for (char c : word.toCharArray()) {
                if (c=='\n' || c=='\r')
                    continue;
                if (!lettersInWordlist.contains(c)) {
                    //System.out.println("Letter: " + Integer.toHexString(c));
                    lettersInWordlist.add(c);
                }
                length++;
            }
            
            //Longest word?
           // sizeOfWords.add(length);
            if (length > longestWord)
                longestWord = length;
        }

        //Build a reverse lookup for encoding our bitstream
        Hashtable<Character, Integer> reverseLookup2 = new Hashtable<Character, Integer>();
        for (int i=0; i<lettersInWordlist.size(); i++)
            reverseLookup2.put(lettersInWordlist.get(i), i);

        //Make Lookup Table Cache
        int maxChildren = 0;
        int maxMatches = 0;
        ArrayList<LookupNode> nodesById = new ArrayList<LookupNode>();
        LookupNode topNode = new LookupNode(null, nodesById.size());
        nodesById.add(topNode);
        for (DictionaryWord entry : wordsInDictionary) {
            String word = entry.wordStr;

            LookupNode currNode = topNode;
            boolean isPrimary = true;
            for (int i=0; i<word.length(); i++) {
                char letter = word.charAt(i);

                //Only track a through z
                boolean wordBreak = false;
                if (letter>='a' && letter<='z') {
                    //Jump to it, add it.
                    if (!currNode.jumpTable.containsKey(letter)) {
                        LookupNode newNode = new LookupNode(currNode, nodesById.size());
                        nodesById.add(newNode);
                        currNode.jumpTable.put(letter, newNode);

                        maxChildren = Math.max(maxChildren, currNode.jumpTable.size());
                    }
                    currNode = currNode.jumpTable.get(letter);
                } else if (letter!='-') {
                    //Break if we haven't added this word yet
                    if (i>0 && word.charAt(i-1)>='a' && word.charAt(i-1)<='z')
                        wordBreak = true;
                }

                //Word break?
                if (wordBreak || i==word.length()-1) {
                    //Add it
                    //int id = wordStartBitIds.get(wordID);
                    if (isPrimary)
                        currNode.primaryMatches.add(entry);
                    else
                        currNode.secondaryMatches.add(entry);

                    //Count
                    maxMatches = Math.max(maxMatches, Math.max(currNode.primaryMatches.size(), currNode.secondaryMatches.size()));

                    //Reset
                    currNode = topNode;

                    //No more primary words
                    isPrimary = false;
                }
            }
        }

        //Sort all lists
        for (LookupNode ln : nodesById) {
            //Sort primary and secondary match array
            Collections.sort(ln.primaryMatches, new Comparator<DictionaryWord>() {
                public int compare(DictionaryWord o1, DictionaryWord o2) {
                    return o1.wordStr.compareTo(o2.wordStr);
                }
            });
            Collections.sort(ln.secondaryMatches, new Comparator<DictionaryWord>() {
                public int compare(DictionaryWord o1, DictionaryWord o2) {
                    return o1.wordStr.compareTo(o2.wordStr);
                }
            });
        }

        //Traverse our tree once and use this to re-order the wordsInDictionary array
        {
            ArrayList<DictionaryWord> newDict = new ArrayList<DictionaryWord>();
            if (buildInOrderDictionary(topNode, newDict) != wordsInDictionary.size())
                throw new RuntimeException("Not all nodes were re-ordered");

            wordsInDictionary = newDict;

            //Re-id
            for (int i=0; i<wordsInDictionary.size(); i++) {
                wordsInDictionary.get(i).wordID = i;
            }
        }

        //Build our lump files
        ArrayList<Character> currDefinitionStream = new ArrayList<Character>();
        ArrayList<Integer> currDefinitionLengths = new ArrayList<Integer>();
        int currLumpID = 1;
        for (int rowID=0; rowID<wordsInDictionary.size(); rowID++) {
            //Append this entry
            String word = wordsInDictionary.get(rowID).wordStr;
            String combinedDef = wordsInDictionary.get(rowID).definitionStr;
            for (char c : combinedDef.toCharArray()) {
                if (c=='\n' || c=='\r')
                    continue;
                currDefinitionStream.add(c);
                if (!lettersAsEncountered.contains(c))
                    lettersAsEncountered.add(c);
            }
            currDefinitionLengths.add(combinedDef.length());

            //Time to write a new lump file?
            if (currDefinitionStream.size()>=bytesPerLump || rowID==wordsInDictionary.size()-1) {
                //Save for later
                numDefinitionsPerLump.add(currDefinitionLengths.size());

                //Create (temporary)
                BufferedOutputStream currLumpFile = null;
                String currLumpPrefix = "lump_" + currLumpID;
                try {
                    File temp = File.createTempFile(currLumpPrefix, "bin", newFileDirectory);
                    temp.deleteOnExit();
                    toZipFiles.put(currLumpPrefix, new File[]{temp, null});
                    currLumpFile = new BufferedOutputStream(new FileOutputStream(temp));
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(this, "Cannot output to file: " + currLumpPrefix+".bin", "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                    return null;
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error making temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                    return null;
                }

                //Write data, validate as we go
                try {
                    //Number of definitions: 3 bytes
                    writeNumber(currLumpFile, "<lump_num_definitions>", currDefinitionLengths.size(), 3);

                    //Number of unique letters: 2 bytes
                    writeNumber(currLumpFile, "<lump_num_unique_letters>", lettersAsEncountered.size(), 2);

                    //Number of letters in each definition: 2 bytes
                    for (int defSize : currDefinitionLengths)
                        writeNumber(currLumpFile, "<lump_def_size>", defSize, 2);

                    //Unicode value for each letter: 2 bytes
                    for (char uniqueLetter : lettersAsEncountered)
                        writeNumber(currLumpFile, "<lump_unique_letter_value>", uniqueLetter, 2);

                    //Build a reverse lookup for encoding our bitstream
                    Hashtable<Character, Integer> reverseLookup = new Hashtable<Character, Integer>();
                    for (int i=0; i<lettersAsEncountered.size(); i++)
                        reverseLookup.put(lettersAsEncountered.get(i), i);

                    //Finally, re-encode the definition stream as a bit-stream
                    int[] utilData = new int[]{0, 0, 0}; //rembits, numrembits, byteswritten
                    int bitsPerLetter = Integer.toBinaryString(lettersAsEncountered.size()-1).length();
                    BitOutputStream out = new BitOutputStream(currLumpFile);
                    for (char c : currDefinitionStream)
                        out.writeNumber(reverseLookup.get(c), bitsPerLetter);

                    //Any last byte?
                    if (utilData[1]>0)
                        currLumpFile.write((byte)(utilData[0]&0xFF));

                    //Note: Interesting to note
                    System.out.println(currDefinitionStream.size() + " bytes re-encoded to " + utilData[2] + " bytes, " + ((utilData[2]*100)/currDefinitionStream.size()) + "% of original");
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                    try {
                        currLumpFile.close();
                    } catch (IOException ex2) {}
                    return null;
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error writing to temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                    try {
                        currLumpFile.close();
                    } catch (IOException ex2) {}
                    return null;
                }


                //Close
                try {
                    currLumpFile.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error closing temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                    return null;
                }


                //Next
                currDefinitionStream.clear();
                currDefinitionLengths.clear();
                lettersAsEncountered.clear();
                currLumpID++;
            }
        }

        //Now, make the word_list-zg2009.bin file
        BufferedOutputStream wordlistFile = null;
        String wordlistPrefix = "word_list-zg2009";
        try {
            File temp = File.createTempFile(wordlistPrefix, "bin", newFileDirectory);
            temp.deleteOnExit();
            toZipFiles.put(wordlistPrefix, new File[]{temp, null});
            wordlistFile = new BufferedOutputStream(new FileOutputStream(temp));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Cannot output to file: " + wordlistPrefix+".bin", "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error making temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        //Append all data, make sure to track
        ArrayList<Integer> wordStartBitIds = new ArrayList<Integer>();
        try {
            //Number of words in dictionary, 3 bytes
            writeNumber(wordlistFile, "<wl_num_words>", wordsInDictionary.size(), 3);

            //Number of unique letters in the word list, 2 bytes
            writeNumber(wordlistFile, "<wl_unique_letters>", lettersInWordlist.size(), 2);

            //Length of the longest word in the dictionary, 2 bytes
            writeNumber(wordlistFile, "<wl_longest_word>", longestWord, 2);

            //Number of lump files, 2 bytes
            writeNumber(wordlistFile, "<wl_num_lumps>", currLumpID-1, 2);

            //Number of definitions in each lump, 3 bytes
            for (int numDef : numDefinitionsPerLump)
                writeNumber(wordlistFile, "<wl_defs_per_lump>", numDef, 3);

            //Unicode value of each letter, in order, 2 bytes
            for (char c : lettersInWordlist)
                writeNumber(wordlistFile, "<wl_letter_values>", c, 2);

            //Following is a bitstream, with X bits for the size of the word, and
            //  Y bits for the letters in that word
            int bitsPerLetter = Integer.toBinaryString(lettersInWordlist.size()-1).length();
            int bitsPerSize = Integer.toBinaryString(longestWord-1).length();

            System.out.println("bits per letter: " + bitsPerLetter);
            System.out.println("bits per size: " + bitsPerSize);
            BitOutputStream out = new BitOutputStream(wordlistFile);
            for (int i=0; i<wordsInDictionary.size(); i++) {
                //Save this location
                wordStartBitIds.add(out.getBitsWritten());

                //Write size
                String word = wordsInDictionary.get(i).wordStr;
                int size = word.length();
                out.writeNumber(size, bitsPerSize);

                //Write letters, re-encoded
                if (i==0)
                    System.out.println("word: " + word);
                for (char c : word.toCharArray()) {
                    if (c=='\n' || c=='\r')
                        continue;

                    out.writeNumber(reverseLookup2.get(c), bitsPerLetter);
                }
            }

            //Any last byte?
            out.flushRemaining();
            

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            try {
                wordlistFile.close();
            } catch (IOException ex2) {}
            return null;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing to temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            try {
                wordlistFile.close();
            } catch (IOException ex2) {}
            return null;
        }


        //Close
        try {
            wordlistFile.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error closing temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        
        
        //Now, serialze this
        int numNodes = nodesById.size();
        int maxBitID = wordStartBitIds.get(wordStartBitIds.size()-1);
        System.out.println("Number of nodes: " + numNodes + " : " + Integer.toBinaryString(numNodes-1).length());
        System.out.println("Max word bit id: " + maxBitID + " : " + Integer.toBinaryString(maxBitID-1).length());
        System.out.println("Most children: " + maxChildren + " : " + Integer.toBinaryString(maxChildren-1).length());
        System.out.println("Most matches: " + maxMatches + " : " + Integer.toBinaryString(maxMatches-1).length());


        //Get an idea of size:
        int bitsPerNumChildren = Integer.toBinaryString(maxChildren-1).length();
        int bitsPerNumMatches = Integer.toBinaryString(maxMatches-1).length();
        int bitsPerNodeID = Integer.toBinaryString(numNodes-1).length();
        int bitsPerWordBitID = Integer.toBinaryString(maxBitID-1).length();
        int bitsPerWordID = Integer.toBinaryString(wordsInDictionary.size()-1).length();
        int bitsPerLetter = Integer.toBinaryString('z'-'a').length();
        int sizeInBits = 0;
        for (int lnID=0; lnID<nodesById.size(); lnID++) {
            LookupNode ln = nodesById.get(lnID);

            //Index
            ln.startBitID = sizeInBits;

            //Counts
            //sizeInBits += bitsPerNumChildren + bitsPerNumMatches*2;

            //For each child
            sizeInBits += ln.jumpTable.size() * (bitsPerLetter + bitsPerNodeID);

            //For each match
            sizeInBits += ln.primaryMatches.size() * bitsPerWordBitID;
            sizeInBits += ln.secondaryMatches.size() * bitsPerWordBitID;
        }
        //Add constant data:
        sizeInBits += nodesById.size()*(bitsPerNumChildren + bitsPerNumMatches*2);
        int maxNodeStartBitID = nodesById.get(nodesById.size()-1).startBitID;
        int bitsPerNodeStartBitID = Integer.toBinaryString(maxNodeStartBitID-1).length();
        sizeInBits += nodesById.size() * bitsPerNodeStartBitID;
        int lumpsOpened = 0;

        System.out.println("Max node start bit id: " + maxNodeStartBitID + ":" + Integer.toBinaryString(maxNodeStartBitID-1).length());

        System.out.println("Total kb required: " + (sizeInBits/(8*1024)) + "  + headers");


        //And finally, write the file
        BufferedOutputStream lookupFile = null;
        BufferedOutputStream lookupVaryFile = null;
        String lookupPrefix = "lookup";
        String lookupVaryPrefix = "lookup_vary";
        try {
            //Lookup file
            File temp1 = File.createTempFile(lookupPrefix, "bin", newFileDirectory);
            temp1.deleteOnExit();
            toZipFiles.put(lookupPrefix, new File[]{temp1, null});
            lookupFile = new BufferedOutputStream(new FileOutputStream(temp1));

            //Variable-width lookup file
            File temp2 = File.createTempFile(lookupVaryPrefix, "bin", newFileDirectory);
            temp2.deleteOnExit();
            toZipFiles.put(lookupVaryPrefix, new File[]{temp2, null});
            lookupVaryFile = new BufferedOutputStream(new FileOutputStream(temp2));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Cannot output to file: " + wordlistPrefix +".bin", "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error making temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        }


        try {
            //Number of nodes in the lookup table: 3 bytes
            writeNumber(lookupFile, "<look_num_nodes>", numNodes, 3);

            //Number of max_children in the lookup table: 3 bytes
            writeNumber(lookupFile, "<look_max_children>", maxChildren, 3);

            //Number of max_matches in the lookup table: 3 bytes
            writeNumber(lookupFile, "<look_max_matches>", maxMatches, 3);

            //Value of max_bit_id in the lookup table: 3 bytes
            writeNumber(lookupFile, "<look_max_bitid>", maxBitID, 3);

            //Value of max_bit_id of nodes in the lookup table: 3 bytes
            writeNumber(lookupFile, "<look_max_node_bitid>", maxNodeStartBitID, 3);

            //Next, write all non-variable data:
            BitOutputStream outMain = new BitOutputStream(lookupFile);
            for (int lnID=0; lnID<nodesById.size(); lnID++) {
                LookupNode ln = nodesById.get(lnID);
                
                //Write number of totalChildren
               // System.out.println("Total sum: " + ln.totalSum);
                outMain.writeNumber(ln.totalSum, bitsPerWordID);

                //Write node offsets:
                outMain.writeNumber(ln.startBitID, bitsPerNodeStartBitID);

                //Write number of children
                outMain.writeNumber(ln.jumpTable.size(), bitsPerNumChildren);

                //Write number of matches
                outMain.writeNumber(ln.primaryMatches.size(), bitsPerNumMatches);
                outMain.writeNumber(ln.secondaryMatches.size(), bitsPerNumMatches);
            }

            //Anything extra?
            outMain.flushRemaining();


            //Finally, write all variable-width data:
            BitOutputStream outVary = new BitOutputStream(lookupVaryFile);

            //Next, write all node data
            for (int lnID=0; lnID<nodesById.size(); lnID++) {
                LookupNode ln = nodesById.get(lnID);
                
                //Write each child, in alphabetical order
                int numWritten = 0;
                for (char c='a'; c<='z'; c++) {
                    if (ln.jumpTable.containsKey(c)) {
                        //Letter, node
                        LookupNode jumpTo = ln.jumpTable.get(c);
                        outVary.writeNumber((c-'a'), bitsPerLetter);
                        outVary.writeNumber(jumpTo.id, bitsPerNodeID);
                        numWritten++;
                    }
                }
                if (numWritten!=ln.jumpTable.size())
                    throw new IllegalArgumentException("Size mismatch in lookup nodes: " + numWritten + "," + ln.jumpTable.size());

                //TEMP:
                if (lnID==23)
                    System.out.println("Node ID 23");

                //Write each primary match
                for (DictionaryWord wrd : ln.primaryMatches) {
                    //TEMP:
                    if (lnID==23)
                        System.out.println("   " + wrd.wordStr);

                    //System.out.println("word: " + wrd.wordID);
                    //System.out.println("word_bit: " + wordStartBitIds.get(wrd.wordID));
                    outVary.writeNumber(wordStartBitIds.get(wrd.wordID), bitsPerWordBitID);
                }

                //Write each secondary match
                for (DictionaryWord wrd : ln.secondaryMatches) {
                    outVary.writeNumber(wordStartBitIds.get(wrd.wordID), bitsPerWordBitID);
                }
            }

            //Any last byte?
            outVary.flushRemaining();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            try {
                lookupFile.close();
            } catch (IOException ex2) {}
            return null;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing to temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            try {
                lookupFile.close();
            } catch (IOException ex2) {}
            return null;
        }


        //Close
        try {
            lookupFile.close();
            lookupVaryFile.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error closing temporary file: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        return toZipFiles;
    }


    private void writeNumber(BufferedOutputStream outFile, String hint, int number, int numBytes) throws IllegalArgumentException, IOException {
        //Limited range for numBytes
        if (numBytes<1 || numBytes>3)
            throw new IllegalArgumentException("Bad parameter for numBytes: " + numBytes);

        //Validate
        int maxVal = numBytes==1 ? 0xFF : numBytes==2 ? 0xFFFF : 0xFFFFFF;
        if (number<0 || number>maxVal)
            throw new IllegalArgumentException("Value of " + hint + " outside range: " + number);

        //Else, write it
        if (numBytes>=3)
            outFile.write((byte)((number>>16)&0xFF));
        if (numBytes>=2)
            outFile.write((byte)((number>>8)&0xFF));
        if (numBytes>=1)
            outFile.write((byte)(number&0xFF));
    }



    /** Initializes the applet OrnagaiCreator */
    public void init() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    initComponents();
                    getContentPane().setBackground(getBackground()); //Weird...

                    //Now, remove all formatted text.
                    txtPathToDictionary1.setText("");
                    cmbStructWord1.removeAllItems();
                    cmbStructPOS1.removeAllItems();
                    cmbStructDef1.removeAllItems();
                    lblSampleEntry1.setText("");
                    lblTblRow1Overlay.setText(newFileSuffix);

                    //For consistency
                    recheckEnabledComponents();

                    //Finally
                    firstPnl1.setVisible(true);
                    secondPanel.setVisible(false);
                    thirdPanel.setVisible(false);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        thirdPanel = new javax.swing.JPanel();
        btnClose3 = new javax.swing.JButton();
        lblCreatingFile = new javax.swing.JLabel();
        lblCurrentPage3 = new javax.swing.JLabel();
        progCreation = new javax.swing.JProgressBar();
        lblProgressAmt = new javax.swing.JLabel();
        lblDone = new javax.swing.JLabel();
        lblDoneDetails = new javax.swing.JLabel();
        btnOpenFolder3 = new javax.swing.JButton();
        secondPanel = new javax.swing.JPanel();
        lblChooseOptions2 = new javax.swing.JLabel();
        lblCurrentPage2 = new javax.swing.JLabel();
        btnNext2 = new javax.swing.JButton();
        pnlFauxTable2 = new javax.swing.JLayeredPane();
        lblTblPropColHeader = new javax.swing.JLabel();
        lblTblPropColHeader2 = new javax.swing.JLabel();
        btnTblRow2Overlay = new javax.swing.JButton();
        lblTblRow1Name = new javax.swing.JLabel();
        lblTblRow1Overlay = new javax.swing.JLabel();
        lblTblRow4Name = new javax.swing.JLabel();
        lblTblRow2Name = new javax.swing.JLabel();
        lblTblRow3Name = new javax.swing.JLabel();
        txtTblRow1Value = new javax.swing.JTextField();
        txtTblRow2Value = new javax.swing.JTextField();
        lblTblRow4Overlay = new javax.swing.JLabel();
        txtTblRow4Value = new javax.swing.JTextField();
        cmbTblRow3Value = new javax.swing.JComboBox();
        btnBack2 = new javax.swing.JButton();
        firstPnl1 = new javax.swing.JPanel();
        btnNext1 = new javax.swing.JButton();
        lblSampleEntry1 = new javax.swing.JLabel();
        lblSample1 = new javax.swing.JLabel();
        cmbStructPOS1 = new javax.swing.JComboBox();
        cmbStructDef1 = new javax.swing.JComboBox();
        lblDefinition1 = new javax.swing.JLabel();
        cmbStructWord1 = new javax.swing.JComboBox();
        lblStructPOS1 = new javax.swing.JLabel();
        btnBrowseForDict1 = new javax.swing.JButton();
        txtPathToDictionary1 = new javax.swing.JTextField();
        lblChooseFile1 = new javax.swing.JLabel();
        lblCurrentPage1 = new javax.swing.JLabel();
        lblStructWord1 = new javax.swing.JLabel();
        lblStructure1 = new javax.swing.JLabel();
        btnRandomDictEntry1 = new javax.swing.JButton();

        setBackground(new java.awt.Color(153, 255, 204));
        setName("firstPnl"); // NOI18N

        jLayeredPane1.setPreferredSize(new java.awt.Dimension(463, 421));

        thirdPanel.setBackground(new java.awt.Color(255, 204, 204));
        thirdPanel.setOpaque(false);

        btnClose3.setFont(new java.awt.Font("Tahoma", 0, 14));
        btnClose3.setText("Close");
        btnClose3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClose3ActionPerformed(evt);
            }
        });

        lblCreatingFile.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblCreatingFile.setText("Please wait, creating file..");

        lblCurrentPage3.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblCurrentPage3.setForeground(new java.awt.Color(153, 153, 153));
        lblCurrentPage3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentPage3.setText("<html>1..2..<span style=\"font-weight: bold; color:black; font-size:26pt;\">3</span></html>");

        progCreation.setValue(30);

        lblProgressAmt.setFont(new java.awt.Font("Courier New", 0, 12));
        lblProgressAmt.setText("Doing: whatever");

        lblDone.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblDone.setText("Done!");

        lblDoneDetails.setFont(new java.awt.Font("Courier New", 0, 12));
        lblDoneDetails.setText("Dictionary file created at: \n  c:\\...");
        lblDoneDetails.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        btnOpenFolder3.setFont(new java.awt.Font("Tahoma", 0, 14));
        btnOpenFolder3.setText("Open Folder");
        btnOpenFolder3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenFolder3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout thirdPanelLayout = new javax.swing.GroupLayout(thirdPanel);
        thirdPanel.setLayout(thirdPanelLayout);
        thirdPanelLayout.setHorizontalGroup(
            thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(thirdPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(thirdPanelLayout.createSequentialGroup()
                        .addComponent(lblCreatingFile)
                        .addContainerGap(271, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, thirdPanelLayout.createSequentialGroup()
                        .addGroup(thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(progCreation, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                            .addComponent(lblCurrentPage3)
                            .addGroup(thirdPanelLayout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(lblProgressAmt, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)))
                        .addGap(23, 23, 23))
                    .addGroup(thirdPanelLayout.createSequentialGroup()
                        .addComponent(lblDone)
                        .addContainerGap(423, Short.MAX_VALUE))))
            .addGroup(thirdPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblDoneDetails, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                    .addComponent(btnOpenFolder3))
                .addContainerGap())
            .addGroup(thirdPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnClose3)
                .addContainerGap(405, Short.MAX_VALUE))
        );
        thirdPanelLayout.setVerticalGroup(
            thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(thirdPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCurrentPage3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblCreatingFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(progCreation, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblProgressAmt)
                .addGap(33, 33, 33)
                .addComponent(lblDone)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblDoneDetails, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnOpenFolder3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 81, Short.MAX_VALUE)
                .addComponent(btnClose3)
                .addGap(37, 37, 37))
        );

        thirdPanel.setBounds(0, 0, 480, 390);
        jLayeredPane1.add(thirdPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        secondPanel.setOpaque(false);

        lblChooseOptions2.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblChooseOptions2.setText("Choose your options:");

        lblCurrentPage2.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblCurrentPage2.setForeground(new java.awt.Color(153, 153, 153));
        lblCurrentPage2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentPage2.setText("<html>1..<span style=\"font-weight: bold; color:black; font-size:26pt;\">2</span>..3</html>");

        btnNext2.setFont(new java.awt.Font("Tahoma", 1, 18));
        btnNext2.setText("Next");
        btnNext2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNext2ActionPerformed(evt);
            }
        });

        pnlFauxTable2.setBackground(new java.awt.Color(255, 255, 255));
        pnlFauxTable2.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        pnlFauxTable2.setOpaque(true);

        lblTblPropColHeader.setBackground(new java.awt.Color(255, 255, 204));
        lblTblPropColHeader.setText("Property Name");
        lblTblPropColHeader.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        lblTblPropColHeader.setOpaque(true);
        lblTblPropColHeader.setBounds(2, 2, 129, 28);
        pnlFauxTable2.add(lblTblPropColHeader, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblPropColHeader2.setBackground(new java.awt.Color(255, 255, 204));
        lblTblPropColHeader2.setText("Property Value");
        lblTblPropColHeader2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        lblTblPropColHeader2.setOpaque(true);
        lblTblPropColHeader2.setBounds(132, 2, 315, 28);
        pnlFauxTable2.add(lblTblPropColHeader2, javax.swing.JLayeredPane.DEFAULT_LAYER);

        btnTblRow2Overlay.setText("...");
        btnTblRow2Overlay.setOpaque(false);
        btnTblRow2Overlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTblRow2OverlayActionPerformed(evt);
            }
        });
        btnTblRow2Overlay.setBounds(408, 46, 40, 17);
        pnlFauxTable2.add(btnTblRow2Overlay, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblRow1Name.setBackground(new java.awt.Color(255, 255, 255));
        lblTblRow1Name.setText("File Name");
        lblTblRow1Name.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        lblTblRow1Name.setOpaque(true);
        lblTblRow1Name.setBounds(2, 30, 130, 17);
        pnlFauxTable2.add(lblTblRow1Name, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblRow1Overlay.setBackground(new java.awt.Color(204, 204, 204));
        lblTblRow1Overlay.setText(".mzdict.zip");
        lblTblRow1Overlay.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lblTblRow1Overlay.setOpaque(true);
        lblTblRow1Overlay.setBounds(391, 30, 56, 17);
        pnlFauxTable2.add(lblTblRow1Overlay, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblRow4Name.setBackground(new java.awt.Color(255, 255, 255));
        lblTblRow4Name.setText("Lump Size");
        lblTblRow4Name.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        lblTblRow4Name.setOpaque(true);
        lblTblRow4Name.setBounds(2, 78, 130, 17);
        pnlFauxTable2.add(lblTblRow4Name, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblRow2Name.setBackground(new java.awt.Color(255, 255, 255));
        lblTblRow2Name.setText("Save In Directory");
        lblTblRow2Name.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        lblTblRow2Name.setOpaque(true);
        lblTblRow2Name.setBounds(2, 46, 130, 17);
        pnlFauxTable2.add(lblTblRow2Name, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblRow3Name.setBackground(new java.awt.Color(255, 255, 255));
        lblTblRow3Name.setText("Format");
        lblTblRow3Name.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        lblTblRow3Name.setOpaque(true);
        lblTblRow3Name.setBounds(2, 62, 130, 17);
        pnlFauxTable2.add(lblTblRow3Name, javax.swing.JLayeredPane.DEFAULT_LAYER);

        txtTblRow1Value.setText("mywords");
        txtTblRow1Value.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateFileName(evt);
            }
        });
        txtTblRow1Value.setBounds(132, 30, 263, 18);
        pnlFauxTable2.add(txtTblRow1Value, javax.swing.JLayeredPane.DEFAULT_LAYER);

        txtTblRow2Value.setText("C:\\Temp");
        txtTblRow2Value.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateDirectory(evt);
            }
        });
        txtTblRow2Value.setBounds(132, 46, 277, 18);
        pnlFauxTable2.add(txtTblRow2Value, javax.swing.JLayeredPane.DEFAULT_LAYER);

        lblTblRow4Overlay.setBackground(new java.awt.Color(204, 204, 204));
        lblTblRow4Overlay.setText("kb");
        lblTblRow4Overlay.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        lblTblRow4Overlay.setOpaque(true);
        lblTblRow4Overlay.setBounds(432, 78, 16, 17);
        pnlFauxTable2.add(lblTblRow4Overlay, javax.swing.JLayeredPane.DEFAULT_LAYER);

        txtTblRow4Value.setText("200");
        txtTblRow4Value.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                validateLumpSize(evt);
            }
        });
        txtTblRow4Value.setBounds(132, 78, 301, 18);
        pnlFauxTable2.add(txtTblRow4Value, javax.swing.JLayeredPane.DEFAULT_LAYER);

        cmbTblRow3Value.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Text", "Optimized" }));
        cmbTblRow3Value.setSelectedIndex(1);
        cmbTblRow3Value.setBounds(132, 62, 315, 17);
        pnlFauxTable2.add(cmbTblRow3Value, javax.swing.JLayeredPane.DEFAULT_LAYER);

        btnBack2.setFont(new java.awt.Font("Tahoma", 0, 14));
        btnBack2.setText("Back");
        btnBack2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBack2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout secondPanelLayout = new javax.swing.GroupLayout(secondPanel);
        secondPanel.setLayout(secondPanelLayout);
        secondPanelLayout.setHorizontalGroup(
            secondPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(secondPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(secondPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(secondPanelLayout.createSequentialGroup()
                        .addComponent(pnlFauxTable2, javax.swing.GroupLayout.PREFERRED_SIZE, 448, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(secondPanelLayout.createSequentialGroup()
                        .addComponent(lblChooseOptions2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 279, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, secondPanelLayout.createSequentialGroup()
                        .addComponent(lblCurrentPage2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, secondPanelLayout.createSequentialGroup()
                        .addComponent(btnBack2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnNext2, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGap(20, 20, 20))
        );
        secondPanelLayout.setVerticalGroup(
            secondPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(secondPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCurrentPage2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblChooseOptions2)
                .addGap(18, 18, 18)
                .addComponent(pnlFauxTable2, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(124, 124, 124)
                .addGroup(secondPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnNext2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBack2))
                .addGap(37, 37, 37))
        );

        secondPanel.setBounds(0, 0, 470, 390);
        jLayeredPane1.add(secondPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        firstPnl1.setOpaque(false);
        firstPnl1.setPreferredSize(new java.awt.Dimension(463, 421));

        btnNext1.setFont(new java.awt.Font("Tahoma", 1, 18));
        btnNext1.setText("Next");
        btnNext1.setEnabled(false);
        btnNext1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNext1ActionPerformed(evt);
            }
        });

        lblSampleEntry1.setBackground(new java.awt.Color(255, 255, 255));
        lblSampleEntry1.setFont(new java.awt.Font("Zawgyi-One", 0, 12));
        lblSampleEntry1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblSampleEntry1.setText("<html><div style=\"background:#DDDDDD; width:142px; height:87px;\"><b>scion</b><br><i>n</i><br>\u1021\u1019\u103A\u102D\u102F\u1038\u1031\u1000\u102C\u1004\u1039\u1038\u101E\u102C\u1038\u104B \u1019\u103A\u102D\u102F\u1038\u1006\u1000\u1039\u104B \u1021\u1015\u1004\u1039\u1015\u103C\u102C\u1038\u101A\u1030\u101B\u1014\u1039\u103B\u1016\u1010\u1039\u1011\u102F\u1010\u1039\u101E\u100A\u1039&shy;\u1037\u1015\u1004\u1039\u1005\u100A\u1039\u1021\u1015\u102D\u102F\u1004\u1039\u1038\u1021\u1005\u104B \u1015\u1004\u1039\u1015\u103C\u102C\u1038\u1019\u103A\u102D\u102F\u1038\u1006\u1000\u1039\u104B</div></html>");
        lblSampleEntry1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        lblSampleEntry1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(102, 102, 102)));
        lblSampleEntry1.setOpaque(true);

        lblSample1.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblSample1.setText("Sample:");

        cmbStructPOS1.setFont(new java.awt.Font("Tahoma", 0, 12));
        cmbStructPOS1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Column 1", "Column 2", "Column 3" }));
        cmbStructPOS1.setSelectedIndex(1);
        cmbStructPOS1.setEnabled(false);
        cmbStructPOS1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStructPOS1ActionPerformed(evt);
            }
        });

        cmbStructDef1.setFont(new java.awt.Font("Tahoma", 0, 12));
        cmbStructDef1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Column 1", "Column 2", "Column 3" }));
        cmbStructDef1.setSelectedIndex(2);
        cmbStructDef1.setEnabled(false);
        cmbStructDef1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStructDef1ActionPerformed(evt);
            }
        });

        lblDefinition1.setFont(new java.awt.Font("Tahoma", 0, 14));
        lblDefinition1.setText("Definition:");

        cmbStructWord1.setFont(new java.awt.Font("Tahoma", 0, 12));
        cmbStructWord1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Column 1", "Column 2", "Column 3" }));
        cmbStructWord1.setEnabled(false);
        cmbStructWord1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStructWord1ActionPerformed(evt);
            }
        });

        lblStructPOS1.setFont(new java.awt.Font("Tahoma", 0, 14));
        lblStructPOS1.setText("P.O.S.:");

        btnBrowseForDict1.setFont(new java.awt.Font("Tahoma", 0, 12));
        btnBrowseForDict1.setText("...");
        btnBrowseForDict1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBrowseForDict1ActionPerformed(evt);
            }
        });

        txtPathToDictionary1.setFont(new java.awt.Font("Courier New", 0, 12));
        txtPathToDictionary1.setText("D:\\Open Source Projects\\ornagai_dict\\NEW! dictionary (tsv) 2008\\convert_entab.tsv");
        txtPathToDictionary1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPathToDictionary1ActionPerformed(evt);
            }
        });

        lblChooseFile1.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblChooseFile1.setText("Choose your dictionary:");

        lblCurrentPage1.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblCurrentPage1.setForeground(new java.awt.Color(153, 153, 153));
        lblCurrentPage1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentPage1.setText("<html><span style=\"font-weight: bold; color:black; font-size:26pt;\">1</span>..2..3</html>");

        lblStructWord1.setFont(new java.awt.Font("Tahoma", 0, 14));
        lblStructWord1.setText("Word:");

        lblStructure1.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblStructure1.setText("Dictionary Structure:");

        btnRandomDictEntry1.setFont(new java.awt.Font("Tahoma", 0, 12));
        btnRandomDictEntry1.setText("(Random Entry)");
        btnRandomDictEntry1.setEnabled(false);
        btnRandomDictEntry1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRandomDictEntry1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout firstPnl1Layout = new javax.swing.GroupLayout(firstPnl1);
        firstPnl1.setLayout(firstPnl1Layout);
        firstPnl1Layout.setHorizontalGroup(
            firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(firstPnl1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, firstPnl1Layout.createSequentialGroup()
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(firstPnl1Layout.createSequentialGroup()
                                .addComponent(lblStructure1)
                                .addGap(81, 81, 81)
                                .addComponent(lblSample1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRandomDictEntry1))
                            .addComponent(lblChooseFile1)
                            .addGroup(firstPnl1Layout.createSequentialGroup()
                                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addComponent(lblStructWord1)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbStructWord1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(txtPathToDictionary1, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addComponent(lblStructPOS1)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbStructPOS1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addComponent(lblDefinition1)
                                        .addGap(18, 18, 18)
                                        .addComponent(cmbStructDef1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addGap(6, 6, 6)
                                        .addComponent(btnBrowseForDict1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(firstPnl1Layout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addComponent(lblSampleEntry1, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)))))
                        .addGap(272, 272, 272))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, firstPnl1Layout.createSequentialGroup()
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnNext1, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblCurrentPage1))
                        .addGap(250, 250, 250))))
        );
        firstPnl1Layout.setVerticalGroup(
            firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(firstPnl1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCurrentPage1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblChooseFile1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(btnBrowseForDict1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtPathToDictionary1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblStructure1)
                    .addComponent(lblSample1)
                    .addComponent(btnRandomDictEntry1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(firstPnl1Layout.createSequentialGroup()
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbStructWord1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStructWord1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbStructPOS1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblStructPOS1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(firstPnl1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbStructDef1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblDefinition1)))
                    .addComponent(lblSampleEntry1, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addComponent(btnNext1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        firstPnl1.setBounds(0, 0, 490, 380);
        jLayeredPane1.add(firstPnl1, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 471, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 374, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnBrowseForDict1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBrowseForDict1ActionPerformed
        //Open a file chooser
        String path = ".";
        if (dictionaryFile!=null)
            path = dictionaryFile.getParent();
        JFileChooser dictChooser = new JFileChooser(new File("."));
        dictChooser.setFileFilter(new FileNameExtensionFilter("Tab-separated dictionaries", "tsv", "txt"));
        dictChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        dictChooser.setMultiSelectionEnabled(false);
        if (dictChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        //They approved; now, get the file.
        File newDictFile = dictChooser.getSelectedFile();
        if (newDictFile.exists())
            readNewDictionaryFile(newDictFile);
    }//GEN-LAST:event_btnBrowseForDict1ActionPerformed

    private void txtPathToDictionary1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPathToDictionary1ActionPerformed
        String filePath = txtPathToDictionary1.getText();
        File newDictFile = new File(filePath);
        if (!newDictFile.exists()) {
            JOptionPane.showMessageDialog(this, "Dictionary file cannot be found at: \n\n"+filePath, "File Not Found", JOptionPane.ERROR_MESSAGE);
            txtPathToDictionary1.setText("");
            if (dictionaryFile!=null)
                txtPathToDictionary1.setText(dictionaryFile.getAbsolutePath());
            return;
        }

        //Found it
        readNewDictionaryFile(newDictFile);
    }//GEN-LAST:event_txtPathToDictionary1ActionPerformed

    private void btnRandomDictEntry1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRandomDictEntry1ActionPerformed
        sampleTextID = rand.nextInt(allDictEntries.size());
        reloadSampleText();
    }//GEN-LAST:event_btnRandomDictEntry1ActionPerformed

    private void cmbStructWord1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStructWord1ActionPerformed
        colidWord = ((JComboBox)evt.getSource()).getSelectedIndex();
        reloadSampleText();
    }//GEN-LAST:event_cmbStructWord1ActionPerformed

    private void cmbStructPOS1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStructPOS1ActionPerformed
        colidPOS = ((JComboBox)evt.getSource()).getSelectedIndex();
        reloadSampleText();
    }//GEN-LAST:event_cmbStructPOS1ActionPerformed

    private void cmbStructDef1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStructDef1ActionPerformed
        colidDefinition = ((JComboBox)evt.getSource()).getSelectedIndex();
        reloadSampleText();
    }//GEN-LAST:event_cmbStructDef1ActionPerformed

    private void btnNext1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNext1ActionPerformed
        //Always check
        recheckEnabledComponents();

        //Swich pages
        firstPnl1.setVisible(false);
        secondPanel.setVisible(true);
        thirdPanel.setVisible(false);
    }//GEN-LAST:event_btnNext1ActionPerformed

    private void btnNext2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNext2ActionPerformed
        //Always check
        recheckEnabledComponents();

        //Reset page 3
        String shorterPath = newFileDirectory.getAbsolutePath();
        int maxLen = 50;
        if (shorterPath.length()>maxLen)
            shorterPath = shorterPath.substring(0, maxLen/2) + "..." + shorterPath.substring(shorterPath.length()-maxLen/2);
        progCreation.setValue(0);
        lblProgressAmt.setText("");
        lblDoneDetails.setText("<html>Dictionary file created at:<br>&nbsp;&nbsp;"+ shorterPath + "</html>");

        //Hide some elements
        lblDone.setVisible(false);
        lblDoneDetails.setVisible(false);
        btnOpenFolder3.setVisible(false);

        //Swich pages
        firstPnl1.setVisible(false);
        secondPanel.setVisible(false);
        thirdPanel.setVisible(true);

        //Start making the dictionary file
        new Thread(new Runnable() {
            public void run() {
                createDictionaryFile();
            }
        }).start();
        
    }//GEN-LAST:event_btnNext2ActionPerformed

    private void validateFileName(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateFileName
        //Did we give this a valid file name?
        JTextField txt = (JTextField)evt.getSource();

        //Simple regex to check J2ME names:
        String j2meRegex = "[a-z][a-z0-9._]+";
        if (txt.getText().matches(j2meRegex) && txt.getText().split("\\.")[0].length()>=3)
            newFilePrefix = txt.getText();
        else
            txt.setText(newFilePrefix);
    }//GEN-LAST:event_validateFileName

    private void validateDirectory(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateDirectory
        JTextField txt = (JTextField)evt.getSource();
        File f = new File(txt.getText());
        if (f.exists() && f.isDirectory()) {
            newFileDirectory = f;
        } else {
            txt.setText(newFileDirectory.getAbsolutePath());
        }
    }//GEN-LAST:event_validateDirectory

    private void validateLumpSize(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_validateLumpSize
        JTextField txt = (JTextField)evt.getSource();
        try {
            int val = Integer.parseInt(txt.getText());
            lumpSizeKb = val;
        } catch (NumberFormatException ex) {
            txt.setText(lumpSizeKb+"");
        }
    }//GEN-LAST:event_validateLumpSize

    private void btnBack2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBack2ActionPerformed
        //Always check
        recheckEnabledComponents();

        //Swich pages
        firstPnl1.setVisible(true);
        secondPanel.setVisible(false);
        thirdPanel.setVisible(false);
    }//GEN-LAST:event_btnBack2ActionPerformed

    private void btnTblRow2OverlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTblRow2OverlayActionPerformed
        //Browse for a directory
        JFileChooser directChooser = new JFileChooser(newFileDirectory.getParentFile());
        //directChooser.setFileFilter(new FileNameExtensionFilter("Directories", ""));
        directChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directChooser.setMultiSelectionEnabled(false);
        if (directChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        //They approved, change it. (Check permissions later?
        File tmpDir = directChooser.getSelectedFile();
        if (tmpDir.exists() && tmpDir.isDirectory()) {
            newFileDirectory = tmpDir;
            txtTblRow2Value.setText(newFileDirectory.getAbsolutePath());
        }
    }//GEN-LAST:event_btnTblRow2OverlayActionPerformed

    private void btnClose3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClose3ActionPerformed
        try {
            //Running on the desktop.
            System.exit(1);
        } catch (SecurityException ex) {
            //Running in a browser.
            this.getContentPane().setBackground(Color.black);
            firstPnl1.setVisible(false);
            secondPanel.setVisible(false);
            thirdPanel.setVisible(false);
        }
    }//GEN-LAST:event_btnClose3ActionPerformed

    private void btnOpenFolder3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenFolder3ActionPerformed
        //Open the folder in explorer
        try {
            Runtime.getRuntime().exec("explorer \"" + newFileDirectory.getAbsolutePath() + "\"");
        } catch (IOException ex) {
            //Linux? Shot-in-the-dark for Ubuntu users...
            try {
                Runtime.getRuntime().exec("nautilus \"" + newFileDirectory.getAbsolutePath() + "\"");
            } catch (IOException ex2) {
                //No more ideas
                JOptionPane.showMessageDialog(this, "Unable to open directory; please browse to it in your file manager of choice.", "Couldn't open file manager", JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnOpenFolder3ActionPerformed





    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack2;
    private javax.swing.JButton btnBrowseForDict1;
    private javax.swing.JButton btnClose3;
    private javax.swing.JButton btnNext1;
    private javax.swing.JButton btnNext2;
    private javax.swing.JButton btnOpenFolder3;
    private javax.swing.JButton btnRandomDictEntry1;
    private javax.swing.JButton btnTblRow2Overlay;
    private javax.swing.JComboBox cmbStructDef1;
    private javax.swing.JComboBox cmbStructPOS1;
    private javax.swing.JComboBox cmbStructWord1;
    private javax.swing.JComboBox cmbTblRow3Value;
    private javax.swing.JPanel firstPnl1;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLabel lblChooseFile1;
    private javax.swing.JLabel lblChooseOptions2;
    private javax.swing.JLabel lblCreatingFile;
    private javax.swing.JLabel lblCurrentPage1;
    private javax.swing.JLabel lblCurrentPage2;
    private javax.swing.JLabel lblCurrentPage3;
    private javax.swing.JLabel lblDefinition1;
    private javax.swing.JLabel lblDone;
    private javax.swing.JLabel lblDoneDetails;
    private javax.swing.JLabel lblProgressAmt;
    private javax.swing.JLabel lblSample1;
    private javax.swing.JLabel lblSampleEntry1;
    private javax.swing.JLabel lblStructPOS1;
    private javax.swing.JLabel lblStructWord1;
    private javax.swing.JLabel lblStructure1;
    private javax.swing.JLabel lblTblPropColHeader;
    private javax.swing.JLabel lblTblPropColHeader2;
    private javax.swing.JLabel lblTblRow1Name;
    private javax.swing.JLabel lblTblRow1Overlay;
    private javax.swing.JLabel lblTblRow2Name;
    private javax.swing.JLabel lblTblRow3Name;
    private javax.swing.JLabel lblTblRow4Name;
    private javax.swing.JLabel lblTblRow4Overlay;
    private javax.swing.JLayeredPane pnlFauxTable2;
    private javax.swing.JProgressBar progCreation;
    private javax.swing.JPanel secondPanel;
    private javax.swing.JPanel thirdPanel;
    private javax.swing.JTextField txtPathToDictionary1;
    private javax.swing.JTextField txtTblRow1Value;
    private javax.swing.JTextField txtTblRow2Value;
    private javax.swing.JTextField txtTblRow4Value;
    // End of variables declaration//GEN-END:variables

}
