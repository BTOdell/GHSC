package ghsc.net.encryption;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ghsc.gui.Application;

/**
 * AES encryption cipher used to protect data passed across local networks.
 */
public class AES {
	
	private static final String CIPHER_TYPE = "AES/CBC/PKCS5Padding";
	private static final String KEY_TYPE = "AES";
	private static final byte[] IV = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	
	public static final AES DEFAULT = new AES(new byte[] { -101, -105, 53, -123, -17, -27, -32, 49, -48, -84, 60, -10, 47, 65, -62, -111 });
	
	private final byte[] key;
	private final Cipher e;
	private final Cipher d;
	
	public AES(final byte[] key) {
		this.key = key;
		this.e = this.create(key, Cipher.ENCRYPT_MODE);
		this.d = this.create(key, Cipher.DECRYPT_MODE);
	}
	
	private Cipher create(final byte[] key, final int mode) {
		try {
			final Cipher c = Cipher.getInstance(CIPHER_TYPE);
			c.init(mode, new SecretKeySpec(key, KEY_TYPE), new IvParameterSpec(IV));
			return c;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public byte[] encrypt(final Object data) {
		return this.encrypt(data.toString().getBytes(Application.CHARSET));
	}
	
	public byte[] encrypt(final byte[] data) {
		return this.encrypt(data, 0, data.length);
	}
	
	public synchronized byte[] encrypt(final byte[] data, final int offset, final int length) {
		try {
			return this.e.doFinal(data, offset, length);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public byte[] decrypt(final byte[] encrypted) {
		return this.decrypt(encrypted, 0, encrypted.length);
	}
	
	public synchronized byte[] decrypt(final byte[] encrypted, final int offset, final int length) {
		try {
			return this.d.doFinal(encrypted, offset, length);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public AES clone() {
		return new AES(this.key);
	}
	
	@Override
	public boolean equals(final Object o) {
		return o instanceof AES && SHA2.verify(((AES) o).key, this.key);
	}
	
	public static byte[] getRandomBytes(final int length) {
		final byte[] bytes = new byte[length];
		final SecureRandom sr = new SecureRandom();
		sr.nextBytes(bytes);
		return bytes;
	}

}