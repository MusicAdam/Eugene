package com.gearworks.eug.shared;

import java.util.List;

import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.CircularBuffer;

/*
 * The simulator takes a snapshot and a history of snapshots and simulates the snapshot forward in time.
 * 
 * Rules are provided to the simulator through callbacks that can be added by the user.
 */
public class Simulator {
	private class SimulationThread extends Thread {
		World world;
		Snapshot snapshot;
		CircularBuffer<Snapshot> history;
		long toTime;
		boolean running = false;
		private volatile boolean doSim = true;
		
		public SimulationThread(World world, Snapshot snapshot, long toTime, CircularBuffer<Snapshot> history){
			this.snapshot = snapshot;
			this.toTime = toTime;
			this.world = world;
			this.history = history;
			this.setName("SimulationThread-" + world.getName());
		}
		
		@Override
		public void run(){
			long simTime = snapshot.getTimestamp(); //Start simulation at the time of the snapshot
			
			long step = (long)(SharedVars.STEP * 1000); //Convert STEP to miliseconds 		
			
			float predicted = (float)(toTime - simTime) / (float)step;
			
			synchronized(lock){
				//Run simulation
				running = true;

				world.snapToSnapshot(snapshot);
				
				while(simTime < toTime && doSim){
					//If there isn't a full step left, calculate partial step
					if(simTime + step > toTime)
						step = toTime - simTime;
					
					synchronized(world.historyLock){
						int inc = 0;
						try{
						while(!history.isEmpty() && history.peek(inc).getTimestamp() <= simTime){
							Snapshot snap = history.peek(inc);
							for(PlayerInput input : snap.getInput()){
								Eug.GetInputMapper().get(input.getEvent()).resolve(world, input);
							}
							inc++;
						}
						}catch (Exception e){
							throw e;
						}
					}
	
					world.update(SharedVars.STEP);
					
					simTime += step; //Step time forward
				}
				
				snapshot = world.generateSnapshot();
				snapshot.setTimestamp(simTime);		
				
				running = false;
			}
		}
		
		public void terminate(){
			doSim = false;
		}
		
		public Snapshot getResult(){
			if(running) return null;
			return snapshot;
		}
	}
	
	public Object lock = new Object();
	private SimulationThread thread;
	private World world;

	public Simulator(){	
		world = new World("Sim world", -1, true);
	}
	
	//Runs a simulation with the given parameters
	public void simulate(Snapshot snapshot, long toTime, CircularBuffer<Snapshot> history){
		//Make copies of the data as it will be changing outside of the simulator
		snapshot = new Snapshot(snapshot);
		
		thread = new SimulationThread(world, snapshot, toTime, history);
		thread.start();
	}
	
	public boolean isRunning(){
		if(thread == null) return false;
		
		return thread.isAlive();
	}
	
	public void terminateSimulation(){
		if(thread == null) return;
		if(!thread.isAlive()) return;
		
		thread.terminate();
	}
	
	public Snapshot getResult(){		
		if(isRunning()) return null;
		if(thread == null) return null;
		
		Snapshot result = thread.getResult();
		if(result != null)
			thread = null;
		
		return result;
	}	
}
