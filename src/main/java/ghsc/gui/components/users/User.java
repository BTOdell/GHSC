package ghsc.gui.components.users;

import ghsc.common.Debug;
import ghsc.common.Images;
import ghsc.event.message.MessageEvent;
import ghsc.event.message.MessageEvent.Type;
import ghsc.gui.Application;
import ghsc.gui.components.chat.Chat;
import ghsc.gui.components.chat.channels.Channel;
import ghsc.gui.components.chat.channels.ChannelElement;
import ghsc.gui.fileshare.FileShare;
import ghsc.gui.fileshare.internal.RemotePackage;
import ghsc.impl.ComplexIdentifiable;
import ghsc.impl.Filter;
import ghsc.impl.Identifiable;
import ghsc.net.sockets.input.MessageThread;
import ghsc.util.Tag;
import ghsc.util.TimeStamp;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Describes a connected user.
 */
public class User implements ComplexIdentifiable, Transferable, Comparable<User> {
	
	public static final Filter<User> ALL = user -> true;
	
	public enum Status {
		
		AVAILABLE, AWAY, BUSY;
		
		public static Image getImage(final Status status, final boolean in) {
			switch (status) {
				case AVAILABLE:
					return in ? Images.STATUS_IN : Images.STATUS_OUT;
				case AWAY:
					return in ? Images.STATUS_IN_AWAY : Images.STATUS_OUT_AWAY;
				case BUSY:
					return in ? Images.STATUS_IN_BUSY : Images.STATUS_OUT_BUSY;
				default:
					throw new IllegalArgumentException("Status is invalid: " + status);
			}
		}
		
	}
	
	public static final String ATT_HOSTNAME = "h";
    public static final String ATT_NICK = "n";
    public static final String ATT_ID = "i";
    public static final String ATT_CHANNEL = "c";
	
	private final UserContainer container;
	
	private final Socket socket;
	private final MessageThread messageThread;
	
	private String hostname;
	private String nick;
	private UUID userID;
	private final ArrayList<String> channels;
	private final Object channelLock = new Object();
	private Status status = Status.AVAILABLE;
	boolean isIgnored;
	boolean isFriend;
	
