package com.ghsc.net.sockets;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class NicManager {
	private final LinkedHashMap<String, InterfaceProperties> interfaces = new LinkedHashMap<String, InterfaceProperties>();
	private String defaultName = null;

	public NicManager() {
		try {
			for (NetworkInterface xface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				for (InterfaceAddress interfaceAddress : xface.getInterfaceAddresses()) {
					InetAddress inetAddress = interfaceAddress.getAddress();
					InetAddress broadcast = interfaceAddress.getBroadcast();
					if ((inetAddress instanceof Inet4Address) && inetAddress.isSiteLocalAddress() && inetAddress.isReachable(3000)) {
						InterfaceProperties parms = new InterfaceProperties(new String(inetAddress.getHostAddress()), new String(broadcast.getHostAddress()), interfaceAddress.getNetworkPrefixLength(), new String(xface.getDisplayName()));
						interfaces.put(xface.getName(),  parms);
						if ((defaultName == null) || !defaultName.startsWith("eth")) {
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
