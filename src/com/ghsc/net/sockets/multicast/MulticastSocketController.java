package com.ghsc.net.sockets.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Set;

import com.ghsc.common.Debug;
import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.IpPort;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.net.encryption.AES;
import com.ghsc.net.sockets.ISocketController;
import com.ghsc.net.update.Version;
import com.ghsc.util.Tag;

/**
 * Multicasting allows GHSC to broadcast it's location (as IP) to anyone who's listening on the multicast address</br>
 * without actually knowing who is receiving the messages. MulticastSocketController handles both sending and receiving of multicast packets. When this MulticastSocketController receives a packet of someone's IP, we can start a TCP connection with them.
 * 
 * @author Odell
 */
public class MulticastSocketController implements ISocketController {
	
	public static final String ATT_IP = "i", ATT_PORT = "p", ATT_USERPORT = "u", ATT_VERSION = "v", ATT_USERNAME = "n";
	public static final int MULTICAST_PORT = 5688;
	private static final String MULTICAST_ADDRESS = "224.0.0.115";
	
	private int RECEIVE_BUFFER = 8192;
	private int SEND_DELAY = 500;
	private int CONNECT_DELAY = 2000;
	private int INTERFACE_UPDATE_DELAY = 10000;
	
	private Set<String> interfaceNames = null;
	
	private final Application application;
	private final MulticastSocket listenSocket;
	private final MulticastSocket sendSocket;
	private final InetAddress multicastInetAddress;
	
	private String selfMulticastIP = null;
	private int selfMulticastPort = 0;
	private int selfTCPPort = 0;
	private volatile boolean isConnecting = false;
	
