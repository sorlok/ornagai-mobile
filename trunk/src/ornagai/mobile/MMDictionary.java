package ornagai.mobile;

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
public class MMDictionary implements ProcessAction {
    //No enums. :(
    private static final int FMT_TEXT = 0;
    private static final int FMT_BINARY = 1;

    //Data - Static
    private AbstractFile dictFile;

    //Data - Runtime
    private LookupNode dictionaryTree;
    private byte[] wordListData;
    private int fileFormat = -1;


    public MMDictionary(AbstractFile dictionaryFile) {
        this.dictFile = dictionaryFile;
    }

    //Load all the things we need to look up a word
    public void loadLookupTree() {
        //This is the only function that doesn't need to check for
        // FMT_TEXT; we know that nothing's been loaded.
        dictFile.openProcessClose(this);
    }


    //This function reads through our dictionary file and loads
    // whatever information we are looking for. This is done in a
    // somewhat implied fashion.
    public void processFile(InputStream file) {
        //If we don't have a list of sub-files in the zip,
        //   load that list. At the same time, load the dictionary tree.
        //   Also, optionally, load all data (text format)
        if (dictionaryTree==null) {
            //Open our file, get all entries
            ZipInputStream zIn = new ZipInputStream(file);
            ZipEntry entry = null;
            for (;;) {
                //Get the next zip entry.
                try {
                    entry = zIn.getNextEntry();
                } catch (IOException ex) {}

                //Any entries left?
                if (entry==null)
                    break;

                //Get entry information.
                String name = entry.getName();

                //Later, we'll be able to save marks, etc., to enable fast re-loading.
                // For now, we'll just store some useful information regarding this file
                if (name.startsWith("word_list")) {
                    //Binary format
                    fileFormat = FMT_BINARY;
                } else if (name.startsWith("words")) {
                    //Text format
                    fileFormat = FMT_TEXT;
                }

                //Now, read the contents of the file
                // NOTE: We need to un-lzma it.
                if (name.startsWith("word")) {
                    try {
                        if (fileFormat==FMT_TEXT) {
                            readTextWordlist(zIn);
                        } else if (fileFormat==FMT_BINARY) {
                            readBinaryWordlist(zIn);
                        }
                    } catch (IOException ex) {
                        //Handle...
                        throw new RuntimeException(ex.toString());
                    }
                }
            }


            //Done
            try {
                zIn.close();
            } catch (IOException ex) {}
        }
    }


    private void readTextWordlist(ZipInputStream zIn) throws IOException {
        
    }

    private void readBinaryWordlist(ZipInputStream zIn) throws IOException {
        //Read header
        byte[] buffer = new byte[1024];
        if (zIn.read(buffer, 0, 9)!=9)
            throw new IOException("Bad binary header length; 9 expected.");
        int numWords = getInt(buffer, 0, 3);
        int numLetters = getInt(buffer, 3, 2);
        int longestWord = getInt(buffer, 5, 2);
        int numLumps = getInt(buffer, 7, 2);

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
                //System.out.println("LUMP " + (i+1) + " contains " + wordsInLump[currLump-1] + " words.");
            }
        }

        //Read value for each letter
        char[] letterValues = new char[numLetters];
        int currLetter = 0;
        while (currLetter<numLetters) {
            int remLetters = Math.min(numLetters-currLetter, buffer.length/2);
            if (zIn.read(buffer, 0, remLetters*2)!=remLetters*2)
                throw new IOException("Error reading header letter data");
            for (int i=0; i<remLetters; i++) {
                letterValues[currLetter++] = (char)getInt(buffer, i*2, 2);
                //System.out.println("character(" + Integer.toHexString(letterValues[currLetter-1]) + "): " + (char)letterValues[currLetter-1]);
            }
        }

        //Do we have enough space to copy all bits?
        //long count = 0;

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

        //Finally, read and process all words
        baos = null;
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



    class LookupNode {
        private LookupNode[] childNodes;
        private char[] childKeys;
        private Object primaryMatchesObj;
        private Object secondaryMatchesObj;

        public LookupNode() {
            this.childNodes = new LookupNode['z'-'a' + 1];
            //this.childKeys = new char['z'-'a' + 1];
            this.primaryMatchesObj = new Vector();
            ((Vector)this.primaryMatchesObj).addElement(new Integer(0)); //For the count
            this.secondaryMatchesObj = new Vector();
        }

        public LookupNode addPath(char key) {
            if (childKeys!=null)
                throw new RuntimeException("Dictionary has already been loaded.");

            int id = key-'a';
            if (childNodes[id] == null) {
                Integer newCount = new Integer((((Integer)((Vector)primaryMatchesObj).elementAt(0))).intValue() + 1);
                ((Vector)primaryMatchesObj).setElementAt(newCount, 0);
                childNodes[id] = new LookupNode();
            }

            return childNodes[id];
        }

        public void addWord(int wordBitID, boolean isPrimaryMatch) {
            if (childKeys!=null)
                throw new RuntimeException("Dictionary has already been loaded.");

            if (isPrimaryMatch) {
                ((Vector)primaryMatchesObj).addElement(new Integer(wordBitID));
            } else {
                ((Vector)secondaryMatchesObj).addElement(new Integer(wordBitID));
            }
        }

        public void compress() {
            if (this.childKeys!=null)
                return;

            //Compress all Vectors into static arrays
            {
                //Count
                int numMatches = ((Integer)((Vector)primaryMatchesObj).elementAt(0)).intValue();

                //Compress
                LookupNode[] childNodesNew = new LookupNode[numMatches];
                char[] childKeysNew = new char[numMatches];
                int nextID = 0;
                for (int i=0; i<numMatches; i++) {
                    //Browse to the next key
                    while (childNodes[nextID]==null)
                        nextID++;

                    //Add it, increment
                    childNodesNew[i] = childNodes[nextID];
                    childKeysNew[i] = childKeys[nextID];
                    nextID++;
                }
                childKeys = childKeysNew;
                childNodes = childNodesNew;
            }

            //And the primary matches
            {
                int size = ((Vector)primaryMatchesObj).size();
                if (size==1)
                    primaryMatchesObj = null;
                else {
                    int[] primaryMatches = new int[size];
                    for (int i=1; i<size; i++) {
                        primaryMatches[i-1] = ((Integer)((Vector)primaryMatchesObj).elementAt(i)).intValue();
                    }
                    primaryMatchesObj = primaryMatches;
                }
            }

            //And the secondary matches
            {
                int size = ((Vector)secondaryMatchesObj).size();
                if (size==0)
                    secondaryMatchesObj = null;
                else {
                    int[] secondaryMatches = new int[size];
                    for (int i=0; i<size; i++) {
                        secondaryMatches[i] = ((Integer)((Vector)secondaryMatchesObj).elementAt(i)).intValue();
                    }
                    secondaryMatchesObj = secondaryMatches;
                }
            }

            //Compress all children
            for (int i=0; i<childNodes.length; i++)
                childNodes[i].compress();
        }
    }
}

