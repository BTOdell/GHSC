package com.ghsc.gui.components.users;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.net.Socket;
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
 * UserContainer is used to show current users in a channel.</br>
 * Not only that, but it also containers all users currently running GHSC.</br>
 * It also sorts users by their status and names. Friends at the top, ignored users at the bottom.
 * @author Odell
 */
public class UserContainer extends JList<Object> {
	
	private static final long serialVersionUID = 1L;
	
	private MainFrame frame;
	private DefaultListModel<Object> model;
	private HashMap<String, User> users;
	private ArrayList<String> pending;
	private ArrayList<UUID> friends, ignored;
	
	/**
	 * Default constructor. :/
	 */
	public UserContainer() {
		super();
	}
	
	/**
	 * Initializes a new UserContainer.
	 * @param frame - the main frame that this UserContainer is visible from.
	 * @param model - a model which can be used to provide easy access to the visual contents of the UserContainer.
	 */
	private UserContainer(MainFrame frame, DefaultListModel<Object> model) {
		super(model);
		this.frame = frame;
		this.model = model;
	}
	
	/**
	 * Initializes a new UserContainer.
	 * @param frame - the main frame that this UserContainer is visible from.
	 * @param allF - the friends list that was retrieved from an earlier save.
	 * @param allI - the ignored list that was retrieved from an earlier save.
	 */
	public UserContainer(MainFrame frame, String[] allF, String[] allI) {
		this(frame, new DefaultListModel<Object>());
		if (frame.getApplication() != null) {
			users = new HashMap<String, User>();
			pending = new ArrayList<String>();
			friends = allF != null ? new ArrayList<UUID>(allF.length) : new ArrayList<UUID>();
			ignored = allI != null ? new ArrayList<UUID>(allI.length) : new ArrayList<UUID>();
			
			for (int n = 0; n < allF.length; n++) {
				final String f = allF[n];
				if (f == null)
					continue;
				try {
					friends.add(UUID.fromString(f));
				} catch (IllegalArgumentException iae) {}
			}
			for (int n = 0; n < allI.length; n++) {
				final String i = allI[n];
				if (i == null)
					continue;
				try {
					ignored.add(UUID.fromString(i));
				} catch (IllegalArgumentException iae) {}
			}
			
			setCellRenderer(new UserCellRenderer());
		}
		setDoubleBuffered(true);
		setDragEnabled(true);
		setTransferHandler(new TransferHandler());
	}
	
	public MainFrame getMainFrame() {
		return frame;
	}
	
	/**
	 * Refreshes the visual display of this UserContainer.</br>
	 * This method is under a synchronized lock to prevent multiple refreshes at the same time.
	 */
	public void refresh() {
		synchronized (users) {
			model.clear();
			final Chat currChat = frame.getChatContainer().getSelectedChat();
			if (currChat != null) {
				if (currChat instanceof Channel) {
					boolean valid = false;
					ArrayList<User> all = getUserCollection();
					if (all.size() > 0) {
						setEnabled(true);
						Collections.sort(all);
						for (User curr : all) {
							if (curr.getPreferredName() != null) {
								valid = true;
								model.addElement(curr);
							}
						}
					}
					if (!valid) {
						setEnabled(false);
						model.addElement("No users currently online.");
					}
				} else {
					// TODO: something here when PM'd?
				}
			}
		}
	}
	
	/**
	 * Determines whether this UserContainer contains a User with the given IP address.
	 * @param ip - the ip to check for.
	 * @return whether this UserContainer contains a User.
	 */
	public boolean containsUser(String ip) {
		return users.containsKey(ip);
	}
	
	/**
	 * Gets a User contained in this UserContainer with the given IP address.
	 * @param ip - the ip to get the user with.
	 * @return a User with the given ip.
	 */
	public User getUser(String ip) {
		synchronized (users) {
			return users.get(ip);
		}
	}
	
	/**
	 * Creates a User object from the given Socket.</br>
	 * Automatically refreshes the user list.</br>
	 * A user will not appear instantly in the list, because the socket has to populate the user's info.
	 * @param socket - the socket to create the user from.
	 * @return whether a new user was added to the list or <tt>false</tt> if a user already existed with the socket ip.
	 */
	public boolean addUser(Socket socket) {
		final String ip = socket.getInetAddress().getHostAddress();
		if (containsUser(ip))
			return false;
		final User u = new User(this, socket);
		synchronized (users) {
			users.put(ip, u);
			refresh();
		}
		return true;
	}
	
