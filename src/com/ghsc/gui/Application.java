package com.ghsc.gui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.ghsc.admin.AdminControl;
import com.ghsc.common.Debug;
import com.ghsc.event.EventProvider;
import com.ghsc.event.IEventProvider;
import com.ghsc.files.FileStorage.Node;
import com.ghsc.files.Profile;
import com.ghsc.files.Settings;
import com.ghsc.gui.components.chat.ChatContainer;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.input.InputWizard;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.popup.PopupManager;
import com.ghsc.gui.components.util.FrameTitleManager;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.gui.tray.TrayManager;
import com.ghsc.impl.ComplexIdentifiable;
import com.ghsc.net.sockets.NicManager;
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
	
	private static Application INSTANCE = null;
	
	static {
		INSTANCE = new Application();
	}
	
	/**
	 * Gets the application instance. Cannot return null.
	 */
	public static Application getInstance() {
		return INSTANCE;
	}
	
	public static Debug DEBUG = Debug.MINOR;
	public static Charset CHARSET = Charset.forName("UTF-8");
	public static Version VERSION = Version.create("0.4.0", "Indev");
	public static File LAST_DIRECTORY;
	private final String PROGRAM_NAME = "GHSC";
	
	private MainFrame frame;
	private FrameTitleManager titleManager;
	private PopupManager popupManager;
	private TrayManager tray;
	private AdminControl adminControl;
	private FileShare fileShare;
	private InputWizard nickDialog;

	// Updating
	private VersionController versionController;
	private Updater updater = null;
	
	// Networking
	private SocketManager socketManager;
	public static NicManager NETWORK = new NicManager();
	
	// Events
	private final EventProvider<String> nickEventProvider = new EventProvider<>();
	
	private String hostname;
	private String nick;
	private UUID userID;
	
	private Application() {}
	
	@Override
	public String getHostname() {
		return hostname;
	}
	
	@Override
	public void setHostname(final String hostname) {
		this.hostname = hostname;
		if (this.nick == null) {
			this.setNick(hostname);
		}
	}
	
	@Override
	public String getNick() {
		return nick;
	}
	
	@Override
	public void setNick(final String nick) {
		this.nick = nick;
		if (this.hostname == null) {
			this.hostname = nick;
		}
		this.nickEventProvider.fireEvent(nick);
	}

    /**
     * Gets the nick event provider.
     */
	public IEventProvider<String> getNickEventProvider() {
	    return this.nickEventProvider;
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
	public void setID(UUID uuid) {
		this.userID = uuid;
	}

	@Override
	public void setID(final String uuid) {
        this.setID(UUID.fromString(uuid));
	}
	
	/**
	 * Changes the version of the application.</br>
	 * Will also visually change the title of the application to match the version.
	 * @param vers - the new version of this application.
	 */
	public void setVersion(Version vers) {
		Application.VERSION = vers;
		if (this.titleManager != null) {
			this.titleManager.appendTitle(null);
		}
	}
	
	/**
	 * If the current application doesn't have focus,</br>
	 * then this method will cause the taskbar item to flash.
	 */
	public void flashTaskbar() {
		if (this.frame != null && !this.frame.isFocused()) {
			this.frame.toFront();
		}
	}
	
	/**
	 * Displays a wizard for current user to modify his display name/username.
	 */
	public void showNickWizard() {
		if (this.nickDialog != null && this.nickDialog.isVisible()) {
            return;
        }
		this.nickDialog = new InputWizard(frame, "Change nickname", "Nickname", getPreferredName(), "Apply", "Applies your new nickname!",
                input -> {
                    if (input != null) {
                        if (input.equals(nick))
                            return;
                        setNick(input);
                        frame.getChatContainer().refreshUser(null);
                    } else {
                        if (Debug.MINOR.compareTo(DEBUG) < 0)
                            System.out.println("Nickname wizard cancelled.");
                    }
                }, text -> {
                    if (text.isEmpty())
                        return new ValidationResult<>("Well, you actually have to type something...", false);
                    if (text.length() > 18)
                        return new ValidationResult<>("Name can't exceed 18 characters.", false);
                    if (text.startsWith("_") || text.startsWith(" "))
                        return new ValidationResult<>("Can't start with any space character.", false);
                    if (text.contains("__") || text.contains("  "))
                        return new ValidationResult<>("Can't contain two spaces anywhere.", false);
                    if (text.contains(" _") || text.contains("_ "))
                        return new ValidationResult<>("Come on...", false);
                    for (char c : text.toCharArray()) {
                        if (Character.isDigit(c) || Character.isLetter(c) || c == ' ' || c == '_') continue;
                        return new ValidationResult<>("Only allowed letters and numbers!", false);
                    }
                    return new ValidationResult<>("Current name is acceptable.", true);
                });
		this.nickDialog.setVisible(true);
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
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
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
        }));
		
		this.socketManager = new SocketManager();
		
		boolean duplicateInstance = false;
		if (!this.socketManager.instanceCheck()) {
			duplicateInstance = true;
			if (JOptionPane.showConfirmDialog(frame, PROGRAM_NAME + " is already running.  Would you like to launch a new instance?", "Application notice", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				System.exit(0);
			}
		}
		try {
			this.socketManager.initControllers();
		} catch (IOException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Unable to initialize network interface.", "Network error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// userid
		Profile.getProfile().addHook(() -> {
            final UUID id = getID();
            return id != null ? new Node(Tag.construct("userID"), id) : null;
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
		//Tray
		if (TrayManager.isSupported()) {
			tray = new TrayManager(null);
			tray.activate();
		}
		// GUI
		SwingUtilities.invokeAndWait(() -> {
            try {
                popupManager = new PopupManager();
                frame = new MainFrame(Application.this);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
		titleManager = new FrameTitleManager(frame, PROGRAM_NAME + " v" + Application.VERSION.getDetailed()) {
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
		
		Profile.getProfile().addHook(() -> new Node(Tag.construct("nick"), getNick()));
		Node nickNode = Profile.getProfile().search("/nick");
		if (nickNode != null) {
			String nickData = nickNode.getData();
			if (nickData != null) {
				setNick(nickData);
			}
		}
		setHostname(System.getProperty("user.name"));
		
		frame.setStatus("Starting network interface");
		socketManager.start();
		
		frame.setStatus("Loading channels");
		ChatContainer chatContainer = frame.getChatContainer();
		
		Settings.getSettings().addHook(() -> new Node(Tag.construct("chats"), getMainFrame().getChatContainer().printChats()));
		Node chatsNode = Settings.getSettings().search("/chats");
        loadChannels: {
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
                        break loadChannels;
                    }
                }
            }
            chatContainer.add(new Channel(chatContainer, "#Global"));
        }
		
		frame.toggleInput(true);
		frame.setStatus("Finalizing startup...", 1000);
		
		if (!duplicateInstance) {
			Profile.getProfile().setSavable(true);
			Settings.getSettings().setSavable(true);
		}
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