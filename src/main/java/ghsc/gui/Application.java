package ghsc.gui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.swing.*;

import ghsc.common.Debug;
import ghsc.event.EventProvider;
import ghsc.event.IEventProvider;
import ghsc.files.FileStorage.Node;
import ghsc.files.Profile;
import ghsc.files.Settings;
import ghsc.gui.components.chat.ChatContainer;
import ghsc.gui.components.chat.channels.Channel;
import ghsc.gui.components.input.InputWizard;
import ghsc.gui.components.input.ValidationResult;
import ghsc.gui.components.popup.PopupManager;
import ghsc.gui.components.util.FrameTitleManager;
import ghsc.gui.fileshare.FileShare;
import ghsc.gui.tray.TrayManager;
import ghsc.impl.ComplexIdentifiable;
import ghsc.net.sockets.NicManager;
import ghsc.net.sockets.SocketManager;
import ghsc.net.update.Updater;
import ghsc.net.update.Version;
import ghsc.net.update.VersionController;
import ghsc.util.Tag;

/**
 * The main application class.
 */
public class Application implements ComplexIdentifiable {
	
	private static final Application INSTANCE;
	
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
	public static Charset CHARSET = StandardCharsets.UTF_8;
	public static Version VERSION = Version.create("0.4.0", "dev");
	public static File LAST_DIRECTORY;
	private static final String PROGRAM_NAME = "GHSC";
	
	private MainFrame frame;
	private FrameTitleManager titleManager;
	private PopupManager popupManager;
	private TrayManager tray;
	private FileShare fileShare;
	private InputWizard nickDialog;

