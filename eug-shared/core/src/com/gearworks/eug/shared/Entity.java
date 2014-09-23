package com.gearworks.eug.shared;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.utils.Array;
import com.gearworks.eug.shared.entities.DiskEntity;
import com.gearworks.eug.shared.entities.LevelBoundsEntity;
import com.gearworks.eug.shared.exceptions.EntityBuildException;
import com.gearworks.eug.shared.exceptions.EntityUpdateException;
import com.gearworks.eug.shared.state.BodyState;
import com.gearworks.eug.shared.state.EntityState;
import com.gearworks.eug.shared.utils.Utils;

public class Entity {	
	public enum Type {
		BaseEntity,
		DiskEntity,
		LevelBoundsEntity
	}
	
	public static final int COLLISION_NONE = 0;
	public static final int COLLISION_UNIT = 1;
	public static final int COLLISION_WALL = 2;
	
	protected int 		id;
	protected Player player;
	protected boolean 	bodyIsDirty = false; //When true the AABB and the size of the body will be calculated
	protected Rectangle aabb;
	protected Sprite 	sprite;
	protected String 	spriteResource; //Name of the file from which the sprite's texture was loaded
	protected float 	selectPadding = 2;
	private boolean 	selectable = true;
	private boolean 	selected;
	private Body body;
	private Type type;
	private boolean isNew = true;
	
	public Entity(int id, Player player){
		this.player = player;
		this.id = id;

		type = Eug.GetEntityType(this);
	}
	
	public Player getPlayer(){
		return player;
	}
	
	public void setPlayer(Player player){
		this.player = player;
	}
	
	public int getInstanceId(){
		return player.getInstanceId();
	}
	
	public Vector2 position(){
		return body.getPosition().scl(SharedVars.BOX_TO_WORLD);
	}
	
	public void position(Vector2 p){ 
		body.setTransform(p.scl(SharedVars.WORLD_TO_BOX), body.getAngle()); 
		body.setAwake(true); //This is to register collisions
	}
	
	public float rotation(){
		return Utils.radToDeg(body.getAngle());
	}
	
	public void rotation(float angle){
		body.setTransform(body.getPosition(), Utils.degToRad(angle));
		body.setAwake(true); //This is to register collisiosn
		bodyIsDirty = true;
	}
	
	public void position(float x, float y){
		position(new Vector2(x, y));
	}
	
	public Rectangle getBounds(){
		cleanBody();
		return aabb;
	}
	
	public void render(SpriteBatch batch, ShapeRenderer r){		
		if(SharedVars.DEBUG_ENTITIES){
			Utils.drawRect(r, Color.GREEN, getBounds().x, getBounds().y, getBounds().width, getBounds().height);
		}
		
		if(selected()){
			float aabbMinPaddedX = getBounds().x - selectPadding;
			float aabbMinPaddedY = getBounds().y - selectPadding;
			float aabbPaddedW =   getBounds().width + selectPadding * 2;
			float aabbPaddedH =  getBounds().height + selectPadding * 2;
			//Draw a box around the entity based on the aabb. Add padding to make the box bigger than the entity itself.
			Utils.drawRect(r, Color.GREEN, aabbMinPaddedX, aabbMinPaddedY, aabbPaddedW, aabbPaddedH);			
		}
	}
	
	public void update(){		
		if(body != null && body.isActive())
			bodyIsDirty = true;
		
		followPhysicsBody();
	}
	
	public void dispose()
	{
		if(body() == null) return;
		
		body().getWorld().destroyBody(body());
	}
	
	protected void followPhysicsBody(){
		if(sprite == null) return;
		if(body() == null) return; 
		
		Vector2 position = position();
		sprite.setPosition(position.x - sprite.getWidth()/2, position.y - sprite.getHeight()/2);
		sprite.setRotation(rotation());
	}
	
	public void beginContactWith(Fixture myFix, Fixture otherFix, Contact contact){}
	public void presolveContactWith(Fixture myFix, Fixture otherFix, Contact contact){}
	public void postsolveContactWith(Fixture myFix, Fixture otherFix, Contact contact){}
	
	//Spawn is responsible for creating the physics body and sprite associated with this entity
	public void spawn(){}
	
	public Vector2 size(){ 
		if(sprite == null) return new Vector2();
		return new Vector2(sprite.getWidth(), sprite.getHeight()); 
	}
	public float width(){ 
		if(sprite == null) return 0;
		
		return sprite.getWidth(); 
	}
	