	/**
	 * Removes the given user from this container if the user exists in the container. 
	 * @param user - the user to remove.
	 * @return whether the user existed and removed, or didn't exist in the container.
	 */
	public boolean removeUser(User user) {
		final String ip = user.getIPString();
		if (!containsUser(ip))
			return false;
		synchronized (users) {
			users.remove(ip).disconnect();
			refresh();
		}
		return true;
	}
	
	/**
	 * Finds a user with the given id (uuid).
	 * @param id the user id.
	 * @return a user with the given id.
	 */
	public User findUser(String id) {
		return findUser(UUID.fromString(id));
	}
	
	/**
	 * Finds a user with the given id (uuid).
	 * @param id the user id.
	 * @return a user with the given id.
	 */
	public User findUser(UUID id) {
		if (id == null)
			return null;
		synchronized (users) {
			for (User u : users.values()) {
				if (u == null)
					continue;
				if (id.equals(u.getID()))
					return u;
			}
		}
		return null;
	}
	
	/**
	 * Sends the given data to all users in the given channel.
	 * @param data - the data to send to the users.
	 * @param channel - the channel to test.
	 */
	public void send(Tag tag, final String channel) {
		send(tag, new Filter<User>() {
			public boolean accept(User o) {
				return o.getPreferredName() != null && o.inChannel(channel);
			}
		});
	}
	
	/**
	 * Sends the given data to all users that qualify using the given Filter.
	 * @param data - the data to send to the users.
	 * @param filter - the filter to use to qualify users.
	 */
	public void send(Tag tag, Filter<User> filter) {
		synchronized (users) {
			for (User u : users.values()) {
				if (filter.accept(u))
					u.send(tag);
			}
		}
	}
	
	/*
	 * Friend and ignored lists
	 */
	
	/**
	 * Checks to see if the user is known as a friend.
	 * @param u - the user to check.
	 * @return whether the given user is known as a friend.
	 */
	public boolean isFriend(User u) {
		return friends.contains(u.getID());
	}
	
	/**
	 * Sets the given user as a friend.
	 * @param u - the user to set as a friend.
	 */
	public void addFriend(User u) {
		final UUID id = u.getID();
		synchronized (friends) {
			if (!friends.contains(id)) {
				friends.add(id);
			}
		}
	}
	
	/**
	 * Sets the friend status of the given user to false.
	 * @param u - the user to change friend status of.
	 */
	public void removeFriend(User u) {
		final UUID id = u.getID();
		synchronized (friends) {
			if (friends.contains(id)) {
				friends.remove(id);
			}
		}
	}
	
	/**
	 * Checks to see if the user is to be ignored.
	 * @param u - the user to check.
	 * @return whether the given user is to be ignored.
	 */
	public boolean isIgnored(User u) {
		return ignored.contains(u.getID());
	}
	
	/**
	 * Sets the given user to be ignored.
	 * @param u - the user to ignore.
	 */
	public void addIgnored(User u) {
		final UUID id = u.getID();
		synchronized (ignored) {
			if (!ignored.contains(id)) {
				ignored.add(id);
			}
		}
	}
	
	/**
	 * Stops ignoring the given user.
	 * @param u - the user to stop ignoring.
	 */
	public void removeIgnored(User u) {
		final UUID id = u.getID();
		synchronized (ignored) {
			if (ignored.contains(id)) {
				ignored.remove(id);
			}
		}
	}
	
	/*
	 * Pending user region
	 */
	
	/**
	 * Checks if the given ip is contained in the pending list.
	 * @param ip - the ip to check if pending.
	 * @return whether the pending list contains the given ip.
	 */
	public boolean isPending(String ip) {
		return pending.contains(ip);
	}
	
	/**
	 * Notifies this UserContainer that a connection with the given IP address is taking place.</br>
	 * {@link #removePending(String)} should be called after the connection was successful.
	 * @param ip - the ip of the user that's in the process of being connected too.
	 * @return <tt>true</tt> if the ip wasn't already pending and was added, otherwise <tt>false</tt>.
	 */
	public boolean addPending(final String ip) {
		synchronized (pending) {
			if (isPending(ip))
				return false;
			return pending.add(ip);	
		}
	}
	
