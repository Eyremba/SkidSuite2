package me.lpk.util;

public class Strings {

	public static String genKey(int len) {
		return genKey(len, 600,900);
	}
	
	public static String genKey(int len, int rangeMin, int rangeMax) {
		String s = "";
		while (s.length() < len) {
			s = s + ((char) (rangeMin +( Math.random() * (rangeMax - rangeMin))));
		}
		return s;
	}

}
