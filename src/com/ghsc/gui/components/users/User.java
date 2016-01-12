package com.ghsc.gui.components.users;

import java.awt.Color;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;

import com.ghsc.common.Images;
import com.ghsc.event.EventListener;
import com.ghsc.event.EventProvider;
import com.ghsc.event.IEventProvider;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.event.message.MessageEvent.Type;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.chat.Chat;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.chat.channels.ChannelElement;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.gui.fileshare.internal.RemotePackage;
import com.ghsc.impl.ComplexIdentifiable;
import com.ghsc.impl.Filter;
import com.ghsc.impl.Identifiable;
import com.ghsc.net.sockets.input.MessageThread;
import com.ghsc.util.Tag;
import com.ghsc.util.TimeStamp;

/**
 * Describes a connected user.
 * 
 * @author Odell
 */
public class User implements ComplexIdentifiable, IEventProvider<User>, Transferable, Comparable<User> {
	
	public static final Filter<User> ALL = new Filter<User>() {
		public boolean accept(User user) {
			return true;
		}
	};
	
	public enum Status {
		
		AVAILABLE, AWAY, BUSY;
		
		public static Image getImage(Status status, boolean in) {
			switch (status) {
				case AVAILABLE:
					return in ? Images.STATUS_IN : Images.STATUS_OUT;
				case AWAY:
					return in ? Images.STATUS_IN_AWAY : Images.STATUS_OUT_AWAY;
				case BUSY:
					return in ? Images.STATUS_IN_BUSY : Images.STATUS_OUT_BUSY;
				default:
					return null;
			}
		}
		
	}
	
	public static final String ATT_USERPORT = "u", ATT_HOSTNAME = "h", ATT_NICK = "n", ATT_ID = "i", ATT_CHANNEL = "c";
	
	private UserContainer container;
	
//	private static final int PAIRING_DURATION = 4000;
//	private static final int PAIRING_DELAY = 100;

	private EventProvider<User> userProvider = new EventProvider<User>();
	
	private Socket socket = null;
	private MessageThread messageThread;
	
	/**
	 * Admin command states HashMap of (TAG, STATE)
	 */
	private HashMap<String, Object> commandStates = new HashMap<String, Object>();
	
	private String hostname = null;
	private String nick = null;
	private UUID userID = null;
	private ArrayList<String> channels;
	Object channelLock = new Object();
	private IpPort pair;
	private int selfPort;
	private Status status = Status.AVAILABLE;
	boolean isIgnored = false, isFriend = false;
	
	protected User(UserContainer container) {
		this.container = container;
		channels = new ArrayList<String>();
	}
	
