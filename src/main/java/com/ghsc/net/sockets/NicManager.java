package com.ghsc.net.sockets;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class NicManager {
	
	private Map<String, String> currentInterfaces;
	
	public NicManager() {
        this.currentInterfaces = this.enumInterfaces();
	}
	
	public Set<String> getInterfaces() {
		return this.currentInterfaces.keySet();
	}
	
	private Map<String, String> enumInterfaces() {
		final LinkedHashMap<String, String> newInterfaces = new LinkedHashMap<>();
		final Enumeration<NetworkInterface> interfaces;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (final SocketException e) {
			System.err.println(e + ":  " + e.getMessage());
			return Collections.emptyMap();
		}
		for (final NetworkInterface xface : Collections.list(interfaces)) {
			//printNetworkInterface(xface);
			boolean isUp = false;
			try {
				isUp = xface.isUp();
			} catch (final SocketException e) {
				System.err.println(e + ":  " + e.getMessage());
			}
			if (isUp) {
				//printNetworkInterface(xface);
				for (final InterfaceAddress interfaceAddress : xface.getInterfaceAddresses()) {
					final InetAddress inetAddress = interfaceAddress.getAddress();
					if ((inetAddress instanceof Inet4Address) &&
						(!inetAddress.isLoopbackAddress())) {
						final boolean isReachable = true;
//						boolean isReachable = false;
//						try {
//							isReachable = inetAddress.isReachable(3000);
//						} catch (IOException e) {
//							System.err.println(e + ":  " + e.getMessage());
//						}
						// TODO should we restrict to only IPv4?
						if (isReachable) {
							newInterfaces.put(xface.getName(), inetAddress.getHostAddress());
							break;
						}
					}
				}
			}
		}
		return newInterfaces;
	}
	
	public Set<String> updateInterfaces() {
		final Map<String, String> newInterfaces = this.enumInterfaces();
		if (this.currentInterfaces.keySet().equals(newInterfaces.keySet())) {
			return null;
		}
        this.currentInterfaces = newInterfaces;
		return newInterfaces.keySet();
	}
	
	public String getIp(final String interfaceName) {
		if (interfaceName != null) {
			return this.currentInterfaces.get(interfaceName);
		}
		return null;
	}
	
	private static void printNetworkInterface(final NetworkInterface networkInterface) {
		final boolean isUp;
		final boolean supportsMulticast;
		try {
			isUp = networkInterface.isUp();
			supportsMulticast = networkInterface.supportsMulticast();
		} catch (final SocketException e) {
			System.err.println(e + ":  " + e.getMessage());
			return;
		}
		System.out.println("Name: " + networkInterface.getName());
		System.out.println("Display name: " + networkInterface.getDisplayName());
		System.out.println("Up: " + isUp);
		System.out.println("Virtual: " + networkInterface.isVirtual());
		System.out.println("Multicast: " + supportsMulticast);
		System.out.println("Interface Addresses:");
		final List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
		if (!interfaceAddresses.isEmpty()) {
			for (final InterfaceAddress interfaceAddress : interfaceAddresses) {
				System.out.print("\tIP: ");
				System.out.print(interfaceAddress.getAddress());
				System.out.print(" - Broadcast: ");
				System.out.print(interfaceAddress.getBroadcast());
				System.out.println();
			}
		} else {
			System.out.println("\tNone");
		}
	}

}
