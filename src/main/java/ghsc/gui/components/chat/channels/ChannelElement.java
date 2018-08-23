package ghsc.gui.components.chat.channels;

import java.awt.Color;

import ghsc.gui.components.chat.ChatElement;
import ghsc.gui.components.chat.ChatElementList;
import ghsc.gui.components.users.User;
import ghsc.util.TimeStamp;

/**
 * Used to display a message/element in a Channel chat list.
 */
public class ChannelElement extends ChatElement {
	
	private static final long serialVersionUID = 1L;
	
	private ChannelElement(final ChatElementList container, final TimeStamp time, final String title, final String message, final boolean me) {
		super(container, time, title, message, me);
	}
	
	/**
	 * Creates for my messages.
	 */
	public ChannelElement(final ChatElementList container, final TimeStamp time, final String title, final String message) {
		super(container, time, title, message);
	}
	
	/**
	 * Creates for automated message.
	 */
	public ChannelElement(final ChatElementList container, final TimeStamp time, final String sender, final String title, final String message) {
		super(container, time, sender, title, message);
	}
	
	public ChannelElement(final ChatElementList container, final TimeStamp time, final String sender, final String title, final String message, final Color color) {
		super(container, time, sender, title, message, color);
	}
	
	/**
	 * Creates for actual user.
	 */
	public ChannelElement(final ChatElementList container, final TimeStamp time, final User sender, final String title, final String message, final boolean show, final Color color) {
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