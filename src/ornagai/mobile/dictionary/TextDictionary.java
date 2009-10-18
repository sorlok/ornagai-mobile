
package ornagai.mobile.dictionary;

import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.events.SelectionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Stack;
import java.util.Vector;
import ornagai.mobile.io.AbstractFile;
import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;
import ornagai.mobile.ProcessAction;

import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;
import ornagai.mobile.EventDispatcher;
import ornagai.mobile.MZMobileDictionary;

/**
 *
 * @author Seth N. Hetu
 */
public class TextDictionary extends MMDictionary implements ProcessAction {
    //Meta-data
    private AbstractFile dictionaryFile;
    private String format;
    private String tabbing;

    //More data
    private int selectedIndex; //Used in a predictable way
    private EventDispatcher selectionListener = new EventDispatcher();

    //Searching
    private LookupNode rootNode = new LookupNode();
    private Vector searchResults = new Vector(); //DictionaryWord
    private int searchInsertID = 0;

    //Load from file
    private Vector wordlist = new Vector(); //DictionaryWord

    public void freeMostData() {
        this.wordlist = null;
        this.rootNode = null;
        this.searchResults = null;
    }


    //Returns the size of the byte array, ignoring any non-finished UTF-8 characters
    // This follows RFC 3629's recommendations, although it is not strictly compliant.
    int getUTF8Size(byte[] src, int length) {
        for (int i=0; i<length; i++) {
            //Handle carefully to avoid the security risk...
            byte curr = src[i];
            if (((((int)(curr>>3))&0xFF)^0x1E)==0 || MZMobileDictionary.debug_outside_bmp) {
                //We can't handle anything outside the BMP
                throw new IllegalArgumentException("Error: can't handle letters outside the BMP");
            } else if (((((int)(curr>>4))&0xFF)^0xE)==0) {
                //Verify the next two bytes, if there's enough
                if (i>=length-2)
                    return i;
                else
                    i+=2;
            } else if (((((int)(curr>>5))&0xFF)^0x6)==0) {
                //Verify the next byte
                if (i>=length-1)
                    return i;
                else
                    i++;
            }
        }

        //Done
        return length;
    }


    //Package-private
    TextDictionary(AbstractFile dictionaryFile, String format, String tabbing) {
        this.dictionaryFile = dictionaryFile;
        this.format = format;
        this.tabbing = tabbing;

        if (this.tabbing.length()!=3)
            throw new IllegalArgumentException("Bad format string: " + this.tabbing);
        if (this.tabbing.indexOf('w')==-1 || this.tabbing.indexOf('p')==-1 || this.tabbing.indexOf('d')==-1)
            throw new IllegalArgumentException("Incomplete format string: " + this.tabbing);

    }

