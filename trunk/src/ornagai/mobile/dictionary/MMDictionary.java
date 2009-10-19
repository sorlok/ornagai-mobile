package ornagai.mobile.dictionary;

import ornagai.mobile.io.AbstractFile;
import ornagai.mobile.*;
import com.sun.lwuit.List;
import com.sun.lwuit.events.DataChangedListener;
import com.sun.lwuit.events.SelectionListener;
import com.sun.lwuit.list.ListModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;

import ornagai.mobile.DictionaryRenderer.DictionaryListEntry;


/**
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

        //Otherwise, we are in error
        throw new IllegalArgumentException("Dictionary file is neither text nor binary.");
    }

    //Package-private; we want to load our dictionary using the static method
    MMDictionary() {}


    //Promised interface
    public abstract void performSearch(String word) throws IOException;
    public abstract String[] getWordTuple(DictionaryListEntry entry);
    public abstract int findWordIDFromEntry(DictionaryListEntry entry, int skipNSimilarWords);
    public abstract void loadLookupTree();
    public abstract String getFormat();
    public abstract void freeMostData();
}


