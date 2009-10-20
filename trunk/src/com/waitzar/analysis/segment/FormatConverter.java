/*
 * Copyright 2009 by Seth N. Hetu
 * Please refer to the end of the file for licensing information
 */

package com.waitzar.analysis.segment;

/**
 * Convert from Zawgyi2009 format to Zawgyi2008 format (for display).
 * NOTE: This converter was intended to be FAST and mild on memory, not necessarily accurate.
 *       It was tested and exhibited decent accuracy for most strings; however, a full
 *       round of spot-checking is necessary before we can remove the experimental label.
 * NOTE: Passing in Zawgyi-2008 strings should not cause any problems; the converter was
 *       designed to output roughly the same string.
 * NOTE: It is best to use the segmentation algorithm (WZSegment) to re-order a few particularly
 *       ugly strings of text.
 *
 * @author Seth N. Hetu
 */
public class FormatConverter {
    //Indices into the scanning array. Each of these letters/combos is referred to by a name I can
    //  remember easily. Apologies if any (most) are non-standard.
    //These also sesrve as identifers returned by the classification function. Finally, one is just the size
    //  of all elements, for easy array creation.
    private static final int ID_NOID = -1;
    private static final int ID_VOWELL_A = 0;
    private static final int ID_YA_YIT = 1;
    private static final int ID_CONSONANT = 2;
    private static final int ID_STACKED_CONSONANT = 3;
    private static final int ID_KINZI = 4;
    private static final int ID_YA_PIN = 5;
    private static final int ID_CIRCLE_BELOW = 6;
    private static final int ID_LEG_BACK = 7;
    private static final int ID_CIRCLE_ABOVE = 8;
    private static final int ID_CIRCLE_ABOVE_CROSSED = 9;
    private static final int ID_SLASH_ABOVE = 10;
    private static final int ID_DOT_ABOVE = 11;
    private static final int ID_AR_TALL = 12;
    private static final int ID_AR_SHORT = 13;
    private static final int ID_LEG_FORWARD = 14;
    private static final int ID_DOUBLE_LEG_FORWARD = 15;
    private static final int ID_ASAT = 16;
    private static final int ID_VISARGA = 17;
    private static final int ID_DOT_BELOW = 18;
    private static final int ID_TOTALIDENTIFIERS = 19;
    


    //EXPERIMENTAL
    public static final String DowngradeZawgyi2009(String str) {
        //Step one: put back all our pat-sint letters, etc.
        str = ScanSimpleReplacements(str);

        //Step two: perform some syntactical searching for the harder letters (cut-off, etc.)
        str = ScanTrickyReplacements(str);

        return str;
    }


