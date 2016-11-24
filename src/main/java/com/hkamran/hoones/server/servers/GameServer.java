package com.hkamran.hoones.server.servers;

import java.io.IOException;
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

import com.hkamran.hoones.server.Payload;
import com.hkamran.hoones.server.payloads.Player;

@ClientEndpoint
@ServerEndpoint(value = "/{roomnumber}")
public class GameServer {

	private final static Logger log = LogManager.getLogger(GameServer.class);

	@OnOpen
	public void OnWebSocketConnect(Session session, @PathParam("roomnumber") final Integer roomnumber) {
		session.setMaxIdleTimeout(600000);

		Room room = GameManager.getRoom(roomnumber);
		
		if (room == null) {
			//does not exists
			send(session, Payload.DESTROYED());
			return;
		}
		
		if (room.hasEmptySeat()) {
			Integer id = room.getEmptySeat();
			Player player = room.takeSeat(id, session);
			
			Payload payload = new Payload(Payload.Type.SERVER_PLAYERINFO, player);
			send(session, payload);			
		} else {
			send(session, Payload.FULL());
			return;
		}

		List<Player> players = room.getPlayers();
		for (Player player : players){
			Payload payload = new Payload(Payload.Type.SERVER_PLAYERCONNECTED, player);
			broadcast(payload, room);
		}

		if (players.size() > 1) {
			synchonize(roomnumber);
		}
	}

	@OnClose
	public void onWebSocketClose(Session session, @PathParam("roomnumber") final Integer roomnumber) {
		Room room = GameManager.getRoom(roomnumber);
		
		if (room == null) {
			send(session, Payload.DESTROYED());
			return;
		}
		
		Player player = room.getPlayer(session);
		if (player == null) {
			send(session, Payload.DESTROYED());
			return;
		}
	
		Payload payload = new Payload(Payload.Type.SERVER_PLAYERDISCONNECTED, player);
		broadcast(payload, room);

		room.leaveSeat(session);
	}

	@OnMessage
	public void onWebSocketText(String message, Session session, @PathParam("roomnumber") final Integer roomnumber) {
		Room room = GameManager.getRoom(roomnumber);
		
		if (room == null) {
			send(session, Payload.DESTROYED());
			return;
		}
		
		room.lastMessage = System.currentTimeMillis();
		
		if (room.status == Room.Status.SYNCING) {
			Player host = room.getHost();
			if (host != null) {
				Payload payload = Payload.parseJSON(message);
				if (payload.type == Payload.Type.PLAYER_SENDSTATE) {
					log.info("Broadcasting state of the host");
					payload.type = Payload.Type.SERVER_PUTSTATE;
					broadcast(payload, room);
					room.status = Room.Status.WAITING;
				}
			}
			return;
		}

		if (room.status == Room.Status.WAITING) {
			// Update player status
			Payload payload = Payload.parseJSON(message);
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
			broadcast(Payload.PLAY(), room);
			return;
		}

		if (room.status == Room.Status.PLAYING) {
			Payload payload = Payload.parseJSON(message);

			if (payload.type == Payload.Type.PLAYER_KEYS) {
				payload.type = Payload.Type.SERVER_PLAYERKEYS;
				broadcast(payload, room);
			} else if (payload.type == Payload.Type.PLAYER_SYNC) {
				log.info("Resynchronizing clients");
				synchonize(roomnumber);
			}
		}

	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	public void synchonize(int roomnumber) {
		log.info("Synchronizing players...");
		Room room = GameManager.getRoom(roomnumber);
		
		broadcast(Payload.STOP(), room);
		room.status = Room.Status.SYNCING;

		for (Session session : room.players.keySet()) {
			Player player = room.players.get(session);
			player.status = Player.Status.WAITING;
		}

		Payload payload = new Payload();
		payload.type = Payload.Type.SERVER_GETSTATE;

		send(room.getHost().session, payload);
	}

	public void send(Session session, Payload payload) {
		try {
			session.getBasicRemote().sendText(payload.toJSON().toString(2));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	public void broadcast(Payload payload, Room room) {
		synchronized (room.players) {
			for (Session session : room.players.keySet()) {
				if (session.isOpen()) {
					try {
						session.getBasicRemote().sendText(payload.toJSON().toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}


	public static Server create(Integer port) throws Exception {
		log.info("Starting HTTP Server at " + port);

		// Set Jersey Classes
		ResourceConfig config = new ResourceConfig();
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(GameServer.class);
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
		container.addEndpoint(GameServer.class);

		server.start();
		return server;
	}

}
