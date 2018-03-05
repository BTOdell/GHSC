package com.ghsc.gui.components.chat;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.ghsc.common.Colors;

public abstract class Chat {

	protected String name;
	
	protected ChatContainer container;
	
	protected JPanel panel;
	protected ChatElementList elements;
	protected JScrollPane scrollPane;
	
	private ChatElement lastElement;
	
	/**
	 * Initializes a new Chat with a container and name.
	 * @param container - a ChatContainer which contains this Chat.
	 * @param name - the name of the chat.
	 */
	public Chat(ChatContainer container, String name) {
		this.container = container;
		this.name = name;

		this.init();
	}
	
	/**
	 * Sets up the visual aspects of this chat.
	 */
	protected abstract void init();
	
	protected void setSelection(ChatElement element) {
		if (this.lastElement != null) {
			this.lastElement.setBackground(null);
		}
		if (element != null) {
			element.setBackground(Colors.CELLRENDER_BACKGROUND);
		}
		this.lastElement = element;
	}
	
	/**
	 * Will scroll to the bottom.
	 */
	public void scrollBottom() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JScrollBar v = Chat.this.scrollPane.getVerticalScrollBar();
				if (v != null) {
                    v.setValue(v.getMaximum());
                }
			}
		});
	}
	
	public void repaint() {
		this.elements.repaint();
	}
	
	/**
	 * @return the container of this Chat.
	 */
	public final ChatContainer getContainer() {
		return this.container;
	}
	
	/**
	 * @return the name of this channel.
	 */
	public final String getName() {
		return this.name;
	}
	
	/**
	 * @return the JPanel displaying this Chat.
	 */
	public final JPanel getPanel() {
		return this.panel;
	}
	
	/**
	 * @return the ChatElementList of this channel.
	 */
	public final ChatElementList getElements() {
		return this.elements;
	}
	
	/**
	 * @return the scrollpane containing ChatElementList.
	 */
	public final JScrollPane getScrollPane() {
		return this.scrollPane;
	}
}