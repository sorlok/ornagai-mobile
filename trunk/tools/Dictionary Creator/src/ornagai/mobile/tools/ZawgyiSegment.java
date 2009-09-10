
package ornagai.mobile.tools;

import java.util.ArrayList;

/**
 *
 * @author Seth N. Hetu
 */

public class ZawgyiSegment {

	public static char THE_BASE = 0x101E;
	public static char THE_END = 0x103D;

	public enum SEMANTICS {
		MY_BASE,
		MY_LEADING,
		MY_TRAILING,
		MY_PARTIAL,
		MY_VIRAMA,
		MY_STOP,
		MY_OTHER,
		MY_PAT_SINT,
		SPACE_OR_PUNCT,
		NOT_MYANMAR
	}

	public static SEMANTICS getSemantics(char unicodeChar) {
		if (unicodeChar < 0x1000) {
			if (unicodeChar==' ' || unicodeChar== ',' || unicodeChar== '.' )
				return SEMANTICS.SPACE_OR_PUNCT;
			else
				return SEMANTICS.NOT_MYANMAR;
		} else if (unicodeChar < 0x102B) {
			if (unicodeChar==0x1000 || unicodeChar==0x1001 || unicodeChar==0x1004 || unicodeChar==0x1005 ||
				unicodeChar==0x1007 || unicodeChar==0x100A || unicodeChar==0x100B || unicodeChar==0x100C ||
				unicodeChar==0x1010 || unicodeChar==0x1012 || unicodeChar==0x1014 || unicodeChar==0x1015 ||
				unicodeChar==0x1016 || unicodeChar==0x1017 || unicodeChar==0x1019 || unicodeChar==0x101A ||
				unicodeChar==0x1002 || unicodeChar==0x100F || unicodeChar==0x1010 || unicodeChar==0x101B ||
				unicodeChar==0x101C || unicodeChar==0x1018 || unicodeChar==0x101D || unicodeChar==0x101E ||
				unicodeChar==0x101F || unicodeChar==0x1025)
				return SEMANTICS.MY_PARTIAL;
			return SEMANTICS.MY_BASE;
		} else if (unicodeChar < 0x1040) {
			if (unicodeChar == 0x1031 || unicodeChar == 0x103B)
				return SEMANTICS.MY_LEADING;
			else if (unicodeChar == 0x1039)
				return SEMANTICS.MY_VIRAMA;
			else
				return SEMANTICS.MY_TRAILING;
		} else if (unicodeChar < 0x1056) {
			if (unicodeChar == 0x104A || unicodeChar == 0x104B)
				return SEMANTICS.MY_STOP;
			else
				return SEMANTICS.MY_BASE;
		} else if (unicodeChar < 0x105B) {
			return SEMANTICS.MY_TRAILING;
		} else if (unicodeChar < 0x1200) {
			//Incomplete
			if (unicodeChar == 0x1060)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1061)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1062)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1064)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1065)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1066)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1067)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1068)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1069)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x106A)
				return SEMANTICS.MY_BASE;
			else if (unicodeChar == 0x106B)
				return SEMANTICS.MY_BASE;
			else if (unicodeChar == 0x106D)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1071)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1072)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1073)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1075)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1076)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1078)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x107B)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x107C)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x107D)
				return SEMANTICS.MY_TRAILING;
			else if (unicodeChar == 0x107E || unicodeChar == 0x107F || unicodeChar == 0x1080 || unicodeChar == 0x1081 || unicodeChar == 0x1082)
				return SEMANTICS.MY_LEADING;
			else if (unicodeChar == 0x1085)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x1086)
				return SEMANTICS.MY_BASE;
			else if (unicodeChar == 0x1087)
				return SEMANTICS.MY_TRAILING;
			else if (unicodeChar == 0x1088)
				return SEMANTICS.MY_TRAILING;
			else if (unicodeChar == 0x1089)
				return SEMANTICS.MY_TRAILING;
			else if (unicodeChar == 0x108A)
				return SEMANTICS.MY_TRAILING;
			else if (unicodeChar == 0x108B)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x108C)
				return SEMANTICS.MY_PAT_SINT;
			else if (unicodeChar == 0x108F)
				return SEMANTICS.MY_PARTIAL;
			else if (unicodeChar == 0x1090)
				return SEMANTICS.MY_BASE;
			else if (unicodeChar == 0x1091)
				return SEMANTICS.MY_BASE;
			else if (unicodeChar == 0x1092)
				return SEMANTICS.MY_BASE;
			else if (unicodeChar == 0x1094 || unicodeChar == 0x1095)
				return SEMANTICS.MY_TRAILING;
			else
				return SEMANTICS.MY_OTHER;
		} else {
			return SEMANTICS.NOT_MYANMAR;
		}
	}

}
