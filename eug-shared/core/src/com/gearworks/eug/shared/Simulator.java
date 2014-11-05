package com.gearworks.eug.shared;

import java.util.List;

import com.badlogic.gdx.utils.Array;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.CircularBuffer;

/*
 * The simulator takes a snapshot and a history of snapshots and simulates the snapshot forward in time.
 * 
 * Rules are provided to the simulator through callbacks that can be added by the user.
 */
public class Simulator {
	private class SimulationThread extends Thread {
		Snapshot snapshot;
		Array<SimulationRule> rules;
		CircularBuffer<Snapshot> history;
		long toTime;
		boolean running = false;
		
		public SimulationThread(Snapshot snapshot, long toTime, CircularBuffer<Snapshot> history, Array<SimulationRule> rules){
			this.snapshot = snapshot;
			this.rules = rules;
			this.toTime = toTime;
		}
		
		@Override
		public void run(){
			System.out.println("SIMULATION THREAD");
			long simTime = snapshot.getTimestamp(); //Start simulation at the time of the snapshot
			
			long step = (long)(SharedVars.STEP * 1000); //Convert STEP to miliseconds 		

			synchronized(lock){
				//Run simulation
				running = true;
				while(simTime < toTime){
					//If there isn't a full step left, calculate partial step
					if(simTime + step > toTime)
						step = toTime - simTime;
					
					//Apply Rules
					for(SimulationRule rule : rules){
						rule.apply(snapshot, step);
					}
	
					simTime += step; //Step time forward
					snapshot.setTimestamp(simTime);				
				}
				running = false;
			}
		}
		
		public Snapshot getResult(){
			if(running) return null;
			return snapshot;
		}
	}
	
	private Array<SimulationRule> rules = new Array<SimulationRule>();
	public Object lock = new Object();
	private SimulationThread thread;
	private Snapshot result;

	public Simulator(){	}
	
	//Runs a simulation with the given parameters
	public void simulate(Snapshot snapshot, long toTime, CircularBuffer<Snapshot> historyParam){
		//Make copies of the data as it will be changing outside of the simulator
		snapshot = new Snapshot(snapshot);
		CircularBuffer<Snapshot> history = new CircularBuffer<Snapshot>(historyParam.size());
		
		int inc = 0;
		while(inc < historyParam.count()){
			history.push(new Snapshot(historyParam.peek(inc)));
			inc++;
		}
		
		//Prune out old data
		while(history.peek() != null && history.peek().getTimestamp() < snapshot.getTimestamp()){
			history.pop();
		}
		
		thread = new SimulationThread(snapshot, toTime, history, rules);
		thread.start();
	}
	
	public boolean isRunning(){
		if(thread == null) return false;
		
		return thread.isAlive();
	}
	
	public Snapshot getResult(){
		if(thread == null && result == null) return null;
		if(!thread.isAlive() && result == null){
			result = thread.getResult();
			thread = null;
		}
		
		return result;
	}
	
	public SimulationRule addRule(SimulationRule rule){
		synchronized(lock){
			rules.add(rule);
			return rule;
		}
	}
	
	
}
