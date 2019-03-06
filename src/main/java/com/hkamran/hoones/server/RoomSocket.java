package com.hkamran.hoones.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONException;
import org.json.JSONObject;

import com.hkamran.hoones.server.dos.Player;
import com.hkamran.hoones.server.dtos.Payload;
import com.hkamran.hoones.server.dtos.factories.PayloadFactory;
import com.hkamran.hoones.server.dtos.mappers.PayloadMapper;

@ClientEndpoint
@ServerEndpoint(value = "/{roomnumber}")
public class RoomSocket {

	private final static Logger LOGGER = LogManager.getLogger(RoomSocket.class);

	@OnOpen
	public void OnWebSocketConnect(Session session, @PathParam("roomnumber") final Integer roomnumber) {
		session.setMaxIdleTimeout(600000);

		Room room = RoomManager.getRoom(roomnumber);
		
		if (room == null) {
			//does not exists
			send(session, PayloadFactory.DESTROYED());
			return;
		}
		
		if (room.hasEmptySeat()) {
			Integer id = room.getEmptySeat();
			Player player = room.takeSeat(id, session);
			
			Payload payload = new Payload(Payload.Type.SERVER_PLAYERINFO, player);
			send(room, session, payload);			
		} else {
			send(session, PayloadFactory.FULL());
			return;
		}

		List<Player> players = room.getPlayers();
		for (Player player : players){
			Payload payload = new Payload(Payload.Type.SERVER_PLAYERCONNECTED, player);
			broadcast(payload, room);
		}

		Payload payload = PayloadFactory.STOP();
		broadcast(payload, room);
		
		if (players.size() > 1) {
			synchronize(roomnumber);
		}
	}

	@OnClose
	public void onWebSocketClose(Session session, @PathParam("roomnumber") final Integer roomnumber) {
		Room room = RoomManager.getRoom(roomnumber);
		
		if (room == null) {
			if (session.isOpen()) {
				send(session, PayloadFactory.DESTROYED());
			}
			return;
		}
		
		Player player = room.getPlayer(session);
		if (player == null) {
			if (session.isOpen()) {
				send(session, PayloadFactory.DESTROYED());
				return;
			}
		}
	
		handleClientDisconnect(room, session);
	}

	@OnMessage
	public void onWebSocketText(String message, Session session, @PathParam("roomnumber") final Integer roomnumber) {
		Room room = RoomManager.getRoom(roomnumber);
		
		if (room == null) {
			send(session, PayloadFactory.DESTROYED());
			return;
		}
		
		room.lastMessage = System.currentTimeMillis();
		
		if (room.status == Room.Status.SYNCING) {
			Player host = room.getHost();
			if (host != null) {
				Payload payload = PayloadMapper.toPacket(message);
				if (payload.type == Payload.Type.PLAYER_SENDSTATE) {
					LOGGER.info("Broadcasting state of the host");
					payload.type = Payload.Type.SERVER_PUTSTATE;
					broadcast(payload, room);
					room.status = Room.Status.WAITING;
				}
			}
			return;
		}

		if (room.status == Room.Status.WAITING) {
			// Update player status
			Payload payload = PayloadMapper.toPacket(message);
			if (payload.type == Payload.Type.PLAYER_WAITING) {
				Player player = room.getPlayer(session);
				player.status = Player.Status.READY;
			}

			// Check if we are all READY!!
			for (Session sess : room.players.keySet()) {
				Player player = room.getPlayer(sess);
				if (player.status == Player.Status.WAITING) {
					return;
				}
			}

			// Play
			room.status = Room.Status.PLAYING;
			broadcast(PayloadFactory.PLAY(), room);
			LOGGER.info("Game " + this.hashCode() + " is now playing!");
			return;
		}

		if (room.status == Room.Status.PLAYING) {
			Payload payload = PayloadMapper.toPacket(message);

			if (payload.type == Payload.Type.PLAYER_KEYS) {
				payload.type = Payload.Type.SERVER_PLAYERKEYS;
				broadcast(payload, room);
			} else if (payload.type == Payload.Type.PLAYER_SYNC) {
				LOGGER.info("Resynchronizing clients");
				synchronize(roomnumber);
			}
		}

	}

	@OnError
	public void onWebSocketError(Session session, Throwable cause) {
		Room room = RoomManager.getRoom(session);
		handleClientDisconnect(room, session);
		cause.printStackTrace(System.err);
	}

	public void synchronize(int roomnumber) {
		Room room = RoomManager.getRoom(roomnumber);
		LOGGER.info("Synchronizing players in room " + room.id);
		
		broadcast(PayloadFactory.STOP(), room);
		room.status = Room.Status.SYNCING;

		for (Session session : room.players.keySet()) {
			Player player = room.players.get(session);
			player.status = Player.Status.WAITING;
		}

		send(room, room.getHost().session, PayloadFactory.GETSTATE());
	}

	public void send(Session session, Payload payload) {
		send(null, session, payload);
	}
	
	public void send(Room room, Session session, Payload payload) {
		try {
			if (session.isOpen()) {
				JSONObject json = PayloadMapper.toJSON(payload);
				session.getBasicRemote().sendText(json.toString(2));
				return;
			} 
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		handleClientDisconnect(room, session);
	}
	
	public void broadcast(Payload payload, Room room) {
		synchronized (room.players) {
			List<Session> staleSessions = new ArrayList<Session>();
			
			List<Player> players = room.getPlayers();
			for (Player player : players) {
				Session session = player.session;
				if (session.isOpen()) {
					try {
						JSONObject json = PayloadMapper.toJSON(payload);
						session.getBasicRemote().sendText(json.toString(2));
					} catch (Exception e) {
						e.printStackTrace();
						staleSessions.add(session);
					}
				} 
			}
			
			for (Session session : staleSessions) {
				handleClientDisconnect(room, session);
			}
		}
	}
	
	public void handleClientDisconnect(Room room, Session session) {
		if (room == null) {
			return;
		}

		Player left = room.getPlayer(session);		
		for (Player player : room.getPlayers()) {
			Session current = player.session;
			if (current.isOpen()) {
				try {
					Payload payload = new Payload(Payload.Type.SERVER_PLAYERDISCONNECTED, left);
					current.getBasicRemote().sendText(PayloadMapper.toJSON(payload).toString(2));
				} catch (JSONException | IOException e) {
					LOGGER.error(e);
				}
			}
		}
		
		room.leaveSeat(session);
	}

	public static Server create(Integer port) throws Exception {
		LOGGER.info("Starting HTTP (Websocket) server at " + port);

		// Set Jersey Classes
		ResourceConfig config = new ResourceConfig();
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(RoomSocket.class);
		config.registerClasses(s);

		Server server = new Server(port);

		// Create WS
		ServletHolder wsServlet = new ServletHolder(new DefaultServlet());
		ServletContextHandler wsHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		wsHandler.setContextPath("/ws");
		wsHandler.addServlet(wsServlet, "/*");

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { wsHandler });

		server.setHandler(handlers);

		ServerContainer container = WebSocketServerContainerInitializer.configureContext(wsHandler);
		container.addEndpoint(RoomSocket.class);

		server.start();
		return server;
	}

}
