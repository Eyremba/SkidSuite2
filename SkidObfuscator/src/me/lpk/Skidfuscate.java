package me.lpk;

public class Skidfuscate {
	private static Skidfuscate instance;
	public static Skidfuscate get(){
		if (instance == null){
			instance = new Skidfuscate();
		}
		return instance;
	}
}
