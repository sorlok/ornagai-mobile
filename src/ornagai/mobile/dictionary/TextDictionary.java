
package ornagai.mobile.dictionary;

import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.events.SelectionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Stack;
import java.util.Vector;
import ornagai.mobile.AbstractFile;
import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;
import ornagai.mobile.ProcessAction;

/**
 *
 * @author Seth N. Hetu
 */
public class TextDictionary extends MMDictionary implements ProcessAction {
    //Meta-data
    private AbstractFile dictionaryFile;
    private String format;
    private String tabbing;

    //Load from file
    private Vector wordlist = new Vector(); //DictionaryWord


    //Returns the size of the byte array, ignoring any non-finished UTF-8 characters
    // This follows RFC 3629's recommendations, although it is not strictly compliant.
    int getUTF8Size(byte[] src) {
        for (int i=0; i<src.length; i++) {
            //Handle carefully to avoid the security risk...
            byte curr = src[i];
            if ((((curr>>3)&0xFF)^0x1E)==0) {
                //We can't handle anything outside the BMP
                throw new IllegalArgumentException("Error: can't handle letters outside the BMP");
            } else if ((((curr>>4)&0xFF)^0xE)==0) {
                //Verify the next two bytes, if there's enough
                if (i>=src.length-2)
                    return i;
                else
                    i+=2;
            } else if ((((curr>>5)&0xFF)^0x6)==0) {
                //Verify the next byte
                if (i>=src.length-1)
                    return i;
                else
                    i++;
            }
        }

        //Done
        return src.length;
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
        for (;;) {
            //Copy remaining
            int bufferStart = 0;
            for (int i=bufferRemID; i<buffer.length; i++)
                buffer[bufferStart++] = buffer[bufferRemID];

            //Read, continue?
            try {
                count = wordFile.read(buffer, bufferStart, buffer.length-bufferStart);
            } catch (IOException ex) {
                throw new RuntimeException("Error reading text file: " + ex.toString());
            }
            if (count==-1)
                break;

            //Convert to a UTF-8 string
            bufferRemID = getUTF8Size(buffer);
            try {
                line = new String(buffer, 0, bufferRemID, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("Error: UTF-8 not supported for some reason");
            }

            //Now, process this line
            for (int i=0; i<line.length(); i++) {
                //Handle double newlines
                char c = line.charAt(i) ;
                if (c=='\r' && i<line.length()-1 && line.charAt(i+1)=='\n')
                    i++;

                //Is it a tab or newline?
                if (c=='\t' || c=='\r' || c=='\n') {
                    //Save it
                    wpd[indices[currIndex]] = sb.toString();
                    sb = new StringBuffer();

                    //Increment, reset if newline
                    currIndex = c=='\t' ? currIndex+1 : 0;

                    //Save a new entry?
                    if (currIndex==0) {
                        wordlist.addElement(new DictionaryWord(wpd[WORD_ID], wpd[POS_ID], wpd[DEF_ID]));
                        //System.out.println("Added: " + wpd[WORD_ID] + " (" + wpd[POS_ID] + ")  " + wpd[DEF_ID]);
                    }
                } else {
                    //Just add it
                    sb.append(c);
                }
            }

        }
    }



    //For now
    public void performSearch(String word) throws IOException {}
    public String[] getWordTuple(DictionaryListEntry entry) {return null;}
    public int findWordIDFromEntry(DictionaryListEntry entry) {return 0;}
    public void addDataChangedListener(DataChangedListener arg0) {}
    public void removeDataChangedListener(DataChangedListener arg0) {}
    public void addItem(Object arg0) {}
    public void addSelectionListener(SelectionListener arg0) {}
    public void removeSelectionListener(SelectionListener arg0) {}
    public Object getItemAt(int arg0) {return null;}
    public int getSelectedIndex() {return -1;}
    public void setSelectedIndex(int arg0) {}
    public void removeItem(int arg0) {}
    public int getSize() { return 0; }


    class DictionaryWord {
        String word;
        String pos;
        String definition;
        DictionaryWord(String word, String pos, String definition) {
            this.word = word;
            this.pos = pos;
            this.definition = definition;
        }
    }



    public void freeModel() {
        wordlist.removeAllElements();
    }

}

