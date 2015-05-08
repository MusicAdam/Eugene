package com.gearworks.eug.shared.utils;

public class Vector2 implements Cloneable{
	public float x;
	public float y;
	
	public Vector2(){
		x = 0;
		y = 0;
	}
	
	public Vector2(float x, float y){
		this.x = x;
		this.y = y;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		Vector2 vec = (Vector2)super.clone();
		return vec;
	}
}
