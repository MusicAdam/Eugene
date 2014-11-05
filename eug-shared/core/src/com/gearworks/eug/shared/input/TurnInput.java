package com.gearworks.eug.shared.input;

import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.input.ClientInput.Event;

public class TurnInput extends ClientInput {

	public TurnInput(){
		super();
		inheritanceLevel = 1;
	}
	
	public TurnInput(int id, Event event, Vector2 dir, int key) {
		super(id, event, dir, key);
		inheritanceLevel = 1;
	}
}