    public void loadLookupTree(){
        //There's only one file: the lookup file.
        String wordFileName = "words-tab" + tabbing + "-" + format + ".txt";
        dictionaryFile.openProcessClose(wordFileName, this);
    }
    public void processFile(InputStream wordFile) {
        try {
            //Each line consists of three items, separated by tabs
            int categories = 3;
            int WORD_ID = 0;
            int POS_ID = 1;
            int DEF_ID = 2;
            String[] wpd = new String[categories];
            int[] indices = new int[categories];
            StringBuffer sb = new StringBuffer();
            int currIndex = 0;
            for (int i=0; i<categories; i++) {
                switch (tabbing.charAt(i)) {
                    case 'w':
                        indices[i] = WORD_ID;
                        break;
                    case 'p':
                        indices[i] = POS_ID;
                        break;
                    case 'd':
                        indices[i] = DEF_ID;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid format character: " + tabbing.charAt(i));
                }
            }

            //Read each line, make a new dictionary word.
            byte[] buffer = new byte[1024];
            String line = "";
            int bufferRemID = buffer.length;
            int count = 0;
            int onLoop = 0;
            int bufferLength = buffer.length;
            String lastWord = "";
            for (;;) {
                //Copy remaining
                int bufferStart = 0;
                for (int i=bufferRemID; i<bufferLength; i++)
                    buffer[bufferStart++] = buffer[i];

                //Read, continue?
                //System.out.println("Reading new buffer");
                try {
                    count = wordFile.read(buffer, bufferStart, buffer.length-bufferStart);
                    //System.out.println(" read " + count + " letters out of a possible " + buffer.length);
                } catch (IOException ex) {
                    throw new RuntimeException("Error reading text file: " + ex.toString());
                }
                if (count==-1)
                    break;

                //Convert to a UTF-8 string
                try {
                    bufferLength = count + bufferStart;
                    bufferRemID = getUTF8Size(buffer, bufferLength);
                    //System.out.println("buffRemID: " + bufferRemID + " of " + count + "," + bufferStart);
                    //System.out.println("Leftover characters: " + ((count+bufferStart) - bufferRemID));
                    try {
                        line = new String(buffer, 0, bufferRemID, "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException("Error: UTF-8 not supported for some reason");
                    }
                    //System.out.println("numletters: " + line.length());
                } catch (ArrayIndexOutOfBoundsException ex) {
                    throw new ArithmeticException("Error in UTF conversion on loop " + onLoop);
                }

                //Now, process this line
                for (int i=0; i<line.length(); i++) {
                    //Handle double newlines
                    char c = line.charAt(i);
                    if (c=='\r')
                        continue;

                    //Append this letter?
                    if (c!='\t' && c!='\n')
                        sb.append(c);

                    //Is it a tab or newline?
                    if (c=='\t' || c=='\n') {
                        //Save it
                        try {
                            wpd[indices[currIndex]] = sb.toString();
                            sb.delete(0, sb.length());
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            throw new IllegalArgumentException("Array index out of bounds, on line " + onLoop + "  last word: " + lastWord + " adding " + sb.toString());
                        }

                        //Increment, reset if newline. 
                        currIndex++;
                        if (c=='\n' || currIndex==3) {
                            if (currIndex!=3)
                                throw new IllegalArgumentException("currIndex is " + currIndex + ", was expecting " + 3 + "  at line " + onLoop);
                            currIndex = 0;
                        }

                        //Save a new entry?
                        if (currIndex==0) {
                            //For debugging
                            onLoop++;

                            int nextID = wordlist.size();
                            DictionaryWord newItem = new DictionaryWord(wpd[WORD_ID], wpd[POS_ID], wpd[DEF_ID], nextID, false);
                            lastWord = newItem.word;
                            //System.out.println("New item: " + wpd[WORD_ID] + "," + wpd[POS_ID] + "," + wpd[DEF_ID]);
                            wordlist.addElement(newItem);
                            addToLookup(newItem);
                        }
                    }
                }
            }
            if (MZMobileDictionary.debug_out_of_memory_text)
                throw new OutOfMemoryError("Debug: out of memory test");
        } catch (OutOfMemoryError err) {
            System.out.println("Out of memory on text dictionary load.");
            throw err;
        }
    }


    private void addToLookup(DictionaryWord item) {
        String word = item.word.toLowerCase();
        LookupNode currNode = rootNode;
        boolean isPrimary = true;
        for (int i=0; i<word.length(); i++) {
            //Only track a through z
            char letter = word.charAt(i);
            boolean wordBreak = false;
            if (letter>='a' && letter<='z') {
                //Jump to it, add it.
                currNode = currNode.checkAndAddChild(letter);
            } else {
                //Break if we haven't added this word yet
                if (i>0) {
                    int prevLetter = Character.toLowerCase(word.charAt(i-1));
                    if (prevLetter>='a' && prevLetter<='z')
                        wordBreak = true;
                }
            }

            //Word break?
            if (wordBreak || i==word.length()-1) {
                //Add it
                currNode.addMatch(item, isPrimary);

                //Reset
                currNode = rootNode;

                //No more primary words
                isPrimary = false;
            }
        }
    }


    private int getReasonableInsertPoint(String word) {
        //First, get a starting point
        char c = '\0';
        for (int i=0; i<word.length(); i++) {
            //Only search on letters
            c = Character.toLowerCase(word.charAt(i));
            if (c>='a' && c<='z')
                break;
        }

        //Worth searching?
        LookupNode startSearch = rootNode.checkNoAddChild(c);
        if (c=='\0' || startSearch==null)
            return 0;

        //Now, advance until we find something
        while (startSearch.primaryMatches.isEmpty()) {
            //Find the first child of this node
            if (startSearch.children.isEmpty())
                return 0;
            for (char next='a'; next<='z'; next++) {
                LookupNode nextNode = startSearch.checkNoAddChild(next);
                if (nextNode!=null) {
                    startSearch = nextNode;
                    break;
                }
            }
        }

        //Done
        int minID = 0;
        for (int i=0; i<startSearch.primaryMatches.size(); i++) {
            DictionaryWord match = (DictionaryWord) startSearch.primaryMatches.elementAt(i);
            if (match.id > minID)
                minID = match.id;
        }
        return minID;
    }

    //Returns the offset into arr of the match, or the "not found" entry.
    private int buildResultsArray(Vector arr, LookupNode node, String searchWord) {
        int resID = 0;
        arr.removeAllElements();

        //Simple
        DictionaryWord notFoundEntry = new DictionaryWord("Not found: " + searchWord, "", "", -1, true);
        if (node==null) {
            arr.addElement(notFoundEntry);
            return resID;
        }

        //Else, build in order
        int nextPrimID = 0;
        int nextSecID = 0;
        boolean passedSeekWord = false;
        while (nextPrimID<node.primaryMatches.size() || nextSecID<node.secondaryMatches.size() || !passedSeekWord) {
            //Get our lineup of potential matches
            DictionaryWord nextPrimaryCandidate = null;
            DictionaryWord nextSecondaryCandidate = null;
            if (nextPrimID<node.primaryMatches.size())
                nextPrimaryCandidate = (DictionaryWord) node.primaryMatches.elementAt(nextPrimID);
            if (nextSecID<node.secondaryMatches.size())
                nextSecondaryCandidate = (DictionaryWord) node.secondaryMatches.elementAt(nextSecID);

            //Special case: only the seek word left (implies it didn't match)
            if (nextPrimaryCandidate==null && nextSecondaryCandidate==null) {
                resID = searchResults.size();
                searchResults.addElement(notFoundEntry);
                passedSeekWord = true;
                continue;
            }

            //Easy cases: one word is null:
            DictionaryWord nextWord = null;
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
                int search = nextWord.compareTo(searchWord);
                if (search==0) {
                    passedSeekWord = true;
                    resID = searchResults.size();
                } else if (search>0) {
                    nextWord.word = "Not found: " + searchWord;
                    nextWord.id = -1;
                    passedSeekWord = true;
                    nextID = 0;
                    resID = searchResults.size();
                }
            }

            //Add it, copy and set the "isresult"
            searchResults.addElement(new DictionaryWord(nextWord.word, nextWord.pos, nextWord.definition, nextWord.id, true));

            //Increment
            if (nextID==1)
                nextPrimID++;
            else if (nextID==2)
                nextSecID++;
        }

        return resID;
    }


