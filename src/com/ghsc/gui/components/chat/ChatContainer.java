package com.ghsc.gui.components.chat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ghsc.common.Fonts;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.popup.Popup;
import com.ghsc.gui.components.popup.PopupBuilder;
import com.ghsc.gui.components.popup.PopupManager;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.impl.Filter;

/**
 * Provides a container for Chats and some basic functions for adding and removing Chats from this container.
 * @author Odell
 */
public class ChatContainer extends JTabbedPane {
	
	private static final long serialVersionUID = 1L;
	
	public MainFrame frame;
	private ArrayList<Chat> chats = new ArrayList<Chat>();
	
	private Chat lastChat = null;
	
	/**
	 * Used for GUI builders.
	 */
	public ChatContainer() {
		super();
		setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		setDoubleBuffered(true);
		setFocusable(false);
	}
	
	/**
	 * Initializes a new ChannelContainer given the supporting main frame.
	 * @param frame - the frame which supports this ChannelContainer.
	 */
	public ChatContainer(MainFrame frame) {
		this();
		if (frame.getApplication() != null) {
			this.frame = frame;
			addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent arg0) {
					UserContainer users = ChatContainer.this.frame.getUsers();
					if (users != null) {
						if (lastChat != null)
							lastChat.setSelection(null);
						lastChat = getSelectedChat();
						users.refresh();
					}
				}
			});
			frame.getApplication().getPopupManager().submit(new PopupBuilder() {
				public boolean build(Popup menu, PopupManager popupManager, Component sender, int x, int y) {
					int tabIndex = indexAtLocation(x, y);
					if (tabIndex >= 0) {
						JMenuItem mi = menu.createItem("Leave", new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								remove(getSelectedChat());
							}
						});
						mi.setFont(Fonts.GLOBAL);
						menu.add(mi);
						JMenuItem ci = menu.createItem("Clear", new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								Chat chat = getSelectedChat();
								if (chat != null) {
									chat.getElements().clear();
								}
							}
						});
						ci.setFont(Fonts.GLOBAL);
						menu.add(ci);
						return true;
					}
					return false;
				}
			}, this);
		}
	}
	
	/**
	 * @return how many chats are contained in this ChatContainer.
	 */
	public int getCount() {
		return chats.size();
	}
	
	/**
	 * @return the current visible/selected chat, or 'null' if the ChatContainer is empty.
	 */
	public Chat getSelectedChat() {
		int index = getSelectedIndex();
		return index >= 0 ? chats.get(index) : null;
	}
	
	/**
	 * Gets the first chat found in this container.
	 * @param name - the name of the chat to search for.
	 * @return a Chat if found, otherwise null.
	 */
	public Chat getChat(String name) {
		synchronized (chats) {
			for (Chat chat : chats) {
				if (chat.getName().equals(name))
					return chat;
			}
		}
		return null;
	}
	
	/**
	 * Adds the given chat to this container as long as one doesn't already exist with the same name.</br>
	 * If the type of chat is a Channel, all users are notified that you've joined this channel.
	 * @param chat - the chat to add to this container.
	 */
	public void add(Chat chat) {
		synchronized (chats) {
			if (getChat(chat.getName()) != null)
				return;
			chats.add(chat);
			//addTab(chat.getName(), null, chat.getPanel(), null);
			add(chat.getName(), chat.getPanel());
			//setTabComponentAt(chats.size() - 1, null);
			// TODO: make X on tabs.
			if (chat instanceof Channel) {
				frame.getUsers().send(MessageEvent.construct(Type.JOIN, User.ATT_CHANNEL, chat.getName()), User.ALL);
			}
		}
	}
	
	/**
	 * Removes the given chat from this container, if found.</br>
	 * If the type of chat is a Channel, all users are notified that you've left this channel.
	 * @param chat - the chat to remove from this container.
	 */
	public void remove(Chat chat) {
		if (chat == null)
			return;
		synchronized (chats) {
			int index = chats.indexOf(chat);
			if (index >= 0) {
				removeTabAt(index);
				chats.remove(index);
				if (chat instanceof Channel) {
					frame.getUsers().send(MessageEvent.construct(Type.LEAVE, User.ATT_CHANNEL, chat.getName()), User.ALL);
				}
			}
		}
	}
	
	/**
	 * @return an array of all the chats in this ChatContainer.
	 */
	public Chat[] getAll() {
		return getAll(new Filter<Chat>() {
			public boolean accept(Chat chat) {
				return true;
			}
		});
	}
	
	public Chat[] getAll(Filter<Chat> filter) {
		final ArrayList<Chat> chats = new ArrayList<Chat>(this.chats.size());
		synchronized (this.chats) {
			for (Chat chat : this.chats) {
				if (chat != null && filter.accept(chat)) {
					chats.add(chat);
				}
			}
		}
		return chats.toArray(new Chat[chats.size()]);
	}
	
	public String[] getAllAsStrings() {
		return getAllAsStrings(new Filter<Chat>() {
			public boolean accept(Chat chat) {
				return true;
			}
		});
	}
	
	public String[] getAllAsStrings(Filter<Chat> filter) {
		final ArrayList<String> chats = new ArrayList<String>(this.chats.size());
		synchronized (this.chats) {
			for (Chat chat : this.chats) {
				if (chat != null && filter.accept(chat)) {
					chats.add(chat.getName());
				}
			}
		}
		return chats.toArray(new String[chats.size()]);
	}
	
	public void refreshUser(User u) {
		synchronized (chats) {
			for (Chat chat : chats) {
				if (chat != null) {
					if (chat instanceof Channel) {
						if (u == null || u.inChannel(chat.getName())) {
							((Channel) chat).refreshUser(u);
						}
					} else {
						// TODO: implement private messaging
					}
				}
			}
		}
	}
	
	/**
	 * @return a String of chat names separated by ','.
	 */
	public String printChats() {
		StringBuilder build = new StringBuilder();
		synchronized (chats) {
			for (int i = 0; i < chats.size(); i++) {
				build.append(chats.get(i).getName());
				if (i + 1 < chats.size())
					build.append(',');
			}
		}
		return build.toString();
	}
	
	/**
	 * @return a String of chat names separated by ','.
	 */
	public String printChannels() {
		StringBuilder build = new StringBuilder();
		synchronized (chats) {
			for (int i = 0; i < chats.size(); i++) {
				Chat c = chats.get(i);
				if (c == null || !(c instanceof Channel))
					continue;
				build.append(c.getName());
				build.append(',');
			}
		}
		if (build.length() > 0)
			build.deleteCharAt(build.length() - 1);
		return build.toString();
	}
	
}