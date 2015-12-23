package com.ghsc.gui;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ghsc.admin.AdminControl;
import com.ghsc.common.Debug;
import com.ghsc.event.EventProvider;
import com.ghsc.event.global.EventManager;
import com.ghsc.files.FileStorage.Hook;
import com.ghsc.files.FileStorage.Node;
import com.ghsc.files.Profile;
import com.ghsc.files.Settings;
import com.ghsc.gui.components.chat.ChatContainer;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.input.InputWizard;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.input.WizardValidator;
import com.ghsc.gui.components.popup.PopupManager;
import com.ghsc.gui.components.util.FrameTitleManager;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.gui.tray.TrayManager;
import com.ghsc.impl.ComplexIdentifiable;
import com.ghsc.net.sockets.SocketManager;
import com.ghsc.net.update.Updater;
import com.ghsc.net.update.Version;
import com.ghsc.net.update.VersionController;
import com.ghsc.util.Tag;

/**
 * This is the main application class.</br>
 * Most of the network interface is managed from this class.</br>
 * Basically the foundation for the entire project :D
 * @author Odell
 */
public class Application implements ComplexIdentifiable {
	
	private static Application applicationInstance = null;
	
	public static Application getApplication() {
		if (applicationInstance == null) {
			applicationInstance = new Application();
		}
		return applicationInstance;
	}
	
	public static Debug DEBUG = Debug.MINOR;
	public static Charset CHARSET = Charset.forName("UTF-8");
	public static Version VERSION = Version.create("0.4.0", "Indev");
	public static File LAST_DIRECTORY = null;
	
	private MainFrame frame = null;
	private FrameTitleManager titleManager = null;
	private PopupManager popupManager = null;
	private TrayManager tray = null;
	private AdminControl adminControl = null;
	private FileShare fileShare = null;
	private InputWizard nickDialog = null;

	// Updating
	private VersionController versionController = null;
	private Updater updater = null;
	
	// Networking
	private SocketManager socketManager = null;
	
	// Events
	public static final String NICK_EVENTPROVIDER = "nick";
	private final EventProvider<String> nickEventProvider = new EventProvider<String>(NICK_EVENTPROVIDER);
	
	private String hostname = null, nick = null;
	private UUID userID = null;
	private static InetAddress address = null;
	
	/**
	 * @return the current local ip address that this application is running on.
	 */
	public static InetAddress getLocalAddress() {
		if (address == null) {
			try {
				address = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				System.err.println("Error: Unable to resolve local host.");
				address = null;
			}
		}
		return address;
	}
	
	@Override
	public String getHostname() {
		return hostname;
	}
	
	@Override
	public void setHostname(String hostname) {
		this.hostname = hostname;
		if (nick == null) {
			setNick(hostname);
		}
	}
	
	@Override
	public String getNick() {
		return nick;
	}
	
