
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

import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;
import ornagai.mobile.EventDispatcher;

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
                        int nextID = wordlist.size();
                        wordlist.addElement(new DictionaryWord(wpd[WORD_ID], wpd[POS_ID], wpd[DEF_ID], nextID, false));
                    }
                } else {
                    //Just add it
                    sb.append(c);
                }
            }

        }
    }



    public void performSearch(String word) throws IOException {
        //TODO
        throw new RuntimeException("Not implemented yet: search");





    }



    public Object getItemAt(int id) {
        DictionaryWord item = (DictionaryWord)wordlist.elementAt(id);
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
        return wordlist.size();
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

    //Not supported
    public void addItem(Object arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"addItem()\"");
    }
    public void removeItem(int arg0) {
        throw new UnsupportedOperationException("MMDictionary does not support \"removeItem()\"");
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