	/**
	 * Initializes a new User by providing a Socket which will be used to dynamically populate the contents of this object.
	 * @param container The container which this User object exists.
	 * @param socket The socket to populate this object.
	 */
	public User(final UserContainer container, final Socket socket) {
		this.container = container;
		this.channels = new ArrayList<>();
		this.socket = socket;
		this.messageThread = new MessageThread(new MessageThread.IOWrapper() {
			public InputStream getInputStream() throws IOException {
				return User.this.socket.getInputStream();
			}
			public OutputStream getOutputStream() throws IOException {
				return User.this.socket.getOutputStream();
			}
		}, msg -> {
            if (Debug.NORMAL.compareTo(Application.DEBUG) <= 0) {
                System.out.println("User: " + msg);
            }
            label:
            switch (msg.getType()) {
                case IDENTIFY:
                    final InetSocketAddress remoteAddress = this.getRemoteSocketAddress();
                    if (!container.removeUserPending(remoteAddress)) {
                        break;
                    }

                    if (container.addUser(remoteAddress, this)) {
                        container.removeMulticaster(remoteAddress);
                        this.sendIntro();
                    } else {
                        System.err.println("Unable to add " + remoteAddress.getAddress() + "@" + remoteAddress.getPort() + " to the users Hashmap.");
                    }

                    String attributeValue;
                    if ((attributeValue = msg.getAttribute(ATT_HOSTNAME)) != null) {
                        this.setHostname(attributeValue);
                    }
                    if ((attributeValue = msg.getAttribute(ATT_NICK)) != null) {
                        this.setNick(attributeValue);
                    }
                    if ((attributeValue = msg.getAttribute(ATT_ID)) != null) {
                        this.setID(attributeValue);
                    }
                    this.setFriend(container.isFriend(this));
                    this.setIgnored(container.isIgnored(this));
                    container.getMainFrame().getChatContainer().refreshUser(this);
                    container.refresh();
                    break;
                case JOIN:
                    final String jchannels = msg.getAttribute(ATT_CHANNEL);
                    if (jchannels != null) {
                        final String[] allChannels = jchannels.split(Pattern.quote(","));
                        this.addChannels(allChannels);
                    }
                    break;
                case LEAVE:
                    final String lchannel = msg.getAttribute(ATT_CHANNEL);
                    if (lchannel != null) {
                        this.removeChannel(lchannel);
                    }
                    break;
                case MESSAGE:
                    final String mchannel = msg.getAttribute(ATT_CHANNEL);
                    if (mchannel != null) {
                        final Chat cchat = container.getMainFrame().getChatContainer().getChat(mchannel);
                        if (cchat != null) {
                            final Channel cchan = (Channel) cchat;
                            final String message = msg.getPost();
                            cchan.addElement(new ChannelElement(cchan.getElements(), TimeStamp.newInstance(), this, null, message, !this.isIgnored, Color.BLACK), true);
                            final Application application = Application.getInstance();
                            if (message.contains(application.getPreferredName())) {
                                final StringBuilder titleBuild = new StringBuilder();
                                titleBuild.append(cchan.getName());
                                titleBuild.append(": ");
                                titleBuild.append(this.getPreferredName());
                                titleBuild.append(" mentioned you!");
                                application.getTrayManager().showInfoMessage("GHSC\n" + titleBuild.toString());
                                titleBuild.insert(0, "- ");
                                application.getTitleManager().submitAppend(titleBuild.toString(), 8000);
                                application.flashTaskbar();
                            }
                        }
                    } else {
                        // TODO: channel will be null if message should be sent to PM
                    }
                    break;
                case FILE_SHARE:
                    final FileShare fs = Application.getInstance().getFileShare();
                    if (fs != null) {
                        final String type = msg.getAttribute(FileShare.ATT_TYPE);
                        if (type == null) {
                            break;
                        }
                        switch (type) {
                            case FileShare.TYPE_NEW:
                                final RemotePackage rp = RemotePackage.parse(this, msg.getPost());
                                if (rp == null) {
                                    break label;
                                }
                                fs.addPackages(rp);
                                break;
                            case FileShare.TYPE_EDIT:

                                break;
                            case FileShare.TYPE_UPDATE:

                                break;
                            case FileShare.TYPE_REMOVE:

                                break;
                        }
                    }
                    break;
                default:
                    break; // other cases (not normal)
            }
        }, () -> {
            System.out.println("We have lost connection with " + this.getPreferredName());
            container.removeUser(this.getRemoteSocketAddress());
        });
	}
	
	public void start() {
		// this is protected from multiple starts...
		this.messageThread.start();
	}
	
	public void sendIntro() {
		final Application application = Application.getInstance();
		this.send(MessageEvent.construct(Type.IDENTIFY, ATT_HOSTNAME, application.getHostname(), ATT_NICK, application.getPreferredName(), ATT_ID, application.getID()));
		final String channels = this.container.getMainFrame().getChatContainer().printChannels();
		if (channels != null && !channels.isEmpty()) {
			this.send(MessageEvent.construct(Type.JOIN, ATT_CHANNEL, channels));
		}
	}
	
	public UserContainer getContainer() {
		return this.container;
	}
	
	public InetSocketAddress getLocalSocketAddress() {
		return (InetSocketAddress) this.socket.getLocalSocketAddress();
	}
	
	public InetSocketAddress getRemoteSocketAddress() {
		return (InetSocketAddress) this.socket.getRemoteSocketAddress();
	}
	
	@Override
	public String getHostname() {
		return this.hostname;
	}
	
