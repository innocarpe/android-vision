package com.zxd.zxdtestmode;

public class Util {
	
	public static native int camera_switch(int i);
	
	public static native int read_camera_switch();
	
	public static native int led_level(int level, int on);
	
	public static native int read_ledLevel();
	
	public static native int read_medenit68();
	
	public static native int read_medenit61();
	
	public static native int write_medenit61(int value);
	
	public static native void write_medicinehw(int value);
	
	public static native int read_medicinehw();
	
	
	
	static{
		System.loadLibrary("UTIL"); 
	}		
}
