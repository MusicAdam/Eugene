package com.gearworks.eug.shared.messages;

public class TestMessage extends Message {
	public String msg;
	
	public TestMessage(String msg){
		this.msg = msg;
	}
	public TestMessage(){
		msg = "Message not set.";
	}
}
