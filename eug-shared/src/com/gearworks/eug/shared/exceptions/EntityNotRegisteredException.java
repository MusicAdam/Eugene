package com.gearworks.eug.shared.exceptions;

public class EntityNotRegisteredException extends Exception{
	private static final long serialVersionUID = 1L;
	
	public EntityNotRegisteredException(String type){
		super(type);
	}
}