package com.gearworks.eug.shared;

import java.util.List;

import com.gearworks.eug.shared.input.PlayerInput;
import com.gearworks.eug.shared.state.Snapshot;
import com.gearworks.eug.shared.utils.CircularBuffer;
import com.gearworks.eug.shared.utils.Utils;

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
			try{
				synchronized(lock){
					//Run simulation
					running = true;
	
					long simTime = snapshot.getTimestamp();
					float step = SharedVars.STEP;
										
					world.snapToSnapshot(snapshot);
					
					while(simTime <= toTime && doSim){	
						if((float)(toTime - simTime) < SharedVars.STEP){
							step = (float)(toTime - simTime);
						}
						
						synchronized(world.historyLock){
							int inc = 0;
							while(!history.isEmpty() && history.peek(inc).getTimestamp() <= simTime){
								Snapshot snap = history.peek(inc);
								for(PlayerInput input : snap.getInput()){
									Eug.GetInputMapper().get(input.getEvent()).resolve(world, input, step);
								}
								inc++;
							}
						}
		
						world.update(step);
					}
					
					snapshot = world.generateSnapshot();
					snapshot.setTimestamp(toTime);
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
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
	private long startTime;

	public Simulator(){	
	}
	
	//Runs a simulation with the given parameters
	public Snapshot simulate(Snapshot snapshot, long toTime, CircularBuffer<Snapshot> history){
		world = new World("Sim world", true); //Initialize a world
		
		long simTime = snapshot.getTimestamp();
		float step = SharedVars.STEP;
		
		for(PlayerState state : snapshot.getPlayers()){
			if(	state.isDisconnected() )
				continue;		
		}
		
		
		world.snapToSnapshot(snapshot);
		
		while(simTime <= toTime){	
			if((float)(toTime - simTime) < SharedVars.STEP){
				step = (float)(toTime - simTime);
			}
			
			synchronized(world.historyLock){
				while(!history.isEmpty() && history.peek().getTimestamp() <= simTime){
					Snapshot snap = history.pop();
					for(PlayerInput input : snap.getInput()){
						Eug.GetInputMapper().get(input.getEvent()).resolve(world, input, step);
					}
				}
			}

			world.update(step);			

			simTime += (long)(SharedVars.STEP * 1000);
		}
		
		snapshot = world.generateSnapshot();
		snapshot.setTimestamp(toTime);
		
		return snapshot;
	}
	
	public long getUptime(){
		if(!isRunning()) return 0;
		
		return (Utils.generateTimeStamp() - startTime);
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