	/**
	 * Removes the given ip from the pending list.</br>
	 * @param ip - the ip to remove.
	 * @return whether the list contained the given ip and was removed successfully.
	 */
	public boolean removePending(String ip) {
		synchronized (pending) {
			return pending.remove(ip);	
		}
	}
	
	/*
	 * End of pending user code
	 */
	
	/**
	 * @return a collection of all users in this UserContainer.
	 */
	public ArrayList<User> getUserCollection() {
		return new ArrayList<User>(users.values());
	}
	
	/**
	 * @return an array of all users in this UserContainer.
	 */
	public User[] getUsers() {
		Collection<User> vals = users.values();
		return vals.toArray(new User[vals.size()]);
	}
	
	/**
	 * @return a String of user's ID who are known as friends. UUIDs are separated by ','.
	 */
	public String printFriends() {
		StringBuilder build = new StringBuilder();
		synchronized (friends) {
			for (int i = 0; i < friends.size(); i++) {
				build.append(friends.get(i));
				if (i < friends.size() - 1)
					build.append(',');
			}
		}
		return build.toString();
	}
	
	/**
	 * @return a String of user's ID who are to be ignored. UUIDs are separated by ','.
	 */
	public String printIgnored() {
		StringBuilder build = new StringBuilder();
		synchronized (ignored) {
			for (int i = 0; i < ignored.size(); i++) {
				build.append(ignored.get(i));
				if (i < ignored.size() - 1)
					build.append(',');
			}
		}
		return build.toString();
	}
	
	/**
	 * Disconnects all users in this container.
	 */
	public void disconnectAll() {
		synchronized (users) {
			for (User u : users.values()) {
				u.disconnect();
			}
		}
	}
	
	/**
	 * Used to visually render the list of users.</br>
	 * Friends are displayed green, normal users are displayed black and ignored users are displayed red.
	 * @author Odell
	 */
	private class UserCellRenderer extends DefaultListCellRenderer {
		
		private static final long serialVersionUID = 1L;

		Font userFont;
		
		/**
		 * Initializes a new UserCellRenderer.
		 */
		public UserCellRenderer() {
			super();
			userFont = Fonts.GLOBAL.deriveFont(Font.BOLD);
		}
		
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object o, int index, boolean selected, boolean hasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, o, index, selected, hasFocus);
			if (o instanceof User) {
				User user = (User) o;
				if (user.isIgnored) {
					label.setForeground(Colors.CELLRENDER_RED);
				} else if (user.isFriend) {
					label.setForeground(Colors.CELLRENDER_GREEN);
				} else {
					label.setForeground(Color.BLACK);
				}
				
				Chat selectedChat = frame.getChatContainer().getSelectedChat();
				label.setIcon(new ImageIcon(Status.getImage(user.getStatus(), selectedChat != null && user.inChannel(selectedChat.getName()))));
				
				label.setPreferredSize(null);
				label.setHorizontalAlignment(JLabel.LEADING);
				label.setFont(userFont);
				
				label.setToolTipText(Application.getApplication().getAdminControl().isAdmin() ? user.getTooltip() : null);
			} else {
				label.setPreferredSize(new Dimension(label.getWidth(), 30));
				label.setHorizontalAlignment(JLabel.CENTER);
				label.setFont(Fonts.GLOBAL);
			}
			label.setBackground(selected ? Colors.CELLRENDER_BACKGROUND : null);
			label.setText(o.toString());
			return label;
		}
		
	}
	
	private class TransferHandler extends javax.swing.TransferHandler {
		
		private static final long serialVersionUID = 1L;

		public TransferHandler() {
			super();
		}
		
		public boolean canImport(TransferHandler.TransferSupport support) {
			return false;
		}
		
		public int getSourceActions(JComponent c) {
		    return COPY;
		}

		public Transferable createTransferable(JComponent c) {
			if (c != null && c instanceof UserContainer) {
				return new StringSelection(((User)((UserContainer) c).getSelectedValue()).getPreferredName());
			}
			return null;
		}

		public void exportDone(JComponent c, Transferable t, int action) {
			if (c != null && c instanceof UserContainer) {
				((UserContainer) c).clearSelection();	
			}
		}
		
	}
	
}