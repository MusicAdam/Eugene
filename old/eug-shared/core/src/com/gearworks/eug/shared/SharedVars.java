package com.gearworks.eug.shared;

import com.badlogic.gdx.math.Vector2;
import com.gearworks.eug.shared.utils.Utils;

public class SharedVars {	
	public final static int TCP_PORT = 54555;
	public final static int UDP_PORT = 54777;
	public static final float 	WORLD_TO_BOX = 0.01f;
	public static final float   BOX_TO_WORLD = 100f;
	public static final float STEP = 1 / 60f;
	public static final int VELOCITY_ITERATIONS = 6;
	public static final int POSITION_ITERATIONS = 8;
	public static final boolean DO_SLEEP = true;
	public static final Vector2 GRAVITY = new Vector2(0, -9.8f);
	public static final int WRITE_BUFFER_SIZE = 32768;
	public static final int OBJECT_BUFFER_SIZE = 4096;
	
	public static final boolean DEBUG_PHYSICS = true; //turns on box2ddbgrenderer
	public static final boolean DEBUG_ENTITIES = false; //turns on aabb rendering
	public static final boolean DEBUG_LOG = false;	//Enables log file writing
	public static final Debug.Reporting DEBUG_LEVEL = Debug.Reporting.Verbose;
	public static final int HISTORY_SIZE = 1000;
	
	public static final float POSITION_TOLERANCE = .1f; //The distance the client can be out of sync before it forces correction
	public static final float ROTATION_TOLERANCE = Utils.degToRad(5); //The angle the client can be out of sync before it forces correction
	public static final float FORCE_POSITION_SNAP_LIMIT = 5.0f; //Distance in meters client can be out of sync before it snaps it to the correct place
	public static final float FORCE_ROTATION_SNAP_LIMIT = Utils.degToRad(10); //Angle in rads client can be out of sync before it snaps it to the correct place
	public static final long TIMESTAMP_EPSILON = 5; //Used when comparing timestamps. Measured in miliseconds.
}