    private static final String ScanSimpleReplacements(String source) {
        //Result will be no longer than the original.
        char[] res = new char[source.length()];
        int nextID = 0;

        //Scan
        char twoBackChar = '\0';
        char prevChar = '\0';
        char currChar = '\0';
        for (int i=0; i<source.length(); i++) {
            //Get next letter
            currChar = source.charAt(i);

            //Match, set remove, change currChar
            int remove = 0;

            //Match three-letter substitutions first. These always remove at least one letter,
            //  so zero means we didn't match anything.
            switch (currChar) {
                //Complex consonants
                case 0x100B:
                    if (prevChar==0x103F && twoBackChar==0x100B) {
                        remove = 2;
                        currChar = 0x1097;
                    }
                    break;
                case 0x100C:
                    if (prevChar==0x103F && twoBackChar==0x100B) {
                        remove = 2;
                        currChar = 0x1092;
                    }
                    break;
                case 0x100D:
                    if (prevChar==0x103F && twoBackChar==0x100D) {
                        remove = 2;
                        currChar = 0x106E;
                    } else if (prevChar==0x103F && twoBackChar==0x100F) {
                        remove = 2;
                        currChar = 0x1091;
                    }
                    break;
                case 0x100E:
                    if (prevChar==0x103F && twoBackChar==0x100D) {
                        remove = 2;
                        currChar = 0x106F;
                    }
                    break;
                case 0x101E:
                    if (prevChar==0x103F && twoBackChar==0x101E) {
                        remove = 2;
                        currChar = 0x1086;
                    }
                    break;

                //Complex stacked "ta"
                case 0x103C:
                    if (prevChar==0x1010 && twoBackChar==0x103F) {
                        remove = 2;
                        currChar = 0x1096;
                    }
                    break;
            }

            //Match two-letter pat-sint substitutions next. These always remove one letter, so
            //  zero means the match didn't succeed for any substitution.
            if (remove==0 && prevChar==0x103F) {
                remove = 1;
                switch (currChar) {
                    case 0x1000:
                        currChar = 0x1060;
                        break;
                    case 0x1001:
                        currChar = 0x1061;
                        break;
                    case 0x1002:
                        currChar = 0x1062;
                        break;
                    case 0x1003:
                        currChar = 0x1063;
                        break;
                    case 0x1004:
                        currChar = 0x1064;
                        break;
                    case 0x1005:
                        currChar = 0x1065;
                        break;
                    case 0x1006:
                        currChar = 0x1066;
                        break;
                    case 0x1007:
                        currChar = 0x1068;
                        break;
                    case 0x1008:
                        currChar = 0x1069;
                        break;
                    case 0x1025:
                        currChar = 0x106A;
                        break;
                    case 0x100A:
                        currChar = 0x106B;
                        break;
                    case 0x100B:
                        currChar = 0x106C;
                        break;
                    case 0x100C:
                        currChar = 0x106D;
                        break;
                    case 0x100F:
                        currChar = 0x1070;
                        break;
                    case 0x1010:
                        currChar = 0x1071;
                        break;
                    case 0x1011:
                        currChar = 0x1073;
                        break;
                    case 0x1012:
                        currChar = 0x1075;
                        break;
                    case 0x1013:
                        currChar = 0x1076;
                        break;
                    case 0x1014:
                        currChar = 0x1077;
                        break;
                    case 0x1015:
                        currChar = 0x1078;
                        break;
                    case 0x1016:
                        currChar = 0x1079;
                        break;
                    case 0x1017:
                        currChar = 0x107A;
                        break;
                    case 0x1018:
                        currChar = 0x107B;
                        break;
                    case 0x1019:
                        currChar = 0x107C;
                        break;
                    case 0x101C:
                        currChar = 0x1085;
                        break;
                    //case 0x1018:   //Appears to be a shifted U+107B. Not in my version of Zawgyi
                    //    currChar = 0x1093;
                    //    break;
                    default:
                        remove = 0;
                }
            }

            //Final match: no need for the additional semantic meaning.
            if (currChar==0x103E)
                currChar = 0x1039;

            //Now, add this character, removing any as specified.
            nextID -= remove;
            res[nextID++] = currChar;

            //Update
            twoBackChar = prevChar;
            prevChar = currChar;
        }

        return new String(res, 0, nextID);
    }


