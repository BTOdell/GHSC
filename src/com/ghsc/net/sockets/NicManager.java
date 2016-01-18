package com.ghsc.net.sockets;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Set;

public class NicManager {
	private LinkedHashMap<String, String> currentInterfaces = null;
	
	public NicManager() {
		currentInterfaces = enumInterfaces();
	}
	
	public Set<String> getInterfaces() {
		if (currentInterfaces == null) {
			currentInterfaces = enumInterfaces();
		}
		return currentInterfaces.keySet();
	}
	
	private LinkedHashMap<String, String> enumInterfaces() {
		final LinkedHashMap<String, String> newInterfaces = new LinkedHashMap<String, String>();
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			System.err.println(e + ":  " + e.getMessage());
			return null;
		}
		for (NetworkInterface xface : Collections.list(interfaces)) {
			boolean isUp = false;
			try {
				isUp = xface.isUp();
			} catch (SocketException e) {
				System.err.println(e + ":  " + e.getMessage());
			}
			if (isUp) {
				printNetworkInterface(xface);
				for (InterfaceAddress interfaceAddress : xface.getInterfaceAddresses()) {
					InetAddress inetAddress = interfaceAddress.getAddress();
					boolean isReachable = true;
//					boolean isReachable = false;
//					try {
//						isReachable = inetAddress.isReachable(3000);
//					} catch (IOException e) {
//						System.err.println(e + ":  " + e.getMessage());
//					}
					if ((inetAddress instanceof Inet4Address) && inetAddress.isSiteLocalAddress() && isReachable) {
						newInterfaces.put(xface.getName(), inetAddress.getHostAddress());
						break;
					}
				}
			}
		}
		return newInterfaces;
	}
	
	public Set<String> updateInterfaces() {
		LinkedHashMap<String, String> newInterfaces = enumInterfaces();
		if (currentInterfaces.keySet().equals(newInterfaces.keySet())) {
			return null;
		}
		currentInterfaces = newInterfaces;
		return newInterfaces.keySet();
	}
	
	public String getIp(String interfaceName) {
		if (interfaceName != null) {
			return currentInterfaces.get(interfaceName);
		}
		return null;
	}
	
	public static void printNetworkInterface(final NetworkInterface networkInterface) {
		boolean isUp;
		boolean supportsMulticast;
		
		try {
			isUp = networkInterface.isUp();
			supportsMulticast = networkInterface.supportsMulticast();
		} catch (SocketException e) {
			System.err.println(e + ":  " + e.getMessage());
			return;
		}
		
		System.out.println("Name: " + networkInterface.getName());
		System.out.println("Display name: " + networkInterface.getDisplayName());
		System.out.println("Up: " + isUp);
		System.out.println("Virtual: " + networkInterface.isVirtual());
		System.out.println("Multicast: " + supportsMulticast);
	}
}
