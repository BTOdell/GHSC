package com.ghsc.net.sockets.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.charset.Charset;

import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.net.encryption.AES;
import com.ghsc.util.Tag;

/**
 * A utility for reading and writing to an IO stream using a dedicated thread.
 */
public class MessageThread {
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	private final IOWrapper io;
	private final Thread workThread;
	private final MessageDecoder decoder;
	private final Runnable endOfStream;

	private boolean started;
	
	public MessageThread(final IOWrapper io, final EventListener<MessageEvent> callback, final Runnable endOfStream) {
		this.io = io;
		this.endOfStream = endOfStream;
		this.decoder = new MessageDecoder(callback);
		this.workThread = new Thread(() -> {
			try {
				final InputStream stream = this.io.getInputStream();
				final byte[] buf = new byte[8192];
				int bufLength;
				while ((bufLength = stream.read(buf)) >= 0) {
					this.decoder.append(buf, bufLength);
				}
			} catch (final IOException ignored) {
			}
			this.endOfStream.run();
		});
		this.workThread.setName("MessageThread");
	}
	
	public void start() {
		if (!this.started) {
            this.started = true;
			this.workThread.start();
		}
	}
	
	public void setEncryption(final AES cipher) {
        this.decoder.setEncryption(cipher);
	}

	/**
	 * @return the IOWrapper that this MessageThread encloses.
	 */
	public IOWrapper getIO() {
		return this.io;
	}
	
	/**
	 * Encrypts the given data string into bytes,</br>
	 * then marks the bytes so they can be reassembled as packets on the other side</br>
	 * and sends them through the TCP socket to be received by the connected user.
	 * @param tag The data to send through the socket.
	 */
	public synchronized void send(final Tag tag) {
		try {
			final OutputStream out = this.io.getOutputStream();
			final byte[] encrypted = this.decoder.getEncryption().encrypt(tag.getEncodedString());
			out.write(("<" + encrypted.length + ">").getBytes(UTF8));
			out.write(encrypted);
			out.flush();
		} catch (final SocketException se) {
			System.out.println("Socket write error.");
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
	
	public interface IOWrapper {
		InputStream getInputStream() throws IOException;
		OutputStream getOutputStream() throws IOException;
	}
	
}