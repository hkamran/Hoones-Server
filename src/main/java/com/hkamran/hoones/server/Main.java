package com.hkamran.hoones.server;

public class Main {
	
	public static void main(String[] args) throws Exception {
		RoomController.addAvailablePorts(8091, 8092);
		RoomController.start(8090);
	}
	
}
