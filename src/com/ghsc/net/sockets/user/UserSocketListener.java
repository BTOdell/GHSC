package com.ghsc.net.sockets.user;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.net.sockets.ISocketController;

/**
 * The task of UserSocketListener is to listen for any incoming TCP connection requests and accept them.
 * @author Odell
 */
public class UserSocketListener implements ISocketController {
	
	public static final int PORT = 5689;
	
	private Application application;
	private ServerSocket socket;
	
	private Thread listener;
	private final Runnable runnable = new Runnable() {
		public void run() {
			try {
				while (true) {
					// somebody tries connecting to us
					Socket s = socket.accept();
					String ip = s.getInetAddress().getHostAddress();
					UserContainer users = application.getMainFrame().getUsers();
					if (!users.containsUser(ip) && !users.isPending(ip)) {
						try {
							users.addPending(ip);
							System.out.println("Receiving socket connection from " + ip);
							if (users.addUser(s)) {
								System.out.println("Successfully received socket connection!");
								continue;
							} else {
								System.out.println("Socket receive failed.");
							}
						} finally {
							users.removePending(ip);
						}
					}
					s.close();
				}
			} catch (IOException e) {
				System.out.println("User socket listener interrupted.");
			}
		}
	};
	
	/**
	 * Initializes a new UserSocketListener.
	 * @param application - the main application.
	 * @throws IOException
	 */
	public UserSocketListener(Application application) throws IOException {
		this.application = application;
		socket = new ServerSocket(PORT, 10, Application.getLocalAddress());
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
	
	/**
	 * Closes the user socket listener, thus stops listening.
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}