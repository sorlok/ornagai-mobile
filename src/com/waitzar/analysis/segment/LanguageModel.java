package com.waitzar.analysis.segment;

/**
 *
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
		if (unicodeChar < 0x1000) {
			if (unicodeChar==' ' || unicodeChar== ',' || unicodeChar== '.' )
				return SPACE_OR_PUNCT;
			else
				return NOT_MYANMAR;
		} else if (unicodeChar < 0x102B) {
			if (unicodeChar==0x1000 || unicodeChar==0x1001 || unicodeChar==0x1004 || unicodeChar==0x1005 ||
				unicodeChar==0x1007 || unicodeChar==0x100A || unicodeChar==0x100B || unicodeChar==0x100C ||
				unicodeChar==0x1010 || unicodeChar==0x1012 || unicodeChar==0x1014 || unicodeChar==0x1015 ||
				unicodeChar==0x1016 || unicodeChar==0x1017 || unicodeChar==0x1019 || unicodeChar==0x101A ||
				unicodeChar==0x1002 || unicodeChar==0x100F || unicodeChar==0x1010 || unicodeChar==0x101B ||
				unicodeChar==0x101C || unicodeChar==0x1018 || unicodeChar==0x101D || unicodeChar==0x101E ||
				unicodeChar==0x101F || unicodeChar==0x1025)
				return MY_PARTIAL;
			return MY_BASE;
		} else if (unicodeChar < 0x1040) {
			if (unicodeChar == 0x1031 || unicodeChar == 0x103B)
				return MY_LEADING;
			else if (unicodeChar == 0x1039)
				return MY_VIRAMA;
			else
				return MY_TRAILING;
		} else if (unicodeChar < 0x1056) {
			if (unicodeChar == 0x104A || unicodeChar == 0x104B)
				return MY_STOP;
			else
				return MY_BASE;
		} else if (unicodeChar < 0x105B) {
			return MY_TRAILING;
		} else if (unicodeChar < 0x1200) {
			//Incomplete
			if (unicodeChar == 0x1060)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1061)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1062)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1064)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1065)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1066)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1067)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1068)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1069)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x106A)
				return MY_BASE;
			else if (unicodeChar == 0x106B)
				return MY_BASE;
			else if (unicodeChar == 0x106D)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1071)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1072)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1073)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1075)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1076)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1078)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x107B)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x107C)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x107D)
				return MY_TRAILING;
			else if (unicodeChar == 0x107E || unicodeChar == 0x107F || unicodeChar == 0x1080 || unicodeChar == 0x1081 || unicodeChar == 0x1082)
				return MY_LEADING;
			else if (unicodeChar == 0x1085)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x1086)
				return MY_BASE;
			else if (unicodeChar == 0x1087)
				return MY_TRAILING;
			else if (unicodeChar == 0x1088)
				return MY_TRAILING;
			else if (unicodeChar == 0x1089)
				return MY_TRAILING;
			else if (unicodeChar == 0x108A)
				return MY_TRAILING;
			else if (unicodeChar == 0x108B)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x108C)
				return MY_PAT_SINT;
			else if (unicodeChar == 0x108F)
				return MY_PARTIAL;
			else if (unicodeChar == 0x1090)
				return MY_BASE;
			else if (unicodeChar == 0x1091)
				return MY_BASE;
			else if (unicodeChar == 0x1092)
				return MY_BASE;
			else if (unicodeChar == 0x1094 || unicodeChar == 0x1095)
				return MY_TRAILING;
			else
				return MY_OTHER;
		} else {
			return NOT_MYANMAR;
		}
	}


}
