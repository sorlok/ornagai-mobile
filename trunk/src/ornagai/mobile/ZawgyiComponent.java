package ornagai.mobile;


import com.sun.lwuit.Component;
import com.sun.lwuit.animations.Motion;
import com.sun.lwuit.geom.Dimension;
import com.sun.lwuit.plaf.Style;
import com.sun.lwuit.Display;
import com.sun.lwuit.Font;
import com.sun.lwuit.Graphics;
import com.sun.lwuit.Image;
import com.sun.lwuit.plaf.Border;
import com.waitzar.analysis.segment.FormatConverter;
import com.waitzar.analysis.segment.WZSegment;
import java.io.IOException;
import java.util.Vector;
import ornagai.mobile.dictionary.MMDictionary;

/**
 * A component that allows us to drag an image file with a physical drag motion
 * effect.
 *
 * @author Thar Htet based on Shai Almog's Code @ http://lwuit.blogspot.com/2008/07/motion-madness-physics-gone-wild.html
 */
public class ZawgyiComponent extends Component {

    private Image fontMapImage;
    private Image textDisplay;
    private java.util.Hashtable fontMap;
    private int lineHeight;
    private int lineBase;
    private int positionX;
    private int positionY;
    private Motion motionX;
    private Motion motionY;
    private int destX;
    private int destY;
    private static final int TIME = 800;
    private static final int DISTANCE_X = Display.getInstance().getDisplayWidth() / 4;
    private static final int DISTANCE_Y = Display.getInstance().getDisplayHeight() / 4;
    private int dragBeginX = -1;
    private int dragBeginY = -1;
    private int dragCount = 0;

    //Segmented text, for display
    private Vector linesOfText = new Vector();
    private String toSegmentTxt;
    private boolean formatAsDictionaryEntry;

    //Fonts
    private Font boldFont;
    private Font italicFont;
    private Font normalFont;