	@Override
	public void setNick(String nick) {
		this.nick = nick;
		if (this.hostname == null)
			this.hostname = nick;
		nickEventProvider.fireEvent(nick);
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
	
	/**
	 * Changes the version of the application.</br>
	 * Will also visually change the title of the application to match the version.
	 * @param vers - the new version of this application.
	 */
	public void setVersion(Version vers) {
		VERSION = vers;
		if (titleManager != null) {
			titleManager.appendTitle(null);
		}
	}
	
	/**
	 * If the current application doesn't have focus,</br>
	 * then this method will cause the taskbar item to flash.
	 */
	public void flashTaskbar() {
		if (frame != null && !frame.isFocused()) {
			frame.toFront();
		}
	}
	
	/**
	 * Displays a wizard for current user to modify his display name/username.
	 */
	public void showNickWizard() {
		if (nickDialog != null && nickDialog.isVisible())
			return;
		nickDialog = new InputWizard(frame, "Change nickname", "Nickname", getPreferredName(), "Apply", "Applies your new nickname!",
		new WizardListener<String>() {
			public void wizardFinished(String input) {
				if (input != null) {
					if (input.equals(nick))
						return;
					setNick(input);
					frame.getChatContainer().refreshUser(null);
				} else {
					if (Debug.MINOR.compareTo(DEBUG) < 0)
						System.out.println("Nickname wizard cancelled.");
				}
			}
		}, new WizardValidator<String, String, Boolean>() {
			public ValidationResult<String, Boolean> validate(String text) {
				if (text.isEmpty())
					return new ValidationResult<String, Boolean>("Well, you actually have to type something...", false);
				if (text.length() > 18)
					return new ValidationResult<String, Boolean>("Name can't exceed 18 characters.", false);
				if (text.startsWith("_") || text.startsWith(" "))
					return new ValidationResult<String, Boolean>("Can't start with any space character.", false);
				if (text.contains("__") || text.contains("  "))
					return new ValidationResult<String, Boolean>("Can't contain two spaces anywhere.", false);
				if (text.contains(" _") || text.contains("_ "))
					return new ValidationResult<String, Boolean>("Come on...", false);
				for (char c : text.toCharArray()) {
					if (Character.isDigit(c) || Character.isLetter(c) || c == ' ' || c == '_') continue;
					return new ValidationResult<String, Boolean>("Only allowed letters and numbers!", false);
				}
				return new ValidationResult<String, Boolean>("Current name is acceptable.", true);
			}
		});
		nickDialog.setVisible(true);
	}
	
	/**
	 * @return the socket manager associated with this application.
	 */
	public SocketManager getSocketManager() {
		return socketManager;
	}

	/**
	 * @return the main GUI frame associated with this application.
	 */
	public MainFrame getMainFrame() {
		return frame;
	}
	
	/**
	 * @return the title manager associated with this application.
	 */
	public FrameTitleManager getTitleManager() {
		return titleManager;
	}
	
	/**
	 * @return the popup manager supporting all components.
	 */
	public PopupManager getPopupManager() {
		return popupManager;
	}
	
	/**
	 * @return the tray icon manager associated with this application.
	 */
	public TrayManager getTrayManager() {
		return tray;
	}
	
	/**
	 * @return the administrator control system of this application.
	 */
	public AdminControl getAdminControl() {
		return adminControl;
	}
	
	/**
	 * @return the file transfer handler of this application.
	 */
	public FileShare getFileShare() {
		return fileShare;
	}
	
	/**
	 * @return the updater associated with this application.
	 */
	public Updater getUpdater() {
		return updater;
	}
	
	/**
	 * @return the version controller associated with this application.
	 */
	public VersionController getVersionController() {
		return versionController;
	}

	/**
	 * Initialize the contents of the application.
	 */
	public void initialize() throws Exception {
		/*
		 * Add this shutdown hook, so that correct de-initialization will take place even if we call System.exit(int).
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if (socketManager != null) {
					socketManager.close();
				}
				if (Profile.getProfile().isSavable()) {
					if (Profile.getProfile().save()) {
						System.out.println("Profile saved.");
					} else {
						System.out.println("Failed to save profile!");
					}
				}
				if (Settings.getSettings().isSavable()) {
					if (Settings.getSettings().save()) {
						System.out.println("Settings saved.");
					} else {
						System.out.println("Failed to save settings!");
					}
				}
				if (frame != null) {
					if (frame.getUsers() != null) {
						frame.getUsers().disconnectAll();
					}
				}
				if (fileShare != null)
					fileShare.dispose();
				System.gc();
			}
		}));
		
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		if (getLocalAddress() == null) { // cache local address
			JOptionPane.showMessageDialog(null, "Error: Unable to resolve local host.", "Local host error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
			return;
		}
		// userid
		Profile.getProfile().addHook(new Hook() {
			public Node onSave() {
				final UUID id = getID();
				return id != null ? new Node(Tag.construct("userID"), id) : null;
			}
		});
		final Node userIdNode = Profile.getProfile().search("/userID");
		if (userIdNode != null) {
			final String userIdString = userIdNode.getData();
			if (userIdString != null) {
				setID(userIdString);
			}
		}
		if (getID() == null) {
			setID(UUID.randomUUID());
		}
		System.out.println("UserID: " + getID());
		// Events
		EventManager.getEventManager().add(nickEventProvider);
		//Tray
		if (TrayManager.isSupported()) {
			tray = new TrayManager(null);
			tray.activate();
		}
		// GUI
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				try {
					popupManager = new PopupManager();
					frame = new MainFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		titleManager = new FrameTitleManager(frame, "GHSC v" + Application.VERSION.getDetailed()) {
			public void onTitleChanged(String title) {
				if (tray != null)
					tray.updateTooltip(title);
			}
		};
		adminControl = new AdminControl();
		fileShare = new FileShare();
		Thread.yield();
		
		System.out.println("Current version: " + VERSION);
		frame.setStatus("Checking for updates");
		versionController = new VersionController();
		System.out.println("Latest version: " + versionController.refresh(true));
		//System.out.println("Compatible: " + versionController.isCompatible(versionController.getLatest()));
		updater = new Updater();

		// for some reason, it freezes inside isJar()...
		if (Application.isJar()) {
			updater.updateCheck(false, true);
		}
		
		versionController.start();
		setVersion(VERSION);
		
		Profile.getProfile().addHook(new Hook() {
			public Node onSave() {
				return new Node(Tag.construct("nick"), getNick());
			}
		});
		Node nickNode = Profile.getProfile().search("/nick");
		if (nickNode != null) {
			String nickData = nickNode.getData();
			if (nickData != null) {
				setNick(nickData);
			}
		}
		setHostname(System.getProperty("user.name"));
		
		frame.setStatus("Starting network interface");
		try {
			socketManager = new SocketManager();
			socketManager.start();
		} catch (BindException be) {
			JOptionPane.showMessageDialog(frame, "Another instance of this application is already running.", "Application error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		} catch (IOException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Unable to initialize network interface.", "Network error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		
		frame.setStatus("Loading channels");
		ChatContainer chatContainer = frame.getChatContainer();
		
		Settings.getSettings().addHook(new Hook() {
			public Node onSave() {
				return new Node(Tag.construct("chats"), getMainFrame().getChatContainer().printChats());
			}
		});
		Node chatsNode = Settings.getSettings().search("/chats");
		while (true) {
			if (chatsNode != null) {
				String chatString = chatsNode.getData();
				if (chatString != null) {
					String[] chats = chatString.split(Pattern.quote(","));
					if (chats.length > 0) {
						for (String chatName : chats) {
							if (chatName.startsWith("#")) {
								chatContainer.add(new Channel(chatContainer, chatName));
							} else {
								// TODO: handle private messaging
							}
						}
						break;
					}
				}
			}
			chatContainer.add(new Channel(chatContainer, "#Global"));
			break;
		}
		
		frame.toggleInput(true);
		frame.setStatus("Finalizing startup...", 1000);
		
		Profile.getProfile().setSavable(true);
		Settings.getSettings().setSavable(true);
	}
	
	/**
	 * Restarts the entire application.</br>
	 * This application will only restart if running from a JAR file.
	 */
	public static void restart() {
		if (isJar()) {
			try {
				Runtime.getRuntime().exec("java -jar \"" + currentRunningPath().trim() + "\"");
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(0);
		} else {
			System.out.println("Can't restart, not running from jar.");
		}
	}
	
	/**
	 * Checks to see if application is currently running from a JAR file.
	 * @return <tt>true</tt> if application is running from a JAR file, otherwise <tt>false</tt>.
	 */
	public static boolean isJar() {
		final String runningPath = currentRunningPath();
		return runningPath.toLowerCase().endsWith(".jar");
	}
	
	/**
	 * @return the current working/running path of this application.
	 */
	public static String currentRunningPath() {
		try {
			return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
		} catch (URISyntaxException e) {
			return null;
		}
	}
	
}