package ghsc.gui.fileshare;

import ghsc.event.EventListener;
import ghsc.event.message.MessageEvent;
import ghsc.files.FileStorage.Node;
import ghsc.files.Settings;
import ghsc.gui.Application;
import ghsc.gui.components.users.User;
import ghsc.gui.components.users.UserContainer;
import ghsc.gui.fileshare.components.PackagePanel;
import ghsc.gui.fileshare.components.PackagePanelList;
import ghsc.gui.fileshare.internal.FilePackage;
import ghsc.gui.fileshare.internal.FilePackage.Visibility.Type;
import ghsc.gui.fileshare.internal.LocalFileNode;
import ghsc.gui.fileshare.internal.LocalPackage;
import ghsc.impl.Filter;
import ghsc.net.encryption.AES;
import ghsc.net.sockets.filetransfer.FileTransferListener;
import ghsc.net.sockets.input.MessageThread;
import ghsc.util.SnapAdapter;
import ghsc.util.Tag;
import ghsc.util.Utilities;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Handles the internal file sharing operations.
 */
public class FileShare {
	
	public static final String ATT_TYPE = "t";
    public static final String TYPE_NEW = "n";
    public static final String TYPE_EDIT = "e";
    public static final String TYPE_UPDATE = "u";
    public static final String TYPE_REMOVE = "r";
    public static final String TYPE_ENCRYPTION = "e";
    public static final String TYPE_UUID = "u";
    public static final String TYPE_PASSWORD = "p";
    public static final String TYPE_REQUEST = "r";
    public static final String ATT_UUID = "u";
    public static final String ATT_PATH = "p";
    public static final String ATT_STATUS = "s";

	public static final int TRANSFER_BUFFER_SIZE = 8192;
	
	private FileShareFrame frame;
	
	/**
	 * Process incoming file sharing sockets
	 */
	private final ArrayList<SocketWorker> socketWorkers = new ArrayList<>();
	
	/**
	 * Map<uuid,package>
	 */
	final Map<String, FilePackage> packages;
	
