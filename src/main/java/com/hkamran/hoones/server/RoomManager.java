package com.hkamran.hoones.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.Session;

public class RoomManager {

	private static ConcurrentHashMap<Integer, Room> rooms = new ConcurrentHashMap<Integer, Room>();
	
	public static Set<Integer> getIds() {
		return rooms.keySet();
	}
	
	public static Room getRoom(Integer id) {
		return rooms.get(id);
	}
	
	public static void addRoom(Integer id, Room room) {
		rooms.put(id, room);
	}
	
	public static List<Room> getRooms() {
		List<Room> list = new ArrayList<Room>();
		for (Integer id : rooms.keySet()) {
			list.add(rooms.get(id));
		}
		return list;
	}
	
	public static Room getRoom(Session session) {
		List<Room> rooms = getRooms();
		for (Room room : rooms) {
			if (room.getPlayer(session) != null) {
				return room;
			}
		}
		return null;
	}

	public static void removeRoom(Integer id) {
		rooms.remove(id);
	}
}
