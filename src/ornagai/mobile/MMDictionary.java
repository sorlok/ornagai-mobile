package ornagai.mobile;

import java.io.IOException;
import java.io.InputStream;
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
                System.out.println("LUMP " + (i+1) + " contains " + wordsInLump[currLump-1] + " words.");
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
                System.out.println("character: " + Integer.toHexString(letterValues[currLetter-1]));
            }
        }

        //
        

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

    }

}

