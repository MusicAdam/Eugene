package com.gearworks.eug.shared;

/*
 * Includes debug functionality for client/server
 */
public class Debug {
	
	public static void println(String msg){
		if(!SharedVars.DEBUG_VERBOSE) return;
		
		System.out.println(msg);
	}
	
	public static void print(String msg){
		if(!SharedVars.DEBUG_VERBOSE) return;
		
		System.out.print(msg);
	}
}
