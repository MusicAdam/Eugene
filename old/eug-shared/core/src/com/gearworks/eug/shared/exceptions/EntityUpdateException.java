package com.gearworks.eug.shared.exceptions;

public class EntityUpdateException extends Exception { 
	private static final long serialVersionUID = 1; 
	
	public EntityUpdateException(String msg){
		super(msg);
	}
}