    private static final int GetIdentifier(char letter) {
        switch (letter) {
            //ID_VOWEL_A
            case 0x1031:
                return ID_VOWELL_A;


            //ID_YA_YIT
            case 0x103B:
            case 0x107E:
            case 0x107F:
            case 0x1080:
            case 0x1081:
            case 0x1082:
            case 0x1083:
            case 0x1084:
                return ID_YA_YIT;


            //ID_CONSONANT
            // Includes things we're treating as consonants (numbers, complex)
            // (we are trying to give reasonable results on a string already
            //  in Zawgyi 2008 format)
            case 0x1000:
            case 0x1001:
            case 0x1002:
            case 0x1003:
            case 0x1004:
            case 0x1005:
            case 0x1006:
            case 0x1007:
            case 0x1008:
            case 0x1009:
            case 0x100A:
            case 0x100B:
            case 0x100C:
            case 0x100D:
            case 0x100E:
            case 0x100F:
            case 0x1010:
            case 0x1011:
            case 0x1012:
            case 0x1013:
            case 0x1014:
            case 0x1015:
            case 0x1016:
            case 0x1017:
            case 0x1018:
            case 0x1019:
            case 0x101A:
            case 0x101B:
            case 0x101C:
            case 0x101D:
            case 0x101E:
            case 0x101F:
            case 0x1020:
            case 0x1021:
          //case 0x1022:
            case 0x1023:
            case 0x1024:
            case 0x1025:
            case 0x1026:
            case 0x1027:
            case 0x1028:
            case 0x1029:
            case 0x1040:
            case 0x1041:
            case 0x1042:
            case 0x1043:
            case 0x1044:
            case 0x1045:
            case 0x1046:
            case 0x1047:
            case 0x1048:
            case 0x1049:
            case 0x104A:
            case 0x104B:
            case 0x104C:
            case 0x104D:
            case 0x104E:
            case 0x104F:
            case 0x106A:
            case 0x106B:
            case 0x106E:
            case 0x106F:
            case 0x1086:
            case 0x108F:
            case 0x1090:
            case 0x1091:
            case 0x1092:
            case 0x1097:
                return ID_CONSONANT;


            //ID_STACKED_CONSONANT
            case 0x1060:
            case 0x1061:
            case 0x1062:
            case 0x1063:
            //case 0x1064: //not kinzi
            case 0x1065:
            case 0x1066:
            case 0x1067:
            case 0x1068:
            case 0x1069:
            case 0x106C:
            case 0x106D:
            case 0x1070:
            case 0x1071:
            case 0x1072:
            case 0x1073:
            case 0x1074:
            case 0x1075:
            case 0x1076:
            case 0x1077:
            case 0x1078:
            case 0x1079:
            case 0x107A:
            case 0x107B:
            case 0x107C:
            case 0x1085:
            case 0x1096:
                return ID_STACKED_CONSONANT;


            //ID_KINZI
            case 0x1064:
            case 0x108B:
            case 0x108C:
            case 0x108D:
                return ID_KINZI;


            //ID_YA_PIN
            case 0x103A:
            case 0x107D:
                return ID_YA_PIN;


            //ID_CIRCLE_BELOW
            case 0x103C:
            case 0x108A:
                return ID_CIRCLE_BELOW;


            //ID_LEG_BACK
            case 0x103D:
            case 0x1087:
            case 0x1088:
            case 0x1089:
                return ID_LEG_BACK;


            //ID_CIRCLE_ABOVE
            case 0x102D:
            case 0x108E:
                return ID_CIRCLE_ABOVE;


            //ID_CIRCLE_ABOVE_CROSSED
            case 0x102E:
                return ID_CIRCLE_ABOVE_CROSSED;


            //ID_SLASH_ABOVE
            case 0x1032:
                return ID_SLASH_ABOVE;


            //ID_DOT_ABOVE
            case 0x1036:
                return ID_DOT_ABOVE;


            //ID_AR_TALL
            case 0x102B:
            case 0x105A:
                return ID_AR_TALL;


            //ID_AR_SHORT
            case 0x102C:
                return ID_AR_SHORT;


            //ID_LEG_FORWARD
            case 0x102F:
            case 0x1033:
                return ID_LEG_FORWARD;


            //ID_DOUBLE_LEG_FORWARD
            case 0x1030:
            case 0x1034:
                return ID_DOUBLE_LEG_FORWARD;


            //ID_ASAT
            case 0x1039:
                return ID_ASAT;


            //ID_VISARGA
            case 0x1038:
                return ID_VISARGA;


            //ID_DOT_BELOW
            case 0x1037:
            case 0x1094:
            case 0x1095:
                return ID_DOT_BELOW;


            //Everything else
            default:
                return ID_NOID;
        }
    }

    
    //Get any additional data for Zawgyi 2008's combinational letters.
    private static final int GetSupplementaryIdentifier(int primmaryID, char letter) {
        //We don't technically have to switch on the primary ID, but I want to 
        //  enforce that this should only be called AFTEr GetIdentifier() is called.
        switch (primmaryID) {
            case ID_STACKED_CONSONANT:
                if (letter==0x1069)
                    return ID_YA_PIN;
                break;
            case ID_KINZI:
                if (letter==0x108B)
                    return ID_CIRCLE_ABOVE;
                else if (letter==0x108C)
                    return ID_CIRCLE_ABOVE_CROSSED;
                else if (letter==0x108D)
                    return ID_DOT_ABOVE;
                break;
            case ID_CIRCLE_BELOW:
                if (letter==0x108A)
                    return ID_LEG_BACK;
                break;
            case ID_LEG_BACK:
                if (letter==0x1088)
                    return ID_LEG_FORWARD;
                else if (letter==0x1089)
                    return ID_DOUBLE_LEG_FORWARD;
                break;
            case ID_CIRCLE_ABOVE:
                if (letter==0x108E)
                    return ID_DOT_ABOVE;
                break;
            case ID_AR_TALL:
                if (letter==0x105A)
                    return ID_ASAT;
                break;
        }
        
        return ID_NOID;
    }


