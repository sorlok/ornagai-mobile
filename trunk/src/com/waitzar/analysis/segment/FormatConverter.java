package com.waitzar.analysis.segment;

/**
 *
 * @author Seth N. Hetu
 */

//NOTE: This converter was intended to be FAST and mild on memory, not necessarily accurate.
//      It was tested and exhibited decent accuracy for most strings; however, a full
//      round of spot-checking is necessary before we can remove the experimental label.
//NOTE: It is best to use the segmentation algorithm (WZSegment) to re-order a few particularly
//      ugly strings of text.
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
        StringBuffer res = new StringBuffer();

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
            if (remove>0)
                res.delete(res.length()-remove, res.length());
            res.append(currChar);

            //Update
            twoBackChar = prevChar;
            prevChar = currChar;
        }

        return res.toString();
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
        String[] identifiers = new String[ID_TOTALIDENTIFIERS];



        return source; //For now...
    }

}










