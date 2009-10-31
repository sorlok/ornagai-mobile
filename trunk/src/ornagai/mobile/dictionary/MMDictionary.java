/*
 * This code is licensed under the terms of the MIT License.
 * Please see the file LICENSE.TXT for the full license text.
 */

package ornagai.mobile.dictionary;

import java.io.*;
import ornagai.mobile.io.AbstractFile;
import ornagai.mobile.*;
import com.sun.lwuit.list.ListModel;
import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;


/**
 * Basic abstract definition of what services a dictionary should provide,
 *  regardless of the format.
 * Also, includes a means of generating the proper dictionary from its
 *  AbstractFile. New dictionary formats should add their detection and
 *  initialization code here.
 *
 * @author Seth N. Hetu
 */
public abstract class MMDictionary implements ListModel {
    public static final String FORMAT_ZG2008 = "zg2008";
    public static final String FORMAT_ZG2009 = "zg2009";

    private static final String[] allowedFormats = new String[]{FORMAT_ZG2008, FORMAT_ZG2009};
    private static final String[] allowedTabbings = new String[]{"wpd", "wdp", "pwd", "pdw", "dpw", "dwp"};
    

    //Load a dictionary, return either a binary or a text-based one, depending on
    //  a partial list of its contents.
    public static MMDictionary createDictionary(AbstractFile dictionaryFile) {
        if (!MZMobileDictionary.debug_neither_text_nor_binary) {
            //Check for binary format first
            for (int fmtID=0; fmtID<allowedFormats.length; fmtID++) {
                String format = allowedFormats[fmtID];
                if (dictionaryFile.exists("word_list-" + format + ".bin"))
                    return new BinaryDictionary(dictionaryFile, format);
            }

            //Now, check for all text foramts
            for (int tabID=0; tabID<allowedTabbings.length; tabID++) {
                String tabbing = allowedTabbings[tabID];
                for (int fmtID=0; fmtID<allowedFormats.length; fmtID++) {
                    String format = allowedFormats[fmtID];
                    if (dictionaryFile.exists("words-tab" + tabbing + "-" + format + ".txt"))
                        return new TextDictionary(dictionaryFile, format, tabbing);
                }
            }
        }

        //Otherwise, we are in error
        throw new IllegalArgumentException("Dictionary file is neither text nor binary.");
    }

    //Package-private; we want to load our dictionary using the static method
    MMDictionary() {}


    //Needed as a fix for Nokia 6500
    public Object getItemAt(int arg0) {
        return null;
    }



    //Promised interface
    public abstract void performSearch(String word) throws IOException;
    public abstract String[] getWordTuple(DictionaryListEntry entry);
    public abstract int findWordIDFromEntry(DictionaryListEntry entry, int skipNSimilarWords);
    public abstract void loadLookupTree();
    public abstract String getFormat();
    public abstract void freeMostData();
}


