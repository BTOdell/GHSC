package com.ghsc.net.sockets.user;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.net.sockets.ISocketController;

/**
 * The task of UserSocketListener is to listen for any incoming TCP connection requests and accept them.
 */
public class UserSocketListener implements ISocketController {
	
	private final ServerSocket listenSocket;
	
	private final Thread listener;
	
	/**
	 * Initializes a new UserSocketListener.
	 * @throws IOException If an error occurs when creating the underlying server socket.
	 */
	public UserSocketListener() throws IOException {
		this.listenSocket = new ServerSocket(0, 10, null);
		this.listener = new Thread(this::listenRunnable);
		this.listener.setName("UserSocketListener");
	}
	
	public int getPort() {
		return this.listenSocket.getLocalPort();
	}
	
	private void listenRunnable() {
		try {
			while (true) {
				// somebody tries connecting to us
				final Socket acceptSocket = this.listenSocket.accept();
				final SocketAddress remoteSocketAddress = acceptSocket.getRemoteSocketAddress();
				if (remoteSocketAddress instanceof InetSocketAddress) {
					final InetSocketAddress remoteAddress = (InetSocketAddress) remoteSocketAddress;
					System.out.println("Accepted socket connection from " + remoteAddress.getAddress() + "@" + remoteAddress.getPort());
					final UserContainer users = Application.getInstance().getMainFrame().getUsers();
					final User user = new User(users, acceptSocket);
					if (users.addUserPending(remoteAddress, user)) {
						System.out.println("Completed INCOMING socket connection.  User is pending.");
						System.out.println("Connected to " + remoteAddress.getAddress() + "@" + remoteAddress.getPort() + " - unknown");
						user.start();
						continue;
					} else {
						System.out.println("Socket receive failed.");
					}
				}
				acceptSocket.close();
			}
		} catch (final IOException e) {
			System.out.println("User socket listener interrupted.");
		}
	}
	
	/**
	 * Starts listening for user connections.
	 */
	@Override
	public void start() {
		this.listener.start();
	}
	
	/**
	 * Closes the user socket listener, thus stops listening.
	 */
	@Override
	public void close() {
		try {
			this.listenSocket.close();
		} catch (final IOException ignored) { }
	}
	
}