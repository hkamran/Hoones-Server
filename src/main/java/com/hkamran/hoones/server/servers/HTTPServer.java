package com.hkamran.hoones.server.servers;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.ws.rs.Path;

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


@Path("/")
public class HTTPServer {

	private final static Logger log = LogManager.getLogger(HTTPServer.class);


	public void start(Integer port) throws ServletException, DeploymentException {
		log.info("Starting HTTP Server at " + port);
		
		//Set Jersey Classes
		ResourceConfig config = new ResourceConfig();
		Set<Class<?>> s = new HashSet<Class<?>>();
        s.add(GameServer.class);
		config.registerClasses(s);
		
		Server server = new Server(port);

		//Create WS
		ServletHolder wsServlet = new ServletHolder(new DefaultServlet());
		ServletContextHandler wsHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		wsHandler.setContextPath("/ws");
		wsHandler.addServlet(wsServlet, "/*");

	    HandlerList handlers = new HandlerList();
	    handlers.setHandlers(new Handler[] {
	    		wsHandler
	    });
				
	    server.setHandler(handlers);
	    
        ServerContainer container = WebSocketServerContainerInitializer.configureContext(wsHandler); 
        container.addEndpoint(GameServer.class); 
        
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
