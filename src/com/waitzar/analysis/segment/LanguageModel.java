/*
 * Copyright 2007 by Seth N. Hetu
 * Please refer to the end of the file for licensing information
 */

package com.waitzar.analysis.segment;

/**
 * Represent some important distinctions between letters in the Myanmar language.
 *  NOTE: My own names for the various letters are used. Moreover, these are the
 *   names I used for them in 2007, and are not the same names I use now, in 2009.
 *   This code is particularly messy.
 * @author Seth N. Hetu
 */
public class LanguageModel {
	public static final char THE_BASE = 0x101E;
	public static final char THE_END = 0x103D;

	public static final int MY_BASE = 0;
	public static final int MY_LEADING = 1;
	public static final int MY_TRAILING = 2;
	public static final int MY_PARTIAL = 3;
	public static final int MY_VIRAMA = 4;
	public static final int MY_STOP = 5;
	public static final int MY_OTHER = 6;
	public static final int MY_PAT_SINT = 7;
	public static final int SPACE_OR_PUNCT = 8;
	public static final int NOT_MYANMAR = 9;
	

	public static int getSemantics(char unicodeChar) {
            switch(unicodeChar) {
                //Punctuation
                case ' ':
                case ',':
                case '.':
                    return SPACE_OR_PUNCT;

                //Partial letters
                case 0x1000:
                case 0x1001:
                case 0x1002:
                case 0x1004:
                case 0x1005:
                case 0x1007:
                case 0x100A:
                case 0x100B:
                case 0x100C:
                case 0x100F:
                case 0x1010:
                case 0x1012:
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
                case 0x1025:
                case 0x108F:
                    return MY_PARTIAL;

                //Base letters only:
                case 0x1003:
                case 0x1006:
                case 0x1008:
                case 0x1009:
                case 0x100D:
                case 0x100E:
                case 0x1011:
                case 0x1013:
                case 0x1020:
                case 0x1021:
                case 0x1022:
                case 0x1023:
                case 0x1024:
                case 0x1026:
                case 0x1027:
                case 0x1028:
                case 0x1029:
                case 0x102A:
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
                case 0x104C:
                case 0x104D:
                case 0x104E:
                case 0x104F:
                case 0x1050:
                case 0x1051:
                case 0x1052:
                case 0x1053:
                case 0x1054:
                case 0x1055:
                case 0x106A:
                case 0x106B:
                case 0x1086:
                case 0x1090:
                case 0x1091:
                case 0x1092:
                    return MY_BASE;

                //Virama
                case 0x1039:
                    return MY_VIRAMA;

                //Leading
                case 0x1031:
                case 0x103B:
                case 0x107E:
                case 0x107F:
                case 0x1080:
                case 0x1081:
                case 0x1082:
                    return MY_LEADING;

                //Trailing:
                case 0x102B:
                case 0x102C:
                case 0x102D:
                case 0x102E:
                case 0x102F:
                case 0x1030:
                case 0x1032:
                case 0x1033:
                case 0x1034:
                case 0x1035:
                case 0x1036:
                case 0x1037:
                case 0x1038:
                case 0x103A:
                case 0x103C:
                case 0x103D:
                case 0x103E:
                case 0x103F:
                case 0x1056:
                case 0x1057:
                case 0x1058:
                case 0x1059:
                case 0x105A:
                case 0x107D:
                case 0x1087:
                case 0x1088:
                case 0x1089:
                case 0x108A:
                case 0x1094:
                case 0x1095:
                    return MY_TRAILING;

                //Stop characters
                case 0x104A:
                case 0x104B:
                    return MY_STOP;


                //Pat-sint letters
                case 0x1060:
                case 0x1061:
                case 0x1062:
                case 0x1064:
                case 0x1065:
                case 0x1066:
                case 0x1067:
                case 0x1068:
                case 0x1069:
                case 0x106D:
                case 0x1071:
                case 0x1072:
                case 0x1073:
                case 0x1075:
                case 0x1076:
                case 0x1078:
                case 0x107B:
                case 0x107C:
                case 0x1085:
                case 0x108B:
                case 0x108C:
                    return MY_PAT_SINT;

                //By default, unknown semantics or not Myanmar
                default:
                    if (unicodeChar<0x1000 || unicodeChar>0x109F)
                        return NOT_MYANMAR;
                    return MY_OTHER;
            }
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
