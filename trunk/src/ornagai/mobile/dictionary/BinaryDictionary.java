package ornagai.mobile.dictionary;

import ornagai.mobile.io.AbstractFile;
import ornagai.mobile.*;
import com.sun.lwuit.events.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.Vector;

import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;


/**
 *
 * @author Seth N. Hetu
 */
public class BinaryDictionary extends MMDictionary implements ProcessAction {
    //No enums. :(
    //private static final int FMT_TEXT = 0;
    //private static final int FMT_BINARY = 1;

    //Data - Static
    private AbstractFile dictFile;

    //Data - Runtime
    private boolean doneWithSearchFiles = false;
    private byte[] wordListData;
    private int[] wordsInLump;
    private WeakReference[] lumpDataCache;
    private int[] currLumpDefinitionSizes;
    private byte[] lookupTableStaticData;
    private byte[] lookupTableVariableData;
    private byte[] currLumpData;
    //private int fileFormat = -1;

    //Binary data
    private int numWords;
    private int numLetters;
    private int longestWord;
    private int numLumps;
    private char[] letterValues;

    //Curr lump binary data
    private char[] currLumpLetters;
    private int currLumpBitsPerLetter;

    private String format="";


    //Constructor is package-private; we only want to load files from MMDictionary
    BinaryDictionary(AbstractFile dictionaryFile, String format) {
        this.dictFile = dictionaryFile;
        this.format = format;
    }

