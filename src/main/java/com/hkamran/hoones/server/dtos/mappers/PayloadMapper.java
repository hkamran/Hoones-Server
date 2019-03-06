package com.hkamran.hoones.server.dtos.mappers;

import org.json.JSONObject;

import com.hkamran.hoones.server.dos.Keys;
import com.hkamran.hoones.server.dos.Player;
import com.hkamran.hoones.server.dos.State;
import com.hkamran.hoones.server.dos.mappers.KeysMapper;
import com.hkamran.hoones.server.dos.mappers.PlayerMapper;
import com.hkamran.hoones.server.dos.mappers.StateMapper;
import com.hkamran.hoones.server.dtos.Payload;

public class PayloadMapper {

	private static final String TYPE = "type";
	private static final String DATA = "data";

	public static JSONObject toJSON(Payload payload) {
		JSONObject json = new JSONObject();
		json.put(TYPE, payload.type);
		
		if (payload.data instanceof Keys) {
			Keys keys = (Keys) payload.data;
			json.put(DATA, KeysMapper.toJSONObject(keys));
		} else if (payload.data instanceof Player) {
			Player player = (Player) payload.data;
			json.put(DATA, PlayerMapper.toJSONObject(player));
		} else if (payload.data instanceof State) {
			State state = (State) payload.data;
			json.put(DATA, StateMapper.toJSONObject(state));
		}
		
		return json;
	}
	
	public static Payload toPacket(String payloadStr) {
		JSONObject json = new JSONObject(payloadStr);
		
		Integer type = json.getInt(TYPE);
		Object result = null;
		
		
		JSONObject jsonObj = json.getJSONObject(DATA);
		if (type == Payload.Type.PLAYER_KEYS) {
			Keys keyStroke = KeysMapper.toKeys(jsonObj);
			result = keyStroke;
		} else if (type == Payload.Type.PLAYER_SENDSTATE) {
			State state = StateMapper.toState(jsonObj);
			result = state;
		} else if (type == Payload.Type.PLAYER_WAITING) {
		} else if (type == Payload.Type.PLAYER_SYNC) {
		} else {
			throw new RuntimeException("Received unknown payload " + payloadStr);
		}
		
		Payload payload = new Payload(type, result);
		
		return payload;
	}
	
}
