/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ornagai.mobile.dictionary;

import java.io.IOException;
import ornagai.mobile.AbstractFile;
import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;

/**
 *
 * @author Seth N. Hetu
 */
public class TextDictionary extends MMDictionary {


    //Package-private
    TextDictionary(AbstractFile dictionaryFile, String format, String tabbing) {
    }


    //For now
    public void performSearch(String word) throws IOException {}
    public String[] getWordTuple(DictionaryListEntry entry) {return null;}
    public int findWordIDFromEntry(DictionaryListEntry entry) {return 0;}
    public void loadLookupTree(){}



}
