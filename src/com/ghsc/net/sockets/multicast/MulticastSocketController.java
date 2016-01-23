package com.ghsc.net.sockets.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import com.ghsc.common.Debug;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.net.encryption.AES;
import com.ghsc.net.sockets.ISocketController;
import com.ghsc.net.update.Version;
import com.ghsc.util.Tag;

/**
 * Multicasting allows GHSC to broadcast it's location (as IP) to anyone who's listening on the multicast address</br>
 * without actually knowing who is receiving the messages. MulticastSocketController handles both sending and receiving of multicast packets. When this MulticastSocketController receives a packet of someone's IP, we can start a TCP connection with them.
 */
public class MulticastSocketController implements ISocketController {
	
	public static final String ATT_IP = "ip", 
			ATT_PORT = "p", 
			ATT_VERSION = "v", 
			ATT_ID = "id", 
			ATT_USERNAME = "n";
	
	private static final int RECEIVE_BUFFER = 8192;
	private static final int SEND_DELAY = 500;
	private static final int CONNECT_DELAY = 2000;
	private static final int INTERFACE_UPDATE_DELAY = 10000;
	
	public static final int MULTICAST_PORT = 5688;
	public static final String MULTICAST_IP_ADDRESS = "224.0.0.115";
	public static final InetAddress MULTICAST_ADDRESS;
	public static final InetSocketAddress MULTICAST_SOCKET_ADDRESS;
	
	static {
		InetAddress multicastAddress;
		try {
			multicastAddress = InetAddress.getByName(MULTICAST_IP_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			multicastAddress = null;
		}
		MULTICAST_ADDRESS = multicastAddress;
		MULTICAST_SOCKET_ADDRESS = new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT);
	}
	
	private Set<String> interfaceNames = null;
	
	private final MulticastSocket receiveSocket;
	private final MulticastSocket sendSocket;
	
	private final int localUserPort;
	private volatile boolean isConnecting = false;
	
	private final Thread receiveWorker;
	private final Thread sendWorker;
	
	private volatile boolean running = true;
	
	/**
	 * Initializes a new MulticastManager.
	 * @throws IOException
	 */
	public MulticastSocketController(final int localUserPort) throws IOException {
		this.localUserPort = localUserPort;
		
		// Bind to wildcard (any) address.
		// This allows multicast packets to come in from all network interfaces.
		this.receiveSocket = new MulticastSocket(MULTICAST_PORT);
		this.receiveSocket.setTimeToLive(255);
		
		// join the multicast Group for all the network interfaces.
		this.interfaceNames = Application.NETWORK.getInterfaces();
		if (this.interfaceNames != null) {
			for (String name : this.interfaceNames) {
				final NetworkInterface xface = NetworkInterface.getByName(name);
				if (xface != null) {
					this.receiveSocket.joinGroup(MULTICAST_SOCKET_ADDRESS, xface);
				}
			}
		}
		
		// Bind to wildcard (any) address.
		// This allows multicast packets to be sent on all network interfaces.
		this.sendSocket = new MulticastSocket();
		this.sendSocket.setTimeToLive(255);
		
		this.receiveWorker = new Thread(this::multicastReceive);
		this.receiveWorker.setName("MulticastSocketController|Receive");
		this.sendWorker = new Thread(this::multicastSend);
		this.sendWorker.setName("MulticastSocketController|Send");
	}
	
	private void multicastReceive() {
		try {
			final byte[] buf = new byte[RECEIVE_BUFFER];
			final DatagramPacket pack = new DatagramPacket(buf, buf.length);
			while (this.running) {
				this.receiveSocket.receive(pack);
				final int length = pack.getLength();
				final byte[] buffer = Arrays.copyOf(buf, length);
				// new Thread(new Runnable() {
				// public void run() {
				final byte[] data = AES.DEFAULT.decrypt(buffer, 0, length);
				final MessageEvent parsedMessageEvent = MessageEvent.parse(new String(data, Application.CHARSET));
				if (parsedMessageEvent != null) {
					//System.out.println(parsedMessageEvent);
					this.multicastMessageReceived(parsedMessageEvent);
				} else {
					System.err.println("Unable to parse multicast message!");
				}
				// }}).start();
				pack.setLength(buf.length);
			}
		} catch (IOException e) {
			System.out.println("Multicast receive interrupted.");
		}
	}
	
