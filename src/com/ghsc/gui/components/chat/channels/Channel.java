package com.ghsc.gui.components.chat.channels;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.ghsc.gui.components.chat.Chat;
import com.ghsc.gui.components.chat.ChatContainer;
import com.ghsc.gui.components.chat.ChatElementList;
import com.ghsc.gui.components.users.User;
import com.ghsc.util.TimeStamp;

/**
 * Provides visual access to view messages from any other users broadcasting on that channel.
 * @author Odell
 */
public class Channel extends Chat {
	
	public Channel(ChatContainer container, String name) {
		super(container, name);
		
		this.addElement(new ChannelElement(this.elements, TimeStamp.newInstance(), this.name, "Welcome!", null), false);
	}
	
	@Override
	protected void init() {
		this.panel = new JPanel();
		this.scrollPane = new JScrollPane();
		this.scrollPane.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent ce) {
				Channel.this.elements.invalidate();
			}
		});
		this.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.scrollPane.getVerticalScrollBar().setUnitIncrement(8);
		this.scrollPane.setViewportView(this.elements = new ChatElementList(this));
		
		GroupLayout gl_panel = new GroupLayout(this.panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addComponent(this.scrollPane, GroupLayout.DEFAULT_SIZE, 399, Short.MAX_VALUE)
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addComponent(this.scrollPane, GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
		);
		this.panel.setLayout(gl_panel);
	}
	
	/**
	 * Adds a ChannelElement to the end of this channel's list model.
	 * @param element - the ChannelElement to add.
	 * @param scroll - whether to scroll this element into view after it appears.
	 */
	synchronized public final void addElement(ChannelElement element, boolean scroll) {
		if (element == null) {
			return;
		}
		this.elements.addElement(element);
		if (scroll) {
			this.scrollBottom();
		}
	}
	
	/**
	 * Refreshes the chat display.
	 * @param u - the user to refresh.
	 */
	public void refreshUser(User u) {
		boolean changed = false;
		for (int i = 0; i < this.elements.getCount(); i++) {
			ChannelElement element = (ChannelElement) this.elements.get(i);
			if (u == null) {
				if (element.isMe()) {
					element.refreshAll();
					changed = true;
				}
			} else if (u.equals(element.getUser())) {
				element.refreshAll();
				changed = true;
			}
		}
		if (changed) {
			this.repaint();
		}
	}
	
	/**
	 * @return the current user count.
	 */
	public int getUserCount() {
		int count = 0;
		for (User u : this.container.frame.getUsers().getUserCollection()) {
			if (u.inChannel(this.name)) {
				count++;
			}
		}
		return count;
	}
	
}