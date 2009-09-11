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
import java.util.Hashtable;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
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
    private int lumpSizeKb = 250;

    //For display and reordering.
    private ArrayList<String[]> allDictEntries = new ArrayList<String[]>();

    //For general usage
    private Random rand = new Random();


    //In case we want to start this with a "main" entry
    /*public static void main(String[] args) {
        new OrnagaiCreator().setVisible(true);
    }*/


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


    private void createDictionaryFile() {
        //Get a list of files to compress (File[] {source, lzma})
        Hashtable<String, File[]> toZipFiles = null;
        if (cmbTblRow3Value.getSelectedIndex()==0) {
            toZipFiles = createTextDictionaryFiles();
        } else {
            toZipFiles = createBinaryOptimizedDictionaryFiles();
        }

        //Compress them all via lzma
        if (!compressAllFiles(toZipFiles))
            return;

        //Step 1: Make a new zip archive
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(new FileOutputStream(new File(newFileDirectory, newFilePrefix + newFileSuffix)));
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Can't create zip file: " + newFilePrefix + newFileSuffix + ": " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
            return;
        }

        //Step 2: Add each file:
        byte[] buff = new byte[1024];
        for (String prefix : toZipFiles.keySet()) {
            //Get our file
            //File tempFile = toZipFiles.get(prefix)[0]; //source
            File tempFile = toZipFiles.get(prefix)[1];   //compressed
            FileInputStream in = null;
            try {
                in = new FileInputStream(tempFile);
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Can't read tempoprary file: " + tempFile.getName() + ": " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //No compression (lzma doesn't need it)
            zipOut.setLevel(0);

            //Add a zip header
            try {
                zipOut.putNextEntry(new ZipEntry(prefix + ".lzma"));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error adding entry: " + prefix + ".lzma: " + ex.toString(), "Error making dictionary", JOptionPane.ERROR_MESSAGE);
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


    private Hashtable<String, File[]> createBinaryOptimizedDictionaryFiles() {
        Hashtable<String, File[]> toZipFiles = new Hashtable<String, File[]>();

        //Necessary for phase 2
        ArrayList<Integer> numDefinitionsPerLump = new ArrayList<Integer>();
        ArrayList<String> wordsInDictionary = new ArrayList<String>();

        //Start with the lump files, since we'll need their sizes later
        ArrayList<Character> currDefinitionStream = new ArrayList<Character>();
        ArrayList<Integer> currDefinitionLengths = new ArrayList<Integer>();
        ArrayList<Character> lettersAsEncountered = new ArrayList<Character>();
        int currLumpID = 1;
        int bytesPerLump = lumpSizeKb*1024;
        for (int rowID=0; rowID<allDictEntries.size(); rowID++) {
            //Locate data
            String[] row = allDictEntries.get(rowID);
            String word = (colidWord<numColumns) ? row[colidWord] : "";
            wordsInDictionary.add(word);
            String combinedDef = null;
            {
                String pos = (colidPOS<numColumns) ? row[colidPOS] : "";
                String definition = (colidDefinition<numColumns) ? row[colidDefinition] : "";
                combinedDef = pos+"\t"+definition;
            }

            //Append this entry
            for (char c : combinedDef.toCharArray()) {
                if (c=='\n' || c=='\r')
                    continue;
                currDefinitionStream.add(c);
                if (!lettersAsEncountered.contains(c))
                    lettersAsEncountered.add(c);
            }
            currDefinitionLengths.add(combinedDef.length());

            //Time to write a new lump file?
            if (currDefinitionStream.size()>=bytesPerLump || rowID==allDictEntries.size()-1) {
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
                    int bitsPerLetter = Integer.toBinaryString(lettersAsEncountered.size()).length();
                    for (char c : currDefinitionStream)
                        writeBits(currLumpFile, reverseLookup.get(c), bitsPerLetter, utilData);

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

        //Gather word list data
        ArrayList<Character> lettersInWordlist = new ArrayList<Character>();
        ArrayList<Integer> sizeOfWords = new ArrayList<Integer>();
        int longestWord = 0;
        for (String word : wordsInDictionary) {
            //Get letter semantics
            int length = 0;
            for (char c : word.toCharArray()) {
                if (c=='\n' || c=='\r')
                    continue;
                if (!lettersInWordlist.contains(c))
                    lettersInWordlist.add(c);
                length++;
            }
            
            //Longest word?
            sizeOfWords.add(length);
            if (length > longestWord)
                longestWord = length;
        }

        //Build a reverse lookup for encoding our bitstream
        Hashtable<Character, Integer> reverseLookup = new Hashtable<Character, Integer>();
        for (int i=0; i<lettersInWordlist.size(); i++)
            reverseLookup.put(lettersInWordlist.get(i), i);

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

        //Append all data
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
            int origBytes = 0;
            int[] utilData = new int[]{0, 0, 0}; //rembits, numrembits, byteswritten
            int bitsPerLetter = Integer.toBinaryString(lettersInWordlist.size()).length();
            int bitsPerSize = Integer.toBinaryString(longestWord).length();
            for (int i=0; i<wordsInDictionary.size(); i++) {
                //Write size
                int size = sizeOfWords.get(i);
                writeBits(wordlistFile, size, bitsPerSize, utilData);
                origBytes += 4;

                //Write letters, re-encoded
                String word = wordsInDictionary.get(i);
                for (char c : word.toCharArray()) {
                    if (c=='\n' || c=='\r')
                        continue;

                    writeBits(wordlistFile, reverseLookup.get(c), bitsPerLetter, utilData);
                    origBytes+=2;
                }
            }

            //Any last byte?
            if (utilData[1]>0)
                wordlistFile.write((byte)(utilData[0]&0xFF));

            //Note: Interesting to note
            System.out.println(origBytes + " bytes re-encoded to " + utilData[2] + " bytes, " + ((utilData[2]*100)/origBytes) + "% of original");
            

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


    private void writeBits(BufferedOutputStream outFile, int number, int bitsToUse, int[] scratchData) throws IllegalArgumentException, IOException {
        //Will it fit?
        String binStr = Integer.toBinaryString(number);
        if (binStr.length() > bitsToUse)
            throw new IllegalArgumentException("Not enough bits to encode " + number + " (" + bitsToUse + ")");

        //Note:
        int[] posFlags = new int[]{0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1};

        //Where were we?
        int partialValue = scratchData[0];
        int currPos = scratchData[1];

        //Write each bit
        for (char c : binStr.toCharArray()) {
            //Update
            if (c=='1')
                partialValue |= posFlags[currPos];
            currPos++;

            //Write a byte?
            if (currPos==8) {
                //Write
                outFile.write((byte)(partialValue&0xFF));

                //Update records & reset
                scratchData[2]++;
                partialValue = 0;
                currPos = 0;
            }
        }

        //Save data back
        scratchData[0] = partialValue;
        scratchData[1] = currPos;
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
        lblCurrentPage3 = new javax.swing.JLabel();
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

        btnClose3.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        btnClose3.setText("Close");
        btnClose3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClose3ActionPerformed(evt);
            }
        });

        lblCurrentPage3.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblCurrentPage3.setForeground(new java.awt.Color(153, 153, 153));
        lblCurrentPage3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentPage3.setText("<html>1..2..<span style=\"font-weight: bold; color:black; font-size:26pt;\">3</span></html>");

        javax.swing.GroupLayout thirdPanelLayout = new javax.swing.GroupLayout(thirdPanel);
        thirdPanel.setLayout(thirdPanelLayout);
        thirdPanelLayout.setHorizontalGroup(
            thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, thirdPanelLayout.createSequentialGroup()
                .addContainerGap(350, Short.MAX_VALUE)
                .addGroup(thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblCurrentPage3)
                    .addComponent(btnClose3))
                .addGap(23, 23, 23))
        );
        thirdPanelLayout.setVerticalGroup(
            thirdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, thirdPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCurrentPage3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 294, Short.MAX_VALUE)
                .addComponent(btnClose3)
                .addGap(38, 38, 38))
        );

        thirdPanel.setBounds(0, 0, 480, 390);
        jLayeredPane1.add(thirdPanel, javax.swing.JLayeredPane.DEFAULT_LAYER);

        secondPanel.setOpaque(false);

        lblChooseOptions2.setFont(new java.awt.Font("Tahoma", 0, 18));
        lblChooseOptions2.setText("Choose your options:");

        lblCurrentPage2.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblCurrentPage2.setForeground(new java.awt.Color(153, 153, 153));
        lblCurrentPage2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblCurrentPage2.setText("<html>1..<span style=\"font-weight: bold; color:black; font-size:26pt;\">2</span>..3</html>");

        btnNext2.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
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
        lblTblRow1Overlay.setBounds(394, 30, 53, 17);
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

        txtTblRow4Value.setText("250");
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

        firstPnl1.setBounds(0, 0, 480, 380);
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

        //Swich pages
        firstPnl1.setVisible(false);
        secondPanel.setVisible(false);
        thirdPanel.setVisible(true);

        //Start making the dictionary file
        createDictionaryFile();
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





    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack2;
    private javax.swing.JButton btnBrowseForDict1;
    private javax.swing.JButton btnClose3;
    private javax.swing.JButton btnNext1;
    private javax.swing.JButton btnNext2;
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
    private javax.swing.JLabel lblCurrentPage1;
    private javax.swing.JLabel lblCurrentPage2;
    private javax.swing.JLabel lblCurrentPage3;
    private javax.swing.JLabel lblDefinition1;
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
    private javax.swing.JPanel secondPanel;
    private javax.swing.JPanel thirdPanel;
    private javax.swing.JTextField txtPathToDictionary1;
    private javax.swing.JTextField txtTblRow1Value;
    private javax.swing.JTextField txtTblRow2Value;
    private javax.swing.JTextField txtTblRow4Value;
    // End of variables declaration//GEN-END:variables

}
