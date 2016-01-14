package com.ghsc.net.sockets.filetransfer;

import java.io.IOException;
import java.net.Inet4Address;
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
						fileShare.process(socket.accept());
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
	 * @param application - the main application.
	 * @throws IOException
	 */
	public FileTransferListener(Application application) throws IOException {
		this.fileShare = application.getFileShare();
		//socket = new ServerSocket(PORT, 10, Application.getLocalAddress());
		socket = new ServerSocket(0, 10, Inet4Address.getByName(Application.NETWORK.getIP()));
		selfPort = socket.getLocalPort();
		listener = new Thread(runnable);
		listener.setName("FileTransferListener");
	}
	
	/**
	 * Starts listening for file transfer connections.
	 */
	@Override
	public void start() {
		listener.start();
	}
	
	public int getPort() {
		return selfPort;
	}
	
	/**
	 * Closes the file transfer socket listener, thus stops listening.
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}