package com.ghsc.net.sockets.input;

import java.util.concurrent.atomic.AtomicReference;

import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.gui.Application;
import com.ghsc.net.encryption.AES;

/**
 * Used to keep track of individual "message packets" as bytes are submitted to the wrapper.
 * @author Odell
 */
public class MessageDecoder {
	
	private enum State {
		NO_TAG, IN_TAG, BODY
	}
	
	private EventListener<MessageEvent> callback;
	private AtomicReference<AES> cipher;
	
	private State state = State.NO_TAG;
	private long tempLength = 0;
	private byte[] buffer = null;
	private int offset = 0;
	
	public MessageDecoder(EventListener<MessageEvent> callback) {
		this(AES.DEFAULT, callback);
	}
	
	public MessageDecoder(AES cipher, EventListener<MessageEvent> callback) {
		this.callback = callback;
		this.cipher = new AtomicReference<AES>(cipher);
	}
	
	public AES getEncryption() {
		return this.cipher.get();
	}
	
	public void setEncryption(AES cipher) {
		synchronized (this.cipher) {
			this.cipher.set(cipher);
		}
	}
	
	public void append(byte[] buf, int bufLen) {
		synchronized (cipher) {
			int bufOff = 0;
			outer:
			while (bufOff < bufLen) {
				switch (state) {
					case NO_TAG:
						for (; bufOff < bufLen; bufOff++) {
							if (((char) buf[bufOff]) == '<') {
								state = State.IN_TAG;
								tempLength = 0L;
								bufOff++;
								continue outer;
							}
						}
						break outer;
					case IN_TAG:
						for (; bufOff < bufLen; bufOff++) {
							final char c = (char) buf[bufOff];
							if (c == '>') {
								state = State.BODY;
								offset = 0;
								buffer = new byte[(int) tempLength];
								bufOff++;
								continue outer;
							}
							final int i = Character.digit(c, 10);
							if (i >= 0) {
								tempLength *= 10;
								tempLength += i;
								if (tempLength <= Integer.MAX_VALUE) {
									continue;
								}
							}
							state = State.NO_TAG;
							tempLength = 0L;
						}
						break outer;
					case BODY:
						final int copyLength = Math.min(buffer.length - offset, bufLen - bufOff);
						System.arraycopy(buf, bufOff, buffer, offset, copyLength);
						offset += copyLength;
						bufOff += copyLength;
						if (offset >= buffer.length) {
							final String parsed = new String(cipher.get().decrypt(buffer), Application.CHARSET);
							if (parsed != null) {
								callback.eventReceived(MessageEvent.parse(parsed));
							}
							state = State.NO_TAG;
							offset = 0;
							buffer = null;
						}
						continue outer;
				}
			}
		}
	}
	
}