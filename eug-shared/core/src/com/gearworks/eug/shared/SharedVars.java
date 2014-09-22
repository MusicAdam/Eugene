package com.gearworks.eug.shared;

import com.badlogic.gdx.math.Vector2;

public class SharedVars {
	public final static int TCP_PORT = 54555;
	public final static int UDP_PORT = 54777;
	public static final float 	WORLD_TO_BOX = 0.01f;
	public static final float   BOX_TO_WORLD = 100f;
	public static final float STEP = 1 / 60f;
	public static final int VELOCITY_ITERATIONS = 6;
	public static final int POSITION_ITERATIONS = 8;
	public static final boolean DO_SLEEP = true;
	public static final Vector2 GRAVITY = new Vector2();
	public static final int WRITE_BUFFER_SIZE = 32768;
	public static final int OBJECT_BUFFER_SIZE = 4096;
	
	public static final boolean DEBUG_VERBOSE = true; //Turns on console printing
	public static final boolean DEBUG_PHYSICS = true; //turns on box2ddbgrenderer
	public static final boolean DEBUG_ENTITIES = false; //turns on aabb rendering
	public static final int HISTORY_SIZE = 1000;
}
