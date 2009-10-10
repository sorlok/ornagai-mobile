package com.waitzar.analysis.segment;

import java.util.Vector;

/**
 *
 * @author Seth N. Hetu
 */
public class WZSegment {

    //Vector<String>
    public static final Vector SegmentText(String input) {
    	Vector firstOrderSegments = WZSegment.SimpleSegment(input);

    	// Post-proccess this array
    	WZSegment.PostProcess(firstOrderSegments);

    	return firstOrderSegments;
    }

    //Vector<String>
    private static final Vector SimpleSegment(String input) {
        Vector res = new Vector();
        StringBuffer sb =  new StringBuffer();
        char[] lineChars = input.toCharArray();
        boolean foundBase = false;
        int mode = 0; //0 = myanmar, 1 = english/other
        for (int index=0; index<lineChars.length; index++ ) {
            char c = lineChars[index];
            int cSem = LanguageModel.getSemantics(c);
            boolean addWord = false;
            boolean addLetter = false;
            boolean partialCheck = false;
            int oldMode = mode;
            mode = cSem==LanguageModel.NOT_MYANMAR ? 1 : 0;
            boolean modeSwitch = mode != oldMode;

            if (mode==1) {
                //English
                if (c=='-' || c==' ' || c==')' || c=='_' || c=='\n')
                    addWord = true;
                addLetter = true;
            } else {
                //Burmese
                switch (cSem) {
                    case LanguageModel.MY_PARTIAL:
                        partialCheck = true; //Assume it's a base character for now.
                    case LanguageModel.MY_BASE:
                        if (foundBase && sb.length()>0)
                            addWord = true;
                        addLetter = true;
                        break;
                    case LanguageModel.MY_LEADING:
                        if (foundBase && sb.length()>0)
                            addWord = true;
                        addLetter = true;
                        break;
                    case LanguageModel.MY_VIRAMA:
                    case LanguageModel.MY_PAT_SINT:
                    case LanguageModel.MY_TRAILING:
                        addLetter = true;
                        break;
                    case LanguageModel.MY_OTHER:
                        throw new RuntimeException("Unknown semantics: " + Integer.toHexString(c).toUpperCase());
                    case LanguageModel.MY_STOP:
                    case LanguageModel.SPACE_OR_PUNCT:
                        addWord = true;
                        addLetter = true;
                        break;
                    default:
                        throw new RuntimeException("Unhandled case: " + LanguageModel.getSemantics(c));
                }

                if (partialCheck) {
                    if (index+1<lineChars.length) {
                        //Check the next character
                        if (LanguageModel.getSemantics(lineChars[index+1])==LanguageModel.MY_VIRAMA) {
                            //Special exception for "the"
                            if (c == LanguageModel.THE_BASE && index+2<lineChars.length)
                                addWord = (lineChars[index+2]==LanguageModel.THE_END);
                            else
                                addWord = false; //This isn't a base character; it's a partial charaacter
                        }
                    }
                }
            }
            
            if (addWord || modeSwitch) { //Always add a word on mode switch
                if (sb.length()>0) {
                    //Add the word to the results list
                    res.addElement(sb.toString());

                    //Clear the buffer for the next word
                    sb.delete(0, sb.length());
                }
                foundBase = false;
            }

            if (addLetter) {
                //System.out.println("  ->Letter");
                sb.append(c);

                if (cSem == LanguageModel.MY_BASE || cSem == LanguageModel.MY_PARTIAL || cSem == LanguageModel.MY_STOP) {
                    foundBase = true;
                }
            }
        }
        
        //Add the last word, if any
        if (sb.length()>0)
            res.addElement(sb.toString());
        return res;
    }


