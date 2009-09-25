package ornagai.mobile.dictionary;

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
public abstract class MMDictionary implements ProcessAction, ListModel {
    

    //Load a dictionary, return either a binary or a text-based one, depending on
    //  a partial list of its contents.
    public static MMDictionary createDictionary(AbstractFile dictionaryFile) {
        
    }



    //Package-private; we want to load our dictionary using the static method
    MMDictionary() {}
}


