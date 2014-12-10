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
	public enum Reporting {
		Verbose,
		Warning,
		Fatal
	}

	private static PrintWriter writer;
	

	public static void println(String msg){ println(msg, Reporting.Verbose); } //Verbose by default
	public static void println(String msg, Reporting level){
		if(!shouldReport(level)) return;
		
		System.out.println(msg);
	}
	
	public static void print(String msg){ print(msg, Reporting.Verbose); } //Verbose by default
	public static void print(String msg, Reporting level){
		if(!shouldReport(level)) return;
		
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
	
	public static boolean shouldReport(Reporting level){
		if(SharedVars.DEBUG_LEVEL == Reporting.Verbose) return true;
		if(level == Reporting.Fatal || level == Reporting.Warning && SharedVars.DEBUG_LEVEL == Reporting.Warning) return true;
		if(level == Reporting.Fatal && SharedVars.DEBUG_LEVEL == Reporting.Fatal) return true;
		
		return false;
	}
}
