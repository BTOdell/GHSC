package com.ghsc.net.sockets.user;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;

import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.IpPort;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.net.sockets.ISocketController;

/**
 * The task of UserSocketListener is to listen for any incoming TCP connection requests and accept them.
 * 
 * @author Odell
 */
public class UserSocketListener implements ISocketController {
	
	public static final int PORT = 5689;
	
	private Application application;
	private ServerSocket socket;
	private final int selfPort;
	private int filePort = 0;
	
	private Thread listener;
	private final Runnable runnable = new Runnable() {
		public void run() {
			try {
				System.out.println("filePort is " + filePort);
				while (true) {
					// somebody tries connecting to us
					Socket newSocket = socket.accept();
					
					IpPort pair = new IpPort(newSocket.getInetAddress().getHostAddress(), newSocket.getPort());
					try {
						System.out.println("Accepted socket connection from " + pair);
						UserContainer users = application.getMainFrame().getUsers();
						final User user = new User(users, pair, newSocket, selfPort);
						if (users.addUserPending(pair, user)) {
							System.out.println("Completed INCOMING socket connection.  User is pending.");
							System.out.println("Connected to " + pair + " - unknown");
							user.start();
							continue;
						} else {
							System.out.println("Socket receive failed.");
						}
					} finally {
					}
					newSocket.close();
				}
			} catch (IOException e) {
				System.out.println("User socket listener interrupted.");
			}
		}
	};
	
	/**
	 * Initializes a new UserSocketListener.
	 * 
	 * @param application
	 *            - the main application.
	 * @throws IOException
	 */
	public UserSocketListener(Application application, int filePort) throws IOException {
		this.application = application;
		this.filePort = filePort;
		
		socket = new ServerSocket(0, 10, Inet4Address.getByName(application.getLocalAddress()));
		// socket = new ServerSocket(PORT, 10, application.getLocalAddress());
		selfPort = socket.getLocalPort();
		listener = new Thread(runnable);
		listener.setName("UserSocketListener");
	}
	
	/**
	 * Starts listening for user connections.
	 */
	@Override
	public void start() {
		listener.start();
	}
	
	public int getPort() {
		return selfPort;
	}
	
	/**
	 * Closes the user socket listener, thus stops listening.
	 * 
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}