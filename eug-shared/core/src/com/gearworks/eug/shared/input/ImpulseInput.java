package com.gearworks.eug.shared.input;

import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.Player;
import com.gearworks.eug.shared.input.ClientInput.Event;

public class ImpulseInput extends ClientInput {

	public ImpulseInput(){
		super();
		inheritanceLevel = 1;
	}
	
	public ImpulseInput(int id, Event event, Vector2 dir, int key) {
		super(id, event, dir, key);
		inheritanceLevel = 1;
	}

	@Override
	public void resolve(Player pl) {
		pl.processInput(this);
	}
}