	private final EventListener<MessageEvent> messageEventListener = new EventListener<MessageEvent>() {
		public void eventReceived(MessageEvent msg) {
			if (Debug.NORMAL.compareTo(Application.DEBUG) < 0)
				System.out.println(msg);
			if (msg == null)
				return;
			// we see somebody available and we try to connect to them
			final String ip = msg.getAttribute(ATT_IP);
			final String port = msg.getAttribute(ATT_PORT);
			if ((ip == null) || (port == null))
				return;
			if (selfMulticastIP.equals(ip) && (selfMulticastPort == Integer.valueOf(port))) {
				return; // make sure it's not ourself
			}
			final String msgPort = msg.getAttribute(ATT_USERPORT);
			final int remoteUserPort = Integer.valueOf(msgPort);
			
			if (!application.getVersionController().isCompatible(Version.parse(msg.getAttribute(ATT_VERSION))))
				return;
			final UserContainer users = application.getMainFrame().getUsers();
			
			IpPort remotePair = new IpPort(ip, remoteUserPort);
			if (!users.addMulticaster(remotePair))
				return;
			{
				// Note: this isn't thread reliable, but will work because of the 500 ms delay.
				if (isConnecting)
					return;
				isConnecting = true;
			}
			System.out.println("Received " + remotePair + " from MULTICASTER " + port);
			
			IpPort selfPair = new IpPort(selfMulticastIP, selfTCPPort);
			if (remotePair.toLong() < selfPair.toLong()) {
				// self is bigger, so self is the connector !!!
				try {
					application.getMainFrame().setStatus("Connecting to " + msg.getAttribute(ATT_USERNAME), 0);
					
					Socket socket = new Socket();
					socket.connect(new InetSocketAddress(ip, remoteUserPort), CONNECT_DELAY);
					final User user = new User(users, remotePair, socket, selfTCPPort);
					
					if (!users.addUser(remotePair, user)) {
						System.err.println("Unable to add " + remotePair + ".  User is already known.");
						socket.close();
					} else {
						System.out.println("Completed OUTGOING socket connection.  User is pending.");
						System.out.println("Connected to " + remotePair + " - " + msg.getAttribute(ATT_USERNAME));
						application.getMainFrame().setStatus("Connected to " + msg.getAttribute(ATT_USERNAME), 1000);
						
						user.sendEndpoint(); // send identifying port info.
						user.sendIntro(); // send identifying user info.
						user.start(); // starts up the receive thread.
						user.setFriend(users.isFriend(user)); // updates various user list status...
						user.setIgnored(users.isIgnored(user));
						users.getMainFrame().getChatContainer().refreshUser(user);
						users.refresh();
						users.removeMulticaster(remotePair); // no longer need to block multicasters.
					}
					
				} catch (IOException e) {
					if (e instanceof SocketTimeoutException) {
						System.out.println("Socket timed out trying to connect to " + remotePair + ". Maybe next time?");
					} else {
						System.out.println("Unable to connect to " + remotePair + ". Maybe next time?");
					}
					application.getMainFrame().setStatus("Failed to connect", 750);
				} finally {
					isConnecting = false;
				}
				
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
					listenSocket.receive(pack);
					final int length = pack.getLength();
					final byte[] buffer = Arrays.copyOf(buf, length);
					// new Thread(new Runnable() {
					// public void run() {
					final byte[] data = AES.DEFAULT.decrypt(buffer, 0, length);
					if (data != null) {
						final MessageEvent parsedMessageEvent = MessageEvent.parse(new String(data, Application.CHARSET));
						System.out.println(parsedMessageEvent);
						messageEventListener.eventReceived(parsedMessageEvent);
					} else {
						System.out.println("Decrypted data is null!");
					}
					// }}).start();
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
				long updateTime = System.currentTimeMillis() + INTERFACE_UPDATE_DELAY;
				while (running) {
					final Tag dEvent = Tag.construct(Type.PING, ATT_VERSION, Application.VERSION, ATT_IP, selfMulticastIP, ATT_PORT, selfMulticastPort, ATT_USERPORT, selfTCPPort, ATT_USERNAME, application.getPreferredName());
					final byte[] data = AES.DEFAULT.encrypt(dEvent.getEncodedString());
					if (data != null) {
						sendSocket.send(new DatagramPacket(data, data.length, multicastInetAddress, MULTICAST_PORT));
					}
					Thread.sleep(SEND_DELAY);
					if (System.currentTimeMillis() > updateTime) {
						updateTime = System.currentTimeMillis() + INTERFACE_UPDATE_DELAY;
						Set<String> newInterfaceNames = Application.NETWORK.updateInterfaces();
						if (newInterfaceNames != null) {
							// Changes have occurred to the active interfaces.
							// First, unjoin interfaces no longer in the list...
							for (String name : interfaceNames) {
								if (!newInterfaceNames.contains(name)) {
									NetworkInterface xface = NetworkInterface.getByName(name);
									if (xface != null) {
										listenSocket.leaveGroup(new InetSocketAddress(multicastInetAddress, MulticastSocketController.MULTICAST_PORT), xface);
									}
								}
							}
							// Second, join new interfaces in the list...
							for (String name : newInterfaceNames) {
								if (!interfaceNames.contains(name)) {
									NetworkInterface xface = NetworkInterface.getByName(name);
									if (xface != null) {
										listenSocket.joinGroup(new InetSocketAddress(multicastInetAddress, MulticastSocketController.MULTICAST_PORT), xface);
									}
								}
							}
							// Establish the new list...
							interfaceNames = newInterfaceNames;
						}
					}
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
	 * 
	 * @param application
	 *            - the main application.
	 * @param event
	 *            - a message callback which gets called this MulticastManager receives a packet and successfully parses it to a MessageEvent.
	 * @throws IOException
	 */
	
	public MulticastSocketController(Application application, int localUserPort) throws IOException {
		this.application = application;
		this.selfTCPPort = localUserPort;
		
		multicastInetAddress = InetAddress.getByName(MULTICAST_ADDRESS);
		
		// Note: Do not bind to the local address, just use the port!
		this.listenSocket = new MulticastSocket(MulticastSocketController.MULTICAST_PORT);
		this.listenSocket.setTimeToLive(255);
		
		// join the multicast Group for all the network interfaces.
		interfaceNames = Application.NETWORK.getInterfaces();
		if (interfaceNames != null) {
			for (String name : interfaceNames) {
				NetworkInterface xface = NetworkInterface.getByName(name);
				if (xface != null) {
					this.listenSocket.joinGroup(new InetSocketAddress(multicastInetAddress, MulticastSocketController.MULTICAST_PORT), xface);
				}
			}
		}
		
		// TODO: Figure out a way to send a multicast message for each interface.
		// TODO: First, test to see if we have to do this...
		this.sendSocket = new MulticastSocket(new InetSocketAddress(Application.defaultIP, 0));
		
		this.selfMulticastIP = sendSocket.getLocalAddress().getHostAddress();
		this.selfMulticastPort = sendSocket.getLocalPort();
		
		this.receiveWorker = new Thread(receive);
		this.receiveWorker.setName("MulticastSocketController|Receive");
		this.sendWorker = new Thread(send);
		this.sendWorker.setName("MulticastSocketController|Send");
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
		running = false;
		this.listenSocket.close();
		this.sendSocket.close();
	}
	
}