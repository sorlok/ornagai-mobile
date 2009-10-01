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
        return source; //For now...
    }

}










