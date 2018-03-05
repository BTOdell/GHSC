package com.ghsc.net.encryption;

import java.security.MessageDigest;

import com.ghsc.gui.Application;

public class SHA2 {
	
	private static final MessageDigest SHA256 = create("SHA-256");
	private static final MessageDigest SHA512 = create("SHA-512");
	
	private static MessageDigest create(String type) {
		try {
			return MessageDigest.getInstance(type);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static byte[] hash256Bytes(byte[] input) {
		return SHA256.digest(input);
	}
	
	public static byte[] hash256Bytes(String input) {
		return hash256Bytes(input.getBytes(Application.CHARSET));
	}
	
	public static String hash256String(byte[] input) {
		byte[] h = hash256Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static String hash256String(String input) {
		byte[] h = hash256Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static byte[] hash512Bytes(byte[] input) {
		return SHA512.digest(input);
	}
	
	public static byte[] hash512Bytes(String input) {
		return hash512Bytes(input.getBytes(Application.CHARSET));
	}
	
	public static String hash512String(byte[] input) {
		byte[] h = hash512Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static String hash512String(String input) {
		byte[] h = hash512Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static boolean verify(byte[] hash, byte[] input) {
		return hash == null ? input == null : input != null && MessageDigest.isEqual(hash, input);
	}
	
}