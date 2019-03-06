package com.hkamran.hoones.server.dtos.mappers;

import org.json.JSONObject;

import com.hkamran.hoones.server.dos.Keys;
import com.hkamran.hoones.server.dos.Player;
import com.hkamran.hoones.server.dos.State;
import com.hkamran.hoones.server.dos.mappers.KeysMapper;
import com.hkamran.hoones.server.dos.mappers.PlayerMapper;
import com.hkamran.hoones.server.dos.mappers.StateMapper;
import com.hkamran.hoones.server.dtos.Packet;

public class PacketMapper {

	private static final String TYPE = "type";
	private static final String DATA = "data";

	public static JSONObject toJSON(Packet packet) {
		JSONObject json = new JSONObject();
		json.put(TYPE, packet.type);
		
		if (packet.data instanceof Keys) {
			Keys keys = (Keys) packet.data;
			json.put(DATA, KeysMapper.toJSONObject(keys));
		} else if (packet.data instanceof Player) {
			Player player = (Player) packet.data;
			json.put(DATA, PlayerMapper.toJSONObject(player));
		} else if (packet.data instanceof State) {
			State state = (State) packet.data;
			json.put(DATA, StateMapper.toJSONObject(state));
		}
		
		return json;
	}
	
	public static Packet toPacket(String str) {
		JSONObject json = new JSONObject(str);
		
		Integer type = json.getInt(TYPE);
		Object result = null;
		
		
		JSONObject jsonObj = json.getJSONObject(DATA);
		if (type == Packet.Type.PLAYER_KEYS) {
			Keys keyStroke = KeysMapper.toKeys(jsonObj);
			result = keyStroke;
		} else if (type == Packet.Type.PLAYER_SENDSTATE) {
			State state = StateMapper.toState(jsonObj);
			result = state;
		} else if (type == Packet.Type.PLAYER_WAITING) {
		} else if (type == Packet.Type.PLAYER_SYNC) {
		} else {
			throw new RuntimeException("Received unknown payload type " + type);
		}
		
		Packet payload = new Packet(type, result);
		
		return payload;
	}
	
}
