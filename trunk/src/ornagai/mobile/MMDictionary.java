package ornagai.mobile;

import com.sun.lwuit.List;
import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.events.SelectionListener;
import com.sun.lwuit.list.ListModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import java.util.Vector;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;


/**
 *
 * @author Seth N. Hetu
 */
public class MMDictionary implements ProcessAction, ListModel {
    //No enums. :(
    private static final int FMT_TEXT = 0;
    private static final int FMT_BINARY = 1;

    //Data - Static
    private AbstractFile dictFile;

    //Data - Runtime
    private boolean doneWithSearchFiles = false;
    private byte[] wordListData;
    private byte[] lookupTableStaticData;
    private byte[] lookupTableVariableData;
    private int fileFormat = -1;

    //Binary data
    private int numWords;
    private int numLetters;
    private int longestWord;
    private int numLumps;
    private char[] letterValues;


    public MMDictionary(AbstractFile dictionaryFile) {
        this.dictFile = dictionaryFile;
    }

    //Load all the things we need to look up a word
    public void loadLookupTree() {
        System.gc();
        System.out.println("Memory in use before loading: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //This is the only function that doesn't need to check for
        // FMT_TEXT; we know that nothing's been loaded.
        dictFile.openProcessClose("word_list-zg2009.bin", this);
        System.gc();
        System.out.println("Memory in use after loading word list: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //And now, load the lookup tree
        dictFile.openProcessClose("lookup.bin", this);
        dictFile.openProcessClose("lookup_vary.bin", this);
        System.gc();
        System.out.println("Memory in use after loading lookup tree: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //Now, the model
        try {
            initModel();
        } catch (IOException ex) {
            throw new RuntimeException("Error initializing model: " + ex.toString());
        }
        doneWithSearchFiles = true;
    }


    //This function reads through our dictionary file and loads
    // whatever information we are looking for. This is done in a
    // somewhat implied fashion.
    public void processFile(InputStream file) {
        //If we don't have a list of sub-files in the zip,
        //   load that list. At the same time, load the dictionary tree.
        //   Also, optionally, load all data (text format)
        if (!doneWithSearchFiles) {
            //What are we loading?
            if (wordListData==null) {
                //Binary format
                fileFormat = FMT_BINARY;

                //Now, read the contents of the file
                // NOTE: We need to un-lzma it.
                try {
                    if (fileFormat==FMT_TEXT) {
                        readTextWordlist(file);
                    } else if (fileFormat==FMT_BINARY) {
                        readBinaryWordlist(file);
                    }
                } catch (IOException ex) {
                    //Handle...
                    throw new RuntimeException(ex.toString());
                } catch (OutOfMemoryError er) {
                    System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");
                    throw new RuntimeException("Out of memory!");
                }
            } else if (lookupTableStaticData==null) {
                try {
                    readBinaryLookupTable(file);
                } catch (IOException ex) {
                    //Handle...
                    throw new RuntimeException(ex.toString());
                } catch (OutOfMemoryError er) {
                    System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used, " + (Runtime.getRuntime().freeMemory()/1024) + " kb free");
                    throw new RuntimeException("Out of memory!");
                }
            } else if (lookupTableVariableData==null) {
                //Read and append
                try {
                    //System.out.println("New Sub-File");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    for (;;) {
                        int count = file.read(buffer);
                        if (count==-1)
                            break;
                        baos.write(buffer, 0, count);
                    }
                    baos.close();

                    lookupTableVariableData = baos.toByteArray();
                } catch (IOException ex) {
                    //Handle...
                    throw new RuntimeException(ex.toString());
                } catch (OutOfMemoryError er) {
                    System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used, " + (Runtime.getRuntime().freeMemory()/1024) + " kb free");
                    throw new RuntimeException("Out of memory!");
                }
            }
        }
    }


    private void readTextWordlist(InputStream zIn) throws IOException {
        
    }

/*    private void buildWordLookupTree() throws IOException {
        System.gc();
        System.out.println("Word list data read, " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //Finally, read and process all words
        dictionaryTree = new LookupNode();
        int bitsPerSize = Integer.toBinaryString(longestWord-1).length();
        int bitsPerLetter = Integer.toBinaryString(numLetters-1).length();
        BitInputStream bin = new BitInputStream(new ByteArrayInputStream(wordListData));
        for (int wordID=0; wordID<numWords; wordID++) {
            int wordSize = bin.readNumber(bitsPerSize);
            StringBuffer currWord = new StringBuffer();

            System.out.println("Adding word: " + wordID);

            LookupNode currNode = dictionaryTree;
            boolean firstWord = true;
            while (wordSize>0) {
                //Add this letter
                int let = bin.readNumber(bitsPerLetter);
                char c = letterValues[let];
                currWord.append(c);
                wordSize--;

                //Build our tree
                c = Character.toLowerCase(c);
                if (c<'a' || c>'z') {
                    //Not a letter. Possible word break?
                    if (c!='-')
                        firstWord = false;
                } else {
                    //It's a letter, track it
                    currNode = currNode.addPath(c);
                }

                //Is this the last letter in that word?
                if (wordSize==0) {
                    //Store the "bit ID" of this word
                    int bitsRead = bin.getBitsRead();
                    currNode.addWord(bitsRead, firstWord);
                }
            }
        }

        //And finally...
        System.out.println("Compress");
        dictionaryTree.compress();
    }*/


    private void readBinaryLookupTable(InputStream zIn) throws IOException {
        //Read header
        byte[] buffer = new byte[1024];
        if (zIn.read(buffer, 0, 15)!=15)
            throw new IOException("Bad binary header length; 15 expected.");
        int numNodes = getInt(buffer, 0, 3);
        int numMaxChildren = getInt(buffer, 3, 3);
        int numMaxMatches = getInt(buffer, 6, 3);
        int numMaxWordBitID = getInt(buffer, 9, 3);
        int numMaxNodeBitID = getInt(buffer, 12, 3);

        //Save
        bitsPerNodeID = Integer.toBinaryString(numNodes-1).length();
        bitsPerNodeBitID = Integer.toBinaryString(numMaxNodeBitID-1).length();
        bitsPerNumMaxChildren = Integer.toBinaryString(numMaxChildren-1).length();
        bitsPerNumMaxMatches = Integer.toBinaryString(numMaxMatches-1).length();
        bitsPerNodeStaticData = bitsPerNodeID + bitsPerNodeBitID + bitsPerNumMaxChildren + 2*bitsPerNumMaxMatches;
        bitsperWordBitID = Integer.toBinaryString(numMaxWordBitID-1).length();


        //Now, read all words into a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int totalCount = 0;
        for (;;) {
            int count = zIn.read(buffer);
            totalCount += count;
            //System.out.println("   " + (totalCount)/1024 + " kb read");
            if (count==-1)
                break;
            baos.write(buffer, 0, count);
        }
        baos.close();
        lookupTableStaticData = baos.toByteArray();

        
    }


    private void readBinaryWordlist(InputStream zIn) throws IOException {
        //Read header
        byte[] buffer = new byte[1024];
        if (zIn.read(buffer, 0, 9)!=9)
            throw new IOException("Bad binary header length; 9 expected.");
        numWords = getInt(buffer, 0, 3);
        numLetters = getInt(buffer, 3, 2);
        longestWord = getInt(buffer, 5, 2);
        numLumps = getInt(buffer, 7, 2);

        //Save some data
        bitsPerLetter = Integer.toBinaryString(numLetters-1).length();
        bitsPerWordSize = Integer.toBinaryString(longestWord-1).length();
        bitsPerWordID = Integer.toBinaryString(numWords-1).length();

        //Temp: write data
        System.out.println("num words: " + numWords);
        System.out.println("num letters: " + numLetters);
        System.out.println("longest word: " + longestWord);
        System.out.println("num lumps: " + numLumps);

        //Read data for each lump
        int[] wordsInLump = new int[numLumps];
        int currLump = 0;
        while (currLump<numLumps) {
            int remLumps = Math.min(numLumps-currLump, buffer.length/3);
            if (zIn.read(buffer, 0, remLumps*3)!=remLumps*3)
                throw new IOException("Error reading header lump data");
            for (int i=0; i<remLumps; i++) {
                wordsInLump[currLump++] = getInt(buffer, i*3, 3);
            }
        }

        //Read value for each letter
        letterValues = new char[numLetters];
        int currLetter = 0;
        while (currLetter<numLetters) {
            int remLetters = Math.min(numLetters-currLetter, buffer.length/2);
            if (zIn.read(buffer, 0, remLetters*2)!=remLetters*2)
                throw new IOException("Error reading header letter data");
            for (int i=0; i<remLetters; i++) {
                letterValues[currLetter++] = (char)getInt(buffer, i*2, 2);
            }
        }

        //Now, read all words into a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;) {
            int count = zIn.read(buffer);
            if (count==-1)
                break;
            baos.write(buffer, 0, count);
        }
        baos.close();
        wordListData = baos.toByteArray();
    }


    private int getInt(byte[] buffer, int offset, int len) {
        //Something we can handle?
        switch(len) {
            case 1:
                return ((int)buffer[offset])&0xFF;
            case 2:
                return ((((int)buffer[offset])&0xFF)<<8) | ((((int)buffer[offset+1])&0xFF));
            case 3:
                return ((((int)buffer[offset])&0xFF)<<16) | ((((int)buffer[offset+1])&0xFF)<<8) | ((((int)buffer[offset+2])&0xFF));
            default:
                throw new IllegalArgumentException("Bad getInt() amount: " + len);
        }
    }



    //Some ListModel implementation details
    private int lastListID;        //If it matches, use existing data
    private int currNodeID;        //Used for finding the next ID
    //private int currNodeBitID;     //Used for finding the string
    private int currNodePrimaryID; //What word we're on in the 

    //More data: bookkeeping
    private int totalPrimaryWords;
    private int selectedIndex; //Used in a predictable way
    private Vector selectionListeners = new Vector();

    //Sizes - static
    private int bitsPerNodeID;
    private int bitsPerNodeBitID;
    private int bitsPerNumMaxChildren;
    private int bitsPerNumMaxMatches;
    private int bitsPerNodeStaticData;
    private int bitsPerTreeLetter = Integer.toBinaryString('z'-'a').length();
    
    //Sizes - variable
    private int bitsperWordBitID;

    //Sizes - dictionary
    private int bitsPerLetter;
    private int bitsPerWordSize;
    private int bitsPerWordID;

    //Readers
    private BitInputStream wordListStr;
    private BitInputStream lookupTableStaticStr;
    private BitInputStream lookupTableVariableStr;

    private void freeModel() {
        try {
            if (wordListStr!=null)
                wordListStr.close();
            if (lookupTableStaticStr!=null)
                lookupTableStaticStr.close();
            if (lookupTableVariableStr!=null)
                lookupTableVariableStr.close();
        } catch (IOException ex) {} //Should never happen

        //Free space
        wordListStr = null;
        lookupTableStaticStr = null;
        lookupTableVariableStr = null;

        //Don't forget to tree the byte[] arrays... but not here.
    }


    //Set the temporary list, item ID, and modified size
    //  Returns false if there's nothing to search for
    public void performSearch(String word) throws IOException {

        //TODO: Search and update


        
    }


    private void initModel() throws IOException {
        //Init our streams
        wordListStr = new BitInputStream(new ByteArrayInputStream(wordListData));
        lookupTableStaticStr = new BitInputStream(new ByteArrayInputStream(lookupTableStaticData));
        lookupTableVariableStr = new BitInputStream(new ByteArrayInputStream(lookupTableVariableData));

        //Init our data
        this.currNodeID = 0;
        //this.currNodeBitID = readNodeBitID(this.currNodeID);
        this.currNodePrimaryID = 0;

        //Go to the first node, alphabetically, with any primary results.
        for (;;) {
            //Stop?
            int numPrimary = readNodeNumPrimaryMatches(this.currNodeID);
            if (numPrimary>0)
                break;

            //Increment; follow the left-most node
            this.currNodeID = nodeAdvance(currNodeID, 0);
        }
        //this.currNodeBitID = readNodeBitID(this.currNodeID);
        this.totalPrimaryWords = numWords;

        //Test:
        //String firstWord = readWordString(currID, currNodePrimaryID);
        //System.out.println("First word: " + firstWord);

        //Test 2:
        /*int tempNode = currID;
        totalPrimaryWords = 0;
        for (;;) {
            //Stop?
            totalPrimaryWords += readNodeNumPrimaryMatches(tempNode);

            //Increment; follow the left-most node
            tempNode = nodeAdvance(tempNode);
            if (tempNode==-1) //Nowhere to go
                break;
        }
        System.out.println("Total primary words: " + totalPrimaryWords);*/
    }
    

    private int readNodeBitID(int nodeID) {
        //Node IDs are at offset 1, after totalReachable
        try {
            return lookupTableStaticStr.readNumberAt(bitsPerNodeStaticData*nodeID + bitsPerWordID, bitsPerNodeBitID);
        } catch (IOException ex) {
            return -1;
        }
    }

    private int readNodeTotalReachableChildren(int nodeID) throws IOException  {
        //Word counts are at offset 0
        return lookupTableStaticStr.readNumberAt(bitsPerNodeStaticData*nodeID, bitsPerWordID);
    }

    private int readNodeNumChildren(int nodeID) throws IOException  {
        //Num children are offset 2, after totalReachable and start-bit
        return lookupTableStaticStr.readNumberAt(bitsPerNodeStaticData*nodeID + bitsPerWordID + bitsPerNodeBitID, bitsPerNumMaxChildren);
    }

    private int readNodeNumPrimaryMatches(int nodeID) throws IOException  {
        //Num primary matches are offset 3, after totalReachable, start-bit, and children
        return lookupTableStaticStr.readNumberAt(bitsPerNodeStaticData*nodeID + bitsPerWordID + bitsPerNodeBitID + bitsPerNumMaxChildren, bitsPerNumMaxMatches);
    }

    private int readNodeChildValue(int nodeID, int childID) throws IOException  {
        //Get variable index
        int nodeBitID = readNodeBitID(nodeID);

        //Skip letter+node for childID entries
        nodeBitID += (bitsPerTreeLetter+bitsPerNodeID)*childID;

        //Read the node value, not the key
        return lookupTableVariableStr.readNumberAt(nodeBitID+bitsPerTreeLetter, bitsPerNodeID);
    }

    private int readNodePrimaryMatch(int nodeID, int primaryID) throws IOException {
        //Get variable index
        int nodeBitID = readNodeBitID(nodeID);

        //Skip letter+node for all child entries
        int numChildren = readNodeNumChildren(nodeID);
        nodeBitID += (bitsPerTreeLetter+bitsPerNodeID)*numChildren;

        //Skip word_bits for each primary before this
        nodeBitID += bitsperWordBitID * primaryID;

        //Read the value, return it
        return lookupTableVariableStr.readNumberAt(nodeBitID, bitsperWordBitID);
    }

    private String readWordString(int nodeID, int wordPrimaryID)  throws IOException {
        int wordBitID = readNodePrimaryMatch(nodeID, wordPrimaryID);

        //Read all letters, translate
        StringBuffer sb = new StringBuffer();

        //Number of letters to read
        int numLetters = wordListStr.readNumberAt(wordBitID, bitsPerWordSize);
        wordBitID += bitsPerWordSize;

        //Each letter
        for (;numLetters>0;numLetters--) {
            int letterID = wordListStr.readNumberAt(wordBitID, bitsPerLetter);
            wordBitID += bitsPerLetter;
            char c = letterValues[letterID];
            sb.append(c);
        }

        return sb.toString();
    }

    //Returns the new primaryID
    /*private int primaryDevance(int nodeID, int primaryID) throws IOException {
        if (primaryID > 0)
            return primaryID-1;
        else
            return -1; //No more
    }

    //Returns the new primaryID
    private int primaryAdvance(int nodeID, int primaryID) throws IOException {
        int numPrimaries = readNodeNumPrimaryMatches(nodeID);
        primaryID++;
        if (primaryID < numPrimaries)
            return primaryID;
        else
            return -1; //No more
    }

    //Set startCountingAt to your id +1 to ignore your node
    private int nodeAdvance(int nodeID, int startCountingAt) throws IOException {
        //General protocol:
        // 1) follow the first child node after <ignoreChildren>, if it exists
        // 2) Else, go to your parent, determine what node ID you are, and call nodeAdvance
        // 3) If your parent is root and you're the last, return -1
        int numChildren = readNodeNumChildren(nodeID);
        if (numChildren>startCountingAt) {
            //Case 1
            return readNodeChildValue(nodeID, startCountingAt);
        } else {
            if (nodeID!=0) {
                //Case 2
                int parentNodeID = readNodeParentID(nodeID);
                int numParentsChildren = readNodeNumChildren(parentNodeID);
                int i;
                for (i=0; i<numParentsChildren; i++) {
                    int parentChildID = readNodeChildValue(parentNodeID, i);
                    if (parentChildID==nodeID)
                        break;
                }
                return nodeAdvance(parentNodeID, i+1);
            } else {
                //Caes 3
                return -1;
            }
        }
    }


    //Set startCountingAt to your id to ignore your node
    private int nodeDevance(int nodeID, int startCountingAt) throws IOException {
        //General protocol:
        // 1) follow the first child node before <ignoreChildren>, if it exists
        // 2) Else, go to your parent, determine what node ID you are, and call nodeDevance
        // 3) If your parent is root and you're the first, return -1
        //int numChildren = readNodeNumChildren(nodeID);
        if (startCountingAt>0) {
            //Case 1
            return readNodeChildValue(nodeID, startCountingAt-1);
        } else {
            if (nodeID!=0) {
                //Case 2
                int parentNodeID = readNodeParentID(nodeID);
                int numParentsChildren = readNodeNumChildren(parentNodeID);
                int i;
                for (i=0; i<numParentsChildren; i++) {
                    int parentChildID = readNodeChildValue(parentNodeID, i);
                    if (parentChildID==nodeID)
                        break;
                }
                return nodeDevance(parentNodeID, i);
            } else {
                //Caes 3
                return -1;
            }
        }
    }*/




    //Actual list model implementation
    public int getSize() {
        System.out.println("Num items: " + totalPrimaryWords);
        return totalPrimaryWords;
    }
    public Object getItemAt(int listID) {
        try {
            //List IDs are aligned with word IDs

            //Bring it up to speed
            System.out.println("Last list item: " + lastListID + "  , now seeking for: " + listID);
            while (lastListID != listID) {
                if (listID>lastListID) {
                    //Move right
                    lastListID++;
                    currNodePrimaryID = primaryAdvance(currNodeID, currNodePrimaryID);
                    if (currNodePrimaryID==-1) { //too far
                        currNodeID = nodeAdvance(currNodeID, 0);
                        if (currNodeID==-1)
                            throw new IllegalArgumentException("Cannot get item at: " + listID);
                        currNodePrimaryID = 0;
                    }
                } else if (listID<lastListID) {
                    //Move left
                    lastListID--;
                    currNodePrimaryID = primaryDevance(currNodeID, currNodePrimaryID);
                    if (currNodePrimaryID==-1) { //too far
                        currNodeID = nodeDevance(currNodeID, readNodeNumChildren(currNodeID));
                        if (currNodeID==-1)
                            throw new IllegalArgumentException("Cannot get item at: " + listID);
                        currNodePrimaryID = 0;
                    }
                }
            }

            //Get the item
            return readWordString(currNodeID, currNodePrimaryID);
        } catch (IOException ex) {
            return null;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int val) {
        int oldIndex = selectedIndex;
        selectedIndex = val;
        notifySelectionChanged(oldIndex, selectedIndex);
    }

    public void addDataChangedListener(DataChangedListener listen) {
        //Data is changed not through listeners, but by calling explicit functions.
        //  Our list will not auto-refresh, but that's fine for our project.
        //dataListeners.addElement(listen);
    }
    public void removeDataChangedListener(DataChangedListener listen) {
        //See above
        //dataListeners.removeElement(listen);
    }


    public void addSelectionListener(SelectionListener listen) {
        selectionListeners.addElement(listen);
    }
    public void removeSelectionListener(SelectionListener listen) {
        selectionListeners.removeElement(listen);
    }
    private void notifySelectionChanged(int oldIndex, int newIndex) {
        for (int i=0; i<selectionListeners.size(); i++) {
            SelectionListener sl = (SelectionListener)selectionListeners.elementAt(i);
            sl.selectionChanged(oldIndex, newIndex);
        }
    }
    
    

    //Not supported
    public void addItem(Object arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"addItem()\"");
    }
    public void removeItem(int arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"removeItem()\"");
    }
    
}


