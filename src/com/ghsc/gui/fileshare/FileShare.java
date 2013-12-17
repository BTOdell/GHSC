package com.ghsc.gui.fileshare;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.files.FileStorage.Hook;
import com.ghsc.files.FileStorage.Node;
import com.ghsc.files.Settings;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.gui.fileshare.components.PackagePanel;
import com.ghsc.gui.fileshare.components.PackagePanelList;
import com.ghsc.gui.fileshare.internal.FilePackage;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility.Type;
import com.ghsc.gui.fileshare.internal.LocalFileNode;
import com.ghsc.gui.fileshare.internal.LocalPackage;
import com.ghsc.impl.Filter;
import com.ghsc.net.encryption.AES;
import com.ghsc.net.sockets.filetransfer.FileTransferListener;
import com.ghsc.net.sockets.input.MessageThread;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

/**
 * Handles the internal file sharing operations.
 * @author Odell
 */
public class FileShare {
	
	public static final String ATT_TYPE = "t", TYPE_NEW = "n", TYPE_EDIT = "e", TYPE_UPDATE = "u", TYPE_REMOVE = "r", 
			TYPE_ENCRYPTION = "e", TYPE_UUID = "u", TYPE_PASSWORD = "p", TYPE_REQUEST = "r", ATT_UUID = "u", ATT_PATH = "p", ATT_STATUS = "s";
	public static final int TRANSFER_BUFFER_SIZE = 8192;
	
	private final Application application;
	private FileShareFrame frame;
	
	/**
	 * Process incoming file sharing sockets
	 */
	final ArrayList<SocketWorker> socketWorkers = new ArrayList<SocketWorker>();
	
	/**
	 * Map<uuid,package>
	 */
	Map<String, FilePackage> packages;
	
	/**
	 * Initializes a new File transfer handling object.
	 * @param application the main application.
	 */
	public FileShare() {
		this.application = Application.getApplication();
		this.packages = Collections.synchronizedMap(new HashMap<String, FilePackage>());
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					frame = new FileShareFrame(FileShare.this);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		Settings.getSettings().addHook(new Hook() {
			public Node onSave() {
				final Node fs = new Node(Tag.construct("fileshare"));
				
				final Node sortNode = new Node(Tag.construct("sort"), frame.getSortByPopup().printSort());
				fs.submit(sortNode);
				
				if (packages.size() > 0) {
					final ArrayList<Node> localPackageNodes = new ArrayList<Node>();
					synchronized (packages) {
						for (FilePackage p : packages.values()) {
							if (p instanceof LocalPackage) {
								LocalPackage lp = (LocalPackage) p;
								localPackageNodes.add(new Node(Tag.construct("lp"), lp.toSaveMeta()));
							}
						}
					}
					final Node localPackagesNode = new Node(Tag.construct("localpackages"), localPackageNodes.toArray(new Node[localPackageNodes.size()]));
					fs.submit(localPackagesNode);
				}
				
				return fs;
			}
		});
		long start = System.currentTimeMillis();
		final Node localPackagesNode = Settings.getSettings().search("/fileshare/localpackages");
		if (localPackagesNode != null) {
			final Node[] localPackageNodes = localPackagesNode.searchAll("/lp");
			if (localPackageNodes.length > 0) {
				application.getMainFrame().setStatus("Loading file packages.");
				final ArrayList<LocalPackage> lps = new ArrayList<LocalPackage>(localPackageNodes.length);
				for (int i = 0; i < localPackageNodes.length; i++) {
					final Node lpNode = localPackageNodes[i];
					if (lpNode == null)
						continue;
					final String localMeta = lpNode.getData();
					if (localMeta == null)
						continue;
					LocalPackage lp = LocalPackage.parseSaveMeta(localMeta);
					if (lp == null)
						continue;
					lps.add(lp);
				}
				addPackages(lps.toArray(new LocalPackage[lps.size()]));
				System.out.println("Loaded all packages in: " + (System.currentTimeMillis() - start) + " milliseconds.");
				application.getMainFrame().setStatus("Loading file packages.", 500);
			}
		}
	}
	
	/**
	 * @return the main application.
	 */
	public Application getApplication() {
		return application;
	}
	
	/**
	 * @return the file share frame.
	 */
	public FileShareFrame getFrame() {
		return frame;
	}
	
