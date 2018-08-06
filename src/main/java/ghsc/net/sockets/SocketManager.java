package ghsc.net.sockets;

import ghsc.net.sockets.filetransfer.FileTransferListener;
import ghsc.net.sockets.multicast.MulticastSocketController;
import ghsc.net.sockets.user.UserSocketListener;

import java.io.IOException;
import java.util.ArrayList;

/**
 * SocketManager is the socket managing class for the application.
 */
public class SocketManager implements ISocketController {
	
	private final ArrayList<ISocketController> controllers;

	/**
	 * Force creation through factory function.
	 */
	public SocketManager() throws IOException {
        // Create controllers
        this.controllers = new ArrayList<>();
        final FileTransferListener fileListener = new FileTransferListener();
        this.controllers.add(fileListener);
        final UserSocketListener userListener = new UserSocketListener();
        this.controllers.add(userListener);
        final MulticastSocketController multicastSocketController = new MulticastSocketController(userListener.getPort());
        this.controllers.add(multicastSocketController);
	}
	
	/**
	 * Starts all the socket controllers in order.
	 */
	@Override
	public void start() {
        for (final ISocketController controller : this.controllers) {
            if (controller != null) {
                controller.start();
            }
        }
	}
	
	/**
	 * Closes all the socket controllers in reverse order.
	 */
	@Override
	public void close() {
        for (int i = this.controllers.size() - 1; i >= 0; i--) {
            final ISocketController controller = this.controllers.get(i);
            if (controller != null) {
                controller.close();
            }
        }
	}
	
}