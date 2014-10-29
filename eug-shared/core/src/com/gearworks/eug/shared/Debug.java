package com.gearworks.eug.shared;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Includes debug functionality for client/server
 */
public class Debug {
	private static PrintWriter writer;
	
	public static void println(String msg){
		if(!SharedVars.DEBUG_VERBOSE) return;
		
		System.out.println(msg);
	}
	
	public static void print(String msg){
		if(!SharedVars.DEBUG_VERBOSE) return;
		
		System.out.print(msg);
	}
	
	public static void log(String msg){
		if(!SharedVars.DEBUG_LOG) return;
		if(writer == null){
			try {
				writer = new PrintWriter("log-" + (new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date())) + ".txt", "UTF-8");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		writer.println(msg);
	}
	
	public static void closeLog(){
		if(!SharedVars.DEBUG_LOG) return;
		if(writer != null)
			writer.close();
	}
}