	@Override
	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}
	
	@Override
	public String getNick() {
		return this.nick;
	}
	
	@Override
	public void setNick(final String nick) {
		this.nick = nick;
	}
	
	@Override
	public String getPreferredName() {
		final String temp = this.getNick();
		if (temp != null) {
			return temp;
		}
		return this.getHostname();
	}
	
	@Override
	public UUID getID() {
		return this.userID;
	}
	
	@Override
	public void setID(final UUID uuid) {
		this.userID = uuid;
	}
	
	@Override
	public void setID(final String uuid) {
		this.setID(UUID.fromString(uuid));
	}
	
	/**
     * Gets the online status of the user.
	 */
	public Status getStatus() {
		return this.status;
	}
	
	/**
     * Sets the online status of the user.
	 * @param status The new status of the user.
	 */
	public void setStatus(final Status status) {
		this.status = status;
	}
	
	/**
	 * @return whether this user is marked as ignored.
	 */
	public boolean isIgnored() {
		return this.isIgnored;
	}
	
	/**
	 * Changes the ignore status of this user.
	 * @param ignored Whether the user should be ignored or not.
	 */
	public void setIgnored(final boolean ignored) {
		this.isIgnored = ignored;
		if (ignored) {
			this.container.addIgnored(this);
		} else {
			this.container.removeIgnored(this);
		}
		this.container.refresh();
	}
	
	/**
	 * @return whether this user is marked as a friend.
	 */
	public boolean isFriend() {
		return this.isFriend;
	}
	
	/**
	 * Changes the friend status of this user.
	 * @param friend Whether the user should be marked as a friend or not.
	 */
	public void setFriend(final boolean friend) {
		this.isFriend = friend;
		if (friend) {
			this.container.addFriend(this);
		} else {
			this.container.removeFriend(this);
		}
		this.container.refresh();
	}
	
	/**
	 * Checks if the user is in the given channel.
	 * @param channel The channel to check if the user is in.
	 * @return Whether the user is in the given channel.
	 */
	public boolean inChannel(final String channel) {
		return this.channels.contains(channel);
	}
	
	/**
	 * Removes the given channel from the user's active channel list.
	 * @param channel The channel to remove.
	 */
	public void removeChannel(final String channel) {
		synchronized (this.channelLock) {
			this.channels.remove(channel);
		}
		this.container.refresh();
	}
	
	/**
	 * Adds all the provided channels to the user's active channel list.
	 * @param channels All the channels to add.
	 */
	public void addChannels(final String... channels) {
		synchronized (this.channelLock) {
			for (final String channel : channels) {
				if (channel.trim().isEmpty()) {
					continue;
				}
				if (!this.channels.contains(channel)) {
					this.channels.add(channel);
				}
			}
		}
		this.container.refresh();
	}
	
	/**
	 * Sends the given data to the user (by using it's MessageWrapper).
	 * @param tag The data to send.
	 */
	public void send(final Tag tag) {
		this.messageThread.send(tag);
	}
	
	/**
	 * Disconnects the user by closing the socket.</br>
	 * The user will automatically be notified of the disconnect and should take appropriate actions.
	 */
	public void disconnect() {
		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (final IOException ignored) {}
		}
	}
	
	@Override
	public String toString() {
		final InetSocketAddress remoteAddress = this.getRemoteSocketAddress();
		return this.getPreferredName() + " - " + remoteAddress.getAddress().getHostAddress() + "@" + remoteAddress.getPort();
	}
	
	@Override
	public boolean equals(final Object o) {
		return o instanceof Identifiable && (this == o || this.getID().equals(((Identifiable) o).getID()));
	}
	
	@Override
	public int compareTo(final User u) {
		final Chat selectedChat = this.container.getMainFrame().getChatContainer().getSelectedChat();
		if (selectedChat != null) {
			final String name = selectedChat.getName();
			final boolean in = this.inChannel(name);
            final boolean uIn = u.inChannel(name);
            if (in != uIn) {
				return in ? -1 : 1;
			}
		}
		final int diff = (u.isIgnored ? -1 : (u.isFriend ? 1 : 0)) - (this.isIgnored ? -1 : (this.isFriend ? 1 : 0));
		return diff == 0 ? this.toString().compareToIgnoreCase(u.toString()) : diff;
	}
	
	@Override
	public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
		if (flavor.equals(DataFlavor.stringFlavor)) {
			return this.getPreferredName();
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}
	
	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.stringFlavor };
	}
	
	@Override
	public boolean isDataFlavorSupported(final DataFlavor arg0) {
		return DataFlavor.stringFlavor.equals(arg0);
	}
	
}