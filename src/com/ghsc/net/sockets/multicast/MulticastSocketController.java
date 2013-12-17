package com.ghsc.net.sockets.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.ghsc.common.Debug;
import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.net.encryption.AES;
import com.ghsc.net.sockets.ISocketController;
import com.ghsc.net.sockets.user.UserSocketListener;
import com.ghsc.net.update.Version;
import com.ghsc.util.Tag;

/**
 * Multicasting allows GHSC to broadcast it's location (as IP) to anyone who's listening on the multicast address</br>
 * without actually knowing who is receiving the messages.
 * MulticastSocketController handles both sending and receiving of multicast packets.
 * When this MulticastSocketController receives a packet of someone's IP, we can start a TCP connection with them.
 * @author Odell
 */
public class MulticastSocketController implements ISocketController {
	
	public static final String ATT_IP = "i", ATT_VERSION = "v", ATT_USERNAME = "u";
	public static final int PORT = 5688;
	private static final String MULTICAST_ADDRESS = "224.0.0.115";
	
	private int RECEIVE_BUFFER = 8192, SEND_DELAY = 500;
	
	private Application application;
	private MulticastSocket socket;
	private InetAddress address;
	private String localString;
	
	private final EventListener<MessageEvent> messageEventListener = new EventListener<MessageEvent>() {
		public void eventReceived(MessageEvent msg) {
			if (Debug.NORMAL.compareTo(Application.DEBUG) < 0)
				System.out.println(msg);
			if (msg == null)
				return;
			// we see somebody available and we try to connect to them
			final String ip = msg.getAttribute(ATT_IP);
			if (ip == null)
				return;
			if (localString.equals(ip))
				return; // make sure it's not ourself
			if (!application.getVersionController().isCompatible(Version.parse(msg.getAttribute(ATT_VERSION))))
				return;
			final UserContainer users = application.getMainFrame().getUsers();
			if (users.containsUser(ip) || users.isPending(ip))
				return;
			try {
				users.addPending(ip);
				application.getMainFrame().setStatus("Connecting to " + msg.getAttribute(ATT_USERNAME), 0);
				System.out.println("Connecting to " + msg.getAttribute(ATT_USERNAME));
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(ip, UserSocketListener.PORT), 2000);
				if (!users.addUser(socket)) {
					socket.close();
					System.out.println("User is already known.");
				} else {
					System.out.println("Connected to " + ip + " - " + msg.getAttribute(ATT_USERNAME));
					application.getMainFrame().setStatus("Connected to " + msg.getAttribute(ATT_USERNAME), 1000);
				}
			} catch (IOException e) {
				if (e instanceof SocketTimeoutException) {
					System.out.println("Socket timed out trying to connect to " + ip + ". Maybe next time?");
				} else {
					System.out.println("Unable to connect to " + ip + ". Maybe next time?");
				}
				application.getMainFrame().setStatus("Failed to connect", 750);
			} finally {
				users.removePending(ip);
			}
		}
	};
	
	private Thread receiveWorker = null, sendWorker = null;
	private Runnable receive = new Runnable() {
		public void run() {
			try {
				final byte[] buf = new byte[RECEIVE_BUFFER];
				final DatagramPacket pack = new DatagramPacket(buf, buf.length);
				while (running) {
					socket.receive(pack);
					final int length = pack.getLength();
					final byte[] buffer = buf.clone();
					new Thread(new Runnable() {
						public void run() {
							final byte[] data = AES.DEFAULT.decrypt(buffer, 0, length);
							if (data != null) {
								messageEventListener.eventReceived(MessageEvent.parse(new String(data, Application.CHARSET)));
							} else {
								System.out.println("Decrypted data is null!");
							}
						}
					}).start();
					pack.setLength(buf.length);
				}
			} catch (IOException e) {
				System.out.println("Multicast receive interrupted.");
			}
		}
	};
	private Runnable send = new Runnable() {
		public void run() {
			try {
				while (running) {
					final Tag dEvent = Tag.construct(Type.PING, ATT_VERSION, Application.VERSION, ATT_IP, localString, ATT_USERNAME, application.getPreferredName());
					final byte[] data = AES.DEFAULT.encrypt(dEvent.getEncodedString());
					if (data != null) {
						socket.send(new DatagramPacket(data, data.length, address, PORT));
					}
					Thread.sleep(SEND_DELAY);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	private boolean running = true;
	
	/**
	 * Initializes a new MulticastManager.
	 * @param application - the main application.
	 * @param event - a message callback which gets called this MulticastManager receives a packet and successfully parses it to a MessageEvent.
	 * @throws IOException
	 */
	public MulticastSocketController(Application application) throws IOException {
		this.application = application;
		this.localString = Application.getLocalAddress().getHostAddress();
		address = InetAddress.getByName(MULTICAST_ADDRESS);
		socket = new MulticastSocket(new InetSocketAddress(Application.getLocalAddress(), PORT));
		socket.setTimeToLive(255);
		socket.joinGroup(address);
		receiveWorker = new Thread(receive);
		receiveWorker.setName("MulticastSocketController|Receive");
		sendWorker = new Thread(send);
		sendWorker.setName("MulticastSocketController|Send");
	}
	
	/**
	 * Starts both sending and receiving.
	 */
	@Override
	public void start() {
		sendWorker.start();
		receiveWorker.start();
	}
	
	/**
	 * Closes the multicast socket.
	 */
	@Override
	public void close() {
		System.out.println("Multicast controller interrupted.");
		running = false;
		socket.close();
	}
	
}