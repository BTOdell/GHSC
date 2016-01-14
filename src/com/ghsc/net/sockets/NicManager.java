package com.ghsc.net.sockets;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class NicManager {
	
	private final LinkedHashMap<String, InterfaceProperties> interfaces = new LinkedHashMap<String, InterfaceProperties>();
	private NetworkInterface defaultNetworkInterface = null;
	private String defaultName = null;

	public NicManager() {
		System.out.println("NicManager");
		try {
			for (NetworkInterface xface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				printNetworkInterface(xface);
				for (InterfaceAddress interfaceAddress : xface.getInterfaceAddresses()) {
					InetAddress inetAddress = interfaceAddress.getAddress();
					InetAddress broadcast = interfaceAddress.getBroadcast();
					System.out.println("Address: " + inetAddress + "[" + (inetAddress != null ? inetAddress.getClass() : null) + "]");
					System.out.println("Broadcast Address: " + broadcast + "[" + (broadcast != null ? broadcast.getClass() : null) + "]");
					if ((inetAddress instanceof Inet4Address) && inetAddress.isSiteLocalAddress() && inetAddress.isReachable(3000)) {
						InterfaceProperties parms = new InterfaceProperties(new String(inetAddress.getHostAddress()), new String(broadcast.getHostAddress()), interfaceAddress.getNetworkPrefixLength(), new String(xface.getDisplayName()));
						interfaces.put(xface.getName(),  parms);
						//if ((defaultName == null) || !defaultName.startsWith("eth")) {
						if ((defaultName == null) || !defaultName.startsWith("vir")) {
							defaultNetworkInterface = xface;
							defaultName = new String(xface.getName());
						}
					}
				}
			}
		} catch (IOException e) {}
	}
	
	public String getIP() {
		InterfaceProperties parms = null;
		if ((defaultName == null) || (parms = interfaces.get(defaultName)) == null) {
			return null;
		}
		return parms.localIP;
	}
	
	public String getBroadcast() {
		InterfaceProperties parms = null;
		if ((defaultName == null) || (parms = interfaces.get(defaultName)) == null) {
			return null;
		}
		return parms.broadcastIP;
	}
	
	public int getPrefixLength() {
		InterfaceProperties parms = null;
		if ((defaultName == null) || (parms = interfaces.get(defaultName)) == null) {
			return 0;
		}
		return parms.prefixLength;
	}
	
	public String getDescription() {
		InterfaceProperties parms = null;
		if ((defaultName == null) || (parms = interfaces.get(defaultName)) == null) {
			return null;
		}
		return parms.description;
	}
	
	public String getDefault() {
		if (defaultName == null) {
			return null;
		}
		return new String(defaultName);
	}
	
	public NetworkInterface getDefaultInterface() {
		return this.defaultNetworkInterface;
	}
	
	public HashSet<String> getNics() {
		return new HashSet<String>(interfaces.keySet());
	}
	
	public InterfaceProperties getProperties(String interfaceName) {
		InterfaceProperties parms = null;
		if ((interfaceName == null) || (parms = interfaces.get(interfaceName)) == null) {
			return null;
		}
		return parms;
	}
	
	public int size() {
		return interfaces.size();
	}
	
	public void setDefault(String interfaceName) {
		if (interfaceName == null) {
			return;
		}
		final Set<String> keysetCopy = new HashSet<String>(interfaces.keySet());
		for (String name : keysetCopy) {
			if (name.equalsIgnoreCase(interfaceName)) {
				defaultName = name;
			}
		}
	}
	
	public static void printNetworkInterface(final NetworkInterface networkInterface) throws SocketException {
		System.out.println("Name: " + networkInterface.getName());
		System.out.println("Display name: " + networkInterface.getDisplayName());
		System.out.println("Up: " + networkInterface.isUp());
		System.out.println("Virtual: " + networkInterface.isVirtual());
		System.out.println("Multicast: " + networkInterface.supportsMulticast());
	}
	
	public class InterfaceProperties {
		
		public final String localIP;
		public final String broadcastIP;
		public final short prefixLength;
		public final String description;
		
		public InterfaceProperties(String localIP, String broadcastIP, short prefixLength, String description) {
			this.localIP = localIP;
			this.broadcastIP = broadcastIP;
			this.prefixLength = prefixLength;
			this.description = description;
		}
	}
}
