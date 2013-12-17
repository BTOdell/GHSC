package com.ghsc.net.sockets;

import java.io.IOException;
import java.util.ArrayList;

import com.ghsc.gui.Application;
import com.ghsc.net.sockets.filetransfer.FileTransferListener;
import com.ghsc.net.sockets.multicast.MulticastSocketController;
import com.ghsc.net.sockets.user.UserSocketListener;

/**
 * SocketManager is the socket managing class for GHSC.</br>
 * @author Odell
 */
public class SocketManager implements ISocketController {
	
	private Application application;
	private ArrayList<ISocketController> controllers = null;
	
	/**
	 * Initializes a new SocketManager.
	 * @param application - the main application.
	 * @throws IOException
	 */
	public SocketManager() throws IOException {
		this.application = Application.getApplication();
		
		initControllers();
	}
	
	/**
	 * Initializes all the socket controllers that are being managed by this SocketManager.
	 * @throws IOException
	 */
	private void initControllers() throws IOException {
		if (controllers == null)
			controllers = new ArrayList<ISocketController>();
		controllers.add(new FileTransferListener(application));
		controllers.add(new UserSocketListener(application));
		controllers.add(new MulticastSocketController(application));
	}
	
	/**
	 * Starts all the socket controllers in order.
	 */
	@Override
	public void start() {
		for (int i = 0; i < controllers.size(); i++) {
			ISocketController controller = controllers.get(i);
			if (controller != null)
				controller.start();
		}
	}
	
	/**
	 * Closes all the socket controllers in reverse order.
	 */
	@Override
	public void close() {
		for (int i = controllers.size() - 1; i >= 0; i--) {
			ISocketController controller = controllers.get(i);
			if (controller != null) {
				try {
					controller.close();
				} catch (IOException e) {}
			}
		}
	}
	
}