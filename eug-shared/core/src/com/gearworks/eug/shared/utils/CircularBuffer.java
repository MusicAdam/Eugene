package com.gearworks.eug.shared.utils;


public class CircularBuffer<T> {

	private T[] data;
	private int head;
	private int tail;
	private Object lock = new Object();
	
	public CircularBuffer(int size){
		data = (T[])new Object[size];
		head = 0; tail = 0;
	}
	
	public void push(T t){
		if(t == null) throw new NullPointerException("May not push null");
		
		synchronized(lock){
			if(head == tail && data[tail] != null)
				head = (head + 1) % data.length;
			
			data[tail] = t;
			tail = (tail + 1) % data.length;
			//System.out.println("Push:" + t.toString());
			//System.out.println(this);
			//System.out.println();
		}
	}
	
	public T pop(){
		synchronized(lock){
			if(isEmpty()) return null;
		
			T t = data[head];
			data[head] = null;
			head = (head + 1) % data.length;
			
			//System.out.println("Pop:");
			//System.out.println(this);
			//System.out.println();
			return t;
		}
	}
	
	public T peek(){
		synchronized(lock){
			if(isEmpty()) return null;
			return data[head];
		}
	}
	
	public T peek(int inc){
		synchronized(lock){
			if(isEmpty()) return null;
			return data[(head + inc)%data.length];
		}		
	}
	
	public boolean isEmpty(){
		synchronized(lock){
			return head == tail && data[head] == null;
		}
	}
	
	public boolean isFull(){
		synchronized(lock){
			return head == tail && data[head] != null;
		}
	}
	
	public int count(){
		synchronized(lock){
			if(isFull())
				return data.length;
			return tail - head;
		}
	}
	
	public int size(){
		return data.length;
	}
	
	@Override
	public String toString(){
		synchronized(lock){
			String s = "";
			int width = 0;
			for(int i = 0; i < data.length; i++){
				if(data[i] == null) continue;
				if(data[i].toString().length() > width){
					width = data[i].toString().length();
				}
			}
			
			width = width + 1; 
			
			for(int i = 0; i < data.length; i++){
				if(data[i] == null){
					s += "-";
					for(int j = 0; j < width - 1; j++){						
						s += " ";
					}
				}else{
					s += data[i].toString();
					for(int j = 0; j < width - data[i].toString().length(); j++){						
						s += " ";
					}
				}
			}
			
			s+= "\n";
			
			for(int i = 0; i < data.length; i++){
				if(tail == head && i == tail){
					s += "^";
				}else if(tail == i){
					s += "T";
				}else if(head == i){
					s += "H";
				}else{
					s += " ";
				}
				for(int j = 0; j < width - 1; j++){						
					s += " ";
				}
			}
			
			s += "\n";
			s += "Full: " + isFull();
			s += "\nEmpty: " + isEmpty();
			
			return s;
		}
	}
}

