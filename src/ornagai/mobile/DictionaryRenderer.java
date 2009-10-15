package ornagai.mobile;

import com.sun.lwuit.*;
import com.sun.lwuit.layouts.BorderLayout;
import com.sun.lwuit.list.ListCellRenderer;
import com.sun.lwuit.plaf.Border;
import com.sun.lwuit.plaf.UIManager;
import java.util.Hashtable;

/**
 *
 * borrowed from DefaultListCellRenderer, as it doesn't make focusComponent protected
 */
public class DictionaryRenderer extends Container implements ListCellRenderer {
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

    private Label mainLabel = new Label("");
    private Label focus = new Label("");

    private int matchBGColor;
    private int normalBGColor;

    //Our main implementation method
    public Component getListCellRendererComponent(List list, Object item, int index, boolean isSelected) {
        //Get basic message
        setFocus(isSelected);
        if(item == null) {
            mainLabel.setText("null");
            return this;
        }

        //Convert
        DictionaryListEntry entry = (DictionaryListEntry)item;
        mainLabel.setText(entry.word);

        //Color this
        if (entry.isMatchedResult)
            this.getStyle().setBgColor(matchBGColor);
        else
            this.getStyle().setBgColor(normalBGColor);
        
        return this;
    }


    public DictionaryRenderer(int matchBGColor, int normalBGColor) {
        setLayout(new BorderLayout());
        addComponent(BorderLayout.CENTER, mainLabel);

        this.getStyle().setBgTransparency(0xCC);
        //this.getStyle().setBorder(Border.createEmpty());

        mainLabel.getStyle().setBorder(Border.createEmpty());
        mainLabel.getStyle().setBgTransparency(0);

        //focus.getStyle().setFgColor(0x0000FF);
        //focus.getStyle().setFgSelectionColor(0x0000FF);
        //focus.getStyle().setBorder(Border.createLineBorder(2, 0x0000FF));
        focus.getStyle().setBgTransparency(0xFF);
        focus.getStyle().setBgColor(0x0000FF);

        //No margins
        this.getStyle().setMargin(0, 0, 0, 0);
        this.getStyle().setPadding(0, 0, 0, 0);
        mainLabel.getStyle().setPadding(2, 3, 5, 5);
        mainLabel.getStyle().setMargin(0, 0, 0, 0);

        //Save
        this.matchBGColor = matchBGColor;
        this.normalBGColor = normalBGColor;
    }

    public Component getListFocusComponent(List list) {
        return focus;
    }

    //Do nothing; avoid meaningless repainting
    public void repaint() {
    }
}


