package com.ghsc.net.sockets;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.ArrayList;

import com.ghsc.net.sockets.filetransfer.FileTransferListener;
import com.ghsc.net.sockets.multicast.MulticastSocketController;
import com.ghsc.net.sockets.user.UserSocketListener;

/**
 * SocketManager is the socket managing class for the application.
 */
public class SocketManager implements ISocketController {
	
	private static final int INSTANCE_CHECK_PORT = 5690;
	
	private ArrayList<ISocketController> controllers;
	private ServerSocket instanceChecker;
	
	/**
	 * Initializes a new SocketManager.
	 */
	public SocketManager() {}
	
	public void initControllers() throws IOException {
		if (this.controllers == null) {
			this.controllers = new ArrayList<>();
			
			final FileTransferListener fileListener = new FileTransferListener();
			this.controllers.add(fileListener);
			
			final UserSocketListener userListener = new UserSocketListener();
			this.controllers.add(userListener);

			this.controllers.add(new MulticastSocketController(userListener.getPort()));
		}
	}
	
	public boolean instanceCheck() {
		if (this.instanceChecker == null) {
			try {
				this.instanceChecker = new ServerSocket(INSTANCE_CHECK_PORT, 10, Inet4Address.getLoopbackAddress());
			} catch (final IOException e) {
				return false;
			}
		}
		return true;
	}
	
	private void instanceCheckClose() {
		if (this.instanceChecker != null) {
			try {
				this.instanceChecker.close();
			} catch (final IOException ignored) {}
			this.instanceChecker = null;
		}
	}
	
	/**
	 * Starts all the socket controllers in order.
	 */
	@Override
	public void start() {
		if (this.controllers != null) {
			this.instanceCheck();
			for (final ISocketController controller : this.controllers) {
				if (controller != null) {
					controller.start();
				}
			}
		}
	}
	
	/**
	 * Closes all the socket controllers in reverse order.
	 */
	@Override
	public void close() {
		if (this.controllers != null) {
			for (int i = this.controllers.size() - 1; i >= 0; i--) {
				final ISocketController controller = this.controllers.get(i);
				if (controller != null) {
					controller.close();
				}
			}
			this.instanceCheckClose();
		}
	}
	
}