package com.hkamran.hoones.server;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;

import com.hkamran.hoones.server.servers.HTTPServer;

public class Main {
	public static void main(String[] args) throws ServletException, DeploymentException {
		HTTPServer server = new HTTPServer();
		server.start(8090);
	}
}