	public float height(){ 
		if(sprite == null) return 0;
		
		return sprite.getHeight(); 
	}
	public void selectable(boolean s){ selectable = s; }
	public boolean selectable(){ return selectable; }
	public void selected(boolean s){ selected = s; }
	public boolean selected(){ return selected; }
	public Body body(){ return body; }
	public void body(Body b){ body = b; bodyIsDirty = true; }
	public Type getType(){ return type; }
	
	//Recalculates AABB and size
	protected void cleanBody(){
		if(!bodyIsDirty) return;
		
		aabb = new Rectangle();
		for(Fixture fix : body().getFixtureList()){
			Shape shape = fix.getShape();
			float rotation = fix.getBody().getTransform().getRotation();
			Vector2 min = new Vector2();
			Vector2 max = new Vector2();
		
			if(shape instanceof CircleShape){
				min = new Vector2(-shape.getRadius(), -shape.getRadius());
				max = new Vector2(shape.getRadius(), shape.getRadius());
			//TODO: The method for determining the min/max of PolygonShape is the same as ChainShape, should try to find a way to combine the two
			}else if(shape instanceof PolygonShape){
				PolygonShape pgon = (PolygonShape)shape;
				for(int i = 0; i < pgon.getVertexCount(); i++){
					Vector2 vert = new Vector2();
					pgon.getVertex(i, vert);
		
					//Transform to local rotation
					//vert.x = vert.x;
					//vert.y = vert.y;
					vert.rotateRad(rotation);
					
					if(vert.x < min.x){
						min.x = vert.x;
					}else if(vert.x > max.x){
						max.x = vert.x;
					}
		
					if(vert.y < min.y){
						min.y = vert.y;
					}else if(vert.y > max.y){
						max.y = vert.y;
					}
				}
				
				
			}else if(shape instanceof ChainShape){
				ChainShape chain = (ChainShape)shape;
				for(int i = 0; i < chain.getVertexCount(); i++){
					Vector2 vert = new Vector2();
					chain.getVertex(i, vert);
		
					//Transform to world rotation
					vert.rotateRad(rotation);
		
					if(vert.x < min.x){
						min.x = vert.x;
					}else if(vert.x > max.x){
						max.x = vert.x;
					}
		
					if(vert.y < min.y){
						min.y = vert.y;
					}else if(vert.y > max.y){
						max.y = vert.y;
					}
				}
			}else if(shape instanceof EdgeShape){
				EdgeShape edge = (EdgeShape)shape;
				Vector2 vert1 = new Vector2();
				Vector2 vert2 = new Vector2();
		
		
				//Transform to world rotation
				vert1.rotateRad(rotation);
				vert2.rotateRad(rotation);
		
				edge.getVertex1(vert1);
				edge.getVertex2(vert2);
		
				if(vert1.x < vert2.x){
					min.x = vert1.x;
					max.x = vert2.x;
				}else{
					min.x = vert2.x;
					max.x = vert1.x;
				}
		
				if(vert1.y < vert2.y){
					min.y = vert1.y;
					max.y = vert2.y;
				}else{
					min.y = vert2.y;
					max.y = vert1.y;
				}
			}
			
			//Calc aabb
			BoundingBox thisBox = new BoundingBox(new Vector3(min.x, min.y, 0f), new Vector3(max.x, max.y, 0f));
		
			if(thisBox.min.x < aabb.x){
				aabb.x = thisBox.min.x * SharedVars.BOX_TO_WORLD;
			}
		
			if(thisBox.min.y < aabb.y){
				aabb.y = thisBox.min.y * SharedVars.BOX_TO_WORLD;
			}
		
			if(thisBox.max.x - thisBox.min.x > aabb.width){
				aabb.width = (thisBox.max.x - thisBox.min.x) * SharedVars.BOX_TO_WORLD;
			}
		
			if(thisBox.max.y - thisBox.min.y > aabb.height){
				aabb.height = (thisBox.max.y - thisBox.min.y) * SharedVars.BOX_TO_WORLD;
			}
		}
		aabb.x += position().x;
		aabb.y += position().y;
		bodyIsDirty = false;
	}
	
	public EntityState getState(){
		int status = EntityState.UPDATE;
		if(isNew){
			isNew = false;
			status = EntityState.CREATE;
		}
		
		return new EntityState(this, status);
	}
	
	public void setId(int id){ this.id = id; }
	public int getId(){ return id; }
	public String getSpriteResource(){ return spriteResource; }
	public void setSprite(String resource){
		if(resource != null){
			sprite = new Sprite(new Texture(Gdx.files.internal(resource)));
			sprite.setOriginCenter();
			spriteResource = resource;
		}else{
			sprite = null;
			spriteResource = null;			
		}
	}
	public void setType(Entity.Type type){ this.type = type; }
	public boolean isNew(){ return isNew; }
}
