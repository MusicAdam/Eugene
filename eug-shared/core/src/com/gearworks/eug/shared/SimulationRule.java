package com.gearworks.eug.shared;

import com.gearworks.eug.shared.state.Snapshot;

//A simulation rule is the logic which controls how the simulation proceeds
public abstract class SimulationRule {
	/*
	 * Apply rules to the snapshot which represents the current state of the simulation.
	 */
	public abstract void apply(Snapshot snapshot, long step);
}
