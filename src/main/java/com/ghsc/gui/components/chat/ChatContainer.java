package com.ghsc.gui.components.chat;

import com.ghsc.common.Fonts;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.impl.Filter;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * Provides a container for Chats and some basic functions for adding and removing Chats from this container.
 */
public class ChatContainer extends JTabbedPane {
	
	private static final long serialVersionUID = 1L;
	
	public final MainFrame frame;
	private final ArrayList<Chat> chats = new ArrayList<>();
	
	private Chat lastChat;
	
	/**
	 * Initializes a new ChannelContainer given the supporting main frame.
	 * @param frame - the frame which supports this ChannelContainer.
	 */
	public ChatContainer(final MainFrame frame) {
		this.frame = frame;
        this.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        this.setDoubleBuffered(true);
        this.setFocusable(false);
        this.addChangeListener(unused -> {
            final UserContainer users = this.frame.getUsers();
            if (users != null) {
                if (this.lastChat != null) {
                    this.lastChat.setSelection(null);
                }
                this.lastChat = this.getSelectedChat();
                users.refresh();
            }
        });
		Application.getInstance().getPopupManager().submit((menu, popupManager, sender, x, y) -> {
            final int tabIndex = this.indexAtLocation(x, y);
            if (tabIndex >= 0) {
                final JMenuItem mi = menu.createItem("Leave", ae -> this.remove(this.getSelectedChat()));
                mi.setFont(Fonts.GLOBAL);
                menu.add(mi);
                final JMenuItem ci = menu.createItem("Clear", ae -> {
                    final Chat chat = this.getSelectedChat();
                    if (chat != null) {
                        chat.getElements().clear();
                    }
                });
                ci.setFont(Fonts.GLOBAL);
                menu.add(ci);
                return true;
            }
            return false;
        }, this);
	}
	
	/**
	 * @return how many chats are contained in this ChatContainer.
	 */
	public int getCount() {
		return this.chats.size();
	}
	
	/**
	 * @return the current visible/selected chat, or 'null' if the ChatContainer is empty.
	 */
	public Chat getSelectedChat() {
		final int index = this.getSelectedIndex();
		return index >= 0 ? this.chats.get(index) : null;
	}
	
	/**
	 * Gets the first chat found in this container.
	 * @param name - the name of the chat to search for.
	 * @return a Chat if found, otherwise null.
	 */
	public Chat getChat(final String name) {
		synchronized (this.chats) {
			for (final Chat chat : this.chats) {
				if (chat.getName().equals(name)) {
                    return chat;
                }
			}
		}
		return null;
	}
	
	/**
	 * Adds the given chat to this container as long as one doesn't already exist with the same name.</br>
	 * If the type of chat is a Channel, all users are notified that you've joined this channel.
	 * @param chat - the chat to add to this container.
	 */
	public void add(final Chat chat) {
		synchronized (this.chats) {
			if (this.getChat(chat.getName()) != null) {
                return;
            }
			this.chats.add(chat);
			//addTab(chat.getName(), null, chat.getPanel(), null);
            this.add(chat.getName(), chat.getPanel());
			//setTabComponentAt(chats.size() - 1, null);
			// TODO: make X on tabs.
			if (chat instanceof Channel) {
				this.frame.getUsers().send(MessageEvent.construct(Type.JOIN, User.ATT_CHANNEL, chat.getName()), User.ALL);
			}
		}
	}
	
	/**
	 * Removes the given chat from this container, if found.</br>
	 * If the type of chat is a Channel, all users are notified that you've left this channel.
	 * @param chat - the chat to remove from this container.
	 */
	public void remove(final Chat chat) {
		if (chat == null) {
            return;
        }
		synchronized (this.chats) {
			final int index = this.chats.indexOf(chat);
			if (index >= 0) {
                this.removeTabAt(index);
				this.chats.remove(index);
				if (chat instanceof Channel) {
					this.frame.getUsers().send(MessageEvent.construct(Type.LEAVE, User.ATT_CHANNEL, chat.getName()), User.ALL);
				}
			}
		}
	}
	
	/**
	 * @return an array of all the chats in this ChatContainer.
	 */
	public Chat[] getAll() {
		return this.getAll(chat -> true);
	}
	
	public Chat[] getAll(final Filter<Chat> filter) {
		final ArrayList<Chat> chats = new ArrayList<>(this.chats.size());
		synchronized (this.chats) {
			for (final Chat chat : this.chats) {
				if (chat != null && filter.accept(chat)) {
					chats.add(chat);
				}
			}
		}
		return chats.toArray(new Chat[chats.size()]);
	}
	
	public String[] getAllAsStrings() {
		return this.getAllAsStrings(chat -> true);
	}
	
	public String[] getAllAsStrings(final Filter<Chat> filter) {
		final ArrayList<String> chats = new ArrayList<>(this.chats.size());
		synchronized (this.chats) {
			for (final Chat chat : this.chats) {
				if (chat != null && filter.accept(chat)) {
					chats.add(chat.getName());
				}
			}
		}
		return chats.toArray(new String[chats.size()]);
	}
	
	public void refreshUser(final User u) {
		synchronized (this.chats) {
			for (final Chat chat : this.chats) {
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
	
	/**
	 * @return a String of chat names separated by ','.
	 */
	public String printChats() {
		final StringJoiner joiner = new StringJoiner(",");
		synchronized (this.chats) {
		    for (final Chat c : this.chats) {
		        joiner.add(c.getName());
            }
		}
		return joiner.toString();
	}
	
	/**
	 * @return a String of chat names separated by ','.
	 */
	public String printChannels() {
		final StringJoiner joiner = new StringJoiner(",");
		synchronized (this.chats) {
			for (final Chat c : this.chats) {
				if (c == null || !(c instanceof Channel)) {
					continue;
				}
				joiner.add(c.getName());
			}
		}
		return joiner.toString();
	}
	
}