package com.ghsc.gui.components.chat.channels;

import java.awt.Color;

import com.ghsc.gui.components.chat.ChatElement;
import com.ghsc.gui.components.chat.ChatElementList;
import com.ghsc.gui.components.users.User;
import com.ghsc.util.TimeStamp;

/**
 * Used to display a message/element in a Channel chat list.
 * @author Odell
 */
public class ChannelElement extends ChatElement {
	
	private static final long serialVersionUID = 1L;
	
	private ChannelElement(ChatElementList container, TimeStamp time, String title, String message, boolean me) {
		super(container, time, title, message, me);
	}
	
	/**
	 * Creates for my messages.
	 */
	public ChannelElement(ChatElementList container, TimeStamp time, String title, String message) {
		super(container, time, title, message);
	}
	
	/**
	 * Creates for automated message.
	 */
	public ChannelElement(ChatElementList container, TimeStamp time, String sender, String title, String message) {
		super(container, time, sender, title, message);
	}
	
	public ChannelElement(ChatElementList container, TimeStamp time, String sender, String title, String message, Color color) {
		super(container, time, sender, title, message, color);
	}
	
	/**
	 * Creates for actual user.
	 */
	public ChannelElement(ChatElementList container, TimeStamp time, User sender, String title, String message, boolean show, Color color) {
		super(container, time, sender, title, message, show, color);
	}
	
	protected void refreshSender() {
		super.refreshSender();
	}
	
	protected void refreshText() {
		super.refreshText();
	}
	
	protected void refreshAll() {
		super.refreshAll();
	}
	
}