package com.ghsc.gui.components.users;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import com.ghsc.common.Colors;
import com.ghsc.common.Fonts;
import com.ghsc.gui.Application;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.chat.Chat;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.users.User.Status;
import com.ghsc.impl.Filter;
import com.ghsc.util.Tag;

/**
 * UserContainer is used to show current users in a channel.
 * Not only that, but it also containers all users currently running the application.
 * It also sorts users by their status and names. Friends at the top, ignored users at the bottom.
 */
public class UserContainer extends JList<User> {
	
	private static final long serialVersionUID = 1L;
	
	private final MainFrame frame;
	private final DefaultListModel<User> model;

	private final HashMap<InetSocketAddress, User> users;
	private final HashMap<InetSocketAddress, User> usersPending;
	private final ArrayList<InetSocketAddress> knownMulticasters;
	private final ArrayList<UUID> friends;
	private final ArrayList<UUID> ignored;
	
	/**
	 * Private helper constructor.
	 */
	private UserContainer(final MainFrame frame, final DefaultListModel<User> model,
                          final String[] allFriends, final String[] allIgnored) {
	    super(model);
        this.frame = frame;
        this.model = model;
        this.users = new HashMap<>();
        this.usersPending = new HashMap<>();
        this.knownMulticasters = new ArrayList<>();
        if (allFriends != null) {
            this.friends = new ArrayList<>(allFriends.length);
            for (final String friendItem : allFriends) {
                if (friendItem == null) {
                    continue;
                }
                this.friends.add(UUID.fromString(friendItem));
            }
        } else {
            this.friends = new ArrayList<>();
        }
        if (allIgnored != null) {
            this.ignored = new ArrayList<>(allIgnored.length);
            for (final String ignoreItem : allIgnored) {
                if (ignoreItem == null) {
                    continue;
                }
                this.ignored.add(UUID.fromString(ignoreItem));
            }
        } else {
            this.ignored = new ArrayList<>();
        }

        this.setCellRenderer(new UserCellRenderer());
        this.setDoubleBuffered(true);
        this.setDragEnabled(true);
        this.setTransferHandler(new TransferHandler());
	}
	
	/**
	 * Initializes a new UserContainer.
	 * @param frame The main frame that this UserContainer is visible from.
	 * @param allFriends The friends list that was retrieved from an earlier save.
	 * @param allIgnored The ignored list that was retrieved from an earlier save.
	 */
	public UserContainer(final MainFrame frame, final String[] allFriends, final String[] allIgnored) {
	    this(frame, new DefaultListModel<>(), allFriends, allIgnored);
	}

    /**
     * Gets the parent main GUI frame of this user container.
     */
	public MainFrame getMainFrame() {
		return this.frame;
	}
	
	/**
	 * Refreshes the visual display of this UserContainer.
	 * This method is under a synchronized lock to prevent multiple refreshes at the same time.
	 */
	public void refresh() {
		synchronized (this.users) {
			this.model.clear();
			final Chat currChat = this.frame.getChatContainer().getSelectedChat();
            if (currChat instanceof Channel) {
                boolean valid = false;
                final ArrayList<User> all = this.getUserCollection();
                if (!all.isEmpty()) {
                    this.setEnabled(true);
                    Collections.sort(all);
                    for (final User curr : all) {
                        if (curr.getPreferredName() != null) {
                            valid = true;
                            this.model.addElement(curr);
                        }
                    }
                }
                if (!valid) {
                    this.setEnabled(false);
                    this.model.addElement(null); // Should display "No users currently online."
                }
            } else {
                // TODO: something here when PM'd?
            }
		}
	}
	
	public boolean containsUserPending(final InetSocketAddress remoteAddress) {
		return this.usersPending.containsKey(remoteAddress);
	}
	
	public User getUserPending(final InetSocketAddress remoteAddress) {
		synchronized (this.users) {
			return this.usersPending.get(remoteAddress);
		}
	}
	
	public boolean addUserPending(final InetSocketAddress remoteAddress, final User user) {
		synchronized (this.users) {
			if (this.usersPending.containsKey(remoteAddress)) {
				return false;
			}
			this.usersPending.put(remoteAddress, user);
		}
		return true;
	}
	
	public boolean removeUserPending(final InetSocketAddress remoteAddress) {
		synchronized (this.users) {
			if (!this.containsUserPending(remoteAddress)) {
				return false;
			}
			this.usersPending.remove(remoteAddress);
			this.refresh();
		}
		return true;
	}
	
	/**
	 * Determines whether this UserContainer contains a User with the given IP address.
	 * @param remoteAddress the address of the user to check.
	 * @return whether this UserContainer contains a User.
	 */
	public boolean containsUser(final InetSocketAddress remoteAddress) {
		synchronized (this.users) {
			return this.containsUserSync(remoteAddress);
		}
	}

	private boolean containsUserSync(final InetSocketAddress remoteAddress) {
	    return this.users.containsKey(remoteAddress);
    }
	
