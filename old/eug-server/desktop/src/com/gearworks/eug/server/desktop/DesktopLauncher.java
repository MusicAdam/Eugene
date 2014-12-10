package com.gearworks.eug.server.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.gearworks.eug.server.EugServer;
import com.gearworks.eug.shared.Eug;

public class DesktopLauncher {
	public static void main (String[] arg) {
		Eug.Set(new EugServer());
		
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		new LwjglApplication(Eug.Get(), config);
	}
}
