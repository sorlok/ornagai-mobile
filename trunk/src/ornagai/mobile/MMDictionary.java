package ornagai.mobile;

import com.sun.lwuit.List;
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
}