	/**
	 * Gets a User contained in this UserContainer with the given IP address.
	 * @param remoteAddress the address of the user to check.
	 * @return a User with the given ip.
	 */
	public User getUser(final InetSocketAddress remoteAddress) {
		synchronized (this.users) {
			return this.users.get(remoteAddress);
		}
	}
	
	/**
	 * Creates a User object from the given Socket.
	 * Automatically refreshes the user list.
	 * A user will not appear instantly in the list, because the socket has to populate the user's info.
	 * @param remoteAddress The address of the user to check.
	 * @param user The user to add.
	 * @return Whether a new user was added to the list or <tt>false</tt> if a user already existed with the socket ip.
	 */
	public boolean addUser(final InetSocketAddress remoteAddress, final User user) {
		synchronized (this.users) {
			if (this.containsUserSync(remoteAddress)) {
				return false;
			}
			this.users.put(remoteAddress, user);
			this.refresh();
		}
		return true;
	}
	
	/**
	 * Removes the given user from this container if the user exists in the container.
	 * @param remoteAddress The address of the user to check.
	 * @return Whether the user existed and removed, or didn't exist in the container.
	 */
	public boolean removeUser(final InetSocketAddress remoteAddress) {
		synchronized (this.users) {
			final User user = this.users.remove(remoteAddress);
			if (user == null) {
				return false;
			}
			user.disconnect();
			this.refresh();
		}
		return true;
	}
	
	/**
	 * Finds a user with the given ID (UUID).
	 * @param id The user id.
	 * @return A user with the given ID.
	 */
	public User findUser(final String id) {
		return this.findUser(UUID.fromString(id));
	}
	
