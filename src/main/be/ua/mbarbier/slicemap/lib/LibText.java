package main.be.ua.mbarbier.slicemap.lib;

import java.util.LinkedHashMap;

public class LibText {

	public static String concatenateStringArray(String[] s, String separator ) {
		String str = "";
		for (int i = 0; i < s.length; i++ ) {
			if (i < s.length-1) {
				str = str + s[i] + separator;
			} else {
				str = str + s[i];
			}
		}

		return str;
	} 

	public static String mapToArgs(LinkedHashMap<String,String> m, String separator, String start, String end) {
		String str = start;
		for ( String k: m.keySet() ) {
			str = str + k + "=" + m.get(k) + separator;
		}
		str = str.substring( 0, str.length()-1 );
		str = str + end;

		return str;
	}

	public static LinkedHashMap<String,String> argsToMap(String str, String separator, String start, String end) {

		LinkedHashMap<String,String> m = new LinkedHashMap<String,String>();
		str = str.substring(start.length(), str.length()-end.length());
		String[] strList = str.split(separator);
		for ( String s: strList ) {
			String[] ss = s.split("=");
			if (ss.length > 1) m.put(ss[0].trim(), ss[1].trim());
			else m.put(ss[0].trim(), "");
		}

		return m;
	}
	
	public static LinkedHashMap<String,String> applyPattern(String pattern, String input) {
		
		LinkedHashMap<String,String> out = new LinkedHashMap<String,String>();
		
		return out; 
	}
}
