package com.hkamran.hoones.server.servers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONObject;

@Path("/")
public class GameManager {

	private final static Logger log = LogManager.getLogger(GameManager.class);
	
	private static final long MAXAGE = 10000;

	private static Map<Integer, Room> rooms = new HashMap<Integer, Room>();
	private static List<Integer> availablePorts = new ArrayList<Integer>();

	public static Room getRoom(Integer id) {
		return rooms.get(id);
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

	
	@GET
	@Path("/createRoom")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createRoom() {
		clearOldServers();
		
		if (availablePorts.size() == 0) {
			JSONObject json = new JSONObject();
			json.put("error", "no available rooms");	
			return Response.status(200).type(MediaType.APPLICATION_JSON)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.entity(json.toString(2)).build();
		}
		
		int port = availablePorts.remove(availablePorts.size() - 1);
		
		try {
			Room room = new Room(port);
			rooms.put(room.id, room);
			JSONObject json = new JSONObject();
			json.put("id", room.id);
			json.put("port", port);
			return Response.status(200).type(MediaType.APPLICATION_JSON)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.entity(json.toString(2)).build();
			
		} catch (Exception e) {
			
			availablePorts.add(port);
			JSONObject json = new JSONObject();
			json.put("error", "cannot start server");	
			return Response.status(200).type(MediaType.APPLICATION_JSON)
					.header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
					.entity(json.toString(2)).build();
		}
	}

	private static void clearOldServers() {
		//If we can still make servers
		if (availablePorts.size() != 0) {
			return;
		}
		
		// Remove old game servers
		List<Integer> oldServers = new ArrayList<Integer>();
		long currentMillis = System.currentTimeMillis();
		for (Integer id : rooms.keySet()) {
			Room room = rooms.get(id);
			if (currentMillis - room.lastMessage > MAXAGE) {
				room.destroy();
				oldServers.add(id);
			}
		}

		for (Integer id : oldServers) {
			Room room = rooms.get(id);
			availablePorts.add(room.port);
			rooms.remove(id);
		}
	}
	
	
	public static void addAvailablePorts(int... ports) {
		for (int port : ports) {
			GameManager.availablePorts.add(port);
		}
	}	

	public static void start(Integer port) {

		log.info("Game Manager started at " + port);
		
		// Set Jersey Classes
		ResourceConfig config = new ResourceConfig();
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(GameManager.class);
		config.registerClasses(s);

		Server server = new Server(port);

		// Create Jersey Handler (function is to handle REST)
		ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(config));
		ServletContextHandler restHandler = new ServletContextHandler(server, "/rest");
		restHandler.addServlet(jerseyServlet, "/*");

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {restHandler });

		server.setHandler(handlers);

		try {
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			server.destroy();
		}
	}
}
