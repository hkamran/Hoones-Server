package com.hkamran.hoones.server.servers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.websocket.ClientEndpoint;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
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
import com.hkamran.hoones.server.payloads.Keys;
import com.hkamran.hoones.server.payloads.Player;

@ClientEndpoint
@ServerEndpoint(value = "/")
public class GameServer {

	private final static Logger log = LogManager.getLogger(GameServer.class);

	// Server Properties
	Server server;
	int port;

	// Game Properties
	public static final int SIZE = 2;
	public Player host;
	public Session[] slots = new Session[SIZE];
	public Map<Session, Player> players = new HashMap<Session, Player>();
	public Status status = Status.PLAYING;

	public long lastMessage = System.currentTimeMillis();

	public static enum Status {
		SYNCING, WAITING, PLAYING
	}

	@OnOpen
	public void OnWebSocketConnect(Session session) {

		session.setMaxIdleTimeout(600000);

		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null)
				continue;

			slots[i] = session;
			Player player = new Player(session, i + 1);
			players.put(session, player);
			log.info(String.format("Player %S connected on session %S", i + 1, session.getId()));

			// Send player id to the user
			Payload payload = new Payload();
			payload.type = Payload.Type.PLAYER;
			payload.data = player;
			send(session, payload);

			break;

		}

		for (int i = 0; i < slots.length; i++) {
			if (slots[i] == null)
				continue;

			// Let everyone know someone connected
			Payload payload = new Payload();
			payload.type = Payload.Type.CONNECTED;
			payload.data = players.get(slots[i]);
			broadcast(payload);
		}

		host = null;
		for (int i = 0; i < slots.length; i++) {
			if (slots[i] == null)
				continue;
			if (slots[i] != null) {
				host = players.get(slots[i]);
				break;
			}
		}

		if (players.size() > 1) {
			synchonize();
		}
	}

	@OnClose
	public void onWebSocketClose(Session session) {
		Player player = players.get(session);
		Payload payload = new Payload();
		payload.type = Payload.Type.DISCONNECTED;
		payload.data = player;
		broadcast(payload);

		for (int i = 0; i < slots.length; i++) {
			if (session == slots[i]) {
				log.info(String.format("Player %d disconnected on session %s ", i + 1, session.getId()));

				slots[i] = null;
				break;
			}
		}

		players.remove(session);
	}

	@OnMessage
	public void onWebSocketText(String message, Session session) {
		
		lastMessage = System.currentTimeMillis();
		
		if (status == Status.SYNCING) {
			if (host != null && host.session == session) {
				Payload payload = Payload.parseJSON(message);
				if (payload.type == Payload.Type.GET) {
					log.info("Broadcasting state of the host");
					payload.type = Payload.Type.PUT;
					broadcast(payload);
					status = Status.WAITING;
				}
			}
			return;
		}

		if (status == Status.WAITING) {
			// Update player status
			Payload payload = Payload.parseJSON(message);
			if (payload.type == Payload.Type.WAITING) {
				Player player = players.get(session);
				player.status = Player.Status.READY;
			}

			// Check if we are all READY!!
			for (Session sess : players.keySet()) {
				Player player = players.get(sess);
				if (player.status == Player.Status.WAITING) {
					return;
				}
			}

			// Play
			status = Status.PLAYING;
			broadcast(Payload.PLAY());
			return;
		}

		if (status == Status.PLAYING) {
			Payload payload = Payload.parseJSON(message);

			if (payload.type == Payload.Type.KEYS) {
				Keys controller = (Keys) payload.data;
				log.info("Received Keys from " + controller.playerId + " at " + controller.cycle);

				Payload resPayload = new Payload();
				resPayload.type = Payload.Type.KEYS;
				resPayload.data = controller;

				broadcast(payload);
			} else if (payload.type == Payload.Type.PUT) {
				log.info("Resynchronizing clients");
				synchonize();
			}
		}

	}

	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	public void synchonize() {
		log.info("Synchronizing players...");

		broadcast(Payload.STOP());
		status = Status.SYNCING;

		for (Session session : players.keySet()) {
			Player player = players.get(session);
			player.status = Player.Status.WAITING;
		}

		Payload payload = new Payload();
		payload.type = Payload.Type.GET;

		send(host.session, payload);
	}

	public void send(Session session, Payload payload) {
		try {
			session.getBasicRemote().sendText(payload.toJSON().toString(2));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
	}

	public void broadcast(Payload payload) {
		Set<Session> ignoring = new HashSet<Session>();
		broadcast(payload, ignoring);
	}

	public void broadcast(Payload payload, Set<Session> ignoring) {

		synchronized (players) {
			for (Session session : players.keySet()) {
				if (ignoring.contains(session)) {
					continue;
				}
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

	public Player getPlayer(Session session) {
		return players.get(session);
	}

	public void start(Integer port) throws Exception {
		log.info("Starting HTTP Server at " + port);

		this.port = port;

		// Set Jersey Classes
		ResourceConfig config = new ResourceConfig();
		Set<Class<?>> s = new HashSet<Class<?>>();
		s.add(GameServer.class);
		config.registerClasses(s);

		server = new Server(port);

		// Create WS
		ServletHolder wsServlet = new ServletHolder(new DefaultServlet());
		ServletContextHandler wsHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		wsHandler.setContextPath("/ws");
		wsHandler.addServlet(wsServlet, "/*");

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { wsHandler });

		server.setHandler(handlers);

		ServerContainer container = WebSocketServerContainerInitializer.configureContext(wsHandler);
		container.addEndpoint(GameServer.class);

		server.start();
	}

	public void close() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			server.destroy();
		}

	}

}