	/**
	 * Initializes a new User by providing a Socket which will be used to dynamically populate the contents of this object.
	 * 
	 * @param c
	 *            the container which this User object exists.
	 * @param s
	 *            the socket to populate this object.
	 */
	public User(UserContainer c, IpPort pair, Socket s, int selfPort) {
		this(c);
		this.pair = new IpPort(pair.ip(), pair.port());
		this.socket = s;
		this.selfPort = selfPort;
		this.messageThread = new MessageThread(new MessageThread.IOWrapper() {
			public InputStream getInputStream() throws IOException {
				return socket.getInputStream();
			}
			public OutputStream getOutputStream() throws IOException {
				return socket.getOutputStream();
			}
		}, new EventListener<MessageEvent>() {
			public void eventReceived(MessageEvent msg) {
				System.out.println(msg);
				switch (msg.getType()) {
					case IDENTIFY:
						String a;
						if ((a = msg.getAttribute(ATT_HOSTNAME)) != null)
							setHostname(a);
						if ((a = msg.getAttribute(ATT_NICK)) != null)
							setNick(a);
						if ((a = msg.getAttribute(ATT_ID)) != null)
							setID(a);
						setFriend(container.isFriend(User.this));
						setIgnored(container.isIgnored(User.this));
						container.getMainFrame().getChatContainer().refreshUser(User.this);
						container.refresh();
						break;
					case ENDPOINT:
						String msgPort;
						if ((msgPort = msg.getAttribute(ATT_USERPORT)) != null) {
							final int userPort = Integer.valueOf(msgPort);
							final User currUser = User.this;
							
							// These names are from the UserB perspective:
							IpPort pairAA = new IpPort(currUser.pair.ip(), userPort);
//							IpPort pairBB = new IpPort(currUser.socket.getLocalAddress().getHostAddress(), selfPort);
//
//							// Find Pending UserAA...
							User userAA = null;
//							long endTime = System.currentTimeMillis() + PAIRING_DURATION;
//							while (System.currentTimeMillis() < endTime) {
//								if (container.containsUserPending(pairAA)) {
//									userAA = container.getUserPending(pairAA);
//									container.removeUserPending(pairAA);
//									break;
//								}
//								try {
//									Thread.sleep(PAIRING_DELAY);
//								} catch (InterruptedException e) {
//									break;
//								}
//							}
//
//							// if remote < self
//							if (pairAA.toLong() < pairBB.toLong()) {
//								// This User object is going to be disconnected, and the multicast-created User will be promoted.
//								if (!container.containsUserPending(currUser.pair())) {
//									break;
//								}
//								container.removeUserPending(currUser.pair());
//								currUser.disconnect();
//								
//							} else {
								// This User object is going to be promoted.
								if (!container.containsUserPending(currUser.pair())) {
									break;
								}
								container.removeUserPending(currUser.pair());
								currUser.pair = pairAA;
								userAA = currUser;
//							}
							
							if ((userAA == null) || !container.addUser(pairAA, userAA)) {
								System.err.println("Unable to add " + pairAA + " to the users Hashmap.");
							} else {
								container.removeMulticaster(pairAA);
								userAA.sendIntro();
								setFriend(container.isFriend(userAA));
								setIgnored(container.isIgnored(userAA));
								container.getMainFrame().getChatContainer().refreshUser(userAA);
								container.refresh();
							}
						}
						break;
					case JOIN:
						final String jchannels = msg.getAttribute(ATT_CHANNEL);
						if (jchannels != null) {
							final String[] allChannels = jchannels.split(Pattern.quote(","));
							addChannels(allChannels);
						}
						break;
					case LEAVE:
						String lchannel = msg.getAttribute(ATT_CHANNEL);
						if (lchannel != null) {
							removeChannel(lchannel);
						}
						break;
					case MESSAGE:
						final String mchannel = msg.getAttribute(ATT_CHANNEL);
						if (mchannel != null) {
							Chat cchat = container.getMainFrame().getChatContainer().getChat(mchannel);
							if (cchat != null) {
								Channel cchan = (Channel) cchat;
								String message = msg.getPost();
								cchan.addElement(new ChannelElement(cchan.getElements(), TimeStamp.newInstance(), User.this, null, message, !isIgnored, Color.BLACK), true);
								Application app = Application.getApplication();
								if (message.contains(app.getPreferredName())) {
									final StringBuilder titleBuild = new StringBuilder();
									titleBuild.append(cchan.getName());;
									titleBuild.append(": ");
									titleBuild.append(User.this.getPreferredName());
									titleBuild.append(" mentioned you!");
									app.getTrayManager().showInfoMessage("GHSC\n" + titleBuild.toString());
									titleBuild.insert(0, "- ");
									app.getTitleManager().submitAppend(titleBuild.toString(), 8000);
									app.flashTaskbar();
								}
							}
						} else {
							// TODO: channel will be null if message should be sent to PM
						}
						break;
					case ADMIN:
						final Tag me = Application.getApplication().getAdminControl().process(User.this, msg);
						if (me != null) {
							send(me);
						}
						break;
					case FILE_SHARE:
						FileShare fs = Application.getApplication().getFileShare();
						if (fs != null) {
							String type = msg.getAttribute(FileShare.ATT_TYPE);
							if (type == null)
								break;
							if (type.equals(FileShare.TYPE_NEW)) {
								final RemotePackage rp = RemotePackage.parse(User.this, msg.getPost());
								if (rp == null) {
									break;
								}
								fs.addPackages(rp);
							} else if (type.equals(FileShare.TYPE_EDIT)) {
							
							} else if (type.equals(FileShare.TYPE_UPDATE)) {
							
							} else if (type.equals(FileShare.TYPE_REMOVE)) {
							
							}
						}
						break;
					default:
						break; // other cases (not normal)
				}
			}
		}, new Runnable() {
			public void run() {
				System.out.println("We have lost connection with " + User.this.getPreferredName());
				container.removeUser(User.this.pair());
			}
		});
	}
	
	public void start() {
		// this is protected from multiple starts...
		this.messageThread.start();
	}
	
	public void sendEndpoint() {
		send(MessageEvent.construct(Type.ENDPOINT, ATT_USERPORT, this.selfPort));
	}
	
	public void sendIntro() {
		send(MessageEvent.construct(Type.IDENTIFY, ATT_HOSTNAME, Application.getApplication().getHostname(), ATT_NICK, Application.getApplication().getPreferredName(), ATT_ID, Application.getApplication().getID()));
		final String channels = container.getMainFrame().getChatContainer().printChannels();
		if (channels != null && !channels.isEmpty()) {
			send(MessageEvent.construct(Type.JOIN, ATT_CHANNEL, channels));
		}
	}
	
	public UserContainer getContainer() {
		return container;
	}
	
	@Override
	public String getHostname() {
		return hostname;
	}
	
	@Override
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	@Override
	public String getNick() {
		return nick;
	}
	
	@Override
	public void setNick(String nick) {
		this.nick = nick;
	}
	
	@Override
	public String getPreferredName() {
		final String temp = getNick();
		if (temp != null)
			return temp;
		return getHostname();
	}
	
	@Override
	public UUID getID() {
		return userID;
	}
	
