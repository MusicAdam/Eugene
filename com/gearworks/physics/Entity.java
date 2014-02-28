package com.gearworks.physics;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

public abstract class Entity implements Disposable, ContactListener{
	private Model model;
	private Body body;
	private BodyDef.BodyType type;
	protected ModelInstance modelInstance;
	public World world;
	
	public Entity(World world, BodyDef.BodyType type){
		this.type = type;
		this.world = world;
	}
	
	public void setModel(Model m){ model = m; }
	public Model getModel(){ return model; }
	public void setBody(Body b){body = b;}
	public Body getBody(){return body;}
	public ModelInstance getInstance(){ return modelInstance; }
	public abstract void instantiate();
	public void setPosition(Vector2 pos){
		body.setTransform(pos, body.getAngle());
		updateModelTransform();
	}
	
	public void setPosition(float x, float y){
		body.setTransform(x, y, body.getAngle());
		updateModelTransform();
	}
	
	public Vector3 getPosition(){
		Vector3 p = new Vector3();
		if(modelInstance != null){
			modelInstance.transform.getTranslation(p);
		}
		
		return p;
	}
	
	//Updates the model's transform to math that of the physics body
	public void updateModelTransform(){
		//Create translation and rotation from physics body
		Vector3 translation = new Vector3(body.getTransform().getPosition().x, body.getTransform().getPosition().y, 0f);
		Quaternion rotation	= new Quaternion(Vector3.Z, MathUtils.radiansToDegrees * body.getAngle());	
		//Set new translation rotation and scale in the model's transform
		modelInstance.transform.set(translation, rotation, new Vector3(1f, 1f, 1f));
	}
	
	public void update(){
		if(type != BodyDef.BodyType.StaticBody && body != null && modelInstance != null){
			updateModelTransform();
		}
	}
	
	@Override
	public void dispose() {
		if(model != null){
			model.dispose();
			model = null;
		}
		if(modelInstance != null){
			//batchController.remove(modelInstance)
			modelInstance = null;
		}
		if(body != null){
			body.getWorld().destroyBody(body);
			body = null;
		}
	}

	public boolean isInstatiated() {
		return (modelInstance != null && body != null);
	}
	
}