    public void performSearch(String word) throws IOException {
        //First: try to find an exact match.
        LookupNode currNode = rootNode;
        for (int i=0; i<word.length(); i++) {
            //Only search on letters
            char c = Character.toLowerCase(word.charAt(i));
            if (c<'a' || c>'z')
                continue;

            //Path exists?
            currNode = currNode.checkNoAddChild(c);
            if (currNode==null)
                break;
        }

        //Second: Build result list
        int additionalOffset = buildResultsArray(searchResults, currNode, word);

        //Third, if no matches, try to match on the first alphabetic letter
        searchInsertID = 0;
        if (currNode==null || currNode.primaryMatches.isEmpty()) {
            searchInsertID = getReasonableInsertPoint(word);
        } else {
            //If matches, our selection id is the lowest id of the primary matches
            for (int i=0; i<currNode.primaryMatches.size(); i++) {
                DictionaryWord match = (DictionaryWord) currNode.primaryMatches.elementAt(i);
                if (match.id > searchInsertID)
                    searchInsertID = match.id;
            }
        }

        //Finally, set the result
        setSelectedIndex(searchInsertID + additionalOffset);
    }



    public Object getItemAt(int listID) {
        //Valid?
        if (listID<0 || listID>=getSize())
            return null;

        //Check our search results before checking our cache
        int adjID = listID - searchInsertID;
        //DictionaryRenderer.DictionaryListEntry res = new DictionaryRenderer.DictionaryListEntry();
        if (adjID>=0 && adjID<searchResults.size()) {
            DictionaryWord res = (DictionaryWord)searchResults.elementAt(adjID);
            return res;
        } else {
            //Adjust our search results based on the result list
            if (listID < searchInsertID)
                adjID = listID;
            else
                adjID = listID - searchResults.size();
        }

        //Else, return the regular result
        DictionaryWord item = (DictionaryWord)wordlist.elementAt(adjID);
        return item;
    }

