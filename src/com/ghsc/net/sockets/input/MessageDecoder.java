package com.ghsc.net.sockets.input;

import java.util.concurrent.atomic.AtomicReference;

import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.gui.Application;
import com.ghsc.net.encryption.AES;

/**
 * Used to keep track of individual "message packets" as bytes are submitted to the wrapper.
 */
public class MessageDecoder {
	
	private enum State {
		NO_TAG, IN_TAG, BODY
	}
	
	private final EventListener<MessageEvent> callback;
	private final AtomicReference<AES> cipher;
	
	private State state = State.NO_TAG;
	private long tempLength;
	private byte[] buffer;
	private int offset;
	
	public MessageDecoder(final EventListener<MessageEvent> callback) {
		this(AES.DEFAULT, callback);
	}
	
	public MessageDecoder(final AES cipher, final EventListener<MessageEvent> callback) {
		this.callback = callback;
		this.cipher = new AtomicReference<>(cipher);
	}
	
	public AES getEncryption() {
		return this.cipher.get();
	}
	
	public void setEncryption(final AES cipher) {
		synchronized (this.cipher) {
			this.cipher.set(cipher);
		}
	}
	
	public void append(final byte[] buf, final int bufLen) {
		synchronized (this.cipher) {
			int bufOff = 0;
			outer:
			while (bufOff < bufLen) {
				switch (this.state) {
					case NO_TAG:
						for (; bufOff < bufLen; bufOff++) {
							if (((char) buf[bufOff]) == '<') {
                                this.state = State.IN_TAG;
                                this.tempLength = 0L;
								bufOff++;
								continue outer;
							}
						}
						break outer;
					case IN_TAG:
						for (; bufOff < bufLen; bufOff++) {
							final char c = (char) buf[bufOff];
							if (c == '>') {
                                this.state = State.BODY;
                                this.offset = 0;
                                this.buffer = new byte[(int) this.tempLength];
								bufOff++;
								continue outer;
							}
							final int i = Character.digit(c, 10);
							if (i >= 0) {
                                this.tempLength *= 10;
                                this.tempLength += i;
								if (this.tempLength <= Integer.MAX_VALUE) {
									continue;
								}
							}
                            this.state = State.NO_TAG;
                            this.tempLength = 0L;
						}
						break outer;
					case BODY:
					    if (this.buffer == null) {
					        throw new IllegalStateException("Buffer has not been initialized yet.");
                        }
						final int copyLength = Math.min(this.buffer.length - this.offset, bufLen - bufOff);
						System.arraycopy(buf, bufOff, this.buffer, this.offset, copyLength);
                        this.offset += copyLength;
						bufOff += copyLength;
						if (this.offset >= this.buffer.length) {
							final String parsed = new String(this.cipher.get().decrypt(this.buffer), Application.CHARSET);
                            this.callback.eventReceived(MessageEvent.parse(parsed));
                            this.state = State.NO_TAG;
                            this.offset = 0;
                            this.buffer = null;
						}
						break;
				}
			}
		}
	}
	
}