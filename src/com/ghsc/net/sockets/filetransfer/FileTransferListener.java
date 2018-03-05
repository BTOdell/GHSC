package com.ghsc.net.sockets.filetransfer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import com.ghsc.gui.Application;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.net.sockets.ISocketController;

/**
 * The task of FileTransferListener is to listen for any incoming TCP file transfer connections and accept them.
 * @author Odell
 */
public class FileTransferListener implements ISocketController {
	
	public static final int PORT = 5687;
	
	private FileShare fileShare;
	private ServerSocket socket;
	private final int selfPort;
	
	private Thread listener;
	private final Runnable runnable = new Runnable() {
		public void run() {
			try {
				try {
					while (true) {
                        FileTransferListener.this.fileShare.process(FileTransferListener.this.socket.accept());
					}
				} catch (SocketException se) {
					throw se;
				} catch (IOException e) {
					e.printStackTrace();
					throw e;
				}
			} catch (Exception e) {
				System.out.println("File transfer socket listener interrupted.");
			}
		}
	};
	
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
		this.listener = new Thread(this.runnable);
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