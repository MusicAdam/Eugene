package com.gearworks.testbed.client;
import org.lwjgl.Sys;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import com.gearworks.eug.client.EugClient;
import com.gearworks.eug.shared.EntityManager;
import com.gearworks.eug.shared.Eug;
import com.gearworks.eug.shared.NetworkedEntity;
import com.gearworks.eug.shared.SharedVars;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityNotRegisteredException;
import com.gearworks.shared.Initializer;
import com.gearworks.testbed.shared.entities.Entity;

import java.nio.ByteBuffer;
 





import java.util.Iterator;
import java.util.Map.Entry;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;


public class Game {

    
    int WIDTH = 300;
    int HEIGHT = 300;
	
	//Eugene stuff
	EugClient client;
	
	//LWJGL
	private GLFWErrorCallback errorCallback;
	private GLFWKeyCallback keyCallback;
	
	private long window;
	
	public void start(){
		System.out.println("LWJGL version " + Sys.getVersion());
		
		try{
			init();
			initEug();
			loop();
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			glfwTerminate();
			errorCallback.release();
		}
	}
	
	private void init(){
		glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));
		
		if(glfwInit() != GL11.GL_TRUE)
			throw new IllegalStateException("Unable to initialze GLFW");
		
		glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable
        
        window = glfwCreateWindow(WIDTH, HEIGHT, "EUG Testbed", NULL, NULL);
        if(window == NULL)
        	throw new RuntimeException("Failed to create the GLFW window");
        
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback(){
        	@Override
        	public void invoke(long window, int key, int scancode, int action, int mods){
                if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                    glfwSetWindowShouldClose(window, GL_TRUE); // We will detect this in our rendering loop
        	}
        });
 
        // Get the resolution of the primary monitor
        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Center our window
        glfwSetWindowPos(
            window,
            (GLFWvidmode.width(vidmode) - WIDTH) / 2,
            (GLFWvidmode.height(vidmode) - HEIGHT) / 2
        );
 
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);
       
        // Make the window visible
        glfwShowWindow(window);
	}
	
	private void initEug(){
		client = (EugClient)Eug.Set(new EugClient());
		Initializer.RegisterClasses();
		client.create();

		Entity.RegisterEntities();
	}
	
	private void loop(){
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the ContextCapabilities instance and makes the OpenGL
        // bindings available for use.
        GLContext.createFromCurrent();
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, WIDTH, 0, HEIGHT, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        // Set the clear color
        glClearColor(0.3f, 0.3f, 0.4f, 0.0f);
        
 
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( glfwWindowShouldClose(window) == GL_FALSE ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            Iterator<Entry<Short, NetworkedEntity>> iterator = client.getEntities().entrySet().iterator();
            
            while(iterator.hasNext()){
            	NetworkedEntity ent = iterator.next().getValue();
            	ent.render();
            }
            
            glfwSwapBuffers(window); // swap the color buffers
       
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
            
            client.update(SharedVars.STEP);
            
        }
        
		client.dispose();
		
	}
	
	public static void main(String[] args){
		new Game().start();
	}
}