    private static final boolean IsLongConsonant(char letter) {
        switch (letter) {
            case 0x1000:
            case 0x1003:
            case 0x1006:
            case 0x100F:
            case 0x1010:
            case 0x1011:
            case 0x1018:
            case 0x101A:
            case 0x101C:
            case 0x101E:
            case 0x101F:
            case 0x1021:
            case 0x103F:
                return true;
            default:
                return false;
        }
    }

    private static final boolean IsShortConsonant(char letter) {
        switch (letter) {
            case 0x1001:
            case 0x1002:
            case 0x1004:
            case 0x1005:
            case 0x1007:
            case 0x100E:
            case 0x1012:
            case 0x1013:
            case 0x1014:
            case 0x1015:
            case 0x1016:
            case 0x1017:
            case 0x1019:
            case 0x101B:
            case 0x101D:
            case 0x1027:
            case 0x108F: //"Na" cut
            case 0x1090: //"Ya" cut
                return true;
            default:
                return false;
        }
    }


    private static final char GetYa(boolean isLong, boolean cutTop, boolean cutBottom) {
        if (isLong) {
            if (cutTop && cutBottom)
                return '\u1084';
            else if (cutTop)
                return '\u1080';
            else if (cutBottom)
                return '\u1082';
            else
                return '\u107E';
        } else {
            if (cutTop && cutBottom)
                return '\u1083';
            else if (cutTop)
                return '\u107F';
            else if (cutBottom)
                return '\u1081';
            else
                return '\u103B';
        }
    }