    private static final void PostProcess(Vector firstOrder) {
        String previousSyllable = "";
        for (int i=0; i<firstOrder.size(); i++) {
            char[] chars = ((String)firstOrder.elementAt(i)).toCharArray();

            //Sort
            boolean merge = false;
            char threeBackC = '\0';
            char twoBackC = '\0';
            char prevC = '\0';
            char currC = '\0';
            for (int cIn=0; cIn<chars.length; cIn++) {
                //Update current character
                currC = chars[cIn];

                //Check for pat sint words
                if (LanguageModel.getSemantics(currC)==LanguageModel.MY_PAT_SINT || currC==0x1091)
                    merge = true;

                //Partially normalize the character stream, to make tagging of complex pat-sint words easier.
                //Step 1: Double-letter substitutions
                boolean swap = false;
                switch (prevC) {
                    case 0x102D:
                        swap = (currC==0x102F || currC==0x103A || currC==0x103D ||  currC==0x1075 || currC==0x1030 || currC==0x103C || currC==0x1088);
                        break;
                    case 0x1087:
                        swap = (currC==0x102D);
                        break;
                    case 0x102E:
                        swap = (currC==0x103D || currC==0x103C);
                        break;
                    case 0x103A:
                        swap = (currC==0x103D || currC==0x1039);
                        break;
                    case 0x1039:
                        swap = (currC==0x1037);
                        break;
                    case 0x1037:
                        swap = (currC==0x1032 || currC==0x1036 || currC==0x1039);
                        break;
                    case 0x1094:
                        swap = (currC==0x1032 || currC==0x1064 || currC==0x102D);
                        break;
                    case 0x1071:
                        swap = (currC==0x102D);
                        break;
                    case 0x1088:
                        swap = (currC==0x1036);
                        break;
                    case 0x1033:
                        swap = (currC==0x102D);
                        break;
                    case 0x1032:
                        swap = (currC==0x103C);
                        break;
                    case 0x102F:
                        swap = (currC==0x1036);
                        break;
                    case 0x103D:
                        swap = (currC==0x1036);
                        break;
                    case 0x103C:
                        swap = (currC==0x1036);
                        break;
                    case 0x107D:
                        swap = (currC==0x103C);
                        break;

                }
                if (swap) {
                    //Swap
                    char temp = chars[cIn];
                    chars[cIn] = chars[cIn-1];
                    chars[cIn-1] = temp;
                    
                    //Update our markers
                    currC = chars[cIn];
                    prevC = chars[cIn-1];
                }

                //Step 2: Triple-letter and over-riding substitutions
                
                //Order: 1019 107B 102C
                //ASSUME: 1019 is stationary
                if (twoBackC==0x1019 && prevC==0x102C && currC==0x107B) {
                    chars[cIn-1]=0x107B;
                    chars[cIn]=0x102C;
                }
                
                //Over-ride: "103A 1033 102D"
                //ASSUME: 103A is stationary
                if (twoBackC==0x103A && prevC==0x102D && currC==0x1033) {
                    chars[cIn-1]=0x1033;
                    chars[cIn]=0x102D;
                }

                //Over-ride: "103C 1033 102D"
                //ASSUME: 103C is stationary
                if (twoBackC==0x103C && prevC==0x1033 && currC==0x102D) {
                    chars[cIn-1]=0x102D;
                    chars[cIn]=0x1033;
                }

                //Order: 1019 107B 102C 1037
                //ASSUME: 1019 is stationary
                if (threeBackC==0x1019 &&
                   (   (twoBackC==0x107B && prevC==0x1037 && currC==0x102C)
                     ||(twoBackC==0x102C && prevC==0x107B && currC==0x1037)
                     ||(twoBackC==0x102C && prevC==0x1037 && currC==0x107B)
                     ||(twoBackC==0x1037 && prevC==0x107B && currC==0x102C)
                   )) {
                    chars[cIn-2]=0x107B;
                    chars[cIn-1]=0x102C;
                    chars[cIn]=0x1037;
                }

                //Order: 107E * 1036 1033
                if (threeBackC==0x107E && prevC==0x1033 && currC==0x1036) {
                    chars[cIn-1]=0x1036;
                    chars[cIn]=0x1033;
                }


                //Step 3: Minor letter fixes, for display purposes only.
                
                //FIX: [103B-->1081] * 103C and [107E-->1082] * 103C
                if (currC==0x103C) {
                    if (twoBackC==0x103B)
                        chars[cIn-2]=0x1081;
                    else if (twoBackC==0x107E)
                        chars[cIn-2]=0x1082;
                }

                //FIX: [100A-->106B]  108A
                if (currC==0x108A && prevC==0x100A) {
                    chars[cIn-1]=0x106B;
                }

                //Small fix: 1072's a bit ugly at times, like after 1010
                if (prevC==0x1010 && currC==0x1072) {
                    chars[cIn]=0x1071;
                }

                //Update our lookback characters
                threeBackC = twoBackC;
                twoBackC = prevC;
                prevC = currC;
            }

            //Set our post-processed array to the character array we just shuffled.
            firstOrder.setElementAt(new String(chars), i);
            String postProccess = (String)firstOrder.elementAt(i);


            //Fix special pat-sint phrases and phonetic sland
            if (previousSyllable.equals("\u101C")) { //LA
                if (postProccess.equals("\u1000\u103A\u102C\u1039"))
                    merge = true;
            } else if (previousSyllable.equals("\u1005\u1000\u1064\u102C")) { //Singapore
                if (postProccess.equals("\u1015\u1030"))
                    merge = true;
            } else if (previousSyllable.equals("\u1005")) { //SA
                if (postProccess.equals("\u1023\u1034"))
                    merge = true;
            } else if (previousSyllable.equals("\u1031\u1019")) { //myitta
                if (postProccess.equals("\u1023\u102C"))
                    merge = true;
            } else if (previousSyllable.equals("\u108F\u103A\u1034\u1038")) { //*sigh*, nukular
                if (postProccess.equals("\u1000"))
                    merge = true;
            } else if (previousSyllable.equals("\u108F\u103A\u1034\u1038\u1000")) { //*sigh*, nukular part 2
                if (postProccess.equals("\u101C\u102E\u1038"))
                    merge = true;
            } else if (previousSyllable.equals("\u108F\u103A\u1034\u1038\u1000\u101C\u102E\u1038")) { //*sigh*, nukular part 3
                if (postProccess.equals("\u101A\u102C\u1038"))
                    merge = true;
            }

            //Combine these two half-words into one word
            if (merge) {
                postProccess = previousSyllable + postProccess;
                firstOrder.setElementAt(postProccess, i);firstOrder.removeElementAt(i-1);
                i--;
            }

            //Save the current syllable
            previousSyllable = postProccess;
        }
    }
}
