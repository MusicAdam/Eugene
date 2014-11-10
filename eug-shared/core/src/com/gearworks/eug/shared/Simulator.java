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
		World world;
		Snapshot snapshot;
		CircularBuffer<Snapshot> history;
		long toTime;
		boolean running = false;
		
		public SimulationThread(World world, Snapshot snapshot, long toTime, CircularBuffer<Snapshot> history){
			this.snapshot = snapshot;
			this.toTime = toTime;
			this.world = world;
			this.history = history;
		}
		
		@Override
		public void run(){
			long simTime = snapshot.getTimestamp(); //Start simulation at the time of the snapshot
			
			long step = (long)(SharedVars.STEP * 1000); //Convert STEP to miliseconds 		

			synchronized(lock){
				//Run simulation
				running = true;

				int inc = 0;
				world.snapToSnapshot(snapshot);
				
				while(simTime < toTime){
					//If there isn't a full step left, calculate partial step
					if(simTime + step > toTime)
						step = toTime - simTime;
					
					if(!history.isEmpty()){					
						while(history.peek(inc).getTimestamp() < simTime){
							while(!history.peek(inc).getClientInput().isEmpty()){
								history.peek(inc).getClientInput().pop().resolve(world);
							}
						}
					}
	
					world.update(step);
					
					simTime += step; //Step time forward
					snapshot = world.generateSnapshot(-1);
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
	
	public Object lock = new Object();
	private SimulationThread thread;
	private World world;

	public Simulator(){	
		world = new World(-1);
		world.simulator = true;
	}
	
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
		
		thread = new SimulationThread(world, snapshot, toTime, history);
		thread.start();
	}
	
	public boolean isRunning(){
		if(thread == null) return false;
		
		return thread.isAlive();
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
