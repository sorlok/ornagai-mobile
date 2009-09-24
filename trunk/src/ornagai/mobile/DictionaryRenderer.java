package ornagai.mobile;

import com.sun.lwuit.*;
import com.sun.lwuit.list.ListCellRenderer;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.UIManager;
import java.util.Hashtable;

/**
 *
 * borrowed from DefaultListCellRenderer, as it doesn't make focusComponent protected
 */
public class DictionaryRenderer extends Label implements ListCellRenderer {
    public static class DictionaryListEntry {
        public String word;
        public int id; //-1 means "not valid", -2 means "must search"
        public boolean isMatchedResult;

        public DictionaryListEntry() {}

        public DictionaryListEntry(String word, int wordID, boolean isMatchedResult) {
            this.word = word;
            this.id = wordID;
            this.isMatchedResult = isMatchedResult;
        }

        public int compareTo(DictionaryListEntry other) {
            return this.compareTo(other.word);
        }
        public int compareTo(String otherWord) {
            return this.word.toLowerCase().compareTo(otherWord.toLowerCase());
        }
    }

    private Label focusComponent = new Label();
    private int matchBGColor;
    private int normalBGColor;

    //Our main implementation method
    public Component getListCellRendererComponent(List list, Object item, int index, boolean isSelected) {
        //Get basic message
        setFocus(isSelected);
        if(item == null) {
            setText("null");
            return this;
        }

        //Convert
        DictionaryListEntry entry = (DictionaryListEntry)item;
        setText(entry.word);

        //No margins
        this.getStyle().setMargin(0, 0, 0, 0);
        this.getStyle().setPadding(3, 2, 5, 5);

        //Color this
        if (entry.isMatchedResult)
            this.getStyle().setBgColor(matchBGColor);
        else
            this.getStyle().setBgColor(normalBGColor);
        
        return this;
    }


    public DictionaryRenderer(int matchBGColor, int normalBGColor) {
        super("");
        setCellRenderer(true);
        setEndsWith3Points(false);
        focusComponent.setFocus(true);

        //Save
        this.matchBGColor = matchBGColor;
        this.normalBGColor = normalBGColor;
    }

    public void refreshTheme() {
        super.refreshTheme();
        focusComponent.refreshTheme();
    }

    public Component getListFocusComponent(List list) {
        return focusComponent;
    }

    //Do nothing; avoid meaningless repainting
    public void repaint() {
    }

    public int getSelectionTransparency() {
        return focusComponent.getStyle().getBgTransparency() & 0xff;
    }

    public void setSelectionTransparency(int selectionTransparency) {
        focusComponent.getStyle().setBgTransparency(selectionTransparency);
    }
}


