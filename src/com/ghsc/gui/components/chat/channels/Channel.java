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
		
		this.addElement(new ChannelElement(elements, TimeStamp.newInstance(), this.name, "Welcome!", null), false);
	}
	
	@Override
	protected void init() {
		panel = new JPanel();
		scrollPane = new JScrollPane();
		scrollPane.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent ce) {
				elements.invalidate();
			}
		});
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(8);
		scrollPane.setViewportView(elements = new ChatElementList(this));
		
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 399, Short.MAX_VALUE)
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE)
		);
		panel.setLayout(gl_panel);
	}
	
	/**
	 * Adds a ChannelElement to the end of this channel's list model.
	 * @param element - the ChannelElement to add.
	 * @param scroll - whether to scroll this element into view after it appears.
	 */
	synchronized public final void addElement(ChannelElement element, boolean scroll) {
		if (element == null)
			return;
		elements.addElement(element);
		if (scroll)
			scrollBottom();
	}
	
	/**
	 * Refreshes the chat display.
	 * @param u - the user to refresh.
	 */
	public void refreshUser(User u) {
		boolean changed = false;
		for (int i = 0; i < elements.getCount(); i++) {
			ChannelElement element = (ChannelElement) elements.get(i);
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
		if (changed)
			repaint();
	}
	
	/**
	 * @return the current user count.
	 */
	public int getUserCount() {
		int count = 0;
		for (User u : container.frame.getUsers().getUserCollection()) {
			if (u.inChannel(name))
				count++;
		}
		return count;
	}
	
}