	/**
	 * Adds local packages to the package map.
	 */
	public boolean addPackages(final FilePackage... fps) {
		if (fps == null)
			return false;
		boolean success = true;
		PackagePanelList list = null;
		for (int i = 0; i < fps.length; i++) {
			final FilePackage fp = fps[i];
			if (fp == null)
				continue;
			String uuid = fp.getUUID().toString();
			if (fp == packages.get(uuid)) {
				success = false;
				continue;
			}
			packages.put(uuid, fp);
			if (list == null)
				list = frame.getPackagePanels();
			list.addPanelsSilently(new PackagePanel(frame, fp));
		}
		if (list != null) {
			list.refresh();
		}
		return success;
	}
	
	public boolean removePackages(final FilePackage... fps) {
		if (fps == null)
			return false;
		boolean success = true;
		PackagePanelList list = null;
		for (int i = 0; i < fps.length; i++) {
			final FilePackage fp = fps[i];
			if (fp == null)
				continue;
			String uuid = fp.getUUID().toString();
			if (fp == packages.get(uuid)) {
				packages.remove(uuid);
				if (list == null)
					list = frame.getPackagePanels();
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
		return frame.isVisible();
	}
	
	/**
	 * Sets the frame of this FileShare to the given visibility.
	 * @param visible
	 * 		whether to show or hide the frame.
	 */
	public void setVisible(boolean visible) {
		if (visible)
			frame.getSnapAdapter().snap(0, true);
		frame.setVisible(visible);
	}
	
	/**
	 * Creates a file transfer socket with the given user.
	 * @param user
	 * 		the user to create the socket with.
	 * @return a newly created socket connected to the user, if null an error has occurred.
	 */
	public Socket connect(User user) {
		return connect(user, 2000);
	}
	
	/**
	 * Creates a file transfer socket with the given user.
	 * @param user
	 * 		the user to create the socket with.
	 * @param timeout
	 * 		connection timeout
	 * @return a newly created socket connected to the user, if null an error has occurred.
	 */
	public Socket connect(User user, int timeout) {
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(user.getIP(), FileTransferListener.PORT), timeout);
		} catch (IOException e) {
			return null;
		}
		return socket;
	}
	