    //Load all the things we need to look up a word
    public void loadLookupTree() {
        System.gc();
        System.out.println("Memory in use before loading: " + (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used");

        //This is the only function that doesn't need to check for
        // FMT_TEXT; we know that nothing's been loaded.
        dictFile.openProcessClose("word_list-"+format+".bin", this);
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
                //Now, read the contents of the file
                // NOTE: remove FMT_TEXT nonsense later.
                try {
                    readBinaryWordlist(file);
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
        for (;;) {
            int count = zIn.read(buffer);
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
        wordsInLump = new int[numLumps];
        lumpDataCache = new WeakReference[numLumps];
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
    private int searchResultsNumPrimaryMatches = 0; //How many entries to obscure.

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
    private BitInputStream currLumpStr;
    private int currLumpID = -1;



    //Our special search function. This function only gets us to the
    //    node ID of the match (or near-match); building the list
    //    of candidates is the responsibility of the next function.
    class SearchResult {
        public boolean fullMatch; //Always true if foundPrimary is true
        public int wordIDOrNearest;
        public int matchedNodeID;
        public int numPrimaryMatches; //Used to hide primary results
    }
    private SearchResult scanTree(String word) {
        //Init
        SearchResult result = new SearchResult();
        result.wordIDOrNearest = 0;
        result.matchedNodeID = 0;
        result.numPrimaryMatches = 0;
        result.fullMatch = false;

        //Goal: search down the tree on each letter; end on the node with
        //      the nearest match.
        try {
            //Main search loop. Operates on each letter.
            SEARCH_LOOP:
            for (int letterID=0; letterID<word.length(); letterID++) {
                //Prepare to check this letter
                char letter = Character.toLowerCase(word.charAt(letterID));
                int prevNodeID = result.matchedNodeID;

                //System.out.println("At letter: " + letter + "  , with count " + result.wordIDOrNearest);

                //Loop through each child
                int numChildren = readNodeNumChildren(result.matchedNodeID);
                for (int currChild=0; currChild<numChildren; currChild++) {
                    //Check the next child
                    char childLetter = readNodeChildKey(result.matchedNodeID, currChild);
                    int childID = readNodeChildValue(result.matchedNodeID, currChild);

                    //System.out.println("   child(" + childLetter + ")");

                    //Have we found our word?
                    if (letter == childLetter) {
                        if (letterID<word.length()-1) {
                            int currCount = readNodeNumPrimaryMatches(childID); //Still have to add the actual matches
                            //System.out.println("      +" + currCount);
                            result.wordIDOrNearest += currCount;
                        }

                        result.matchedNodeID = childID;
                        break;
                    } else if (letter > childLetter) {
                        //Count up, if we're not past a matching point
                        int currCount = readNodeTotalReachableChildren(childID);
                        //System.out.println("      +" + currCount);
                        result.wordIDOrNearest += currCount;
                    }
                }

                //Are we at the end of the word? Alternatively, did we fail a match?
                if (letterID==word.length()-1 || result.matchedNodeID==prevNodeID) {
                    if (result.matchedNodeID != prevNodeID) {
                        //We're at the end of the word.
                        result.fullMatch = true;
                        result.numPrimaryMatches = readNodeNumPrimaryMatches(result.matchedNodeID);
                        break SEARCH_LOOP;
                    } else {
                        //Didn't find any matches, primary or secondary
                        result.matchedNodeID = 0;
                        break SEARCH_LOOP;
                    }
                }
            }
        } catch (IOException ex) {
            //Attempt to recover
            result.matchedNodeID = 0;
            result.wordIDOrNearest = 0;
            result.fullMatch = false;
            result.numPrimaryMatches = 0;
        }

        return result;
    }


    
    //Set the temporary list, item ID, and modified size
    //  Returns false if there's nothing to search for
    public void performSearch(String word) throws IOException {
        //Init
        DictionaryListEntry notFoundEntry = new DictionaryListEntry("Not found: " + word, -1, true);
        searchResults.removeAllElements();

        //Perform a match; parse the results
        SearchResult match = scanTree(word);
        int resOffset = 0;
        if (!match.fullMatch) {
            //No match, or IOException. Either way, the returned values should be valid.
            searchResultsMatchNodeID = match.matchedNodeID;
            searchResultsStartID = match.wordIDOrNearest;
            searchResultsNumPrimaryMatches = match.numPrimaryMatches;
            searchResults.removeAllElements();
            searchResults.addElement(notFoundEntry);
            setSelectedIndex(searchResultsStartID);
        } else {
            //Set relevant data fields
            searchResultsMatchNodeID = match.matchedNodeID;
            searchResultsStartID = match.wordIDOrNearest;
            searchResultsNumPrimaryMatches = match.numPrimaryMatches;

            //System.out.println("Found word \"" + word + "\" at " + searchResultsStartID);

            //Result containers
            int numPrimary = readNodeNumPrimaryMatches(searchResultsMatchNodeID);
            DictionaryListEntry[] primaryResults = new DictionaryListEntry[numPrimary];
            int numSecondary = readNodeNumSecondaryMatches(searchResultsMatchNodeID);
            DictionaryListEntry[] secondaryResults = new DictionaryListEntry[numSecondary];

            //Get a nifty cache of results
            //System.out.print("primary results: ");
            for (int i=0; i<numPrimary; i++) {
                primaryResults[i] = new DictionaryListEntry(readWordString(searchResultsMatchNodeID, i), -2, true);
                //System.out.print(primaryResults[i].word + (i<numPrimary-1 ? "  ,  " : ""));
            }
            //System.out.println();
            //System.out.print("secondary results: ");
            for (int i=0; i<numSecondary; i++) {
                secondaryResults[i] = new DictionaryListEntry(readWordSecondaryString(searchResultsMatchNodeID, i), -2, true);
                //System.out.print(secondaryResults[i].word + (i<numSecondary-1 ? "  ,  " : ""));
            }
            //System.out.println();

            //Now, combine our results into one vector.
            //  If we pass the point where our word should have matched, insert a "not found" message.
            int nextPrimID = 0;
            int nextSecID = 0;
            boolean passedSeekWord = false;
            while (nextPrimID<numPrimary || nextSecID<numSecondary || !passedSeekWord) {
                //Get our lineup of potential matches
                DictionaryListEntry nextPrimaryCandidate = nextPrimID<numPrimary ? primaryResults[nextPrimID] : null;
                DictionaryListEntry nextSecondaryCandidate = nextSecID<numSecondary ? secondaryResults[nextSecID] : null;

                //Special case: only the seek word left (implies it didn't match)
                if (nextPrimaryCandidate==null && nextSecondaryCandidate==null) {
                    resOffset = searchResults.size();
                    searchResults.addElement(notFoundEntry);
                    passedSeekWord = true;
                    continue;
                }

                //Easy cases: one word is null:
                DictionaryListEntry nextWord = null;
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
                    if (nextPrimaryCandidate.compareTo(nextSecondaryCandidate)<=0) {
                        nextWord = nextPrimaryCandidate;
                        nextID = 1;
                    } else {
                        nextWord = nextSecondaryCandidate;
                        nextID = 2;
                    }
                }

                //Is the next match at or past our search word?
                if (!passedSeekWord) {
                    int search = nextWord.compareTo(word);
                    if (search==0) {
                        passedSeekWord = true;
                        resOffset = searchResults.size();
                    } else if (search>0) {
                        nextWord.word = "Not found: " + word;
                        nextWord.id = -1;
                        passedSeekWord = true;
                        nextID = 0;
                        resOffset = searchResults.size();
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
            /*System.out.print("sorted results: ");
            for (int i=0; i<searchResults.size(); i++) {
                System.out.print(searchResults.elementAt(i) + (i<searchResults.size()-1 ? "  ,  " : ""));
            }
            System.out.println();*/

            //Set the selected index
            this.setSelectedIndex(searchResultsStartID + resOffset);
        }
    }

    public int findWordIDFromEntry(DictionaryListEntry entry) {
        //Step 1: Search for the first word (in the compound word)
        String word = MZMobileDictionary.getFirstWord(entry.word);
        if (word.length()==0)
            return-1;
        SearchResult res = scanTree(word);
        if (res.numPrimaryMatches==0)
            return -1;

        //Step 2: From the matched wordID, check each primary match and
        //        add 1 until we either reach the full word, or no match can be made.
        try {
            int numPrimary = readNodeNumPrimaryMatches(res.matchedNodeID);
            for (int i=0; i<numPrimary; i++) {
                String primaryMatch = readWordString(res.matchedNodeID, i);
                if (entry.compareTo(primaryMatch)==0)
                    return res.wordIDOrNearest;
                res.wordIDOrNearest++;
            }
            return -1; //No match
        } catch (IOException ex) {
            return -1;
        }
    }

    public String[] getWordTuple(DictionaryListEntry entry) {
        //Any chance?
        if (entry.id<0)
            return null;

        //Prepare result set
        String[] result = new String[3];
        result[0] = entry.word;
        
        //Figure out which lump contains this definition
        int lumpID = 0;
        int startLumpOffset = 0;
        int lumpAdjID = 0;
        for (;lumpID<wordsInLump.length; lumpID++) {
            lumpAdjID = entry.id - startLumpOffset;
            startLumpOffset += wordsInLump[lumpID];
            if (startLumpOffset>entry.id)
                break;
            if (lumpID==wordsInLump.length-1)
                return null;
        }

        //Is this the ID of the lump that we've cached? If not, read this lump.
        if (lumpID != currLumpID) {
            //Set
            currLumpID = lumpID;

            //Flush previous data
            currLumpLetters = null;
            currLumpDefinitionSizes = null;
            currLumpData = null;
            currLumpStr = null;

            //Try to load from our cache
            if (lumpDataCache[currLumpID]!=null)
                currLumpData = (byte[])lumpDataCache[currLumpID].get();

            //Reference no longer intact?
            if (currLumpData==null) {
                //Save, read, append
                dictFile.openProcessClose("lump_" + (lumpID+1) + ".bin", new ProcessAction() {
                    public void processFile(InputStream file) {
                        try {
                            //Read static data
                            byte[] buffer = new byte[5];
                            if (file.read(buffer) != buffer.length)
                                throw new RuntimeException("Error reading lump file: too short?.");
                            currLumpLetters = new char[getInt(buffer, 3, 2)];
                            currLumpBitsPerLetter = Integer.toBinaryString(currLumpLetters.length-1).length();

                            //Read "size" values
                            currLumpDefinitionSizes = new int[wordsInLump[currLumpID]];
                            buffer = new byte[currLumpDefinitionSizes.length*2];
                            if (file.read(buffer) != buffer.length)
                                throw new RuntimeException("Error reading lump file: too short?.");
                            for (int i=0; i<currLumpDefinitionSizes.length; i++)
                                currLumpDefinitionSizes[i] = getInt(buffer, i*2, 2);

                            //Read "letter" lookup
                            buffer = new byte[currLumpLetters.length*2];
                            if (file.read(buffer) != buffer.length)
                                throw new RuntimeException("Error reading lump file: too short?.");
                            for (int i=0; i<currLumpLetters.length; i++)
                                currLumpLetters[i] = (char)getInt(buffer, i*2, 2);

                            //Read remaining bitstream
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            buffer = new byte[1024];
                            for (;;) {
                                int count = file.read(buffer);
                                if (count==-1)
                                    break;
                                baos.write(buffer, 0, count);
                            }
                            baos.close();

                            currLumpData = baos.toByteArray();
                            currLumpStr = new BitInputStream(new ByteArrayInputStream(currLumpData));

                            //Cache
                            lumpDataCache[currLumpID] = new WeakReference(currLumpData);
                        } catch (IOException ex) {
                            //Handle...
                            throw new RuntimeException(ex.toString());
                        } catch (OutOfMemoryError er) {
                            System.out.println((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024 + " kb used, " + (Runtime.getRuntime().freeMemory()/1024) + " kb free");
                            throw new RuntimeException("Out of memory!");
                        }
                    }
                });
            }
        }

        //Get the word's bit ID within the lump file. Also get its number of letters
        int lumpBitID = readWordLumpBitID(lumpAdjID);
        System.out.println("Reading: " + lumpAdjID);
        int numLettersInWord = currLumpDefinitionSizes[lumpAdjID];

        //Now, read each letter and append it. Look for a tab to break between the POS and the definitoin.
        StringBuffer sb = new StringBuffer();
        boolean foundTab = false;
        for (int i=0; i<numLettersInWord; i++) {
            //First time, jump. After that, just advance.
            int letterID = 0;
            try {
                if (i==0)
                    letterID = currLumpStr.readNumberAt(lumpBitID, currLumpBitsPerLetter);
                else
                    letterID = currLumpStr.readNumber(currLumpBitsPerLetter);
            } catch (IOException ex) {
                return null;
            }

            //Append
            char letter = currLumpLetters[letterID];
            if (letter!='\t')
                sb.append(letter);

            if (letter=='\t' || i==numLettersInWord-1) {
                if (!foundTab)
                    result[1] = sb.toString();
                else
                    result[2] = sb.toString();
                sb = new StringBuffer();

                if (letter=='\t')
                    foundTab = true;
            }
        }

        //Debug
        /*System.out.println("Result: ");
        System.out.println(result[0]);
        System.out.print("     ");
        for (int i=0; i<result[0].length(); i++)
            System.out.print("0x"+Integer.toHexString(result[0].charAt(i)) + "  ");
        System.out.println();

        System.out.println(result[1]);
        System.out.print("     ");
        for (int i=0; i<result[1].length(); i++)
            System.out.print("0x"+Integer.toHexString(result[1].charAt(i)) + "  ");
        System.out.println();

        System.out.println(result[2]);
        System.out.print("     ");
        for (int i=0; i<result[2].length(); i++)
            System.out.print("0x"+Integer.toHexString(result[2].charAt(i)) + "  ");
        System.out.println();*/

        //Valid?
        if (foundTab)
            return result;
        else
            return null;
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
        int lettersLeftToRead = wordListStr.readNumberAt(wordBitID, bitsPerWordSize);

        //Each letter
        for (;lettersLeftToRead>0;lettersLeftToRead--) {
            int letterID = wordListStr.readNumber(bitsPerLetter);
            if (letterID>=letterValues.length)
                return "<error reading string>";
            char c = letterValues[letterID];
            sb.append(c);
        }

        return sb.toString();
    }

    private int readWordLumpBitID(int adjWordID) {
        //Add up
        int sum = 0;
        for (int i=0; i<adjWordID; i++)
            sum += currLumpDefinitionSizes[i];

        //Multiply
        return sum * currLumpBitsPerLetter;
    }

    private String readWordSecondaryString(int nodeID, int wordSecondaryID)  throws IOException {
        int wordBitID = readNodeSecondaryMatch(nodeID, wordSecondaryID);

        return readWordStringFromBitID(wordBitID);
    }

    //Actual list model implementation
    public int getSize() {
        //This equation should be self-evident, but I'm going to describe it just to make sure:
        //   1) totalPrimaryWords = natural number of words in the dictionary
        //   2) searchResults.size() = number of search results, including things like "not found" and secondary matches
        //   3) searchResultsNumPrimaryMatches = number of primary matches. We do not show primary matches twice.
        return totalPrimaryWords + searchResults.size() - searchResultsNumPrimaryMatches;
    }
    public Object getItemAt(int listID) {
        //Valid?
        if (listID<0 || listID>=getSize())
            return null;

        //Check our search results before checking our cache
        int adjID = listID - searchResultsStartID;
        if (adjID>=0 && adjID<searchResults.size()) {
            DictionaryListEntry res = (DictionaryListEntry)searchResults.elementAt(adjID);
            return res;
        } else {
            //Adjust our search results based on the result list
            if (listID < searchResultsStartID)
                adjID = listID;
            else
                adjID = listID - searchResults.size() + searchResultsNumPrimaryMatches;
        }

        //Check our cache before getting this item directly.
        for (int i=0; i<cachedIDs.length; i++) {
            if (cachedIDs[i] == adjID) {
                DictionaryListEntry res = cachedVals[i];
                //System.out.println("Get item: " + listID + " (cached)  : " + res);
                return res;
            }
        }

        //System.out.println("Searching for word: " + listID + "  (" + adjID + "), with " + searchResults.size() + " search results starting at " + searchResultsStartID);
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
            DictionaryListEntry res = new DictionaryListEntry(readWordString(nodeID, primaryWordID), adjID, false);

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

    public String getFormat() {
        return format;
    }
    
}