    public ZawgyiComponent() {
        this.init_font_map();

        Style defaultStyle = new Style();
        defaultStyle.setBgColor(0xf0f0f0);
        defaultStyle.setBgSelectionColor(0xf0f0f0);
        defaultStyle.setBorder(Border.createRoundBorder(5, 5));
        this.setStyle(defaultStyle);
    }

    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        //return new Dimension(textDisplay.getWidth() + s.getPadding(LEFT) + s.getPadding(RIGHT),
        //    textDisplay.getHeight() + s.getPadding(TOP) + s.getPadding(BOTTOM));
        return new Dimension(Display.getInstance().getDisplayWidth(),
                textDisplay.getHeight() + s.getPadding(TOP) + s.getPadding(BOTTOM));
    }

    public void initComponent() {
        getComponentForm().registerAnimated(this);
    }

    private boolean hasMM(String str) {
        for (int i=0; i<str.length(); i++) {
            if (str.charAt(i)>=0x1000 && str.charAt(i)<=0x109F)
                return true;
        }
        return false;
    }

    public void paint(Graphics g) {
        Style s = getStyle();
        readyForNewText();

        if (boldFont==null)
            boldFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
        if (normalFont==null)
            normalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        if (italicFont==null)
            italicFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);

        textDisplay = Image.createImage(Display.getInstance().getDisplayWidth(),
                predictHeight(this.getText(), this.getWidth() - s.getPadding(LEFT)));//Default 20 lines display

        //Segment
        if (this.toSegmentTxt!=null) {
            segmentAndAdd(linesOfText, toSegmentTxt);
            toSegmentTxt = null;
        }

        //Layout all strings
        Graphics gTxt = textDisplay.getGraphics();
        int xAcc = s.getPadding(Component.LEFT);
        int yAcc = s.getPadding(Component.TOP);
        int margin = 2;
        int padding = textDisplay.getWidth() - (s.getPadding(Component.LEFT) + s.getPadding(Component.RIGHT));
        padding -= 10;

        for (int i=0; i<linesOfText.size(); i++) {
            String currLine = (String)linesOfText.elementAt(i);
            if (!formatAsDictionaryEntry || i>=2) {
                //Just draw it
                drawZGString(gTxt, currLine, xAcc, yAcc, padding);
                yAcc += fontMapImage.getHeight();
            } else {
                //Draw one of the first two lines. Use the native font, if applicable.
                if (i==0) {
                    //Word. Bold
                    if (hasMM(currLine)) {
                        //Draw twice, faux bold
                        drawZGString(gTxt, currLine, xAcc, yAcc, padding);
                        drawZGString(gTxt, currLine, xAcc, yAcc, padding);
                        yAcc += fontMapImage.getHeight();
                    } else {
                        gTxt.setFont(boldFont);
                        gTxt.drawString(currLine, 0, yAcc);
                        yAcc += gTxt.getFont().getHeight();
                    }

                    //Add a margin
                    yAcc += margin;
                } else {
                    //Pos: Italic
                    if (hasMM(currLine)) {
                        //Todo: Oblique
                        drawZGString(gTxt, currLine, xAcc, yAcc, padding);
                        yAcc += fontMapImage.getHeight();
                    } else {
                        gTxt.setFont(boldFont);
                        gTxt.drawString(currLine, 0, yAcc);
                        yAcc += gTxt.getFont().getHeight();
                    }

                    //Add a margin
                    yAcc += (5*margin)/2;
                }
            }
        }

        //Re-draw the display
        g.drawImage(textDisplay, getX() - positionX + s.getPadding(LEFT), getY() - positionY + s.getPadding(TOP));
    }

    public void setText(String s, String formatString) {
        //Reset
        this.formatAsDictionaryEntry = false;
        this.linesOfText.removeAllElements();

        //Convert?
        if (formatString.equals(MMDictionary.FORMAT_ZG2009))
            s = FormatConverter.DowngradeZawgyi2009(s);

        //Set and word break
        this.toSegmentTxt = s;
    }

    public void setTextToDictionaryEntry(String word, String pos, String definition, String formatString) {
        //Reset
        this.formatAsDictionaryEntry = true;
        this.linesOfText.removeAllElements();

        //Convert?
        if (formatString.equals(MMDictionary.FORMAT_ZG2009)) {
            word = FormatConverter.DowngradeZawgyi2009(word);
            pos = FormatConverter.DowngradeZawgyi2009(pos);
            definition = FormatConverter.DowngradeZawgyi2009(definition);
        }

        //Set
        this.linesOfText.addElement(word);
        this.linesOfText.addElement(pos);

        //Word break the definition line, later
        this.toSegmentTxt = definition;
    }

    private void segmentAndAdd(Vector arr, String word) {
        int lineWidth = textDisplay.getWidth() - (getStyle().getPadding(Component.LEFT) + getStyle().getPadding(Component.RIGHT));
        StringBuffer currLine = new StringBuffer();
        Vector segments = WZSegment.SegmentText(word);
        for (int i=0; i<segments.size(); i++) {
            String seg = (String)segments.elementAt(i);
            int testLength = getStringWidth(currLine.toString() + seg);

            //Append, and avoid looping forever on empty lines.
            if (testLength<=lineWidth || currLine.length()==0)
                currLine.append(seg);

            //Break?
            if (testLength>lineWidth || i==segments.size()-1) {
                arr.addElement(currLine.toString());
                currLine.delete(0, currLine.length());
            }
        }
    }

    private int getStringWidth(String str) {
        //Simple case
        if (str==null)
            return 0;

        //Add each letter
        int nextCharX = 0;
        for (int i=0; i<str.length(); i++) {
            //Get its index. Use "?" if it's not in the font map
            char c = str.charAt(i);
            if (!fontMap.containsKey(new Integer(c)))
                c = (int)'?';
            if (c=='\r' || c=='\n')
                continue;

            //Advance x
            int[] finfo = (int[]) fontMap.get(new Integer(c));
            int x_advance = finfo[6];
            nextCharX += x_advance;
        }

        //Done; nextCharX is the width
        return nextCharX;
    }

    public String getText() {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<linesOfText.size(); i++)
            sb.append(linesOfText.elementAt(i).toString() + "\n");
        return sb.toString();
    }

    public void keyPressed(int keyCode) {
        //super.keyPressed(keyCode);
        switch (Display.getInstance().getGameAction(keyCode)) {
            case Display.GAME_DOWN:
                destY = Math.min(destY + DISTANCE_Y, textDisplay.getHeight() - Display.getInstance().getDisplayHeight());
                motionY = Motion.createLinearMotion(positionY, destY, TIME);
                motionY.start();
                break;
            case Display.GAME_UP:
                destY = Math.max(destY - DISTANCE_Y, 0);
                motionY = Motion.createSplineMotion(positionY, destY, TIME);
                motionY.start();
                break;
            case Display.GAME_LEFT:
                //destX = Math.max(destX - DISTANCE_X, 0);
                //motionX = Motion.createSplineMotion(positionX, destX, TIME);
                //motionX.start();
                break;
            case Display.GAME_RIGHT:
                //destX = Math.min(destX + DISTANCE_X, textDisplay.getWidth() - Display.getInstance().getDisplayWidth());
                //motionX = Motion.createSplineMotion(positionX, destX, TIME);
                //motionX.start();
                break;
            default:
                return;
        }
    }

    public void pointerDragged(int x, int y) {
        if (dragBeginX == -1) {
            dragBeginX = x;
            dragBeginY = y;
        }
        positionX = Math.max(0, Math.min(positionX + x - dragBeginX, textDisplay.getWidth() - Display.getInstance().getDisplayWidth()));
        positionY = Math.max(0, Math.min(positionY + y - dragBeginY, textDisplay.getHeight() - Display.getInstance().getDisplayHeight()));
        dragCount++;
    }

    public void pointerReleased(int x, int y) {
        // this is a result of a more significant drag operation, some VM's always
        // send a pointerDragged so we should ignore too few drag events
        if (dragCount > 4) {
            float velocity = -0.2f;
            if (dragBeginX < x) {
                velocity = 0.2f;
            }
            motionX = Motion.createFrictionMotion(positionX, velocity, 0.0004f);
            motionX.start();

            if (dragBeginY < y) {
                velocity = 0.2f;
            } else {
                velocity = -0.2f;
            }
            motionY = Motion.createFrictionMotion(positionY, velocity, 0.0004f);
            motionY.start();
        }
        dragCount = 0;
        dragBeginX = -1;
        dragBeginY = -1;
    }

    public boolean animate() {
        boolean val = false;
        if (motionX != null) {
            positionX = motionX.getValue();
            if (motionX.isFinished()) {
                motionX = null;
            }
            // velocity might exceed image bounds
            positionX = Math.max(0, Math.min(positionX, textDisplay.getWidth() - Display.getInstance().getDisplayWidth()));
            val = true;
        }
        if (motionY != null) {
            positionY = motionY.getValue();
            if (motionY.isFinished()) {
                motionY = null;
            }
            positionY = Math.max(0, Math.min(positionY, textDisplay.getHeight() - Display.getInstance().getDisplayHeight()));
            val = true;
        }
        return val;
    }

    public void drawZGChar(
            Graphics g,
            int f_x, int f_y, int f_width, int f_height, int x_offset,
            int y_offset, int x_advance, int[] drawPos) {

        //g.drawRect(0, 0, 10, 10);

        //FORMULA
        // g.setClip(
        // drawPos[0] + XOffset,
        // total_prev_line_height  + (lineheight - base) + YOffset,
        // width, height);
        // g.drawImage(textDisplay,
        // (-1 * x) + drawPos[0] + XOffset,
        // (-1 * y) + total_prev_line_height + (lineheight - base) + YOffset,
        // Graphics.TOP | Graphics.LEFT);
        // drawPos[0] += XAdvance;

        g.setClip(
                drawPos[0] + x_offset,
                drawPos[1] + (lineHeight - lineBase) + y_offset,
                f_width, f_height);
        g.drawImage(fontMapImage,
                (-1 * f_x) + drawPos[0] + x_offset,
                (-1 * f_y) + drawPos[1] + (lineHeight - lineBase) + y_offset);

        drawPos[0] += x_advance;
    }

    public void readyForNewText() {
        textDisplay = null;
        System.gc();
    }

    public void drawZGString(Graphics g, String sTxt, int x, int y, int canvas_margin) {
        // get the strings length
        int len = sTxt.length();

        // if nothing to draw return
        if (len == 0) {
            return;
        }

        //Drawing locations
        int[] drawPos = new int[]{x, y};

        // loop through all the characters in the string
        for (int i = 0; i < len; i++) {

            // get current character
            char c = sTxt.charAt(i);

            // get ordinal value or ASCII equivalent
            int cIndex = (int) c;
            if (!fontMap.containsKey(new Integer(cIndex)))
                cIndex = (int)'?';

            if (cIndex != 10 && cIndex != 13) {
                try {
                    // lookup the width of the character
                    int[] finfo = (int[]) fontMap.get(new Integer(cIndex));
                    int f_x = finfo[0];
                    int f_y = finfo[1];
                    int f_width = finfo[2];
                    int f_height = finfo[3];
                    int x_offset = finfo[4];
                    int y_offset = finfo[5];
                    int x_advance = finfo[6];

                    //Text is pre-processed; no need to wrap at runtime.
                    /*if ((drawPos[0] + f_width) > canvas_margin) {
                        drawPos[1] += lineHeight;
                        drawPos[0] = x;
                    }*/
                    // draw the character
                    drawZGChar(g, f_x, f_y, f_width, f_height, x_offset, y_offset, x_advance, drawPos);
                } catch (Exception ex) {
                    //Ingore Invalid Character
                }
            } else {
                //new Line feed
                drawPos[1] += lineHeight;
                drawPos[0] = x;
            }
        }
    }

    public int predictHeight(String sTxt, int canvas_margin) {
        int theHeight = 0;
        int theWidth = 0;

        // get the strings length
        int len = sTxt.length();

        // if nothing to draw return
        if (len == 0) {
            return 0;
        }

        // loop through all the characters in the string
        for (int i = 0; i < len; i++) {
            // get current character
            char c = sTxt.charAt(i);

            // get ordinal value or ASCII equivalent
            int cIndex = (int) c;
            if (!fontMap.containsKey(new Integer(cIndex)))
                cIndex = (int)'?';

            // If not LF(10) and CR(13)
            if (cIndex != 10 && cIndex != 13) {
                // lookup the width of the character
                try {
                    int[] finfo = (int[]) fontMap.get(new Integer(cIndex));
                    int x_advance = finfo[6];

                    theWidth += x_advance;
                    if ((theWidth + x_advance) > canvas_margin) {
                        theHeight += lineHeight;
                        theWidth = 0;
                    }
                } catch (Exception ex) {
                    //Ignore invalid characters
                }
            } else {
                theHeight += lineHeight;
                theWidth = 0;
            }
        }
        return theHeight + (lineHeight * 5);
    }

    public void init_font_map() {
        fontMap = new java.util.Hashtable();
        lineHeight = 22;
        lineBase = 16;
        try {
            fontMapImage = Image.createImage(this.getClass().getResourceAsStream("/ZawGyiBitMap_00.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        int[] tempArray_0 = {136, 36, 11, 11, 2, 5, 14};
        fontMap.put(new Integer(-1), tempArray_0);
        int[] tempArray_1 = {245, 55, 1, 1, 0, 16, 4};
        fontMap.put(new Integer(32), tempArray_1);
        int[] tempArray_2 = {254, 41, 1, 10, 1, 6, 4};
        fontMap.put(new Integer(33), tempArray_2);
        int[] tempArray_3 = {232, 85, 4, 4, 1, 5, 6};
        fontMap.put(new Integer(34), tempArray_3);
        int[] tempArray_4 = {95, 50, 8, 10, 1, 6, 10};
        fontMap.put(new Integer(35), tempArray_4);
        int[] tempArray_5 = {90, 36, 6, 13, 1, 5, 8};
        fontMap.put(new Integer(36), tempArray_5);
        int[] tempArray_6 = {236, 30, 12, 10, 1, 6, 14};
        fontMap.put(new Integer(37), tempArray_6);
        int[] tempArray_7 = {20, 51, 9, 10, 0, 6, 9};
        fontMap.put(new Integer(38), tempArray_7);
        int[] tempArray_8 = {223, 15, 1, 4, 1, 5, 3};
        fontMap.put(new Integer(39), tempArray_8);
        int[] tempArray_9 = {61, 36, 4, 14, 1, 5, 5};
        fontMap.put(new Integer(40), tempArray_9);
        int[] tempArray_10 = {66, 36, 4, 14, 0, 5, 5};
        fontMap.put(new Integer(41), tempArray_10);
        int[] tempArray_11 = {225, 77, 7, 7, 0, 5, 8};
        fontMap.put(new Integer(42), tempArray_11);
        int[] tempArray_12 = {217, 77, 7, 7, 1, 8, 10};
        fontMap.put(new Integer(43), tempArray_12);
        int[] tempArray_13 = {236, 41, 3, 4, 1, 14, 4};
        fontMap.put(new Integer(44), tempArray_13);
        int[] tempArray_14 = {79, 96, 4, 1, 0, 11, 5};
        fontMap.put(new Integer(45), tempArray_14);
        int[] tempArray_15 = {245, 52, 1, 2, 1, 14, 4};
        fontMap.put(new Integer(46), tempArray_15);
        int[] tempArray_16 = {103, 36, 5, 13, 0, 5, 5};
        fontMap.put(new Integer(47), tempArray_16);
        int[] tempArray_17 = {161, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(48), tempArray_17);
        int[] tempArray_18 = {33, 62, 5, 10, 2, 6, 8};
        fontMap.put(new Integer(49), tempArray_18);
        int[] tempArray_19 = {14, 62, 6, 10, 1, 6, 8};
        fontMap.put(new Integer(50), tempArray_19);
        int[] tempArray_20 = {0, 62, 6, 10, 1, 6, 8};
        fontMap.put(new Integer(51), tempArray_20);
        int[] tempArray_21 = {169, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(52), tempArray_21);
        int[] tempArray_22 = {249, 30, 6, 10, 1, 6, 8};
        fontMap.put(new Integer(53), tempArray_22);
        int[] tempArray_23 = {177, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(54), tempArray_23);
        int[] tempArray_24 = {7, 62, 6, 10, 1, 6, 8};
        fontMap.put(new Integer(55), tempArray_24);
        int[] tempArray_25 = {185, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(56), tempArray_25);
        int[] tempArray_26 = {193, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(57), tempArray_26);
        int[] tempArray_27 = {44, 82, 1, 8, 2, 8, 5};
        fontMap.put(new Integer(58), tempArray_27);
        int[] tempArray_28 = {43, 62, 3, 10, 1, 8, 5};
        fontMap.put(new Integer(59), tempArray_28);
        int[] tempArray_29 = {153, 78, 7, 7, 1, 8, 10};
        fontMap.put(new Integer(60), tempArray_29);
        int[] tempArray_30 = {223, 85, 8, 4, 1, 10, 10};
        fontMap.put(new Integer(61), tempArray_30);
        int[] tempArray_31 = {201, 77, 7, 7, 1, 8, 10};
        fontMap.put(new Integer(62), tempArray_31);
        int[] tempArray_32 = {247, 41, 6, 10, 0, 6, 7};
        fontMap.put(new Integer(63), tempArray_32);
        int[] tempArray_33 = {114, 36, 12, 12, 0, 6, 13};
        fontMap.put(new Integer(64), tempArray_33);
        int[] tempArray_34 = {201, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(65), tempArray_34);
        int[] tempArray_35 = {209, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(66), tempArray_35);
        int[] tempArray_36 = {77, 51, 8, 10, 0, 6, 9};
        fontMap.put(new Integer(67), tempArray_36);
        int[] tempArray_37 = {10, 51, 9, 10, 0, 6, 10};
        fontMap.put(new Integer(68), tempArray_37);
        int[] tempArray_38 = {217, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(69), tempArray_38);
        int[] tempArray_39 = {240, 41, 6, 10, 0, 6, 7};
        fontMap.put(new Integer(70), tempArray_39);
        int[] tempArray_40 = {86, 50, 8, 10, 0, 6, 9};
        fontMap.put(new Integer(71), tempArray_40);
        int[] tempArray_41 = {68, 51, 8, 10, 0, 6, 9};
        fontMap.put(new Integer(72), tempArray_41);
        int[] tempArray_42 = {39, 62, 3, 10, 0, 6, 4};
        fontMap.put(new Integer(73), tempArray_42);
        int[] tempArray_43 = {21, 62, 5, 10, 0, 6, 6};
        fontMap.put(new Integer(74), tempArray_43);
        int[] tempArray_44 = {225, 47, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(75), tempArray_44);
        int[] tempArray_45 = {233, 47, 6, 10, 0, 6, 7};
        fontMap.put(new Integer(76), tempArray_45);
        int[] tempArray_46 = {30, 51, 9, 10, 0, 6, 10};
        fontMap.put(new Integer(77), tempArray_46);
        int[] tempArray_47 = {104, 50, 8, 10, 0, 6, 9};
        fontMap.put(new Integer(78), tempArray_47);
        int[] tempArray_48 = {40, 51, 9, 10, 0, 6, 10};
        fontMap.put(new Integer(79), tempArray_48);
        int[] tempArray_49 = {113, 50, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(80), tempArray_49);
        int[] tempArray_50 = {80, 36, 9, 13, 0, 6, 10};
        fontMap.put(new Integer(81), tempArray_50);
        int[] tempArray_51 = {50, 51, 8, 10, 0, 6, 9};
        fontMap.put(new Integer(82), tempArray_51);
        int[] tempArray_52 = {121, 49, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(83), tempArray_52);
        int[] tempArray_53 = {0, 51, 9, 10, -1, 6, 8};
        fontMap.put(new Integer(84), tempArray_53);
        int[] tempArray_54 = {59, 51, 8, 10, 0, 6, 9};
        fontMap.put(new Integer(85), tempArray_54);
        int[] tempArray_55 = {129, 49, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(86), tempArray_55);
        int[] tempArray_56 = {222, 36, 13, 10, 0, 6, 14};
        fontMap.put(new Integer(87), tempArray_56);
        int[] tempArray_57 = {153, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(88), tempArray_57);
        int[] tempArray_58 = {145, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(89), tempArray_58);
        int[] tempArray_59 = {137, 48, 7, 10, 0, 6, 8};
        fontMap.put(new Integer(90), tempArray_59);
        int[] tempArray_60 = {71, 36, 4, 14, 0, 5, 5};
        fontMap.put(new Integer(91), tempArray_60);
        int[] tempArray_61 = {97, 36, 5, 13, 0, 5, 5};
        fontMap.put(new Integer(92), tempArray_61);
        int[] tempArray_62 = {56, 36, 4, 14, 0, 5, 5};
        fontMap.put(new Integer(93), tempArray_62);
        int[] tempArray_63 = {226, 30, 8, 5, 1, 6, 10};
        fontMap.put(new Integer(94), tempArray_63);
        int[] tempArray_64 = {70, 96, 8, 1, 0, 17, 8};
        fontMap.put(new Integer(95), tempArray_64);
        int[] tempArray_65 = {66, 96, 3, 3, 2, 4, 8};
        fontMap.put(new Integer(96), tempArray_65);
        int[] tempArray_66 = {7, 82, 6, 8, 0, 8, 7};
        fontMap.put(new Integer(97), tempArray_66);
        int[] tempArray_67 = {174, 36, 7, 11, 0, 5, 8};
        fontMap.put(new Integer(98), tempArray_67);
        int[] tempArray_68 = {0, 82, 6, 8, 0, 8, 7};
        fontMap.put(new Integer(99), tempArray_68);
        int[] tempArray_69 = {182, 36, 7, 11, 0, 5, 8};
        fontMap.put(new Integer(100), tempArray_69);
        int[] tempArray_70 = {14, 82, 6, 8, 0, 8, 7};
        fontMap.put(new Integer(101), tempArray_70);
        int[] tempArray_71 = {214, 36, 5, 11, 0, 5, 4};
        fontMap.put(new Integer(102), tempArray_71);
        int[] tempArray_72 = {198, 36, 7, 11, 0, 8, 8};
        fontMap.put(new Integer(103), tempArray_72);
        int[] tempArray_73 = {206, 36, 7, 11, 0, 5, 8};
        fontMap.put(new Integer(104), tempArray_73);
        int[] tempArray_74 = {47, 62, 1, 10, 0, 6, 2};
        fontMap.put(new Integer(105), tempArray_74);
        int[] tempArray_75 = {109, 36, 4, 13, -1, 6, 4};
        fontMap.put(new Integer(106), tempArray_75);
        int[] tempArray_76 = {166, 36, 7, 11, 0, 5, 7};
        fontMap.put(new Integer(107), tempArray_76);
        int[] tempArray_77 = {220, 36, 1, 11, 0, 5, 2};
        fontMap.put(new Integer(108), tempArray_77);
        int[] tempArray_78 = {15, 73, 9, 8, 1, 8, 12};
        fontMap.put(new Integer(109), tempArray_78);
        int[] tempArray_79 = {234, 67, 7, 8, 0, 8, 8};
        fontMap.put(new Integer(110), tempArray_79);
        int[] tempArray_80 = {242, 67, 7, 8, 0, 8, 8};
        fontMap.put(new Integer(111), tempArray_80);
        int[] tempArray_81 = {158, 36, 7, 11, 0, 8, 8};
        fontMap.put(new Integer(112), tempArray_81);
        int[] tempArray_82 = {190, 36, 7, 11, 0, 8, 8};
        fontMap.put(new Integer(113), tempArray_82);
        int[] tempArray_83 = {32, 82, 4, 8, 0, 8, 5};
        fontMap.put(new Integer(114), tempArray_83);
        int[] tempArray_84 = {250, 61, 5, 8, 0, 8, 6};
        fontMap.put(new Integer(115), tempArray_84);
        int[] tempArray_85 = {27, 62, 5, 10, 0, 6, 5};
        fontMap.put(new Integer(116), tempArray_85);
        int[] tempArray_86 = {226, 68, 7, 8, 0, 8, 8};
        fontMap.put(new Integer(117), tempArray_86);
        int[] tempArray_87 = {35, 73, 9, 8, -1, 8, 8};
        fontMap.put(new Integer(118), tempArray_87);
        int[] tempArray_88 = {25, 73, 9, 8, 0, 8, 10};
        fontMap.put(new Integer(119), tempArray_88);
        int[] tempArray_89 = {248, 52, 7, 8, 0, 8, 8};
        fontMap.put(new Integer(120), tempArray_89);
        int[] tempArray_90 = {148, 36, 9, 11, -1, 8, 8};
        fontMap.put(new Integer(121), tempArray_90);
        int[] tempArray_91 = {21, 82, 5, 8, 0, 8, 6};
        fontMap.put(new Integer(122), tempArray_91);
        int[] tempArray_92 = {44, 36, 5, 14, 0, 5, 7};
        fontMap.put(new Integer(123), tempArray_92);
        int[] tempArray_93 = {253, 15, 1, 14, 2, 5, 5};
        fontMap.put(new Integer(124), tempArray_93);
        int[] tempArray_94 = {26, 36, 5, 14, 1, 5, 7};
        fontMap.put(new Integer(125), tempArray_94);
        int[] tempArray_95 = {205, 85, 8, 4, 1, 9, 10};
        fontMap.put(new Integer(126), tempArray_95);
        int[] tempArray_96 = {65, 62, 15, 9, 0, 8, 15};
        fontMap.put(new Integer(4096), tempArray_96);
        int[] tempArray_97 = {163, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4097), tempArray_97);
        int[] tempArray_98 = {172, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4098), tempArray_98);
        int[] tempArray_99 = {232, 58, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4099), tempArray_99);
        int[] tempArray_100 = {190, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4100), tempArray_100);
        int[] tempArray_101 = {199, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4101), tempArray_101);
        int[] tempArray_102 = {200, 59, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4102), tempArray_102);
        int[] tempArray_103 = {73, 72, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4103), tempArray_103);
        int[] tempArray_104 = {61, 21, 11, 14, 0, 8, 9};
        fontMap.put(new Integer(4104), tempArray_104);
        int[] tempArray_105 = {16, 21, 15, 14, 0, 8, 8};
        fontMap.put(new Integer(4105), tempArray_105);
        int[] tempArray_106 = {32, 21, 15, 14, 0, 8, 15};
        fontMap.put(new Integer(4106), tempArray_106);
        int[] tempArray_107 = {144, 21, 9, 14, 0, 8, 8};
        fontMap.put(new Integer(4107), tempArray_107);
        int[] tempArray_108 = {217, 21, 8, 14, 0, 8, 8};
        fontMap.put(new Integer(4108), tempArray_108);
        int[] tempArray_109 = {104, 21, 9, 14, 0, 8, 8};
        fontMap.put(new Integer(4109), tempArray_109);
        int[] tempArray_110 = {55, 72, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4110), tempArray_110);
        int[] tempArray_111 = {103, 61, 16, 8, 0, 8, 17};
        fontMap.put(new Integer(4111), tempArray_111);
        int[] tempArray_112 = {216, 59, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4112), tempArray_112);
        int[] tempArray_113 = {136, 60, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4113), tempArray_113);
        int[] tempArray_114 = {208, 68, 8, 8, 0, 8, 8};
        fontMap.put(new Integer(4114), tempArray_114);
        int[] tempArray_115 = {145, 69, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4115), tempArray_115);
        int[] tempArray_116 = {124, 21, 9, 14, -1, 8, 9};
        fontMap.put(new Integer(4116), tempArray_116);
        int[] tempArray_117 = {100, 71, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4117), tempArray_117);
        int[] tempArray_118 = {91, 71, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4118), tempArray_118);
        int[] tempArray_119 = {82, 71, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4119), tempArray_119);
        int[] tempArray_120 = {168, 59, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4120), tempArray_120);
        int[] tempArray_121 = {109, 70, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4121), tempArray_121);
        int[] tempArray_122 = {184, 59, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4122), tempArray_122);
        int[] tempArray_123 = {134, 21, 9, 14, 0, 8, 9};
        fontMap.put(new Integer(4123), tempArray_123);
        int[] tempArray_124 = {49, 62, 15, 9, 0, 8, 15};
        fontMap.put(new Integer(4124), tempArray_124);
        int[] tempArray_125 = {136, 69, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4125), tempArray_125);
        int[] tempArray_126 = {120, 61, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4126), tempArray_126);
        int[] tempArray_127 = {152, 59, 15, 8, 0, 8, 15};
        fontMap.put(new Integer(4127), tempArray_127);
        int[] tempArray_128 = {199, 21, 8, 14, 0, 8, 8};
        fontMap.put(new Integer(4128), tempArray_128);
        int[] tempArray_129 = {0, 73, 14, 8, 0, 8, 14};
        fontMap.put(new Integer(4129), tempArray_129);
        int[] tempArray_130 = {240, 0, 15, 14, 0, 8, 15};
        fontMap.put(new Integer(4131), tempArray_130);
        int[] tempArray_131 = {125, 0, 17, 20, 0, 2, 17};
        fontMap.put(new Integer(4132), tempArray_131);
        int[] tempArray_132 = {190, 21, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4133), tempArray_132);
        int[] tempArray_133 = {199, 0, 8, 20, 0, 2, 9};
        fontMap.put(new Integer(4134), tempArray_133);
        int[] tempArray_134 = {127, 70, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4135), tempArray_134);
        int[] tempArray_135 = {71, 0, 17, 20, 0, 2, 17};
        fontMap.put(new Integer(4137), tempArray_135);
        int[] tempArray_136 = {0, 0, 32, 20, 0, 2, 33};
        fontMap.put(new Integer(4138), tempArray_136);
        int[] tempArray_137 = {18, 36, 7, 14, -5, 2, 2};
        fontMap.put(new Integer(4139), tempArray_137);
        int[] tempArray_138 = {64, 72, 8, 8, -2, 8, 7};
        fontMap.put(new Integer(4140), tempArray_138);
        int[] tempArray_139 = {177, 77, 7, 7, -8, 2, 0};
        fontMap.put(new Integer(4141), tempArray_139);
        int[] tempArray_140 = {185, 77, 7, 7, -8, 2, 0};
        fontMap.put(new Integer(4142), tempArray_140);
        int[] tempArray_141 = {245, 76, 3, 7, -6, 15, 0};
        fontMap.put(new Integer(4143), tempArray_141);
        int[] tempArray_142 = {233, 77, 5, 7, -7, 15, 0};
        fontMap.put(new Integer(4144), tempArray_142);
        int[] tempArray_143 = {217, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4145), tempArray_143);
        int[] tempArray_144 = {214, 85, 8, 4, -9, 4, 0};
        fontMap.put(new Integer(4146), tempArray_144);
        int[] tempArray_145 = {76, 36, 3, 14, 0, 8, 2};
        fontMap.put(new Integer(4147), tempArray_145);
        int[] tempArray_146 = {38, 36, 5, 14, 0, 8, 4};
        fontMap.put(new Integer(4148), tempArray_146);
        int[] tempArray_147 = {247, 84, 3, 4, -6, 4, 0};
        fontMap.put(new Integer(4150), tempArray_147);
        int[] tempArray_148 = {237, 85, 4, 4, -7, 16, 0};
        fontMap.put(new Integer(4151), tempArray_148);
        int[] tempArray_149 = {37, 82, 3, 8, 0, 8, 4};
        fontMap.put(new Integer(4152), tempArray_149);
        int[] tempArray_150 = {209, 77, 7, 7, -8, 2, 0};
        fontMap.put(new Integer(4153), tempArray_150);
        int[] tempArray_151 = {32, 36, 5, 14, -3, 8, 2};
        fontMap.put(new Integer(4154), tempArray_151);
        int[] tempArray_152 = {188, 0, 10, 20, 0, 2, 2};
        fontMap.put(new Integer(4155), tempArray_152);
        int[] tempArray_153 = {177, 85, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4156), tempArray_153);
        int[] tempArray_154 = {249, 76, 3, 7, -7, 15, 0};
        fontMap.put(new Integer(4157), tempArray_154);
        int[] tempArray_155 = {181, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4160), tempArray_155);
        int[] tempArray_156 = {154, 68, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4161), tempArray_156);
        int[] tempArray_157 = {163, 21, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4162), tempArray_157);
        int[] tempArray_158 = {154, 21, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4163), tempArray_158);
        int[] tempArray_159 = {9, 36, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4164), tempArray_159);
        int[] tempArray_160 = {0, 36, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4165), tempArray_160);
        int[] tempArray_161 = {244, 15, 8, 14, 0, 2, 9};
        fontMap.put(new Integer(4166), tempArray_161);
        int[] tempArray_162 = {235, 15, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4167), tempArray_162);
        int[] tempArray_163 = {118, 70, 8, 8, 0, 8, 9};
        fontMap.put(new Integer(4168), tempArray_163);
        int[] tempArray_164 = {226, 15, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4169), tempArray_164);
        int[] tempArray_165 = {41, 82, 2, 8, 0, 8, 3};
        fontMap.put(new Integer(4170), tempArray_165);
        int[] tempArray_166 = {27, 82, 4, 8, 0, 8, 5};
        fontMap.put(new Integer(4171), tempArray_166);
        int[] tempArray_167 = {208, 0, 8, 20, 0, 2, 8};
        fontMap.put(new Integer(4172), tempArray_167);
        int[] tempArray_168 = {177, 0, 10, 20, 0, 2, 10};
        fontMap.put(new Integer(4173), tempArray_168);
        int[] tempArray_169 = {33, 0, 19, 20, 0, 2, 20};
        fontMap.put(new Integer(4174), tempArray_169);
        int[] tempArray_170 = {48, 21, 12, 14, 0, 2, 12};
        fontMap.put(new Integer(4175), tempArray_170);
        int[] tempArray_171 = {94, 21, 9, 14, -5, 2, 4};
        fontMap.put(new Integer(4186), tempArray_171);
        int[] tempArray_172 = {15, 91, 12, 6, -14, 16, 0};
        fontMap.put(new Integer(4192), tempArray_172);
        int[] tempArray_173 = {129, 87, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4193), tempArray_173);
        int[] tempArray_174 = {193, 85, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4194), tempArray_174);
        int[] tempArray_175 = {86, 80, 12, 7, -14, 15, 0};
        fontMap.put(new Integer(4195), tempArray_175);
        int[] tempArray_176 = {240, 52, 4, 5, -7, 3, 0};
        fontMap.put(new Integer(4196), tempArray_176);
        int[] tempArray_177 = {113, 87, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4197), tempArray_177);
        int[] tempArray_178 = {73, 81, 12, 7, -14, 15, 0};
        fontMap.put(new Integer(4198), tempArray_178);
        int[] tempArray_179 = {99, 80, 12, 7, -11, 15, 0};
        fontMap.put(new Integer(4199), tempArray_179);
        int[] tempArray_180 = {121, 87, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4200), tempArray_180);
        int[] tempArray_181 = {73, 21, 10, 14, -8, 8, 2};
        fontMap.put(new Integer(4201), tempArray_181);
        int[] tempArray_182 = {208, 21, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4202), tempArray_182);
        int[] tempArray_183 = {0, 21, 15, 14, 0, 8, 15};
        fontMap.put(new Integer(4203), tempArray_183);
        int[] tempArray_184 = {239, 76, 5, 7, -7, 15, 0};
        fontMap.put(new Integer(4204), tempArray_184);
        int[] tempArray_185 = {0, 91, 14, 6, -16, 16, 0};
        fontMap.put(new Integer(4205), tempArray_185);
        int[] tempArray_186 = {84, 21, 9, 14, 0, 8, 8};
        fontMap.put(new Integer(4206), tempArray_186);
        int[] tempArray_187 = {172, 21, 8, 14, 0, 8, 9};
        fontMap.put(new Integer(4207), tempArray_187);
        int[] tempArray_188 = {46, 82, 13, 7, -15, 15, 0};
        fontMap.put(new Integer(4208), tempArray_188);
        int[] tempArray_189 = {66, 89, 11, 6, -14, 16, 0};
        fontMap.put(new Integer(4209), tempArray_189);
        int[] tempArray_190 = {78, 89, 11, 6, -10, 16, 0};
        fontMap.put(new Integer(4210), tempArray_190);
        int[] tempArray_191 = {41, 91, 12, 6, -14, 16, 0};
        fontMap.put(new Integer(4211), tempArray_191);
        int[] tempArray_192 = {28, 91, 12, 6, -11, 16, 0};
        fontMap.put(new Integer(4212), tempArray_192);
        int[] tempArray_193 = {137, 87, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4213), tempArray_193);
        int[] tempArray_194 = {161, 77, 7, 7, -8, 15, 0};
        fontMap.put(new Integer(4214), tempArray_194);
        int[] tempArray_195 = {169, 77, 7, 7, -8, 15, 0};
        fontMap.put(new Integer(4215), tempArray_195);
        int[] tempArray_196 = {145, 86, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4216), tempArray_196);
        int[] tempArray_197 = {153, 86, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4217), tempArray_197);
        int[] tempArray_198 = {169, 85, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4218), tempArray_198);
        int[] tempArray_199 = {54, 90, 11, 6, -13, 16, 0};
        fontMap.put(new Integer(4219), tempArray_199);
        int[] tempArray_200 = {185, 85, 7, 6, -8, 16, 0};
        fontMap.put(new Integer(4220), tempArray_200);
        int[] tempArray_201 = {50, 36, 5, 14, -3, 8, 2};
        fontMap.put(new Integer(4221), tempArray_201);
        int[] tempArray_202 = {107, 0, 17, 20, 0, 2, 2};
        fontMap.put(new Integer(4222), tempArray_202);
        int[] tempArray_203 = {166, 0, 10, 20, 0, 2, 2};
        fontMap.put(new Integer(4223), tempArray_203);
        int[] tempArray_204 = {53, 0, 17, 20, 0, 2, 2};
        fontMap.put(new Integer(4224), tempArray_204);
        int[] tempArray_205 = {155, 0, 10, 20, 0, 2, 2};
        fontMap.put(new Integer(4225), tempArray_205);
        int[] tempArray_206 = {89, 0, 17, 20, 0, 2, 2};
        fontMap.put(new Integer(4226), tempArray_206);
        int[] tempArray_207 = {217, 0, 5, 20, 0, 2, 2};
        fontMap.put(new Integer(4227), tempArray_207);
        int[] tempArray_208 = {143, 0, 11, 20, 0, 2, 2};
        fontMap.put(new Integer(4228), tempArray_208);
        int[] tempArray_209 = {60, 81, 12, 7, -14, 15, 0};
        fontMap.put(new Integer(4229), tempArray_209);
        int[] tempArray_210 = {81, 62, 21, 8, 0, 8, 22};
        fontMap.put(new Integer(4230), tempArray_210);
        int[] tempArray_211 = {201, 85, 3, 6, -7, 15, 0};
        fontMap.put(new Integer(4231), tempArray_211);
        int[] tempArray_212 = {193, 77, 7, 7, -7, 15, 0};
        fontMap.put(new Integer(4232), tempArray_212);
        int[] tempArray_213 = {123, 79, 9, 7, -9, 15, 0};
        fontMap.put(new Integer(4233), tempArray_213);
        int[] tempArray_214 = {102, 88, 10, 6, -11, 16, 0};
        fontMap.put(new Integer(4234), tempArray_214);
        int[] tempArray_215 = {133, 79, 9, 7, -10, 2, 0};
        fontMap.put(new Integer(4235), tempArray_215);
        int[] tempArray_216 = {143, 78, 9, 7, -10, 2, 0};
        fontMap.put(new Integer(4236), tempArray_216);
        int[] tempArray_217 = {161, 85, 7, 6, -7, 2, 0};
        fontMap.put(new Integer(4237), tempArray_217);
        int[] tempArray_218 = {112, 79, 10, 7, -8, 2, 0};
        fontMap.put(new Integer(4238), tempArray_218);
        int[] tempArray_219 = {45, 73, 9, 8, -1, 8, 9};
        fontMap.put(new Integer(4239), tempArray_219);
        int[] tempArray_220 = {127, 36, 8, 12, 0, 8, 9};
        fontMap.put(new Integer(4240), tempArray_220);
        int[] tempArray_221 = {223, 0, 16, 14, 0, 8, 17};
        fontMap.put(new Integer(4241), tempArray_221);
        int[] tempArray_222 = {181, 21, 8, 14, 0, 8, 8};
        fontMap.put(new Integer(4242), tempArray_222);
        int[] tempArray_223 = {251, 84, 3, 4, -3, 16, 0};
        fontMap.put(new Integer(4244), tempArray_223);
        int[] tempArray_224 = {242, 84, 4, 4, 0, 16, 4};
        fontMap.put(new Integer(4245), tempArray_224);
        int[] tempArray_225 = {90, 88, 11, 6, -14, 16, 0};
        fontMap.put(new Integer(4246), tempArray_225);
        int[] tempArray_226 = {114, 21, 9, 14, 0, 8, 8};
        fontMap.put(new Integer(4247), tempArray_226);

    }
}