	/**
	 * Initializes a new File transfer handling object.
	 */
	public FileShare() {
		this.packages = Collections.synchronizedMap(new HashMap<>());
		final Application application = Application.getInstance();
		try {
			SwingUtilities.invokeAndWait(() -> this.frame = new FileShareFrame(application.getMainFrame(), this));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		Settings.getSettings().addHook(() -> {
            final Node fs = new Node(Tag.construct("fileshare"));

            final Node sortNode = new Node(Tag.construct("sort"), this.frame.getSortByPopup().printSort());
            fs.submit(sortNode);

            if (!this.packages.isEmpty()) {
                final ArrayList<Node> localPackageNodes = new ArrayList<>();
                synchronized (this.packages) {
                    for (final FilePackage p : this.packages.values()) {
                        if (p instanceof LocalPackage) {
                            final LocalPackage lp = (LocalPackage) p;
                            localPackageNodes.add(new Node(Tag.construct("lp"), lp.toSaveMeta()));
                        }
                    }
                }
                final Node localPackagesNode = new Node(Tag.construct("localpackages"), localPackageNodes.toArray(new Node[0]));
                fs.submit(localPackagesNode);
            }

            return fs;
        });
		final long start = System.currentTimeMillis();
		final Node localPackagesNode = Settings.getSettings().search("/fileshare/localpackages");
		if (localPackagesNode != null) {
			final Node[] localPackageNodes = localPackagesNode.searchAll("/lp");
			if (localPackageNodes.length > 0) {
				application.getMainFrame().setStatus("Loading file packages.");
				final ArrayList<LocalPackage> lps = new ArrayList<>(localPackageNodes.length);
				for (final Node lpNode : localPackageNodes) {
					if (lpNode == null) {
                        continue;
                    }
					final String localMeta = lpNode.getData();
					if (localMeta == null) {
                        continue;
                    }
					final LocalPackage lp = LocalPackage.parseSaveMeta(localMeta);
					if (lp == null) {
                        continue;
                    }
					lps.add(lp);
				}
				this.addPackages(lps.toArray(new LocalPackage[0]));
				System.out.println("Loaded all packages in: " + (System.currentTimeMillis() - start) + " milliseconds.");
				application.getMainFrame().setStatus("Loading file packages.", 500);
			}
		}
	}
	
	/**
	 * @return the file share frame.
	 */
	public FileShareFrame getFrame() {
		return this.frame;
	}
	
	/**
	 * Adds local packages to the package map.
	 */
	public boolean addPackages(final FilePackage... fps) {
		if (fps == null) {
			return false;
		}
		boolean success = true;
		PackagePanelList list = null;
		for (final FilePackage fp : fps) {
			if (fp == null) {
				continue;
			}
			final String uuid = fp.getUUID().toString();
			if (fp == this.packages.get(uuid)) {
				success = false;
				continue;
			}
			this.packages.put(uuid, fp);
			if (list == null) {
                list = this.frame.getPackagePanels();
            }
			list.addPanelsSilently(new PackagePanel(this.frame, fp));
		}
		if (list != null) {
			list.refresh();
		}
		return success;
	}
	
	public boolean removePackages(final FilePackage... fps) {
		if (fps == null) {
            return false;
        }
		boolean success = true;
		PackagePanelList list = null;
		for (final FilePackage fp : fps) {
			if (fp == null) {
                continue;
            }
			final String uuid = fp.getUUID().toString();
			if (fp == this.packages.get(uuid)) {
				this.packages.remove(uuid);
				if (list == null) {
                    list = this.frame.getPackagePanels();
                }
				list.removePanelsSilently(fp);
			} else {
				success = false;
			}
		}
		if (list != null) {
			list.refresh();
		}
		return success;
	}
	
	public boolean isVisible() {
		return this.frame.isVisible();
	}
	
	/**
	 * Sets the frame of this FileShare to the given visibility.
	 * @param visible
	 * 		whether to show or hide the frame.
	 */
	public void setVisible(final boolean visible) {
		if (visible) {
			final SnapAdapter snapAdapter = this.frame.getSnapAdapter();
			if (snapAdapter != null) {				
				snapAdapter.snap(0, true);
			}
		}
		this.frame.setVisible(visible);
	}
	
	/**
	 * Creates a file transfer socket with the given user.
	 * @param user
	 * 		the user to create the socket with.
	 * @return a newly created socket connected to the user, if null an error has occurred.
	 */
	public Socket connect(final User user) {
		return this.connect(user, 2000);
	}
	
	/**
	 * Creates a file transfer socket with the given user.
	 * @param user
	 * 		the user to create the socket with.
	 * @param timeout
	 * 		connection timeout
	 * @return a newly created socket connected to the user, if null an error has occurred.
	 */
	public Socket connect(final User user, final int timeout) {
		final Socket socket = new Socket();
		try {
			final InetSocketAddress remoteAddress = user.getRemoteSocketAddress();
			socket.connect(new InetSocketAddress(remoteAddress.getAddress(), FileTransferListener.PORT), timeout);
		} catch (final IOException e) {
			return null;
		}
		return socket;
	}
	
	/**
	 * Processes the socket given.
	 * @param s the socket to process.
	 */
	public void process(final Socket s) {
		if (s == null) {
			return;
		}
		final Application application = Application.getInstance();
		// Check to make sure that's an actual user we have connected to
		// TODO currently broken because the remote socket address isn't the user socket address
		final InetSocketAddress remoteAddress = (InetSocketAddress) s.getRemoteSocketAddress();
		if (!application.getMainFrame().getUsers().containsUser(remoteAddress)) {
			try {
				s.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return;
		}
		final SocketWorker sw = new SocketWorker(s);
		synchronized (this.socketWorkers) {
			this.socketWorkers.add(sw);
		}
	}
	
	/**
	 * Closes all SocketWorkers that qualify the given filter.
	 * @param filter the filter to qualify SocketWorkers.
	 * @return whether or not all the SocketWorkers which qualified closed without problems.
	 */
	public boolean closeAll(final Filter<SocketWorker> filter) {
		if (this.socketWorkers.size() <= 0) {
            return true;
        }
		boolean success = true;
		synchronized (this.socketWorkers) {
            for (final SocketWorker sw : this.socketWorkers) {
                if (sw != null && filter.accept(sw)) {
                    success &= sw.close();
                }
            }
		}
		return success;
	}
	
	public void dispose() {
		synchronized (this.socketWorkers) {
			if (!this.socketWorkers.isEmpty()) {
				final Iterator<SocketWorker> swI = this.socketWorkers.iterator();
				while (swI.hasNext()) {
					final SocketWorker sw = swI.next();
					if (sw != null) {
                        if (sw.close()) {
                            swI.remove();
                        }
                    }
				}
			}
		}
	}
	
	/**
	 * Converts raw bytes to a "human readable" string representation.<br>
	 * @param bytes The raw byte count.
     * @param si Whether to use SI units (1000 vs 1024).
	 * @return A human readable string.
	 */
	public static String toHumanReadable(long bytes, final boolean si) {
		bytes = Math.abs(bytes);
	    final int unit = si ? 1000 : 1024;
	    if (bytes < unit) {
            return bytes + " B";
        }
	    final int exp = (int) (Math.log(bytes) / Math.log(unit));
	    final String pre = (si ? "KMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
	    return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	private static final char[] PRIVATE_KEY_DICTIONARY = {
		'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 
		'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 
		'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};
	
	/**
	 * Generates a random private key in the format: XXX-XXX-XXX<br>
	 * Valid characters: a-z, A-Z, 0-9
	 * @return a random private key.
	 */
	public static String generatePrivateKey() {
		final SecureRandom sr = new SecureRandom();
		final StringBuilder pk = new StringBuilder();
		for (int i = 0; i < 9; i++) {
			if (i != 0 && i % 3 == 0) {
                pk.append('-');
            }
			pk.append(PRIVATE_KEY_DICTIONARY[sr.nextInt(PRIVATE_KEY_DICTIONARY.length)]);
		}
		return pk.toString();
	}
	
	/**
	 * Handles incoming file transfer requests. (Uploader)
	 */
	public class SocketWorker {
		
		private final Socket socket;
		private final MessageThread messageThread;
		private String uuid;
		private LocalPackage lPackage;
		
		private boolean authenticated;
		private boolean running = true;
		
		/*
		 * TODO: keep track of downloads
		 * make sure it doesn't count downloads that were paused and then restarted.
		 * that includes if the user closed his program and a download had to start another time.
		 */
		
		private SocketWorker(final Socket socket) {
			this.socket = socket;
			this.messageThread = new MessageThread(new MessageThread.IOWrapper() {
				public InputStream getInputStream() throws IOException {
					return socket.getInputStream();
				}
				public OutputStream getOutputStream() throws IOException {
					return socket.getOutputStream();
				}
			}, new EventListener<MessageEvent>() {
				public void eventReceived(final MessageEvent msg) {
					if (msg.getType() != MessageEvent.Type.FILE_SHARE) {
						return;
					}
					final String type = msg.getAttribute(ATT_TYPE);
					if (type == null) {
						return;
					}
                    switch (type) {
                        case TYPE_ENCRYPTION:
                            final byte[] post = msg.getPost().getBytes(Application.CHARSET);
                            if (post.length == 16) {
                                SocketWorker.this.messageThread.setEncryption(new AES(post));
                            }
                            SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, Utilities.resolveToString(post.length == 16)));
                            break;
                        case TYPE_UUID:
                            SocketWorker.this.uuid = msg.getAttribute(ATT_UUID);
                            final FilePackage tempPackage = FileShare.this.packages.get(SocketWorker.this.uuid);
                            final boolean lpValid = tempPackage instanceof LocalPackage;
                            if (lpValid) {
                                SocketWorker.this.lPackage = (LocalPackage) tempPackage;
                                final Application application = Application.getInstance();
                                final UserContainer users = application.getMainFrame().getUsers();

                                // Check to make sure that's an actual user we have connected to
                                // TODO currently broken because the remote socket address isn't the user socket address
                                final InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
                                final User user = users.getUser(remoteAddress);
                                if (user != null) {
                                    final Type vType = SocketWorker.this.lPackage.getVisibility().getType();
                                    if (vType == Type.CHANNEL || vType == Type.USER) {
                                        final String vData = SocketWorker.this.lPackage.getVisibility().getData().toString();
                                        final String[] vDataSplit = vData.split(Pattern.quote(","));
                                        switch (vType) { // check visibility
                                            case CHANNEL:
                                                boolean inChannel = false;
                                                for (final String channel : vDataSplit) {
                                                    if (channel == null) {
                                                        continue;
                                                    }
                                                    if (user.inChannel(channel)) {
                                                        inChannel = true;
                                                        break;
                                                    }
                                                }
                                                if (!inChannel) {
                                                    SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, "a")); // access error
                                                    return;
                                                }
                                                break;
                                            case USER:
                                                final String userIDString = user.getID().toString();
                                                boolean found = false;
                                                for (final String str : vDataSplit) {
                                                    final int index = str.indexOf('|');
                                                    if (index >= 0 && userIDString.equals(str.substring(index + 1))) {
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                                if (!found) {
                                                    SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, "a")); // access error
                                                    return;
                                                }
                                                break;
                                            default:
                                        }
                                    }
                                    SocketWorker.this.authenticated = !SocketWorker.this.lPackage.isPasswordProtected();
                                } else {
                                    SocketWorker.this.close();
                                    return;
                                }
                            } else {
                                SocketWorker.this.authenticated = false;
                            }
                            SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, Utilities.resolveToBoolean(lpValid)));
                            break;
                        case TYPE_PASSWORD:
                            if (!SocketWorker.this.authenticated) {
                                if (SocketWorker.this.lPackage != null) {
                                    final String password = msg.getPost();
                                    SocketWorker.this.authenticated = SocketWorker.this.lPackage.verifyPassword(password);
                                }
                            }
                            SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, Utilities.resolveToBoolean(SocketWorker.this.authenticated)));
                            break;
                        default:
                            if (SocketWorker.this.authenticated) {
                                if (type.equals(TYPE_REQUEST)) {
                                    if (SocketWorker.this.lPackage != null) {
                                        final String path = msg.getAttribute(ATT_PATH);
                                        final LocalFileNode lFile = SocketWorker.this.lPackage.getFile(path);
                                        if (lFile != null) {
                                            // found file
                                            InputStream is = null;
                                            try {
                                                is = lFile.openInputStream();
                                                final byte[] buf = new byte[TRANSFER_BUFFER_SIZE];
                                                int read;
                                                while (SocketWorker.this.running && (read = is.read(buf)) >= 0) {
                                                    SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, Utilities.resolveToBoolean(true), new String(buf, 0, read, Application.CHARSET)));
                                                }
                                            } catch (final IOException e) {
                                                e.printStackTrace();
                                                SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, "ex"));
                                            } finally {
                                                if (is != null) {
                                                    try {
                                                        is.close();
                                                    } catch (final IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, SocketWorker.this.running ? "d" : "c")); // done or cancelled
                                            }
                                        } else {
                                            // file not found
                                            SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, "er"));
                                        }
                                    }
                                }
                            } else {
                                SocketWorker.this.messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, TYPE_PASSWORD)); // remind password protected
                            }
                            break;
                    }
				}
			}, () -> {
                // when the socket disconnects
                // TODO
            });
			this.messageThread.start();
		}
		
		public boolean isRunning() {
			return this.running;
		}
		
		public boolean isAuthenticated() {
			return this.authenticated;
		}
		
		public boolean isActivePackage(final FilePackage fp) {
			if (fp == null) {
                return false;
            }
			if (this.lPackage != null) {
                return fp.equals(this.lPackage);
			} else if (this.uuid != null) {
                return fp.getUUID().toString().equals(this.uuid);
			}
			return false;
		}
		
		public boolean close() {
			this.running = false;
			if (this.socket != null) {
				try {
					this.socket.close();
				} catch (final IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}
		
	}

}