	private void multicastMessageReceived(final MessageEvent message) {
		if (message == null) {
			return;
		}
		if (Debug.NORMAL.compareTo(Application.DEBUG) < 0) {
			System.out.println(message);
		}
		// Make sure we don't connect to ourselves and
		// only connect to someone who has a smaller UUID than we do
		// this prevents race conditions with who connects first
		final Application application = Application.getInstance();
		final String remoteUUIDString = message.getAttribute(ATT_ID);
		final UUID remoteUUID = UUID.fromString(remoteUUIDString);
		final UUID localUUID = application.getID();
		// If remoteUUID >= localUUID, then don't connect
		if (remoteUUID.compareTo(localUUID) >= 0) {
			return;
		}
		System.out.println(message);
		// Get remove address info
		final String remoteIP = message.getAttribute(ATT_IP);
		final String remotePortString = message.getAttribute(ATT_PORT);
		if ((remoteIP == null) || (remotePortString == null)) {
			return;
		}
		final int remotePort = Integer.parseInt(remotePortString);
		if (!application.getVersionController().isCompatible(Version.parse(message.getAttribute(ATT_VERSION)))) {
			return;
		}
		final UserContainer users = application.getMainFrame().getUsers();
		try {
			final InetSocketAddress remoteAddress = new InetSocketAddress(InetAddress.getByName(remoteIP), remotePort);
			if (!users.addMulticaster(remoteAddress)) {
				return;
			}
			{
				// Note: this isn't thread reliable, but will work because of the 500 ms delay.
				if (isConnecting) {
					return;
				}
				isConnecting = true;
			}
			System.out.println("Received " + remoteAddress.getAddress().getHostAddress() + " from MULTICASTER " + remotePort);
			
			application.getMainFrame().setStatus("Connecting to " + message.getAttribute(ATT_USERNAME), 0);
			
			final Socket socket = new Socket();
			socket.connect(remoteAddress, CONNECT_DELAY);
			final User user = new User(users, socket);
			if (!users.addUser(remoteAddress, user)) {
				System.err.println("Unable to add " + remoteAddress.getAddress().getHostAddress() + "@" + remoteAddress.getPort() + ".  User is already known.");
				socket.close();
			} else {
				System.out.println("Completed OUTGOING socket connection.  User is pending.");
				System.out.println("Connected to " + remoteAddress.getAddress().getHostAddress() + "@" + remoteAddress.getPort() + " - " + message.getAttribute(ATT_USERNAME));
				application.getMainFrame().setStatus("Connected to " + message.getAttribute(ATT_USERNAME), 1000);
				
				user.sendIntro(); // send identifying user info.
				user.start(); // starts up the receive thread.
				user.setFriend(users.isFriend(user)); // updates various user list status...
				user.setIgnored(users.isIgnored(user));
				users.getMainFrame().getChatContainer().refreshUser(user);
				users.refresh();
				users.removeMulticaster(remoteAddress); // no longer need to block multicasters.
			}
			
		} catch (IOException e) {
			if (e instanceof SocketTimeoutException) {
				System.out.println("Socket timed out trying to connect to " + remoteIP + "@" + remotePort + ". Maybe next time?");
			} else {
				System.out.println("Unable to connect to " + remoteIP + "@" + remotePort + ". Maybe next time?");
			}
			application.getMainFrame().setStatus("Failed to connect", 750);
		} finally {
			isConnecting = false;
		}
	}
	
	private void multicastSend() {
		try {
			final Application application = Application.getInstance();
			final UUID uuid = application.getID();
			final String uuidString = uuid.toString();
			long updateTime = System.currentTimeMillis() + INTERFACE_UPDATE_DELAY;
			while (this.running) {
				for (final String interfaceName : Application.NETWORK.getInterfaces()) {
					final String localIP = Application.NETWORK.getIp(interfaceName);
					final Tag pingMessage = Tag.construct(Type.PING, 
							ATT_VERSION, Application.VERSION, 
							ATT_IP, localIP, 
							ATT_PORT, this.localUserPort, 
							ATT_ID, uuidString, 
							ATT_USERNAME, application.getPreferredName());
					final byte[] data = AES.DEFAULT.encrypt(pingMessage.getEncodedString());
					if (data != null) {
						this.sendSocket.send(new DatagramPacket(data, data.length, MULTICAST_SOCKET_ADDRESS));
					}
				}
				Thread.sleep(SEND_DELAY);
				if (System.currentTimeMillis() > updateTime) {
					updateTime = System.currentTimeMillis() + INTERFACE_UPDATE_DELAY;
					Set<String> newInterfaceNames = Application.NETWORK.updateInterfaces();
					if (newInterfaceNames != null) {
						// Changes have occurred to the active interfaces.
						// First, unjoin interfaces no longer in the list...
						for (final String name : this.interfaceNames) {
							if (!newInterfaceNames.contains(name)) {
								NetworkInterface xface = NetworkInterface.getByName(name);
								if (xface != null) {
									this.receiveSocket.leaveGroup(MULTICAST_SOCKET_ADDRESS, xface);
								}
							}
						}
						// Second, join new interfaces in the list...
						for (String name : newInterfaceNames) {
							if (!this.interfaceNames.contains(name)) {
								NetworkInterface xface = NetworkInterface.getByName(name);
								if (xface != null) {
									this.receiveSocket.joinGroup(MULTICAST_SOCKET_ADDRESS, xface);
								}
							}
						}
						// Establish the new list...
						this.interfaceNames = newInterfaceNames;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts both sending and receiving.
	 */
	@Override
	public void start() {
		this.sendWorker.start();
		this.receiveWorker.start();
	}
	
	/**
	 * Closes the multicast socket.
	 */
	@Override
	public void close() {
		this.running = false;
		this.receiveSocket.close();
		this.sendSocket.close();
	}
	
}