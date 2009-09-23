package ornagai.mobile;

import com.sun.lwuit.List;
import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.events.SelectionListener;
import com.sun.lwuit.list.ListModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
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
        bitsPerNodeStaticData = bitsPerWordID + bitsPerNodeBitID + bitsPerNumMaxChildren + 2*bitsPerNumMaxMatches;
        bitsperWordBitID = Integer.toBinaryString(numMaxWordBitID-1).length();
        System.out.println("Bits per lookup letter: " + bitsPerTreeLetter);
        System.out.println("Bits per node ID: " + bitsPerNodeID);
        System.out.println("Bits per word bit ID: " + bitsperWordBitID);
        System.out.println("Bits per node static data: " + bitsPerNodeStaticData);


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
    private Vector searchResults = new Vector();
    private int searchResultsStartID = 0; //Where to insert it
    private int searchResultsMatchNodeID = 0; //Match found?

    //More data: bookkeeping
    private int totalPrimaryWords;
    private int selectedIndex; //Used in a predictable way
    private EventDispatcher selectionListener = new EventDispatcher();

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

    //Cache
    private static final int MAX_CACHED_WORDS = 20;
    private int[] cachedIDs = new int[MAX_CACHED_WORDS];
    private DictionaryRenderer.DictionaryListEntry[] cachedVals = new DictionaryRenderer.DictionaryListEntry[MAX_CACHED_WORDS];
    private int evictID = 0;

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
        for (int i=0; i<cachedVals.length; i++)
            cachedVals[i] = null;

        //Don't forget to tree the byte[] arrays... but not here.
    }


    //Set the temporary list, item ID, and modified size
    //  Returns false if there's nothing to search for
    public void performSearch(String word) throws IOException {
        //First: clear our cache
        //for (int i=0; i<cachedIDs.length; i++)
        //    cachedIDs[i] = -1;
        //evictID = 0;

        //Goal: Search down the tree on each letter; put together primary and seccondary matches
        int resID = 0;
        try {
            searchResultsMatchNodeID = 0;
            searchResultsStartID = 0;
            searchResults.removeAllElements();
            SEARCH_LOOP:
            for (int letterID=0; letterID<word.length(); letterID++) {
                //Check all children
                char letter = Character.toLowerCase(word.charAt(letterID));
                int numChildren = readNodeNumChildren(searchResultsMatchNodeID);
                int prevNodeID = searchResultsMatchNodeID;

                //Loop through each child
                for (int currChild=0; currChild<numChildren; currChild++) {
                    //Check the next child
                    char childLetter = readNodeChildKey(searchResultsMatchNodeID, currChild);
                    int childID = readNodeChildValue(searchResultsMatchNodeID, currChild);

                    //Have we found our word?
                    if (letter == childLetter) {
                        searchResultsMatchNodeID = childID;
                        break;
                    } else {
                        //Count
                        int currCount = readNodeTotalReachableChildren(childID);
                        searchResultsStartID += currCount;
                    }
                }

                //Are we at the end of the word? Alternatively, did we fail a match?
                if (letterID==word.length()-1 || searchResultsMatchNodeID==prevNodeID) {
                    if (searchResultsMatchNodeID != prevNodeID) {
                        //Result containers
                        int numPrimary = readNodeNumPrimaryMatches(searchResultsMatchNodeID);
                        String[] primaryResults = new String[numPrimary];
                        int numSecondary = readNodeNumSecondaryMatches(searchResultsMatchNodeID);
                        String[] secondaryResults = new String[numSecondary];
                        
                        //Get a nifty cache of results
                        System.out.print("primary results: ");
                        for (int i=0; i<numPrimary; i++) {
                            primaryResults[i] = readWordString(searchResultsMatchNodeID, i);
                            System.out.print(primaryResults[i] + (i<numPrimary-1 ? "  ,  " : ""));
                        }
                        System.out.println();
                        System.out.print("secondary results: ");
                        for (int i=0; i<numSecondary; i++) {
                            secondaryResults[i] = readWordSecondaryString(searchResultsMatchNodeID, i);
                            System.out.print(secondaryResults[i] + (i<numSecondary-1 ? "  ,  " : ""));
                        }
                        System.out.println();

                        //Now, combine our results into one vector.
                        //  If we pass the point where our word should have matched, insert a "not found" message.
                        int nextPrimID = 0;
                        int nextSecID = 0;
                        boolean passedSeekWord = false;
                        while (nextPrimID<numPrimary || nextSecID<numSecondary || !passedSeekWord) {
                            //Get our lineup of potential matches
                            String nextPrimaryCandidate = nextPrimID<numPrimary ? primaryResults[nextPrimID] : null;
                            String nextSecondaryCandidate = nextSecID<numSecondary ? secondaryResults[nextSecID] : null;

                            //Special case: only the seek word left (implies it didn't match)
                            if (nextPrimaryCandidate==null && nextSecondaryCandidate==null) {
                                resID = searchResults.size();
                                searchResults.addElement("Not found: " + word);
                                passedSeekWord = true;
                                continue;
                            }
                            
                            //Easy cases: one word is null:
                            String nextWord = null;
                            int nextID = 0; //1,2 for prim/sec. 0 for nil
                            if (nextPrimaryCandidate==null) {
                                nextWord = nextSecondaryCandidate;
                                nextID = 2;
                            } else if (nextSecondaryCandidate==null) {
                                nextWord = nextPrimaryCandidate;
                                nextID = 1;
                            }

                            //Slightly  harder case: neither word is null:
                            if (nextWord==null) {
                                if (nextPrimaryCandidate.toLowerCase().compareTo(nextSecondaryCandidate.toLowerCase())<=0) {
                                    nextWord = nextPrimaryCandidate;
                                    nextID = 1;
                                } else {
                                    nextWord = nextSecondaryCandidate;
                                    nextID = 2;
                                }
                            }

                            //Is the next match at or past our search word?
                            if (!passedSeekWord) {
                                int search = nextWord.toLowerCase().compareTo(word.toLowerCase());
                                if (search==0) {
                                    passedSeekWord = true;
                                    resID = searchResults.size();
                                } else if (search>0) {
                                    nextWord = "Not found: " + word;
                                    passedSeekWord = true;
                                    nextID = 0;
                                    resID = searchResults.size();
                                }
                            }

                            //Add it
                            searchResults.addElement(nextWord);

                            //Increment
                            if (nextID==1)
                                nextPrimID++;
                            else if (nextID==2)
                                nextSecID++;
                        }

                        //Double-check:
                        System.out.print("sorted results: ");
                        for (int i=0; i<searchResults.size(); i++) {
                            System.out.print(searchResults.elementAt(i) + (i<searchResults.size()-1 ? "  ,  " : ""));
                        }
                        System.out.println();
                    } else {
                        //Didn't find any matches, primary or secondary
                        searchResults.addElement("Not found: " + word);
                        searchResultsMatchNodeID = 0;
                    }
                }
            }
        } catch (IOException ex) {
            //Attempt to recover
            searchResultsMatchNodeID = 0;
            searchResultsStartID = 0;
            searchResults.removeAllElements();
            setSelectedIndex(0);
        }


        //Now, just set the index
        this.setSelectedIndex(searchResultsStartID + resID);
    }


    private void initModel() throws IOException {
        //Init our streams
        wordListStr = new BitInputStream(new ByteArrayInputStream(wordListData));
        lookupTableStaticStr = new BitInputStream(new ByteArrayInputStream(lookupTableStaticData));
        lookupTableVariableStr = new BitInputStream(new ByteArrayInputStream(lookupTableVariableData));

        //Init our cache
        for (int i=0; i<cachedIDs.length; i++)
            cachedIDs[i] = -1;

        //Init our data
        this.totalPrimaryWords = numWords;
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

    private int readNodeNumSecondaryMatches(int nodeID) throws IOException  {
        //Num primary matches are offset 3, after totalReachable, start-bit, children, and primary id
        return lookupTableStaticStr.readNumberAt(bitsPerNodeStaticData*nodeID + bitsPerWordID + bitsPerNodeBitID + bitsPerNumMaxChildren + bitsPerNumMaxMatches, bitsPerNumMaxMatches);
    }

    private char readNodeChildKey(int nodeID, int childID) throws IOException  {
        //Get variable index
        int nodeBitID = readNodeBitID(nodeID);

        //Skip letter+node for childID entries
        nodeBitID += (bitsPerTreeLetter+bitsPerNodeID)*childID;

        //Read the node value, not the key
        return (char)(lookupTableVariableStr.readNumberAt(nodeBitID, bitsPerTreeLetter)+'a');
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
        int res = lookupTableVariableStr.readNumberAt(nodeBitID, bitsperWordBitID);

        //System.out.println("Reading: " + nodeBitID + "   " + bitsperWordBitID + "   " + res);
        return res;
    }

    private int readNodeSecondaryMatch(int nodeID, int secondaryID) throws IOException {
        //Get variable index
        int nodeBitID = readNodeBitID(nodeID);

        //Skip letter+node for all child entries
        int numChildren = readNodeNumChildren(nodeID);
        nodeBitID += (bitsPerTreeLetter+bitsPerNodeID)*numChildren;

        //Skip all primary matches
        int numPrimary = readNodeNumPrimaryMatches(nodeID);
        nodeBitID += bitsperWordBitID * numPrimary;

        //Skip word_bits for each secondary before this
        nodeBitID += bitsperWordBitID * secondaryID;

        //Read the value, return it
        int res = lookupTableVariableStr.readNumberAt(nodeBitID, bitsperWordBitID);

        //System.out.println("Reading: " + nodeBitID + "   " + bitsperWordBitID + "   " + res);
        return res;
    }

    private String readWordString(int nodeID, int wordPrimaryID)  throws IOException {
        int wordBitID = readNodePrimaryMatch(nodeID, wordPrimaryID);

        return readWordStringFromBitID(wordBitID);
    }
    private String readWordStringFromBitID(int wordBitID) throws IOException {
        //Read all letters, translate
        StringBuffer sb = new StringBuffer();

        //Number of letters to read
        int numLetters = wordListStr.readNumberAt(wordBitID, bitsPerWordSize);

        //Each letter
        for (;numLetters>0;numLetters--) {
            int letterID = wordListStr.readNumber(bitsPerLetter);
            if (letterID>=letterValues.length)
                return "<error reading string>";
            char c = letterValues[letterID];
            sb.append(c);
        }

        return sb.toString();
    }

    private String readWordSecondaryString(int nodeID, int wordSecondaryID)  throws IOException {
        int wordBitID = readNodeSecondaryMatch(nodeID, wordSecondaryID);

        return readWordStringFromBitID(wordBitID);
    }

    //Actual list model implementation
    public int getSize() {
        return totalPrimaryWords + searchResults.size();
    }
    public Object getItemAt(int listID) {
        //Valid?
        if (listID<0 || listID>=getSize())
            return null;

        //Check our search results before checking our cache
        int adjID = listID - searchResultsStartID;
        DictionaryRenderer.DictionaryListEntry res = new DictionaryRenderer.DictionaryListEntry();
        if (adjID>=0 && adjID<searchResults.size()) {
            res.word = (String)searchResults.elementAt(adjID);
            res.isMatchedResult = true;
            return res;
        } else {
            //Adjust our search results based on the result list
            if (listID < searchResultsStartID)
                adjID = listID;
            else
                adjID = listID - searchResults.size();
        }

        //Check our cache before getting this item directly.
        for (int i=0; i<cachedIDs.length; i++) {
            if (cachedIDs[i] == adjID) {
                res = cachedVals[i];
                //System.out.println("Get item: " + listID + " (cached)  : " + res);
                return res;
            }
        }

        try {
            //Due to the way words are stored, the fastest way to
            //  find a word's starting ID is to browse from the top of the
            //  node down
            int nodeID = 0;
            int primaryWordID = -1;
            int nodeStartID = 0; //Necessary to start at index zero.
            for (;primaryWordID==-1;) {
                //Check all children
                int numChildren = readNodeNumChildren(nodeID);
                int totalCount = 0;

                //Loop through each child
                for (int currChild=0; currChild<numChildren; currChild++) {
                    //Advance to the next child
                    int childID = readNodeChildValue(nodeID, currChild);
                    int currCount = readNodeTotalReachableChildren(childID);
                    totalCount += currCount;

                    //System.out.println("  Next child has: " + currCount + "  total: " + totalCount + "   start ID: " + nodeStartID);

                    //Stop here if we know the child is along the right path.
                    if (nodeStartID+totalCount > adjID) {
                        //Set up to advance
                        nodeID = childID;
                        nodeStartID = nodeStartID + totalCount - currCount;

                        //Does this child _actually_ contain the wordID (directly)?
                        int numPrimary = readNodeNumPrimaryMatches(nodeID);
                        //System.out.println("    TAKE: " + numPrimary + " primary");
                        if (nodeStartID+numPrimary > adjID)
                            primaryWordID = adjID-nodeStartID;
                        else
                            nodeStartID += numPrimary;

                        //Advance
                        break;
                    } else if (currChild==numChildren-1)
                        throw new RuntimeException("No matches from node: " + nodeID + " on list: " + adjID + " total count: " + totalCount);
                }
            }

            //Set up results
            res.word = readWordString(nodeID, primaryWordID);
            res.isMatchedResult = false;

            //Add to our stack
            cachedIDs[evictID] = adjID;
            cachedVals[evictID] = res;
            evictID++;
            if (evictID >= cachedIDs.length)
                evictID = 0;

            return res;
        } catch (IOException ex) {
            //System.out.println("Get item: " + listID + "  : " + null);
            return null;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int val) {
        int oldIndex = selectedIndex;
        selectedIndex = val;

        selectionListener.fireSelectionEvent(oldIndex, selectedIndex);
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
        selectionListener.addListener(listen);
    }
    public void removeSelectionListener(SelectionListener listen) {
        selectionListener.removeListener(listen);
    }
    
    

    //Not supported
    public void addItem(Object arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"addItem()\"");
    }
    public void removeItem(int arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"removeItem()\"");
    }
    
}


