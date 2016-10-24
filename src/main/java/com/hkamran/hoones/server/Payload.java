package com.hkamran.hoones.server;

import org.json.JSONObject;

import com.hkamran.hoones.server.payloads.Controller;
import com.hkamran.hoones.server.payloads.Player;
import com.hkamran.hoones.server.payloads.State;

public class Payload {

	public Type type;
	public Object payload;
	
	public static enum Type {
		SYNCHRONIZE, CONTROLLER, PLAYER, STOP, PLAY, SLOWDOWN
	}
	
	public static Payload STOP() {
		Payload payload = new Payload();
		payload.type = Type.STOP;
		return payload;
	}
	
	public static Payload PLAY() {
		Payload payload = new Payload();
		payload.type = Type.PLAY;
		return payload;
	}
	
	public static Payload SLOWDOWN() {
		Payload payload = new Payload();
		payload.type = Type.PLAY;
		return payload;
	}	
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("type", type.toString());
		
		if (payload instanceof Controller) {
			json.put("payload", ((Controller) payload).toJSON());
		} else if (payload instanceof Player) {
			json.put("payload", ((Player) payload).toJSON());
		} else if (payload instanceof State) {
			json.put("payload", ((State) payload).toJSON());
		}
		
		return json;
	}
	
	public static Payload parseJSON(String str) {
		JSONObject json = new JSONObject(str);
		
		Type type = Type.valueOf(json.getString("type"));
		Object result = null;
		
		
		JSONObject jsonObj = json.getJSONObject("payload");
		if (type == Type.CONTROLLER) {
			Controller keyStroke = Controller.parseJSON(jsonObj);
			result = keyStroke;
		} else if (type == Type.PLAYER) {
			Player player = Player.parseJSON(jsonObj);
			result = player;
		} else if (type == Type.SYNCHRONIZE) {
			State state = State.parseJSON(jsonObj);
			result = state;
		}
		
		Payload payload = new Payload();
		payload.type = type;
		payload.payload = result;
		
		return payload;
	}

}
