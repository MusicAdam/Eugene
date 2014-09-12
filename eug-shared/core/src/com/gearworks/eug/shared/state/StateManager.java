package com.gearworks.eug.shared.state;

/*
 * State machine implementation
 */
public class StateManager {
	protected State state;
	
	//Compares state id's
	public static boolean statesEqual(State state, State cState){
		if(state == null || cState == null) return false;
		if(state.getId() == cState.getId()){
			return true;
		}
		
		return false;
	}
	
	public StateManager(){}
	
	public void update(){
		if(state != null)
			state.update();
	}
	
	public void render(){
		if(state != null)
			state.render();
	}
	
	public boolean setState(State toState){
		//Don't change states if they are the same
		if(statesEqual(state, toState)) return false;
		
		if(state == null && toState.canEnterState()){
			state = toState;
			state.onEnter();
		}else if(state != null && state.canExitState() && toState.canEnterState()){
			state.onExit();
			state = toState;
			state.onEnter();
		}else{
			return false;
		}
		
		return true;
	}
	
	public State state(){ return state; }
	
}
