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
                if (c=='-' || c==' ' || c==')' || c=='_')
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
		String last = null;
		for (int i=0; i<firstOrder.size(); i++) {
			char[] chars = ((String)firstOrder.elementAt(i)).toCharArray();
			StringBuffer sb = new StringBuffer();

			//Sort
			boolean merge = false;
			for (int chIn=0; chIn<chars.length; chIn++) {
				//Check for pat sint words
				if (LanguageModel.getSemantics(chars[chIn])==LanguageModel.MY_PAT_SINT || chars[chIn]==0x1091)
					merge = true;

				//Properly order: 102F 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x102F) {
					chars[chIn-1]=0x102F;
					chars[chIn]=0x102D;
				}
				//Properly order: 103A 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x103A) {
					chars[chIn-1]=0x103A;
					chars[chIn]=0x102D;
				}
				//Properly order: 103D 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x103D) {
					chars[chIn-1]=0x103D;
					chars[chIn]=0x102D;
				}
				//Properly order: 1075 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x1075) {
					chars[chIn-1]=0x1075;
					chars[chIn]=0x102D;
				}
				//Properly order: 102D 1087
				if (chIn>0 && chars[chIn-1]==0x1087 && chars[chIn]==0x102D) {
					chars[chIn-1]=0x102D;
					chars[chIn]=0x1087;
				}
				//Properly order: 103D 102E
				if (chIn>0 && chars[chIn-1]==0x102E && chars[chIn]==0x103D) {
					chars[chIn-1]=0x103D;
					chars[chIn]=0x102E;
				}
				//Properly order: 103D 103A
				if (chIn>0 && chars[chIn-1]==0x103A && chars[chIn]==0x103D) {
					chars[chIn-1]=0x103D;
					chars[chIn]=0x103A;
				}
				//Properly order: 1039 103A -Note that this won't actually merge this fix!
				// Possibly set merged = true... ?
				if (chIn>0 && chars[chIn-1]==0x103A && chars[chIn]==0x1039) {
					chars[chIn-1]=0x1039;
					chars[chIn]=0x103A;
				}
				//Properly order: 1030 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x1030) {
					chars[chIn-1]=0x1030;
					chars[chIn]=0x102D;
				}
				//Properly order: 1037 1039
				if (chIn>0 && chars[chIn-1]==0x1039 && chars[chIn]==0x1037) {
					chars[chIn-1]=0x1037;
					chars[chIn]=0x1039;
				}
				//Properly order: 1032 1037
				if (chIn>0 && chars[chIn-1]==0x1037 && chars[chIn]==0x1032) {
					chars[chIn-1]=0x1032;
					chars[chIn]=0x1037;
				}
				//Properly order: 1032 1094
				if (chIn>0 && chars[chIn-1]==0x1094 && chars[chIn]==0x1032) {
					chars[chIn-1]=0x1032;
					chars[chIn]=0x1094;
				}
				//Properly order: 1064 1094
				if (chIn>0 && chars[chIn-1]==0x1094 && chars[chIn]==0x1064) {
					chars[chIn-1]=0x1064;
					chars[chIn]=0x1094;
				}
				//Properly order: 102D 1094
				if (chIn>0 && chars[chIn-1]==0x1094 && chars[chIn]==0x102D) {
					chars[chIn-1]=0x102D;
					chars[chIn]=0x1094;
				}
				//Properly order: 102D 1071
				if (chIn>0 && chars[chIn-1]==0x1071 && chars[chIn]==0x102D) {
					chars[chIn-1]=0x102D;
					chars[chIn]=0x1071;
				}
				//Properly order: 1036 1037
				if (chIn>0 && chars[chIn-1]==0x1037 && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x1037;
				}
				//Properly order: 1036 1088
				if (chIn>0 && chars[chIn-1]==0x1088 && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x1088;
				}
				//Properly order: 1039 1037
				// ###NOTE: I don't know how [XXXX][1037][1039] can parse correctly...
				if (chIn>0 && chars[chIn-1]==0x1037 && chars[chIn]==0x1039) {
					chars[chIn-1]=0x1039;
					chars[chIn]=0x1037;
				}
				//Properly order: 102D 1033
				//NOTE that this is later reversed for "103A 1033 102D" below
				// Also for 103C 1033 102D, what a mess...
				if (chIn>0 && chars[chIn-1]==0x1033 && chars[chIn]==0x102D) {
					chars[chIn-1]=0x102D;
					chars[chIn]=0x1033;
				}
				//Properly order: 103C 1032
				if (chIn>0 && chars[chIn-1]==0x1032 && chars[chIn]==0x103C) {
					chars[chIn-1]=0x103C;
					chars[chIn]=0x1032;
				}
				//Properly order: 103C 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x103C) {
					chars[chIn-1]=0x103C;
					chars[chIn]=0x102D;
				}
				//Properly order: 103C 102E
				if (chIn>0 && chars[chIn-1]==0x102E && chars[chIn]==0x103C) {
					chars[chIn-1]=0x103C;
					chars[chIn]=0x102E;
				}
				//Properly order: 1036 102F
				if (chIn>0 && chars[chIn-1]==0x102F && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x102F;
				}
				//Properly order: 1036 1088
				if (chIn>0 && chars[chIn-1]==0x1088 && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x1088;
				}
				//Properly order: 1036 103D
				if (chIn>0 && chars[chIn-1]==0x103D && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x103D;
				}
				//Properly order: 1036 103C
				if (chIn>0 && chars[chIn-1]==0x103C && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x103C;
				}
				//Properly order: 103C 107D
				if (chIn>0 && chars[chIn-1]==0x107D && chars[chIn]==0x103C) {
					chars[chIn-1]=0x103C;
					chars[chIn]=0x107D;
				}
				//Properly order: 1088 102D
				if (chIn>0 && chars[chIn-1]==0x102D && chars[chIn]==0x1088) {
					chars[chIn-1]=0x1088;
					chars[chIn]=0x102D;
				}
				//Properly order: 1019 107B 102C
				//ASSUME: 1019 is stationary
				if (chIn>1 && chars[chIn-2]==0x1019 && chars[chIn-1]==0x102C && chars[chIn]==0x107B) {
					chars[chIn-1]=0x107B;
					chars[chIn]=0x102C;
				}
				//Properly order: 103A 1033 102D
				//NOTE that this directly overrides "102D 1033" as entered above
				//ASSUME: 103A is stationary
				if (chIn>1 && chars[chIn-2]==0x103A && chars[chIn-1]==0x102D && chars[chIn]==0x1033) {
					chars[chIn-1]=0x1033;
					chars[chIn]=0x102D;
				}
				//Properly order: 103C 102D 1033
				if (chIn>1 && chars[chIn-2]==0x103C && chars[chIn-1]==0x1033 && chars[chIn]==0x102D) {
					chars[chIn-1]=0x102D;
					chars[chIn]=0x1033;
				}
				//Properly order: 1019 107B 102C 1037
				if (chIn>2 && chars[chIn-3]==0x1019 &&
						(   (chars[chIn-2]==0x107B && chars[chIn-1]==0x1037 && chars[chIn]==0x102C)
						  ||(chars[chIn-2]==0x102C && chars[chIn-1]==0x107B && chars[chIn]==0x1037)
						  ||(chars[chIn-2]==0x102C && chars[chIn-1]==0x1037 && chars[chIn]==0x107B)
						  ||(chars[chIn-2]==0x1037 && chars[chIn-1]==0x107B && chars[chIn]==0x102C)
						)) {
					chars[chIn-2]=0x107B;
					chars[chIn-1]=0x102C;
					chars[chIn]=0x1037;
				}

				//Properly order: 107E XXXX 1036 1033
				if (chIn>2 && chars[chIn-3]==0x107E && chars[chIn-1]==0x1033 && chars[chIn]==0x1036) {
					chars[chIn-1]=0x1036;
					chars[chIn]=0x1033;
				}

				//FIX: [103B-->1081 XXXX 103C] and [107E-->1082 XXXX 103C]
				if (chIn>1 && chars[chIn]==0x103C) {
					if (chars[chIn-2]==0x103B)
						chars[chIn-2]=0x1081;
					else if (chars[chIn-2]==0x107E)
						chars[chIn-2]=0x1082;
				}
				//FIX: [100A-->106B  108A]
				if (chIn>0 && chars[chIn]==0x108A && chars[chIn-1]==0x100A) {
					chars[chIn-1]=0x106B;
				}

				//Small fix 1072's a bit ugly at times
				if (chIn>0 && chars[chIn-1]==0x1010 && chars[chIn]==0x1072) {
					chars[chIn]=0x1071;
				}
			}
			sb.append(chars);

			//String preProccess = firstOrder.get(i);
			firstOrder.setElementAt(sb.toString(), i);
			String postProccess = (String)firstOrder.elementAt(i);


			//Mandalay
			if (last != null) {
				//To add: special mergecases for certain foreign words with customary spellings
				//   e.g., Singapore, English


				//Speical cases for phonetic slang, too:
				if (last.equals("\u101C")) { //LA
					if (postProccess.equals("\u1000\u103A\u102C\u1039"))
						merge = true;
				} else if (last.equals("\u1005\u1000\u1064\u102C")) { //Singapore
					if (postProccess.equals("\u1015\u1030"))
						merge = true;
				} else if (last.equals("\u1005")) { //SA
					if (postProccess.equals("\u1023\u1034"))
						merge = true;
				} else if (last.equals("\u1031\u1019")) { //myitta
					if (postProccess.equals("\u1023\u102C"))
						merge = true;
				} else if (last.equals("\u108F\u103A\u1034\u1038")) { //*sigh*, nukular
					if (postProccess.equals("\u1000"))
						merge = true;
				} else if (last.equals("\u108F\u103A\u1034\u1038\u1000")) { //*sigh*, nukular part 2
					if (postProccess.equals("\u101C\u102E\u1038"))
						merge = true;
				} else if (last.equals("\u108F\u103A\u1034\u1038\u1000\u101C\u102E\u1038")) { //*sigh*, nukular part 3
					if (postProccess.equals("\u101A\u102C\u1038"))
						merge = true;
				}

				if (merge) {
					postProccess = last + postProccess;
					firstOrder.setElementAt(postProccess, i);
					firstOrder.removeElementAt(i-1);
					i--;
				}
			}

			last = postProccess;
		}
    }
}