    //Requires short and tall legs to already be combined
    private static final void CombineAndReduce(String[] identifiers, int currID) {
        //The main part of our algorithm. What to combine, and when, and why.
        //I have included empty branches so that they stand out.
        char letter = identifiers[currID].length()==1 ? identifiers[currID].charAt(0) : '\0';
        char consonant = identifiers[ID_CONSONANT].length()==1 ? identifiers[ID_CONSONANT].charAt(0) : '\0';
        switch (currID) {
            case ID_VOWELL_A:
                //No change
                break;
            case ID_YA_YIT:
                //Long/short variants. Mildly complex
                boolean isLong = IsLongConsonant(consonant);
                boolean cutTop = identifiers[ID_CIRCLE_ABOVE]!=null || identifiers[ID_DOT_ABOVE]!=null
                        || identifiers[ID_CIRCLE_ABOVE_CROSSED]!=null || identifiers[ID_KINZI]!=null || identifiers[ID_SLASH_ABOVE]!=null;
                boolean cutBottom = identifiers[ID_CIRCLE_BELOW]!=null || identifiers[ID_STACKED_CONSONANT]!=null;
                identifiers[currID] = ""+GetYa(isLong, cutTop, cutBottom);
                break;
            case ID_CONSONANT:
                //Cut short some letters
                if (letter==0x1014) {
                    //Na -cut if something's under it
                    //Simple case:
                    boolean cut = identifiers[ID_YA_PIN]!=null || identifiers[ID_LEG_BACK]!=null
                                || identifiers[ID_CIRCLE_BELOW]!=null || identifiers[ID_STACKED_CONSONANT]!=null;

                    //Complex case: cut if there's a SHORT leg
                    if (!cut && identifiers[ID_LEG_FORWARD]!=null || identifiers[ID_DOUBLE_LEG_FORWARD]!=null) {
                        if (identifiers[ID_LEG_FORWARD]!=null && identifiers[ID_LEG_FORWARD].charAt(0)=='\u102F')
                            cut = true;
                        if (identifiers[ID_DOUBLE_LEG_FORWARD]!=null && identifiers[ID_DOUBLE_LEG_FORWARD].charAt(0)=='\u1030')
                            cut = true;
                    }

                    if (cut)
                        identifiers[currID] = "\u108F";
                } else if (letter==0x101B) {
                    //Ya -cut if something's near its endpoint
                    if (identifiers[ID_LEG_BACK]!=null) {
                        if (identifiers[ID_LEG_FORWARD]!=null || identifiers[ID_DOUBLE_LEG_FORWARD]!=null)
                            identifiers[currID] = "\u1090";
                    }
                //} else if (letter==0x1009) {  //We could get this here, but for now we leave it.
                } else if (letter==0x100A) {
                    //Shorten "nya" if necessary
                    if (identifiers[ID_CIRCLE_BELOW]!=null)
                        identifiers[currID] = "\u106B";
                } else if (letter==0x1025) {
                    //Shorten a similar letter: "o"
                    if (identifiers[ID_STACKED_CONSONANT]!=null)
                        identifiers[currID] = "\u106A";
                }

                break;
            case ID_STACKED_CONSONANT:
                //Move a few stacked consonants to the right
                if (letter=='\u1066' || letter=='\u1071' || letter=='\u1073') {
                    //For onece the math is easy
                    if (IsShortConsonant(consonant)) {
                        letter++;
                        identifiers[currID] = letter+"";
                    }
                }
                
                break;
            case ID_KINZI:
                //Combine kinzi with "(crossed) circle above" and "dot above"
                if (identifiers[ID_CIRCLE_ABOVE]!=null && identifiers[ID_CIRCLE_ABOVE].length()>0) {
                    identifiers[currID] = "\u108B";
                    identifiers[ID_CIRCLE_ABOVE] = "";
                } else if (identifiers[ID_CIRCLE_ABOVE_CROSSED]!=null && identifiers[ID_CIRCLE_ABOVE_CROSSED].length()>0) {
                    identifiers[currID] = "\u108C";
                    identifiers[ID_CIRCLE_ABOVE_CROSSED] = "";
                } else if (identifiers[ID_DOT_ABOVE]!=null && identifiers[ID_DOT_ABOVE].length()>0) {
                    identifiers[currID] = "\u108D";
                    identifiers[ID_DOT_ABOVE] = "";
                }

                break;
            case ID_YA_PIN:
            case ID_CIRCLE_BELOW:
            case ID_LEG_BACK:
                //Handle all three in a row, setting the others to null as necessary.
                // This avoids ordering issues.
                //First, check if we've already completed merging:
                if (   (identifiers[ID_YA_PIN]!=null && identifiers[ID_YA_PIN].length()==0)
                    || (identifiers[ID_CIRCLE_BELOW]!=null && identifiers[ID_CIRCLE_BELOW].length()==0)
                    || (identifiers[ID_LEG_BACK]!=null && identifiers[ID_LEG_BACK].length()==0))
                    break; //There are cases this doesn't catch, but they are reentrant.

                //Need to cut ya-pin if there are any pat-sint characters.
                //Need to combine leg and circle if both exist
                //Need to shorten leg in some pesky cases
                //Tag leg "standing" combinations
                boolean cutYa = identifiers[ID_YA_PIN]!=null && (identifiers[ID_STACKED_CONSONANT]!=null || identifiers[ID_CIRCLE_BELOW]!=null);
                boolean tagCircle = identifiers[ID_CIRCLE_BELOW]!=null && identifiers[ID_LEG_BACK]!=null;
                boolean shortLeg = identifiers[ID_LEG_BACK]!=null && identifiers[ID_YA_YIT]!=null;
                if (!shortLeg) {
                    shortLeg = identifiers[ID_CONSONANT].length()==1
                            && (identifiers[ID_CONSONANT].charAt(0)=='\u1020' || identifiers[ID_CONSONANT].charAt(0)=='\u100A');
                }
                boolean twoLegs = identifiers[ID_LEG_BACK]!=null && identifiers[ID_LEG_FORWARD]!=null && identifiers[ID_LEG_FORWARD].length()==1 && identifiers[ID_LEG_FORWARD].charAt(0)=='\u102F';
                boolean threeLegs = identifiers[ID_LEG_BACK]!=null && identifiers[ID_DOUBLE_LEG_FORWARD]!=null && identifiers[ID_DOUBLE_LEG_FORWARD].length()==1 && identifiers[ID_DOUBLE_LEG_FORWARD].charAt(0)=='\u1030';

                //Now, apply our combinations
                if (tagCircle) {
                    identifiers[ID_LEG_BACK] = "";
                    identifiers[ID_CIRCLE_BELOW] = "\u108A";
                } else if (twoLegs) {
                    identifiers[ID_LEG_FORWARD] = "";
                    identifiers[ID_LEG_BACK] = "\u1088";
                } else if (threeLegs) {
                    identifiers[ID_LEG_FORWARD] = "";
                    identifiers[ID_LEG_BACK] = "\u1089";
                } else if (shortLeg) {
                    identifiers[ID_LEG_BACK] = "\u1087";
                }
                if (cutYa)
                    identifiers[ID_YA_PIN] = "\u107D";

                //Finally, combine these and store them in the current element, zeroeing out all others
                int[] C_IDS = new int[]{ID_YA_PIN, ID_CIRCLE_BELOW, ID_LEG_BACK};
                StringBuffer builtRes = new StringBuffer();
                for (int i=0; i<C_IDS.length; i++) {
                    if (identifiers[C_IDS[i]]!=null) {
                        builtRes.append(identifiers[C_IDS[i]]);
                        identifiers[C_IDS[i]] = "";
                    }
                }
                identifiers[currID] = builtRes.toString();

                break;
            case ID_CIRCLE_ABOVE:
                //Combine with "dot above"
                if (identifiers[ID_DOT_ABOVE]!=null && identifiers[ID_DOT_ABOVE].length()>0) {
                    identifiers[currID] = "\u108E";
                    identifiers[ID_DOT_ABOVE] = "";
                }

                break;
            case ID_CIRCLE_ABOVE_CROSSED:
                //Nothing to see here
                break;
            case ID_SLASH_ABOVE:
                //Nothing here also
                break;
            case ID_DOT_ABOVE:
                //Nada
                break;
            case ID_AR_TALL:
                //Combine with "asat". This rule will have to be modified if killed consonants are
                //  parsed in the same pass as base ones; add a "identifiers[ID_KILLED_CONS]!=null check", I suppose.
                if (identifiers[ID_ASAT]!=null && identifiers[ID_ASAT].length()>0) {
                    identifiers[currID] = "\u105A";
                    identifiers[ID_ASAT] = "";
                }

                break;
            case ID_AR_SHORT:
                //Nothing to do here
                break;
            case ID_LEG_FORWARD:
                //Was pre-computed
                break;
            case ID_DOUBLE_LEG_FORWARD:
                //Likewise
                break;
            case ID_ASAT:
                //No need
                break;
            case ID_VISARGA:
                //Very simple character
                break;
            case ID_DOT_BELOW:
                //Also done before
                break;
        }
    }


