package com.ghsc.net.sockets;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.ArrayList;

import com.ghsc.net.sockets.filetransfer.FileTransferListener;
import com.ghsc.net.sockets.multicast.MulticastSocketController;
import com.ghsc.net.sockets.user.UserSocketListener;

/**
 * SocketManager is the socket managing class for GHSC.</br>
 */
public class SocketManager implements ISocketController {
	
	public static final int INSTANCE_CHECK_PORT = 5690;
	
	private ArrayList<ISocketController> controllers = null;
	private ServerSocket instanceChecker = null;
	
	/**
	 * Initializes a new SocketManager.
	 */
	public SocketManager() {}
	
	public void initControllers() throws IOException {
		if (controllers == null) {
			controllers = new ArrayList<>();
			
			FileTransferListener fileListener = new FileTransferListener();
			controllers.add(fileListener);
			
			UserSocketListener userListener = new UserSocketListener();
			controllers.add(userListener);
			
			controllers.add(new MulticastSocketController(userListener.getPort()));
		}
	}
	
	public boolean instanceCheck() {
		if (instanceChecker == null) {
			try {
				instanceChecker = new ServerSocket(INSTANCE_CHECK_PORT, 10, Inet4Address.getLoopbackAddress());
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}
	
	private void instanceCheckClose() {
		if (instanceChecker != null) {
			try {
				instanceChecker.close();
			} catch (IOException e) {}
			instanceChecker = null;
		}
	}
	
	/**
	 * Starts all the socket controllers in order.
	 */
	@Override
	public void start() {
		if (controllers != null) {
			instanceCheck();
			for (int i = 0; i < controllers.size(); i++) {
				ISocketController controller = controllers.get(i);
				if (controller != null)
					controller.start();
			}
		}
	}
	
	/**
	 * Closes all the socket controllers in reverse order.
	 */
	@Override
	public void close() {
		if (controllers != null) {
			for (int i = controllers.size() - 1; i >= 0; i--) {
				ISocketController controller = controllers.get(i);
				if (controller != null) {
					try {
						controller.close();
					} catch (IOException e) {}
				}
			}
			instanceCheckClose();
		}
	}
	
}