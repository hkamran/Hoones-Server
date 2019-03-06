package com.hkamran.hoones.server.dtos.factories;

import com.hkamran.hoones.server.dtos.Packet;
import com.hkamran.hoones.server.dtos.Packet.Type;

public class PacketFactory {
	
	public static Packet STOP() {
		Packet payload = new Packet(Type.SERVER_STOP);
		return payload;
	}
	
	public static Packet FULL() {
		Packet payload = new Packet(Type.SERVER_FULL);
		return payload;		
	}
	
	public static Packet PLAY() {
		Packet payload = new Packet(Type.SERVER_PLAY);
		return payload;
	}
	
	public static Packet DESTROYED() {
		Packet payload = new Packet(Type.SERVER_DESTROYED);
		return payload;
	}
	
	public static Packet GETSTATE() {
		Packet payload = new Packet(Type.SERVER_GETSTATE);
		return payload;
	}

}