    private static final String ScanTrickyReplacements(String source) {
        //Our algorithm is fairly simple. It operates on strings of partial syllables, scanning
        //  until it finds a consonant OR the vowel "ay" OR the medial "ya yit" (the only
        //  two letters which can appear before a consonant). For the purpose of simplicity, scanning
        //  stops at either a base consonant or a killed one (but not a stacked one). This slows the
        //  scanning process down slightly, but I doubt it's a real performance issue.
        //The algorithm scans each string twice. The first time, it tags each syntactic entity of interest
        //  (e.g., "a stacked letter", "a consonant", "circle above"), storing the actual matched item in
        //  an "identifiers" array. So, identifiers[STACKED] would contain WHICHEVER of the stacked letters
        //  matched. (Part of the reason we don't tag killed consonants is because some rare words contain two).
        //On the second pass, for each letter, the algorithm either adds it (if it has no identifier), or it performs
        //  a series of modification and combination rules. These rules can use the identifiers array to check if
        //  a syntactic entity exists. For example, "ya pin" can check if "circle below" is not null, and thus
        //  if it should use a shortened version of itself. At this point, it can either "modify" itself or the
        //  circle below entry, or it can "combine" the two. Combined letters should be stored with the ID of the
        //  first matched component ("ya pin", if the string is properly normalized). The second matched component
        //  can either be set to null, to indicate that its syntactic purpose is no longer valid, or to the empty
        //  string, "", to indicate that it should still count as a syntactic match for future letters, but its
        //  content no longer exists for hte purpose of combination. Which setting is acceptable is a judgement call.
        //After modification and combination takes place, the string indexed by the current element is added to the
        //  result string, and the next letter is considered. At any time, if the indexed value is "", the algorithm
        //  skips that letter.
        //Care should be taken to consider non-standard normalization orders (such as "circle below" before "ya pin").
        //  In general, though, the order defined by Ko Soe Min is always assumed. (We do not, for example, bother
        //  to consider vowel "ay" after "ya yit", since such ordering is sloppy).
        //One final note: our font does not contain 0x1093 (shifted "tha"), so we do not substitute it.
        //Apologies for the long comment, but I'd like to give hackers some chance at understanding
        //  why we chose to lex the source this way.

        //Begin the scanning process
        String[] identifiers = new String[ID_TOTALIDENTIFIERS];
        StringBuffer res = new StringBuffer();
        for (int startID=0; startID<source.length();startID++) {
            //First, advance startID past any non-Myanmar letters
            if (source.charAt(startID)<0x1000 || source.charAt(startID)>0x109F) {
                //System.out.println("source[" + startID + "] is " + ((int)source.charAt(startID)) + "  " + ((int)'\n'));
                res.append(source.charAt(startID));
                continue;
            }

            //Reset identifiers
            for (int i=0; i<identifiers.length; i++)
                identifiers[i] = null;

            //Next, scan until the first prefix vowel or consonant (base or killed is ok)
            // Meanwhile, set all entries in the identifiers array
            int endID = startID;
            for (;endID<source.length();endID++) {
                char nextChar = source.charAt(endID);
                int nextID = GetIdentifier(nextChar);
                if (nextID==ID_NOID)
                    continue;

                //Time to quit, if we've already passed a consonant
                if (nextID==ID_CONSONANT || nextID==ID_VOWELL_A || nextID==ID_YA_YIT  || nextChar<0x1000 || nextChar>0x0109F) {
                    if (identifiers[ID_CONSONANT]!=null)
                        break;
                }

                //Fake quit case: exit early if we'd be losing information
                if (identifiers[nextID]!=null) {
                    System.out.println("Warning: identifier already matched: " + nextID);
                    break;
                }

                //Normal case: just store it
                identifiers[nextID] = ""+nextChar;

                //Special cases for text already in Zawgyi 2008
                int suppID = GetSupplementaryIdentifier(nextID, nextChar);
                if (suppID!=ID_NOID)
                    identifiers[suppID] = "";
            }

            //Catch looping forever:
            if (startID==endID)
                throw new RuntimeException("Format Conversion algorithm exited in error.");

            //Before general processing, we convert short-to-tall for the "u" sounding vowells first, to make things easier later
            if (identifiers[ID_LEG_FORWARD]!=null || identifiers[ID_DOUBLE_LEG_FORWARD]!=null) {
                //Elongate it?
                boolean longer = identifiers[ID_YA_PIN]!=null || identifiers[ID_YA_YIT]!=null
                        || identifiers[ID_STACKED_CONSONANT]!=null || identifiers[ID_CIRCLE_BELOW]!=null;
                if (!longer) {
                    //Check a few more cases
                    if (identifiers[ID_CONSONANT].length()==0) {
                        char cons = identifiers[ID_CONSONANT].charAt(0);
                        longer = cons=='\u1009' || cons=='\u1025' || cons=='\u102A';
                    }
                }

                //Handle both at once
                if (longer && identifiers[ID_LEG_FORWARD]!=null)
                    identifiers[ID_LEG_FORWARD] = "\u1033";
                if (longer && identifiers[ID_DOUBLE_LEG_FORWARD]!=null)
                    identifiers[ID_DOUBLE_LEG_FORWARD] = "\u1034";
            }

            //One more thing: move the dot-below as well
            if (identifiers[ID_DOT_BELOW]!=null) {
                //Move a little bit?
                boolean moveALittle = identifiers[ID_LEG_FORWARD]!=null && identifiers[ID_LEG_FORWARD].charAt(0)=='\u102F';
                moveALittle = moveALittle || identifiers[ID_DOUBLE_LEG_FORWARD]!=null && identifiers[ID_DOUBLE_LEG_FORWARD].charAt(0)=='\u1030';
                moveALittle = moveALittle || identifiers[ID_LEG_BACK]!=null;
                moveALittle = moveALittle || identifiers[ID_AR_TALL]!=null;
                moveALittle = moveALittle || (identifiers[ID_CONSONANT].length()==1 && identifiers[ID_CONSONANT].charAt(0)=='\u1014');
                boolean moveALot = identifiers[ID_LEG_FORWARD]!=null && identifiers[ID_LEG_FORWARD].charAt(0)=='\u1033';
                moveALot = moveALot || identifiers[ID_DOUBLE_LEG_FORWARD]!=null && identifiers[ID_DOUBLE_LEG_FORWARD].charAt(0)=='\u1034';
                moveALot = moveALot || identifiers[ID_YA_PIN]!=null;
                moveALot = moveALot || identifiers[ID_STACKED_CONSONANT]!=null;
                moveALot = moveALot || identifiers[ID_CIRCLE_BELOW]!=null;
                moveALot = moveALot || (identifiers[ID_CONSONANT].length()==1 && identifiers[ID_CONSONANT].charAt(0)=='\u101B');
                if (moveALot)
                    identifiers[ID_DOT_BELOW] = "\u1095";
                else if (moveALittle)
                    identifiers[ID_DOT_BELOW] = "\u1094";
            }

            //Now, scan through the same string again
            for (int i=startID; i<endID; i++) {
                char currChar = source.charAt(i);
                int currID = GetIdentifier(currChar);
                if (currID==ID_NOID || currChar<0x1000 || currChar>0x109F) { //Shouldn't be any non-Myanmar here, but just in case...
                    //Append unknown characters. Possibly unwise, but I want to avoid
                    //  silent errors for the "testing" phase
                    res.append(currChar);
                } else {
                    //Has this already been processed
                    if (identifiers[currID]==null || identifiers[currID].length()==0)
                        continue;

                    //Otherwise, handle the main conversion process
                    CombineAndReduce(identifiers, currID);

                    //We are guaranteed that our identifiers array will have at least
                    //  the empty string after this process
                    res.append(identifiers[currID]);
                }
            }

            //Increment
            startID = endID-1;
        }

        return res.toString();
    }

}


/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