	/**
	 * Finds a user with the given id (uuid).
	 * @param id the user id.
	 * @return a user with the given id.
	 */
	public User findUser(final UUID id) {
		if (id != null) {
			synchronized (this.users) {
				for (final User u : this.users.values()) {
					if (u == null) {
						continue;
					}
					if (id.equals(u.getID())) {
						return u;
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Sends the given data to all users in the given channel.
	 * @param tag The data to send to the users.
	 * @param channel the channel to test.
	 */
	public void send(final Tag tag, final String channel) {
		this.send(tag, o -> o.getPreferredName() != null && o.inChannel(channel));
	}
	
	/**
	 * Sends the given data to all users that qualify using the given Filter.
	 * @param tag The data to send to the users.
	 * @param filter The filter to use to qualify users.
	 */
	public void send(final Tag tag, final Filter<User> filter) {
		synchronized (this.users) {
			for (final User u : this.users.values()) {
				if (filter.accept(u)) {
                    u.send(tag);
                }
			}
		}
	}
	
	/*
	 * Friend and ignored lists
	 */
	
	/**
	 * Checks to see if the user is known as a friend.
	 * @param user The user to check.
	 * @return whether the given user is known as a friend.
	 */
	public boolean isFriend(final User user) {
		return this.friends.contains(user.getID());
	}
	
	/**
	 * Sets the given user as a friend.
	 * @param user The user to set as a friend.
	 */
	public void addFriend(final User user) {
		final UUID id = user.getID();
		synchronized (this.friends) {
			if (!this.friends.contains(id)) {
				this.friends.add(id);
			}
		}
	}
	
	/**
	 * Sets the friend status of the given user to false.
	 * @param user The user to change friend status of.
	 */
	public void removeFriend(final User user) {
		final UUID id = user.getID();
		synchronized (this.friends) {
			if (this.friends.contains(id)) {
				this.friends.remove(id);
			}
		}
	}
	
	/**
	 * Checks to see if the user is to be ignored.
	 * @param user The user to check.
	 * @return whether the given user is to be ignored.
	 */
	public boolean isIgnored(final User user) {
		return this.ignored.contains(user.getID());
	}
	
	/**
	 * Sets the given user to be ignored.
	 * @param user The user to ignore.
	 */
	public void addIgnored(final User user) {
		final UUID id = user.getID();
		synchronized (this.ignored) {
			if (!this.ignored.contains(id)) {
				this.ignored.add(id);
			}
		}
	}
	
	/**
	 * Stops ignoring the given user.
	 * @param user The user to stop ignoring.
	 */
	public void removeIgnored(final User user) {
		final UUID id = user.getID();
		synchronized (this.ignored) {
			if (this.ignored.contains(id)) {
				this.ignored.remove(id);
			}
		}
	}
	
	/*
	 * Pending user region
	 */
	
	/**
	 * Checks if the given ip is contained in the pending list.
	 * @param address The ip address to check if pending.
	 * @return whether the pending list contains the given ip.
	 */
	public boolean isMulticaster(final InetSocketAddress address) {
		synchronized (this.users) {
			return this.knownMulticasters.contains(address);
		}
	}
	
	/**
	 * Notifies this UserContainer that a connection with the given IP address is taking place.
	 * @param address The IP address of the user that's in the process of being connected too.
	 * @return <tt>true</tt> if the ip wasn't already pending and was added, otherwise <tt>false</tt>.
	 */
	public boolean addMulticaster(final InetSocketAddress address) {
		synchronized (this.users) {
            return !this.containsUserSync(address) && !this.isMulticaster(address) && this.knownMulticasters.add(address);
        }
	}
	
	/**
	 * Removes the given ip from the pending list.</br>
	 * @param address The IP address to remove.
	 * @return whether the list contained the given ip and was removed successfully.
	 */
	public boolean removeMulticaster(final InetSocketAddress address) {
		synchronized (this.users) {
			return this.knownMulticasters.remove(address);
		}
	}
	
	/*
	 * End of pending user code
	 */
	
	/**
	 * @return a collection of all users in this UserContainer.
	 */
	public ArrayList<User> getUserCollection() {
		return new ArrayList<>(this.users.values());
	}
	
	/**
	 * @return an array of all users in this UserContainer.
	 */
	public User[] getUsers() {
		final Collection<User> vals = this.users.values();
		return vals.toArray(new User[vals.size()]);
	}
	
	/**
	 * @return a String of user's ID who are known as friends. UUIDs are separated by ','.
	 */
	public String printFriends() {
		final StringBuilder build = new StringBuilder();
		synchronized (this.friends) {
			for (int i = 0; i < this.friends.size(); i++) {
				build.append(this.friends.get(i));
				if (i < this.friends.size() - 1) {
                    build.append(',');
                }
			}
		}
		return build.toString();
	}
	
	/**
	 * @return a String of user's ID who are to be ignored. UUIDs are separated by ','.
	 */
	public String printIgnored() {
		final StringBuilder build = new StringBuilder();
		synchronized (this.ignored) {
			for (int i = 0; i < this.ignored.size(); i++) {
				build.append(this.ignored.get(i));
				if (i < this.ignored.size() - 1) {
                    build.append(',');
                }
			}
		}
		return build.toString();
	}
	
	/**
	 * Disconnects all users in this container.
	 */
	public void disconnectAll() {
		synchronized (this.users) {
			for (final User u : this.users.values()) {
				u.disconnect();
			}
		}
	}
	
	/**
	 * Used to visually render the list of users.</br>
	 * Friends are displayed green, normal users are displayed black and ignored users are displayed red.
	 * 
	 * @author Odell
	 */
	private class UserCellRenderer extends DefaultListCellRenderer {
		
		private static final long serialVersionUID = 1L;
		
		Font userFont;
		
		/**
		 * Initializes a new UserCellRenderer.
		 */
        UserCellRenderer() {
			super();
			this.userFont = Fonts.GLOBAL.deriveFont(Font.BOLD);
		}
		
		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object o, final int index, final boolean selected, final boolean hasFocus) {
			final JLabel label = (JLabel) super.getListCellRendererComponent(list, o, index, selected, hasFocus);
			if (o == null) {
				label.setPreferredSize(new Dimension(label.getWidth(), 30));
				label.setHorizontalAlignment(JLabel.CENTER);
				label.setFont(Fonts.GLOBAL);
				label.setBackground(selected ? Colors.CELLRENDER_BACKGROUND : null);
				label.setText("No users currently online.");
				return label;
			} else if (o instanceof User) {
				final User user = (User) o;
				if (user.isIgnored) {
					label.setForeground(Colors.CELLRENDER_RED);
				} else if (user.isFriend) {
					label.setForeground(Colors.CELLRENDER_GREEN);
				} else {
					label.setForeground(Color.BLACK);
				}
				final Chat selectedChat = UserContainer.this.frame.getChatContainer().getSelectedChat();
				label.setIcon(new ImageIcon(Status.getImage(user.getStatus(), selectedChat != null && user.inChannel(selectedChat.getName()))));
				label.setPreferredSize(null);
				label.setHorizontalAlignment(JLabel.LEADING);
				label.setFont(this.userFont);
				label.setToolTipText(Application.getInstance().getAdminControl().isAdmin() ? user.getTooltip() : null);
				label.setBackground(selected ? Colors.CELLRENDER_BACKGROUND : null);
				label.setText(o.toString());
				return label;
			}
			// This should never happen.
			// label.setPreferredSize(new Dimension(label.getWidth(), 30));
			// label.setHorizontalAlignment(JLabel.CENTER);
			// label.setFont(Fonts.GLOBAL);
			// label.setBackground(selected ? Colors.CELLRENDER_BACKGROUND : null);
			// label.setText(o.toString());
			return label;
		}
	}
	
	private class TransferHandler extends javax.swing.TransferHandler {
		
		private static final long serialVersionUID = 1L;
		
		TransferHandler() {
			super();
		}
		
		public boolean canImport(final TransferHandler.TransferSupport support) {
			return false;
		}
		
		public int getSourceActions(final JComponent c) {
			return COPY;
		}
		
		public Transferable createTransferable(final JComponent c) {
			if (c != null && c instanceof UserContainer) {
				return new StringSelection(((UserContainer) c).getSelectedValue().getPreferredName());
			}
			return null;
		}
		
		public void exportDone(final JComponent c, final Transferable t, final int action) {
			if (c != null && c instanceof UserContainer) {
				((UserContainer) c).clearSelection();
			}
		}
		
	}
	
}