    public int findWordIDFromEntry(DictionaryListEntry entry) {
        //Not sure if this is ever necessary, but our data structure makes it easy
        if (entry.id==-2)
            throw new IllegalArgumentException("Text dictionaries must always have ids");
        return entry.id;
    }

    public String[] getWordTuple(DictionaryListEntry entry) {
        //Again, this is easy for dictionary words
        DictionaryWord item = (DictionaryWord)entry;
        return new String[]{item.word, item.pos, item.definition};
    }

    public int getSize() {
        return wordlist.size() + searchResults.size();
    }

/*    public void freeModel() {
        wordlist.removeAllElements();
    }*/
    
    
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

    public String getFormat() {
        return format;
    }

    //Not supported
    public void addItem(Object arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"addItem()\"");
    }
    public void removeItem(int arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"removeItem()\"");
    }



    class LookupNode {
        public Vector children = new Vector(); //Object[]{char, LookupNode}
        public Vector primaryMatches = new Vector(); //DictionaryWord, sorted
        public Vector secondaryMatches = new Vector(); //DictionaryWord, sorted

        public LookupNode checkAndAddChild(char key) {
            LookupNode res = checkNoAddChild(key);

            //No matches? Then add it
            if (res==null) {
                res = new LookupNode();
                Character keyObj = new Character(key);
                Object[] item = new Object[]{keyObj, res};
                children.addElement(item);
            }
            return res;
        }

        public LookupNode checkNoAddChild(char key) {
            for (int i=0; i<children.size(); i++) {
                Object[] o = (Object[])children.elementAt(i);
                char c = ((Character)o[0]).charValue();
                if (c==key)
                   return (LookupNode)o[1];
            }
            return null;
        }

        public void addMatch(DictionaryWord item, boolean isPrimary) {
            //Keep sorted
            Vector arr = isPrimary ? primaryMatches : secondaryMatches;
            int insertIndex = 0;
            for (;insertIndex<arr.size();insertIndex++) {
                DictionaryWord nextElem = (DictionaryWord)arr.elementAt(insertIndex);
                if (item.compareTo(nextElem)>0)
                    break;
            }
            arr.insertElementAt(item, insertIndex);
        }
    }



    class DictionaryWord extends DictionaryListEntry {
        String pos;
        String definition;
        DictionaryWord(String word, String pos, String definition, int id, boolean isResult) {
            this.word = word;
            this.pos = pos;
            this.definition = definition;
            this.id = id;
            this.isMatchedResult = isResult;
        }
    }

}

