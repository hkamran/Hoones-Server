package com.hkamran.hoones.server.dtos.factories;

import com.hkamran.hoones.server.dtos.Payload;
import com.hkamran.hoones.server.dtos.Payload.Type;

public class PayloadFactory {
	
	public static Payload STOP() {
		Payload payload = new Payload(Type.SERVER_STOP);
		return payload;
	}
	
	public static Payload FULL() {
		Payload payload = new Payload(Type.SERVER_FULL);
		return payload;		
	}
	
	public static Payload PLAY() {
		Payload payload = new Payload(Type.SERVER_PLAY);
		return payload;
	}
	
	public static Payload DESTROYED() {
		Payload payload = new Payload(Type.SERVER_DESTROYED);
		return payload;
	}
	
	public static Payload GETSTATE() {
		Payload payload = new Payload(Type.SERVER_GETSTATE);
		return payload;
	}

}