	/**
	 * Processes the socket given.
	 * @param socket the socket to process.
	 */
	public void process(final Socket s) {
		if (s == null)
			return;
		if (!application.getMainFrame().getUsers().containsUser(s.getInetAddress().getHostAddress())) {
			try {
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		final SocketWorker sw = new SocketWorker(s);
		synchronized (socketWorkers) {
			socketWorkers.add(sw);
		}
	}
	
	/**
	 * Closes all SocketWorkers that qualify the given filter.
	 * @param filter the filter to qualify SocketWorkers.
	 * @return whether or not all the SocketWorkers which qualified closed without problems.
	 */
	public boolean closeAll(final Filter<SocketWorker> filter) {
		if (socketWorkers.size() <= 0)
			return true;
		boolean success = true;
		synchronized (socketWorkers) {
			final Iterator<SocketWorker> swI = socketWorkers.iterator();
			while (swI.hasNext()) {
				final SocketWorker sw = swI.next();
				if (sw != null && filter.accept(sw)) {
					success &= sw.close();
				}
			}
		}
		return success;
	}
	
	public void dispose() {
		synchronized (socketWorkers) {
			if (socketWorkers.size() > 0) {
				final Iterator<SocketWorker> swI = socketWorkers.iterator();
				while (swI.hasNext()) {
					final SocketWorker sw = swI.next();
					if (sw != null)
						if (sw.close())
							swI.remove();
				}
			}
		}
	}
	
	/**
	 * Converts raw bytes to a "human readable" string representation.<br>
	 * @param bytes the raw byte count.
	 * @return a human readable string.
	 */
	public static String toHumanReadable(long bytes, boolean si) {
		bytes = Math.abs(bytes);
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit)
	    	return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "KMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
	    return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static char[] PRIVATE_KEY_DICTIONARY = new char[] {
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
			if (i != 0 && i % 3 == 0)
				pk.append('-');
			pk.append(PRIVATE_KEY_DICTIONARY[sr.nextInt(PRIVATE_KEY_DICTIONARY.length)]);
		}
		return pk.toString();
	}
	
	/**
	 * Handles incoming file transfer requests. (Uploader)
	 * @author Odell
	 */
	public class SocketWorker {
		
		private final Socket socket;
		private final MessageThread messageThread;
		private String uuid = null;
		private LocalPackage lPackage = null;
		
		private boolean authenticated = false;
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
				public void eventReceived(MessageEvent msg) {
					if (msg.getType() != MessageEvent.Type.FILE_SHARE)
						return;
					final String type = msg.getAttribute(ATT_TYPE);
					if (type == null)
						return;
					switch (type) {
						case TYPE_ENCRYPTION: // set encryption key
							byte[] post = msg.getPost().getBytes(Application.CHARSET);
							if (post.length == 16)
								messageThread.setEncryption(new AES(post));
							messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, Utilities.resolveToString(post.length == 16)));
							break;
						case TYPE_UUID:
							uuid = msg.getAttribute(ATT_UUID);
							final FilePackage tempPackage = packages.get(uuid);
							boolean lpValid = tempPackage != null && tempPackage instanceof LocalPackage;
							if (lpValid) {
								lPackage = (LocalPackage) tempPackage;
								final UserContainer users = application.getMainFrame().getUsers();
								final String ipString = socket.getInetAddress().getHostAddress();
								User user;
								if ((user = users.getUser(ipString)) != null) {
									final Visibility.Type vType = lPackage.getVisibility().getType();
									if (vType == Type.CHANNEL || vType == Type.USER) {
										final String vData = lPackage.getVisibility().getData().toString();
										final String[] vDataSplit = vData.split(Pattern.quote(","));
										switch (vType) { // check visibility
											case CHANNEL:
												boolean inChannel = false;
												for (final String channel : vDataSplit) {
													if (channel == null)
														continue;
													if (user.inChannel(channel)) {
														inChannel |= true;
														break;
													}
												}
												if (!inChannel) {
													messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, "a")); // access error
													return;
												}
												break;
											case USER:
												final String userIDString = user.getID().toString();
												boolean found = false;
												for (int i = 0; i < vDataSplit.length; i++) {
													final String str = vDataSplit[i];
													final int index = str.indexOf('|');
													if (index >= 0 && userIDString.equals(str.substring(index + 1, str.length()))) {
														found = true;
														break;
													}
												}
												if (!found) {
													messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, "a")); // access error
													return;
												}
												break;
										}
									}
									authenticated = !lPackage.isPasswordProtected();
								} else {
									close();
									return;
								}
							} else {
								authenticated = false;
							}
							messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, Utilities.resolveToBoolean(lpValid)));
							break;
						case TYPE_PASSWORD:
							if (!authenticated) {
								if (lPackage != null) {
									String password = msg.getPost();
									authenticated |= lPackage.verifyPassword(password);
								}
							}
							messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, Utilities.resolveToBoolean(authenticated)));
							break;
						default:
							if (authenticated) {
								switch (type) {
									case TYPE_REQUEST:
										if (lPackage != null) {
											String path = msg.getAttribute(ATT_PATH);
											LocalFileNode lFile = lPackage.getFile(path);
											if (lFile != null) {
												// found file
												InputStream is = null;
												try {
													is = lFile.openInputStream();
													byte[] buf = new byte[TRANSFER_BUFFER_SIZE];
													int read;
													while (running && (read = is.read(buf)) >= 0) {
														messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, Utilities.resolveToBoolean(true), new String(buf, 0, read, Application.CHARSET)));
													}
												} catch (IOException e) {
													e.printStackTrace();
													messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, "ex"));
												} finally {
													if (is != null) {
														try {
															is.close();
														} catch (IOException e) {
															e.printStackTrace();
														}
													}
													messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, running ? "d" : "c")); // done or cancelled
												}
											} else {
												// file not found
												messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, type, ATT_PATH, path, ATT_STATUS, "er"));
											}
										}
										break;
								}
							} else {
								messageThread.send(MessageEvent.construct(MessageEvent.Type.FILE_SHARE, ATT_TYPE, TYPE_PASSWORD)); // remind password protected
							}
					}
				}
			}, new Runnable() {
				public void run() {
					// when the socket disconnects
					
				}
			});
		}
		
		public boolean isRunning() {
			return running;
		}
		
		public boolean isAuthenticated() {
			return authenticated;
		}
		
		public boolean isActivePackage(final FilePackage fp) {
			if (fp == null)
				return false;
			if (lPackage != null) {
				if (fp.equals(lPackage))
					return true;
			} else if (uuid != null) {
				if (fp.getUUID().toString().equals(uuid))
					return true;
			}
			return false;
		}
		
		public boolean close() {
			running = false;
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			return true;
		}
		
	}

}