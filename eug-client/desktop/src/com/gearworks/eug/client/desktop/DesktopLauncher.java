package com.gearworks.eug.client.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.Debug;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.utils.CircularBuffer;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Eug.Set(new EugClient());
		
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(Eug.Get(), config);
	}
}
