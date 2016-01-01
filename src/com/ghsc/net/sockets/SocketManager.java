package com.ghsc.net.sockets;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.ArrayList;

import com.ghsc.gui.Application;
//import com.ghsc.net.sockets.filetransfer.FileTransferListener;
import com.ghsc.net.sockets.multicast.MulticastSocketController;
import com.ghsc.net.sockets.user.UserSocketListener;

/**
 * SocketManager is the socket managing class for GHSC.</br>
 * @author Odell
 */
public class SocketManager implements ISocketController {
	
	private Application application;
	private ArrayList<ISocketController> controllers = null;
	private ServerSocket instanceChecker = null;
	private final String networkIP;
	
	/**
	 * Initializes a new SocketManager.
	 * @param application - the main application.
	 * @throws IOException
	 */
	public SocketManager(String networkIP) {
		this.application = Application.getApplication();
		this.networkIP = networkIP;
	}
	
	/**
	 * Initializes all the socket controllers that are being managed by this SocketManager.
	 * returns the User Port.
	 * @throws IOException
	 */
	private int userPort = 0;
	
	public int initControllers() throws IOException {
		if (controllers == null) {
			controllers = new ArrayList<ISocketController>();
			
			int filePort = 9999;
			//FileTransferListener fileListener = new FileTransferListener(application);
			//int filePort = fileListener.getPort();
			//controllers.add(fileListener);
			
			UserSocketListener userListener = new UserSocketListener(application, filePort);
			userPort = userListener.getPort();
			controllers.add(userListener);
			
			controllers.add(new MulticastSocketController(application, userPort));
		}
		return userPort;
	}
	
	public boolean instanceCheck() {
		if (instanceChecker == null) {
			try {
				instanceChecker = new ServerSocket(5690, 10, Inet4Address.getByName(networkIP));
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