package com.ghsc.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;

import com.ghsc.admin.commands.AdminCommand;
import com.ghsc.admin.commands.flash.FlashCommand;
import com.ghsc.admin.commands.kick.KickCommand;
import com.ghsc.common.Colors;
import com.ghsc.common.Debug;
import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.event.EventListener;
import com.ghsc.event.message.MessageEvent;
import com.ghsc.files.FileStorage.Node;
import com.ghsc.files.Profile;
import com.ghsc.gui.components.antispam.SpamControl;
import com.ghsc.gui.components.antispam.SpamControl.SpamBan;
import com.ghsc.gui.components.chat.Chat;
import com.ghsc.gui.components.chat.ChatContainer;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.components.chat.channels.ChannelElement;
import com.ghsc.gui.components.chat.input.ChatInput;
import com.ghsc.gui.components.input.InputWizard;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.status.StatusLabel;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.users.UserContainer;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.gui.tray.TrayManager;
import com.ghsc.util.Tag;
import com.ghsc.util.TimeStamp;
import com.ghsc.util.Utilities;

/**
 * The main GUI interface.
 */
public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	/*
	 * Custom variables
	 */
	private SpamControl spamControl;
	
	/*
	 * Event variables
	 */
	private final EventListener<String> nickListener = (final String nick) -> {
        final Application application = Application.getInstance();
        setNick(nick);
        getUsers().send(MessageEvent.construct(MessageEvent.Type.IDENTIFY, User.ATT_HOSTNAME, application.getHostname(), User.ATT_NICK, application.getPreferredName()), User.ALL);
    };
	private final EventListener<String> sendMessageListener = new EventListener<String>() {
		public void eventReceived(final String text) {
			final Chat chat = chatContainer.getSelectedChat();
			if (chat != null) {
				final String trimmedText = text.trim();
				chatTextInput.setText("");
				if (!trimmedText.isEmpty()) {
					String currChat = chat.getName();
					if (chat instanceof Channel) {
						Channel chan = (Channel) chat;
						if (!Application.getInstance().getAdminControl().isAdmin() && chan.getUserCount() > 0) {
							switch (spamControl.filter(trimmedText, currChat)) {
								case 1:
									//JOptionPane.showMessageDialog(MainFrame.this, "GHSC has detected you spamming " + currChannel + ".\nIf you continue, you will be temporarily banned from the channel.", "Spam warning!", JOptionPane.WARNING_MESSAGE);
									chan.addElement(new ChannelElement(chan.getElements(), TimeStamp.newInstance(), chan.getName(), "Please stop spamming or I'll have to ban you!", null, Colors.MESSAGE_ORANGE), true);
									return;
								case 2:
									StringBuilder build2 = new StringBuilder();
									build2.append("You've been banned because you continued spamming.");
									SpamBan sb2 = spamControl.getChannelBan(currChat);
									if (sb2 != null) { // this is just spam control, has nothing to do 
										build2.append("\nBan will last for ");
										build2.append((long)(((double) sb2.remaining()) / 1000.0D));
										build2.append(" seconds.");
										//JOptionPane.showMessageDialog(MainFrame.this, build2.toString(), "You have been banned!", JOptionPane.ERROR_MESSAGE);
										chan.addElement(new ChannelElement(chan.getElements(), TimeStamp.newInstance(), chan.getName(), null, build2.toString(), Colors.MESSAGE_RED), true);
										return;
									}
									break;
								case 3:
									SpamBan sb3 = spamControl.getChannelBan(currChat);
									if (sb3 != null) {
										String build3 = "You are still banned. Try again in " +
												(long) (((double) sb3.remaining()) / 1000.0D) +
												" seconds!";
										//JOptionPane.showMessageDialog(MainFrame.this, build3.toString(), "You are banned!", JOptionPane.ERROR_MESSAGE);	
										chan.addElement(new ChannelElement(chan.getElements(), TimeStamp.newInstance(), chan.getName(), "Haha!", build3, Colors.MESSAGE_RED), true);
										return;
									}
									break;
							}
						}
						MainFrame.this.getUsers().send(MessageEvent.construct(MessageEvent.Type.MESSAGE, User.ATT_CHANNEL, currChat, trimmedText), currChat);
						chan.addElement(new ChannelElement(chan.getElements(), TimeStamp.newInstance(), null, trimmedText), true);
					} else {
						// TODO: not a Channel, handle PMs?
					}
				}
			}
		}
	};
	
	private InputWizard joinChannelWizard = null;
	
	private JPanel canvas;
	private JPanel userPanel;
	private StatusLabel statusLabel;
	private JScrollPane userScrollPane;
	private UserContainer userList;
	private JLabel mainImageLabel;
	private JLabel nickLabel;
	private JPanel chatPanel;
	private ChatContainer chatContainer;
	private ChatInput chatTextInput;
	private JButton sendMessageButton;
	private JToolBar userToolBar;
	private JScrollPane chatTextInputScroll;
	private JButton joinChannelButton;
	private JToggleButton adminButton;
	private JButton settingsButton;
	private JButton fileTransferButton;
	
	/**
	 * Create the frame.
	 */
	public MainFrame(final Application application) {

	    application.getNickEventProvider().subscribe(this.nickListener);
		
		this.spamControl = new SpamControl();
		Profile.getProfile().addHook(() -> new Node(Tag.construct("bannedchannels"), this.spamControl.printBanned()));
		Node bannedChannelsNode = Profile.getProfile().search("/bannedchannels");
		if (bannedChannelsNode != null) {
			String bannedChannelString = bannedChannelsNode.getData();
			if (bannedChannelString != null) {
				String[] bannedChannels = bannedChannelString.split(Pattern.quote(","));
				this.spamControl.load(bannedChannels);
			}
		}
		
		initComponents();
	}
	
	/*
	 * Custom functions
	 */
	
	/**
	 * Changes the current status of the GUI.
	 */
	public void setStatus(String status) {
		setStatus(status, 0);
	}
	
	/**
	 * Changes the current status of the GUI, for a given amount of time.</br>
	 * The status will default back to "Idle" after the given amount of time.
	 */
	public void setStatus(String status, int period) {
		statusLabel.submit(status, period);
	}
	
	/**
	 * Changes the current nick label to given name.
	 * @param nick - name to show as nick.
	 */
	public void setNick(String nick) {
		nickLabel.setText(nick);
	}
	
	/**
	 * @return the spam controller for this frame.
	 */
	public SpamControl getSpamControl() {
		return spamControl;
	}
	
	/**
	 * Toggles whether the user can use the chat controls.
	 */
	public void toggleInput(boolean enabled) {
		chatTextInput.setEnabled(enabled);
		sendMessageButton.setEnabled(enabled);
	}
	
	/*
	 * End of custom functions
	 */
	
	/**
	 * Constructs all the components of the main frame.
	 */
	private void initComponents() {
		if (TrayManager.isSupported()) {
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					setVisible(false);
					final TrayManager trayManager = Application.getInstance().getTrayManager();
					if (trayManager != null) {
						trayManager.onFrameClosed();
					}
				}
			});
		}
		setFont(Fonts.GLOBAL.deriveFont(Font.BOLD));
		setMinimumSize(new Dimension(700, 450));
		setTitle("GHSC");
		setIconImage(Images.ICON_32);
		setDefaultCloseOperation(TrayManager.isSupported() ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.EXIT_ON_CLOSE);
		setSize(700, 450);
		setLocationRelativeTo(null);
		
		canvas = new JPanel();
		canvas.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(canvas);
		GroupLayout gl_canvas = new GroupLayout(canvas);
		gl_canvas.setHorizontalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(getUserPanel(), GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getChatPanel(), GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
					.addContainerGap())
		);
		gl_canvas.setVerticalGroup(
			gl_canvas.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_canvas.createParallelGroup(Alignment.TRAILING)
						.addComponent(getChatPanel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
						.addComponent(getUserPanel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE))
					.addContainerGap())
		);
		canvas.setLayout(gl_canvas);
	}
	
	public JPanel getUserPanel() {
		if (userPanel == null) {
			userPanel = new JPanel();
			userPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
			GroupLayout gl_userPanel = new GroupLayout(userPanel);
			gl_userPanel.setHorizontalGroup(
				gl_userPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_userPanel.createSequentialGroup()
						.addGap(0)
						.addGroup(gl_userPanel.createParallelGroup(Alignment.TRAILING)
							.addComponent(getStatusLabel(), GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
							.addGroup(gl_userPanel.createSequentialGroup()
								.addComponent(getMainImageLabel())
								.addGap(10)
								.addComponent(getNickLabel(), GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE))))
					.addComponent(getUserScrollPane(), GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
					.addComponent(getUserToolBar(), GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
			);
			gl_userPanel.setVerticalGroup(
				gl_userPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_userPanel.createSequentialGroup()
						.addGroup(gl_userPanel.createParallelGroup(Alignment.LEADING)
							.addComponent(getMainImageLabel())
							.addGroup(gl_userPanel.createSequentialGroup()
								.addContainerGap()
								.addComponent(getNickLabel())))
						.addGap(7)
						.addComponent(getStatusLabel(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(getUserScrollPane(), GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(getUserToolBar(), GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE))
			);
			userPanel.setLayout(gl_userPanel);
		}
		return userPanel;
	}
	
	public StatusLabel getStatusLabel() {
		if (statusLabel == null) {
			statusLabel = new StatusLabel(true);
			statusLabel.setStatus("Starting up...");
			statusLabel.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD, 14));
		}
		return statusLabel;
	}
	
	public JScrollPane getUserScrollPane() {
		if (userScrollPane == null) {
			userScrollPane = new JScrollPane();
			userScrollPane.setViewportView(getUsers());
		}
		return userScrollPane;
	}
	
	public UserContainer getUsers() {
		if (userList == null) {
			Profile.getProfile().addHook(() -> new Node(Tag.construct("friends"), userList.printFriends()));
			Profile.getProfile().addHook(() -> new Node(Tag.construct("ignored"), userList.printIgnored()));
			final Node friendsNode = Profile.getProfile().search("/friends");
			final String[] friends = friendsNode != null && friendsNode.getData() != null ? friendsNode.getData().split(Pattern.quote(",")) : new String[0];
			final Node ignoredNode = Profile.getProfile().search("/ignored");
			final String[] ignored = ignoredNode != null && ignoredNode.getData() != null ? ignoredNode.getData().split(Pattern.quote(",")) : new String[0];
			userList = new UserContainer(this, friends, ignored);
			final Application application = Application.getInstance();
			application.getPopupManager().submit((menu, popupManager, sender, x, y) -> {
                if (!userList.isEnabled()) {
                    return false;
                }
                int index = userList.locationToIndex(new Point(x, y));
                if (index >= 0) {
                    userList.setSelectedIndex(index);
                    final User u = userList.getSelectedValue();
                    if (u != null) {
                        JMenuItem fi = menu.createItem(u.isFriend() ? "Unmark friend" : "Mark as friend", e -> u.setFriend(!u.isFriend()));
                        fi.setFont(Fonts.GLOBAL);
                        menu.add(fi);

                        JMenuItem ii = menu.createItem(u.isIgnored() ? "Show messages" : "Ignore messages", e -> u.setIgnored(!u.isIgnored()));
                        ii.setFont(Fonts.GLOBAL);
                        menu.add(ii);

                        if (application.getAdminControl().isAdmin()) {
                            menu.addSeparator();

                            JMenu adminMenu = menu.createMenu("Admin");
                            {
                                // TODO: make this dynamic!
                                JMenuItem kI = menu.createItem("Kick", new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        u.send(AdminCommand.composeCommand(KickCommand.TAG, KickCommand.ATT_CHANNEL, chatContainer.getSelectedChat().getName()));
                                    }
                                });
                                kI.setFont(Fonts.GLOBAL);
                                adminMenu.add(kI);

                                final boolean flashStatus = Utilities.resolveToBoolean(u.getCommandState(FlashCommand.TAG));
                                JMenuItem flI = menu.createItem(flashStatus ? "Disable flashing" : "Enable flashing", new ActionListener() {
                                    public void actionPerformed(ActionEvent e) {
                                        u.send(AdminCommand.composeCommand(FlashCommand.TAG, FlashCommand.ATT_ENABLE, Utilities.resolveToString(!flashStatus)));
                                    }
                                });
                                flI.setFont(Fonts.GLOBAL);
                                adminMenu.add(flI);
                            }
                            adminMenu.setFont(Fonts.GLOBAL);
                            menu.add(adminMenu);
                        }

                        menu.addSeparator();

                        JMenuItem ci = menu.createItem("Cancel", e -> menu.setVisible(false));
                        ci.setFont(Fonts.GLOBAL);
                        menu.add(ci);
                        return true;
                    }
                }
                return false;
            }, userList);
		}
		return userList;
	}
	
	public JLabel getMainImageLabel() {
		if (mainImageLabel == null) {
			mainImageLabel = new JLabel();
			mainImageLabel.setIcon(new ImageIcon(Images.ICON_LARGE));
		}
		return mainImageLabel;
	}
	
	public JLabel getNickLabel() {
		if (nickLabel == null) {
			nickLabel = new JLabel("...");
			nickLabel.setToolTipText("Double click to change nick.");
			nickLabel.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD, 18));
			nickLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 &&
							e.getClickCount() > 1) {
						Application.getInstance().showNickWizard();
					}
				}
			});
			Application.getInstance().getPopupManager().submit((menu, popupManager, sender, x, y) -> {
                JMenuItem cdn = menu.createItem("Change nick", e -> Application.getInstance().showNickWizard());
                cdn.setFont(Fonts.GLOBAL);
                menu.add(cdn);
                return true;
            }, nickLabel);
		}
		return nickLabel;
	}
	
	public JPanel getChatPanel() {
		if (chatPanel == null) {
			chatPanel = new JPanel();
			GroupLayout gl_chatPanel = new GroupLayout(chatPanel);
			gl_chatPanel.setHorizontalGroup(
				gl_chatPanel.createParallelGroup(Alignment.TRAILING)
					.addComponent(getChatContainer(), GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
					.addGroup(gl_chatPanel.createSequentialGroup()
						.addComponent(getChatTextInputScroll(), GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(getSendMessageButton(), GroupLayout.PREFERRED_SIZE, 102, GroupLayout.PREFERRED_SIZE))
			);
			gl_chatPanel.setVerticalGroup(
				gl_chatPanel.createParallelGroup(Alignment.TRAILING)
					.addGroup(gl_chatPanel.createSequentialGroup()
						.addComponent(getChatContainer(), GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_chatPanel.createParallelGroup(Alignment.LEADING, false)
							.addComponent(getChatTextInputScroll(), 23, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(getSendMessageButton(), Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)))
			);
			chatPanel.setLayout(gl_chatPanel);
		}
		return chatPanel;
	}
	
	public ChatContainer getChatContainer() {
		if (chatContainer == null) {
			chatContainer = new ChatContainer(this);
		}
		return chatContainer;
	}
	
	public JScrollPane getChatTextInputScroll() {
		if (chatTextInputScroll == null) {
			chatTextInputScroll = new JScrollPane();
			chatTextInputScroll.setDoubleBuffered(true);
			chatTextInputScroll.setBorder(UIManager.getBorder("TextField.border"));
			chatTextInputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			chatTextInputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			chatTextInputScroll.setViewportView(getChatTextInput());
		}
		return chatTextInputScroll;
	}
	
	public ChatInput getChatTextInput() {
		if (chatTextInput == null) {
			chatTextInput = new ChatInput(this);
			chatTextInput.getEventProvider().subscribe(this.sendMessageListener);
		}
		return chatTextInput;
	}
	
	public JButton getSendMessageButton() {
		if (sendMessageButton == null) {
			sendMessageButton = new JButton("Send message");
			sendMessageButton.setEnabled(false);
			sendMessageButton.addActionListener(e -> this.chatTextInput.sendMessage());
		}
		return sendMessageButton;
	}
	
	public JToolBar getUserToolBar() {
		if (userToolBar == null) {
			userToolBar = new JToolBar();
			userToolBar.setFloatable(false);
			userToolBar.setDoubleBuffered(true);
			userToolBar.add(Box.createHorizontalGlue());
			userToolBar.add(getJoinButton());
			userToolBar.add(getFileTransferButton());
			userToolBar.add(getAdminButton());
			userToolBar.add(getSettingsButton());
			userToolBar.add(Box.createHorizontalGlue());
		}
		return userToolBar;
	}
	
	public JButton getJoinButton() {
		if (joinChannelButton == null) {
			joinChannelButton = new JButton();
			joinChannelButton.setIconTextGap(0);
			joinChannelButton.setHorizontalTextPosition(SwingConstants.CENTER);
			joinChannelButton.setIcon(new ImageIcon(Images.ADD_DARK));
			joinChannelButton.addActionListener(arg0 -> {
                if (joinChannelWizard != null && joinChannelWizard.isVisible()) {
                    return;
                }
                joinChannelWizard = new InputWizard(MainFrame.this, "Join a channel.", "Channel name", null, "Join", "Creates and joins a new Channel!",
                        input -> {
                            if (input != null) {
                                input = input.trim();
                                Channel chan = new Channel(chatContainer, "#" + input);
                                chatContainer.add(chan);
                                chatContainer.setSelectedComponent(chan.getPanel());
                            } else {
                                System.out.println("Channel wizard cancelled.");
                            }
                        }, text -> {
                            text = text.trim();
                            if (text.isEmpty())
                                return new ValidationResult<>("Well, you actually have to type something...", false);
                            if (chatContainer.getChat("#" + text) != null)
                                return new ValidationResult<>("You're already in this channel.", false);
                            for (char c : text.toCharArray()) {
                                if (Character.isDigit(c) || Character.isLetter(c) || c == ' ') continue;
                                return new ValidationResult<>("Only allowed letters and numbers!", false);
                            }
                            return new ValidationResult<>("Current name is acceptable.", true);
                        });
                joinChannelWizard.setVisible(true);
            });
			joinChannelButton.setFont(Fonts.GLOBAL);
			joinChannelButton.setToolTipText("Join a channel");
			joinChannelButton.setDoubleBuffered(true);
			joinChannelButton.setFocusable(false);
		}
		return joinChannelButton;
	}
	
	public JButton getFileTransferButton() {
		if (fileTransferButton == null) {
			fileTransferButton = new JButton();
			fileTransferButton.setHorizontalTextPosition(SwingConstants.CENTER);
			fileTransferButton.setIconTextGap(0);
			fileTransferButton.setIcon(new ImageIcon(Images.PAGE_GO));
			fileTransferButton.addActionListener(unused -> {
                if (Debug.NONE.compareTo(Application.DEBUG) < 0) {
                    FileShare ft = Application.getInstance().getFileShare();
                    if (ft != null) {
                        ft.setVisible(true);
                    }
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, "Feature not supported yet!", "Feature not supported.", JOptionPane.ERROR_MESSAGE);
                }
            });
			fileTransferButton.setFont(Fonts.GLOBAL);
			fileTransferButton.setToolTipText("File sharing");
			fileTransferButton.setDoubleBuffered(true);
			fileTransferButton.setFocusable(false);
		}
		return fileTransferButton;
	}
	
	public JToggleButton getAdminButton() {
		if (adminButton == null) {
			adminButton = new JToggleButton();
			adminButton.setHorizontalTextPosition(SwingConstants.CENTER);
			adminButton.setIconTextGap(0);
			adminButton.setIcon(new ImageIcon(Images.KEY));
			adminButton.addActionListener(unused -> {
                final Application application = Application.getInstance();
                if (application.getAdminControl().isLoginVisible()) {
                    adminButton.setSelected(!adminButton.isSelected());
                    return;
                }
                if (!adminButton.isSelected()) { // logout
                    if (JOptionPane.showConfirmDialog(MainFrame.this, "Are you sure you want to logout of admin controls?", "Logout?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        application.getAdminControl().setAdmin(false);
                        adminButton.setToolTipText("Login as admin");
                    } else {
                        adminButton.setSelected(true);
                    }
                } else { // login
                    if (application.getAdminControl().isReady()) {
                        application.getAdminControl().showLogin();
                    } else {
                        adminButton.setSelected(false);
                        JOptionPane.showMessageDialog(application.getMainFrame(), "Due to network complications, passwords entered can't be validated.", "Admin control is currently unavailable.", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
			adminButton.setFont(Fonts.GLOBAL);
			adminButton.setToolTipText("Login as admin");
			adminButton.setDoubleBuffered(true);
			adminButton.setFocusable(false);
		}
		return adminButton;
	}
	
	public JButton getSettingsButton() {
		if (settingsButton == null) {
			settingsButton = new JButton();
			settingsButton.setHorizontalTextPosition(SwingConstants.CENTER);
			settingsButton.setIconTextGap(0);
			settingsButton.setIcon(new ImageIcon(Images.COG));
			settingsButton.addActionListener(unused -> JOptionPane.showMessageDialog(MainFrame.this, "Feature not supported yet!", "Feature not supported.", JOptionPane.ERROR_MESSAGE));
			settingsButton.setFont(Fonts.GLOBAL);
			settingsButton.setToolTipText("Settings");
			settingsButton.setDoubleBuffered(true);
			settingsButton.setFocusable(false);
		}
		return settingsButton;
	}
	
}