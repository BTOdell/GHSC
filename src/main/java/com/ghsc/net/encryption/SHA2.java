package com.ghsc.net.encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.ghsc.gui.Application;

public class SHA2 {
	
	private static final MessageDigest SHA256 = create("SHA-256");
	private static final MessageDigest SHA512 = create("SHA-512");
	
	private static MessageDigest create(final String type) {
		try {
			return MessageDigest.getInstance(type);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] hash256Bytes(final byte[] input) {
		return SHA256.digest(input);
	}
	
	public static byte[] hash256Bytes(final String input) {
		return hash256Bytes(input.getBytes(Application.CHARSET));
	}
	
	public static String hash256String(final byte[] input) {
		final byte[] h = hash256Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static String hash256String(final String input) {
		final byte[] h = hash256Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static byte[] hash512Bytes(final byte[] input) {
		return SHA512.digest(input);
	}
	
	public static byte[] hash512Bytes(final String input) {
		return hash512Bytes(input.getBytes(Application.CHARSET));
	}
	
	public static String hash512String(final byte[] input) {
		final byte[] h = hash512Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static String hash512String(final String input) {
		final byte[] h = hash512Bytes(input);
		if (h == null) {
            return null;
        }
		return new String(h, Application.CHARSET);
	}
	
	public static boolean verify(final byte[] hash, final byte[] input) {
		return hash == null ? input == null : input != null && MessageDigest.isEqual(hash, input);
	}
	
}