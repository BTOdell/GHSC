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
 * Created by Eclipse IDE.
 * @author Odell
 */
public class MessageThread {
	
	private static Charset UTF8 = Charset.forName("UTF-8");
	
	private IOWrapper io;
	private Thread workThread;
	private MessageDecoder decoder;
	private Runnable endOfStream;
	private boolean started = false;
	
	public MessageThread(IOWrapper io, EventListener<MessageEvent> callback, Runnable endOfStream) {
		this.io = io;
		this.endOfStream = endOfStream;
		this.decoder = new MessageDecoder(callback);
		this.workThread = new Thread(workRunnable);
		this.workThread.setName("MessageThread");
	}
	
	public void start() {
		if (!started) {
			started = true;
			this.workThread.start();
		}
	}
	
	public void setEncryption(AES cipher) {
		decoder.setEncryption(cipher);
	}
	
	private Runnable workRunnable = new Runnable() {
		public void run() {
			try {
				InputStream stream = io.getInputStream();
				byte[] buf = new byte[8192];
				int bufLength;
				while ((bufLength = stream.read(buf)) >= 0) {
					decoder.append(buf, bufLength);
				}
			} catch (IOException io) {}
			MessageThread.this.endOfStream.run();
		}
	};
	
	/**
	 * @return the IOWrapper that this MessageThread encloses.
	 */
	public IOWrapper getIO() {
		return io;
	}
	
	/**
	 * Encrypts the given data string into bytes,</br>
	 * then marks the bytes so they can be reassembled as packets on the other side</br>
	 * and sends them through the TCP socket to be received by the connected user.
	 * @param data - the data to send through the socket.
	 */
	public synchronized void send(Tag tag) {
		if (io != null) {
			try {
				OutputStream out = io.getOutputStream();
				byte[] encrypted = decoder.getEncryption().encrypt(tag.getEncodedString());
				out.write(("<" + encrypted.length + ">").getBytes(UTF8));
				out.write(encrypted);
				out.flush();
			} catch (SocketException se) {
				System.out.println("Socket write error.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public interface IOWrapper {
		public InputStream getInputStream() throws IOException;
		public OutputStream getOutputStream() throws IOException;
	}
	
}