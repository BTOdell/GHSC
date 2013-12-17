package com.ghsc.net.encryption;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.ghsc.gui.Application;

/**
 * AES encryption cipher used to protect data passed across local networks.
 * @author Odell
 */
public class AES {
	
	public static final AES DEFAULT = new AES(new byte[] { -101, -105, 53, -123, -17, -27, -32, 49, -48, -84, 60, -10, 47, 65, -62, -111 });
	private static final String type = "AES";
	
	private final byte[] key;
	private final Cipher e, d;
	
	public AES(final byte[] key) {
		this.key = key;
		this.e = create(key, Cipher.ENCRYPT_MODE);
		this.d = create(key, Cipher.DECRYPT_MODE);
	}
	
	private Cipher create(final byte[] key, final int mode) {
		try {
			Cipher c = Cipher.getInstance(type);
			c.init(mode, new SecretKeySpec(key, type));
			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] encrypt(final Object data) {
		return encrypt(data.toString().getBytes(Application.CHARSET));
	}
	
	public byte[] encrypt(final byte[] data) {
		return encrypt(data, 0, data.length);
	}
	
	public synchronized byte[] encrypt(final byte[] data, final int offset, final int length) {
		try {
			return e.doFinal(data, offset, length);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] decrypt(final byte[] encrypted) {
		return decrypt(encrypted, 0, encrypted.length);
	}
	
	public synchronized byte[] decrypt(final byte[] encrypted, final int offset, final int length) {
		try {
			return d.doFinal(encrypted, offset, length);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public AES clone() {
		return new AES(key);
	}
	
	@Override
	public boolean equals(Object o) {
		return o != null && o instanceof AES && SHA2.verify(((AES) o).key, key);
	}
	
	public static byte[] getRandomBytes(int length) {
		byte[] bytes = new byte[length];
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(bytes);
		return bytes;
	}

}