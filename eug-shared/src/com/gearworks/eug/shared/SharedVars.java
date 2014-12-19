package com.gearworks.eug.shared;

import com.gearworks.eug.shared.utils.Utils;

public class SharedVars {	
	public final static int TCP_PORT = 54555;
	public final static int UDP_PORT = 54777;
	public static final int WRITE_BUFFER_SIZE = 32768;
	public static final int OBJECT_BUFFER_SIZE = 4096;
	public static final float STEP = 1/60f;
	public static final short MAX_ENTITIES = Short.MAX_VALUE; //This is the maximum possible number of entities, because short is used as the unique ID
	
	public static final boolean DEBUG_ENTITIES = false; //turns on aabb rendering
	public static final boolean DEBUG_LOG = false;	//Enables log file writing
	public static final boolean DEBUG_PRINT_TIME = true; //Prints a timestamp on console output
	public static final Debug.Reporting DEBUG_LEVEL = Debug.Reporting.Warning;
	public static final int HISTORY_SIZE = 1000;
	
	public static final float POSITION_TOLERANCE = .1f; //The distance the client can be out of sync before it forces correction
	public static final float ROTATION_TOLERANCE = Utils.degToRad(5); //The angle the client can be out of sync before it forces correction
	public static final float FORCE_POSITION_SNAP_LIMIT = 5.0f; //Distance in meters client can be out of sync before it snaps it to the correct place
	public static final float FORCE_ROTATION_SNAP_LIMIT = Utils.degToRad(10); //Angle in rads client can be out of sync before it snaps it to the correct place
	public static final long TIMESTAMP_EPSILON = 5; //Used when comparing timestamps. Measured in miliseconds.
}