	@Override
	public void setID(UUID uuid) {
		this.userID = uuid;
	}
	
	@Override
	public void setID(String uuid) {
		try {
			setID(UUID.fromString(uuid));
		} catch (IllegalArgumentException iae) {}
	}
	
	@Override
	public boolean subscribe(EventListener<User> listener) {
		return userProvider.subscribe(listener);
	}
	
	@Override
	public boolean unsubscribe(EventListener<User> listener) {
		return userProvider.unsubscribe(listener);
	}
	
	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * @param status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * @return whether this user is marked as ignored.
	 */
	public boolean isIgnored() {
		return isIgnored;
	}
	
	/**
	 * Changes the ignore status of this user.
	 * 
	 * @param ignored
	 *            - whether the user should be ignored or not.
	 */
	public void setIgnored(boolean ignored) {
		if (isIgnored = ignored)
			container.addIgnored(this);
		else
			container.removeIgnored(this);
		container.refresh();
	}
	
	/**
	 * @return whether this user is marked as a friend.
	 */
	public boolean isFriend() {
		return isFriend;
	}
	
	/**
	 * Changes the friend status of this user.
	 * 
	 * @param friend
	 *            - whether the user should be marked as a friend or not.
	 */
	public void setFriend(boolean friend) {
		if (isFriend = friend)
			container.addFriend(this);
		else
			container.removeFriend(this);
		container.refresh();
	}
	
	/**
	 * @return the user's ip:port pair.
	 */
	public IpPort pair() {
		return pair;
	}
	
	/**
	 * Checks if the user is in the given channel.
	 * 
	 * @param chan
	 *            - the channel to check if the user is in.
	 * @return whether the user is in the given channel.
	 */
	public boolean inChannel(String chan) {
		return channels.contains(chan);
	}
	
	/**
	 * Removes the given channel from the user's active channel list.
	 * 
	 * @param chan
	 *            - the channel to remove.
	 */
	public void removeChannel(String chan) {
		synchronized (channelLock) {
			channels.remove(chan);
		}
		container.refresh();
	}
	
	/**
	 * Adds all the provided channels to the user's active channel list.
	 * 
	 * @param chans
	 *            - all the channels to add.
	 */
	public void addChannels(String... chans) {
		synchronized (channelLock) {
			for (String chan : chans) {
				if (chan.trim().isEmpty())
					continue;
				if (!channels.contains(chan)) {
					channels.add(chan);
				}
			}
		}
		container.refresh();
	}
	
	/*
	 * Admin commands methods
	 */
	
	public Object getCommandState(String tag) {
		return commandStates.get(tag);
	}
	
	public Object setCommandState(String tag, Object state) {
		return commandStates.put(tag, state);
	}
	
	/**
	 * Sends the given data to the user (by using it's MessageWrapper).
	 * 
	 * @param data
	 *            - the data to send.
	 */
	public void send(Tag tag) {
		if (messageThread != null)
			messageThread.send(tag);
	}
	
	/**
	 * Disconnects the user by closing the socket.</br>
	 * The user will automatically be notified of the disconnect and should take appropriate actions.
	 */
	public void disconnect() {
		if (socket != null) {
			try {
				socket.close();
				System.gc();
			} catch (IOException e) {}
		}
	}
	
	public String getTooltip() {
		StringBuilder build = new StringBuilder().append("<html>").append("Nick: ").append(getPreferredName()).append("<br>");
		String[] channels;
		synchronized (channelLock) {
			channels = User.this.channels.toArray(new String[User.this.channels.size()]);
		}
		final int perLine = 5;
		for (int i = 0; i < channels.length; i++) {
			build.append(channels[i]);
			if (i < channels.length - 1) {
				build.append(", ");
			}
			if (i % perLine == perLine - 1) {
				build.append("<br>");
			}
		}
		return build.append("</html>").toString();
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append(getPreferredName()).append(" - ").append(pair()).toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Identifiable))
			return false;
		return this == o || getID().equals(((Identifiable) o).getID());
	}
	
	@Override
	public int compareTo(User u) {
		Chat selectedChat = container.getMainFrame().getChatContainer().getSelectedChat();
		if (selectedChat != null) {
			String name = selectedChat.getName();
			boolean in = inChannel(name), uIn = u.inChannel(name);
			if (in != uIn) {
				return in ? -1 : 1;
			}
		}
		int diff = (u.isIgnored ? -1 : (u.isFriend ? 1 : 0)) - (isIgnored ? -1 : (isFriend ? 1 : 0));
		return diff == 0 ? toString().compareToIgnoreCase(u.toString()) : diff;
	}
	
	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (flavor.equals(DataFlavor.stringFlavor)) {
			return getPreferredName();
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.stringFlavor };
	}
	
	@Override
	public boolean isDataFlavorSupported(DataFlavor arg0) {
		return DataFlavor.stringFlavor.equals(arg0);
	}
	
}