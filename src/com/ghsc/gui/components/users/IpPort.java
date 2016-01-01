package com.ghsc.gui.components.users;

public class IpPort implements Comparable<IpPort> {
	private final String ip;
	private final int port;
	
	public IpPort(String ip, int port) {
		this.ip = new String(ip);
		this.port = port;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || !(obj instanceof IpPort))
			return false;
		if (obj == this)
			return true;
		IpPort o = (IpPort) obj;
		if (ip.equals(o.ip) && (port == o.port)) {
			return true;
		}
		return false;
	}
	
	public long toLong() {
		return toLong(ip, port);
	}
	
	public static long toLong(String ip, int port) {
		long result = 0;
		String[] fields = ip.split("\\.");
		for (String field : fields) {
			result <<= 8;
			result |= Long.parseLong(field);
		}
		result <<= 16;
		result |= port;
		return result;
	}
	
	public String toString() {
		return ((ip == null) ? "unknown" : ip) + ":" + port;
	}
	
	public String ip() {
		return new String(ip);
	}
	
	public int port() {
		return port;
	}
	
	@Override
	public int compareTo(IpPort arg) {
		if (arg == this)
			return 0;
		long longArg = arg.toLong();
		long longThis = this.toLong();
		if (longThis == longArg) {
			return 0;
		} else if (longThis < longArg) {
			return -1;
		}
		return 1;
	}
	
    @Override
    public int hashCode() {
        return (ip.hashCode() << 16) | port;
    }
}
