package ghsc.net.sockets.filetransfer;

import ghsc.gui.Application;
import ghsc.gui.fileshare.FileShare;
import ghsc.net.sockets.ISocketController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * The task of FileTransferListener is to listen for any incoming TCP file transfer connections and accept them.
 */
public class FileTransferListener implements ISocketController {
	
	public static final int PORT = 5687;
	
	private final FileShare fileShare;
	private final ServerSocket socket;
	private final int selfPort;
	
	private final Thread listener;

	/**
	 * Initializes a new FileTransferListener.
	 * @throws IOException If an error occurs when creating the underlying server socket.
	 */
	public FileTransferListener() throws IOException {
		final Application application = Application.getInstance();
		this.fileShare = application.getFileShare();
		//socket = new ServerSocket(0, 10, Inet4Address.getByName(Application.NETWORK.getIP()));
		this.socket = new ServerSocket(0, 10, null);
		this.selfPort = this.socket.getLocalPort();
		this.listener = new Thread(() -> {
			try {
				try {
                    while (true) {
                        this.fileShare.process(this.socket.accept());
                    }
                } catch (final SocketException se) {
				    throw se;
				} catch (final IOException e) {
					e.printStackTrace();
					throw e;
				}
			} catch (final Exception e) {
				System.out.println("File transfer socket listener interrupted.");
			}
		});
		this.listener.setName("FileTransferListener");
	}
	
	/**
	 * Starts listening for file transfer connections.
	 */
	@Override
	public void start() {
        this.listener.start();
	}
	
	public int getPort() {
		return this.selfPort;
	}
	
	/**
	 * Closes the file transfer socket listener, thus stops listening.
	 */
	@Override
	public void close() {
		try {
			this.socket.close();
		} catch (final IOException ignored) { }
	}
	
}