	// Updating
	private VersionController versionController;

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
		return this.hostname;
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
		return this.nick;
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
	public void setID(final UUID uuid) {
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
	public void setVersion(final Version vers) {
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
		this.nickDialog = new InputWizard(this.frame, "Change nickname", "Nickname", this.getPreferredName(), "Apply", "Applies your new nickname!",
                input -> {
                    if (input != null) {
                        if (input.equals(this.nick)) {
							return;
						}
						this.setNick(input);
						this.frame.getChatContainer().refreshUser(null);
                    } else {
                        if (Debug.MINOR.compareTo(DEBUG) < 0) {
							System.out.println("Nickname wizard cancelled.");
						}
                    }
                }, text -> {
                    if (text.isEmpty()) {
						return new ValidationResult<>("Well, you actually have to type something...", false);
					}
                    if (text.length() > 18) {
						return new ValidationResult<>("Name can't exceed 18 characters.", false);
					}
                    if (text.startsWith("_") || text.startsWith(" ")) {
						return new ValidationResult<>("Can't start with any space character.", false);
					}
                    if (text.contains("__") || text.contains("  ")) {
						return new ValidationResult<>("Can't contain two spaces anywhere.", false);
					}
                    if (text.contains(" _") || text.contains("_ ")) {
						return new ValidationResult<>("Come on...", false);
					}
                    for (final char c : text.toCharArray()) {
                        if (Character.isDigit(c) || Character.isLetter(c) || c == ' ' || c == '_') {
							continue;
						}
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
		return this.socketManager;
	}

	/**
	 * @return the main GUI frame associated with this application.
	 */
	public MainFrame getMainFrame() {
		return this.frame;
	}
	
	/**
	 * @return the title manager associated with this application.
	 */
	public FrameTitleManager getTitleManager() {
		return this.titleManager;
	}
	
	/**
	 * @return the popup manager supporting all components.
	 */
	public PopupManager getPopupManager() {
		return this.popupManager;
	}
	
	/**
	 * @return the tray icon manager associated with this application.
	 */
	public TrayManager getTrayManager() {
		return this.tray;
	}
	
	/**
	 * @return the file transfer handler of this application.
	 */
	public FileShare getFileShare() {
		return this.fileShare;
	}
	
	/**
	 * @return the version controller associated with this application.
	 */
	public VersionController getVersionController() {
		return this.versionController;
	}

	/**
	 * Initialize the contents of the application.
	 */
	public void initialize() throws InvocationTargetException, InterruptedException {
		/*
		 * Add this shutdown hook, so that correct de-initialization will take place even if we call System.exit(int).
		 */
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (this.socketManager != null) {
				this.socketManager.close();
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
            if (this.frame != null) {
                if (this.frame.getUsers() != null) {
					this.frame.getUsers().disconnectAll();
                }
            }
            if (this.fileShare != null) {
				this.fileShare.dispose();
			}
            System.gc();
        }));

        try {
            this.socketManager = new SocketManager();
        } catch (final IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this.frame, "Unable to initialize network interface.", "Network error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {}

        // userid
		Profile.getProfile().addHook(() -> {
            final UUID id = this.getID();
            return id != null ? new Node(Tag.construct("userID"), id) : null;
        });
		final Node userIdNode = Profile.getProfile().search("/userID");
		if (userIdNode != null) {
			final String userIdString = userIdNode.getData();
			if (userIdString != null) {
				this.setID(userIdString);
			}
		}
		if (this.getID() == null) {
			this.setID(UUID.randomUUID());
		}
		System.out.println("UserID: " + this.getID());
		//Tray
		if (TrayManager.isSupported()) {
			this.tray = new TrayManager(null);
			this.tray.activate();
		}
		// GUI
		SwingUtilities.invokeAndWait(() -> {
            try {
				this.popupManager = new PopupManager();
				this.frame = new MainFrame(this);
				this.frame.setVisible(true);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
		this.titleManager = new FrameTitleManager(this.frame, PROGRAM_NAME + " v" + Application.VERSION.getDetailed()) {
			public void onTitleChanged(final String title) {
				if (Application.this.tray != null) {
					Application.this.tray.updateTooltip(title);
				}
			}
		};
		this.fileShare = new FileShare();
		Thread.yield();
		
		System.out.println("Current version: " + VERSION);
		this.frame.setStatus("Checking for updates");
		this.versionController = VersionController.createFromLatest();
		System.out.println("Latest version: " + this.versionController.getKnownLatest());
		//System.out.println("Compatible: " + versionController.isCompatible(versionController.getLatest()));

		// for some reason, it freezes inside isJar()...
		if (Application.isJar()) {
			Updater.updateCheck(true);
		}

		this.versionController.start();
		this.setVersion(VERSION);
		
		Profile.getProfile().addHook(() -> new Node(Tag.construct("nick"), this.getNick()));
		final Node nickNode = Profile.getProfile().search("/nick");
		if (nickNode != null) {
			final String nickData = nickNode.getData();
			if (nickData != null) {
				this.setNick(nickData);
			}
		}
		this.setHostname(System.getProperty("user.name"));

		this.frame.setStatus("Starting network interface");
		this.socketManager.start();

		this.frame.setStatus("Loading channels");
		final ChatContainer chatContainer = this.frame.getChatContainer();
		
		Settings.getSettings().addHook(() -> new Node(Tag.construct("chats"), this.getMainFrame().getChatContainer().printChats()));
		final Node chatsNode = Settings.getSettings().search("/chats");
        loadChannels: {
            if (chatsNode != null) {
                final String chatString = chatsNode.getData();
                if (chatString != null) {
                    final String[] chats = chatString.split(Pattern.quote(","));
                    if (chats.length > 0) {
                        for (final String chatName : chats) {
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

		this.frame.toggleInput(true);
		this.frame.setStatus("Finalizing startup...", 1000);

        Profile.getProfile().setSavable(true);
        Settings.getSettings().setSavable(true);
	}
	
	/**
	 * Restarts the entire application.
	 * This application will only restart if running from a JAR file.
	 */
	public static void restart() {
		try {
			final String runningPath = currentRunningPath();
			if (isJar(runningPath)) {
				Runtime.getRuntime().exec("java -jar \"" + runningPath.trim() + "\"");
				System.exit(0);
			}
		} catch (final URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("Can't restart, not running from jar.");
	}
	
	/**
	 * Checks to see if application is currently running from a JAR file.
	 * @return <tt>true</tt> if application is running from a JAR file, otherwise <tt>false</tt>.
	 */
	public static boolean isJar() {
        try {
            return isJar(currentRunningPath());
        } catch (final URISyntaxException e) {
            return false;
        }
    }

	private static boolean isJar(final String runningPath) {
		return runningPath.toLowerCase().endsWith(".jar");
	}
	
	/**
	 * @return the current working/running path of this application.
	 */
	public static String currentRunningPath() throws URISyntaxException {
		return new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
	}
	
}