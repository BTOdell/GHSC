package com.ghsc.gui;

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

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.regex.Pattern;

/**
 * The main GUI interface.
 */
public class MainFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	/*
	 * Custom variables
	 */
	private SpamControl spamControl;

	private final EventListener<String> sendMessageListener = (final String text) -> {
        final Chat chat = this.chatContainer.getSelectedChat();
        if (chat != null) {
            final String trimmedText = text.trim();
			this.chatTextInput.setText("");
            if (!trimmedText.isEmpty()) {
                String currChat = chat.getName();
                if (chat instanceof Channel) {
                    Channel chan = (Channel) chat;
                    if (!Application.getInstance().getAdminControl().isAdmin() && chan.getUserCount() > 0) {
                        switch (this.spamControl.filter(trimmedText, currChat)) {
                            case 1:
                                //JOptionPane.showMessageDialog(MainFrame.this, "GHSC has detected you spamming " + currChannel + ".\nIf you continue, you will be temporarily banned from the channel.", "Spam warning!", JOptionPane.WARNING_MESSAGE);
                                chan.addElement(new ChannelElement(chan.getElements(), TimeStamp.newInstance(), chan.getName(), "Please stop spamming or I'll have to ban you!", null, Colors.MESSAGE_ORANGE), true);
                                return;
                            case 2:
                                StringBuilder build2 = new StringBuilder();
                                build2.append("You've been banned because you continued spamming.");
                                SpamBan sb2 = this.spamControl.getChannelBan(currChat);
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
                                SpamBan sb3 = this.spamControl.getChannelBan(currChat);
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
					this.getUsers().send(MessageEvent.construct(MessageEvent.Type.MESSAGE, User.ATT_CHANNEL, currChat, trimmedText), currChat);
                    chan.addElement(new ChannelElement(chan.getElements(), TimeStamp.newInstance(), null, trimmedText), true);
                } else {
                    // TODO: not a Channel, handle PMs?
                }
            }
        }
    };
	
	private InputWizard joinChannelWizard;

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
    MainFrame(final Application application) {
        this.spamControl = new SpamControl();

		application.getNickEventProvider().subscribe((final String nick) -> {
			this.setNick(nick);
			this.getUsers().send(MessageEvent.construct(MessageEvent.Type.IDENTIFY, User.ATT_HOSTNAME, application.getHostname(), User.ATT_NICK, application.getPreferredName()), User.ALL);
		});

		Profile.getProfile().addHook(() -> new Node(Tag.construct("bannedchannels"), this.spamControl.printBanned()));
		final Node bannedChannelsNode = Profile.getProfile().search("/bannedchannels");
		if (bannedChannelsNode != null) {
			final String bannedChannelString = bannedChannelsNode.getData();
			if (bannedChannelString != null) {
				final String[] bannedChannels = bannedChannelString.split(Pattern.quote(","));
				this.spamControl.load(bannedChannels);
			}
		}

		this.initComponents();
	}
	
	/*
	 * Custom functions
	 */
	
	/**
	 * Changes the current status of the GUI.
	 */
	public void setStatus(final String status) {
		this.setStatus(status, 0);
	}
	
	/**
	 * Changes the current status of the GUI, for a given amount of time.</br>
	 * The status will default back to "Idle" after the given amount of time.
	 */
	public void setStatus(final String status, final int period) {
		this.statusLabel.submit(status, period);
	}
	
	/**
	 * Changes the current nick label to given name.
	 * @param nick - name to show as nick.
	 */
	public void setNick(final String nick) {
		this.nickLabel.setText(nick);
	}
	
	/**
	 * @return the spam controller for this frame.
	 */
	public SpamControl getSpamControl() {
		return this.spamControl;
	}
	
	/**
	 * Toggles whether the user can use the chat controls.
	 */
	public void toggleInput(final boolean enabled) {
		this.chatTextInput.setEnabled(enabled);
		this.sendMessageButton.setEnabled(enabled);
	}
	
	/*
	 * End of custom functions
	 */
	
	/**
	 * Constructs all the components of the main frame.
	 */
	private void initComponents() {
		if (TrayManager.isSupported()) {
			this.addWindowListener(new WindowAdapter() {
				public void windowClosing(final WindowEvent e) {
					MainFrame.this.setVisible(false);
					final TrayManager trayManager = Application.getInstance().getTrayManager();
					if (trayManager != null) {
						trayManager.onFrameClosed();
					}
				}
			});
		}
		this.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD));
		this.setMinimumSize(new Dimension(700, 450));
		this.setTitle("GHSC");
		this.setIconImage(Images.ICON_32);
		this.setDefaultCloseOperation(TrayManager.isSupported() ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.EXIT_ON_CLOSE);
		this.setSize(700, 450);
		this.setLocationRelativeTo(null);

        final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.setContentPane(contentPane);
		final GroupLayout gl_canvas = new GroupLayout(contentPane);
		gl_canvas.setHorizontalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(this.getUserPanel(), GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getChatPanel(), GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
					.addContainerGap())
		);
		gl_canvas.setVerticalGroup(
			gl_canvas.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_canvas.createParallelGroup(Alignment.TRAILING)
						.addComponent(this.getChatPanel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
						.addComponent(this.getUserPanel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE))
					.addContainerGap())
		);
		contentPane.setLayout(gl_canvas);
	}
	
	public JPanel getUserPanel() {
		if (this.userPanel == null) {
			this.userPanel = new JPanel();
			this.userPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
			final GroupLayout gl_userPanel = new GroupLayout(this.userPanel);
			gl_userPanel.setHorizontalGroup(
				gl_userPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_userPanel.createSequentialGroup()
						.addGap(0)
						.addGroup(gl_userPanel.createParallelGroup(Alignment.TRAILING)
							.addComponent(this.getStatusLabel(), GroupLayout.PREFERRED_SIZE, 250, GroupLayout.PREFERRED_SIZE)
							.addGroup(gl_userPanel.createSequentialGroup()
								.addComponent(this.getMainImageLabel())
								.addGap(10)
								.addComponent(this.getNickLabel(), GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE))))
					.addComponent(this.getUserScrollPane(), GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
					.addComponent(this.getUserToolBar(), GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
			);
			gl_userPanel.setVerticalGroup(
				gl_userPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_userPanel.createSequentialGroup()
						.addGroup(gl_userPanel.createParallelGroup(Alignment.LEADING)
							.addComponent(this.getMainImageLabel())
							.addGroup(gl_userPanel.createSequentialGroup()
								.addContainerGap()
								.addComponent(this.getNickLabel())))
						.addGap(7)
						.addComponent(this.getStatusLabel(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(this.getUserScrollPane(), GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(this.getUserToolBar(), GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE))
			);
			this.userPanel.setLayout(gl_userPanel);
		}
		return this.userPanel;
	}
	
	public StatusLabel getStatusLabel() {
		if (this.statusLabel == null) {
			this.statusLabel = new StatusLabel();
			this.statusLabel.setStatus("Starting up...");
			this.statusLabel.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD, 14));
		}
		return this.statusLabel;
	}
	
	public JScrollPane getUserScrollPane() {
		if (this.userScrollPane == null) {
			this.userScrollPane = new JScrollPane();
			this.userScrollPane.setViewportView(this.getUsers());
		}
		return this.userScrollPane;
	}
	
	public UserContainer getUsers() {
		if (this.userList == null) {
			Profile.getProfile().addHook(() -> new Node(Tag.construct("friends"), this.userList.printFriends()));
			Profile.getProfile().addHook(() -> new Node(Tag.construct("ignored"), this.userList.printIgnored()));
			final Node friendsNode = Profile.getProfile().search("/friends");
			final String[] friends = friendsNode != null && friendsNode.getData() != null ? friendsNode.getData().split(Pattern.quote(",")) : new String[0];
			final Node ignoredNode = Profile.getProfile().search("/ignored");
			final String[] ignored = ignoredNode != null && ignoredNode.getData() != null ? ignoredNode.getData().split(Pattern.quote(",")) : new String[0];
			this.userList = new UserContainer(this, friends, ignored);
			final Application application = Application.getInstance();
			application.getPopupManager().submit((menu, popupManager, sender, x, y) -> {
                if (!this.userList.isEnabled()) {
                    return false;
                }
                final int index = this.userList.locationToIndex(new Point(x, y));
                if (index >= 0) {
					this.userList.setSelectedIndex(index);
                    final User u = this.userList.getSelectedValue();
                    if (u != null) {
                        final JMenuItem fi = menu.createItem(u.isFriend() ? "Unmark friend" : "Mark as friend", e -> u.setFriend(!u.isFriend()));
                        fi.setFont(Fonts.GLOBAL);
                        menu.add(fi);

                        final JMenuItem ii = menu.createItem(u.isIgnored() ? "Show messages" : "Ignore messages", e -> u.setIgnored(!u.isIgnored()));
                        ii.setFont(Fonts.GLOBAL);
                        menu.add(ii);

                        if (application.getAdminControl().isAdmin()) {
                            menu.addSeparator();

                            final JMenu adminMenu = menu.createMenu("Admin");
                            {
                                // TODO: make this dynamic!
                                final JMenuItem kI = menu.createItem("Kick", e -> u.send(AdminCommand.composeCommand(KickCommand.TAG, KickCommand.ATT_CHANNEL, this.chatContainer.getSelectedChat().getName())));
                                kI.setFont(Fonts.GLOBAL);
                                adminMenu.add(kI);

                                final boolean flashStatus = Utilities.resolveToBoolean(u.getCommandState(FlashCommand.TAG));
                                final JMenuItem flI = menu.createItem(flashStatus ? "Disable flashing" : "Enable flashing", e -> u.send(AdminCommand.composeCommand(FlashCommand.TAG, FlashCommand.ATT_ENABLE, Utilities.resolveToString(!flashStatus))));
                                flI.setFont(Fonts.GLOBAL);
                                adminMenu.add(flI);
                            }
                            adminMenu.setFont(Fonts.GLOBAL);
                            menu.add(adminMenu);
                        }

                        menu.addSeparator();

                        final JMenuItem ci = menu.createItem("Cancel", e -> menu.setVisible(false));
                        ci.setFont(Fonts.GLOBAL);
                        menu.add(ci);
                        return true;
                    }
                }
                return false;
            }, this.userList);
		}
		return this.userList;
	}
	
	public JLabel getMainImageLabel() {
		if (this.mainImageLabel == null) {
			this.mainImageLabel = new JLabel();
			this.mainImageLabel.setIcon(new ImageIcon(Images.ICON_LARGE));
		}
		return this.mainImageLabel;
	}
	
	public JLabel getNickLabel() {
		if (this.nickLabel == null) {
			this.nickLabel = new JLabel("...");
			this.nickLabel.setToolTipText("Double click to change nick.");
			this.nickLabel.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD, 18));
			this.nickLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(final MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
						Application.getInstance().showNickWizard();
					}
				}
			});
			Application.getInstance().getPopupManager().submit((menu, popupManager, sender, x, y) -> {
                final JMenuItem cdn = menu.createItem("Change nick", e -> Application.getInstance().showNickWizard());
                cdn.setFont(Fonts.GLOBAL);
                menu.add(cdn);
                return true;
            }, this.nickLabel);
		}
		return this.nickLabel;
	}
	
	public JPanel getChatPanel() {
		if (this.chatPanel == null) {
			this.chatPanel = new JPanel();
			final GroupLayout gl_chatPanel = new GroupLayout(this.chatPanel);
			gl_chatPanel.setHorizontalGroup(
				gl_chatPanel.createParallelGroup(Alignment.TRAILING)
					.addComponent(this.getChatContainer(), GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)
					.addGroup(gl_chatPanel.createSequentialGroup()
						.addComponent(this.getChatTextInputScroll(), GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(this.getSendMessageButton(), GroupLayout.PREFERRED_SIZE, 102, GroupLayout.PREFERRED_SIZE))
			);
			gl_chatPanel.setVerticalGroup(
				gl_chatPanel.createParallelGroup(Alignment.TRAILING)
					.addGroup(gl_chatPanel.createSequentialGroup()
						.addComponent(this.getChatContainer(), GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addGroup(gl_chatPanel.createParallelGroup(Alignment.LEADING, false)
							.addComponent(this.getChatTextInputScroll(), 23, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(this.getSendMessageButton(), Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 24, GroupLayout.PREFERRED_SIZE)))
			);
			this.chatPanel.setLayout(gl_chatPanel);
		}
		return this.chatPanel;
	}
	
	public ChatContainer getChatContainer() {
		if (this.chatContainer == null) {
			this.chatContainer = new ChatContainer(this);
		}
		return this.chatContainer;
	}
	
	public JScrollPane getChatTextInputScroll() {
		if (this.chatTextInputScroll == null) {
			this.chatTextInputScroll = new JScrollPane();
			this.chatTextInputScroll.setDoubleBuffered(true);
			this.chatTextInputScroll.setBorder(UIManager.getBorder("TextField.border"));
			this.chatTextInputScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			this.chatTextInputScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			this.chatTextInputScroll.setViewportView(this.getChatTextInput());
		}
		return this.chatTextInputScroll;
	}
	
	public ChatInput getChatTextInput() {
		if (this.chatTextInput == null) {
			this.chatTextInput = new ChatInput(this);
			this.chatTextInput.getEventProvider().subscribe(this.sendMessageListener);
		}
		return this.chatTextInput;
	}
	
	public JButton getSendMessageButton() {
		if (this.sendMessageButton == null) {
			this.sendMessageButton = new JButton("Send message");
			this.sendMessageButton.setEnabled(false);
			this.sendMessageButton.addActionListener(e -> this.chatTextInput.sendMessage());
		}
		return this.sendMessageButton;
	}
	
	public JToolBar getUserToolBar() {
		if (this.userToolBar == null) {
			this.userToolBar = new JToolBar();
			this.userToolBar.setFloatable(false);
			this.userToolBar.setDoubleBuffered(true);
			this.userToolBar.add(Box.createHorizontalGlue());
			this.userToolBar.add(this.getJoinButton());
			this.userToolBar.add(this.getFileTransferButton());
			this.userToolBar.add(this.getAdminButton());
			this.userToolBar.add(this.getSettingsButton());
			this.userToolBar.add(Box.createHorizontalGlue());
		}
		return this.userToolBar;
	}
	
	public JButton getJoinButton() {
		if (this.joinChannelButton == null) {
			this.joinChannelButton = new JButton();
			this.joinChannelButton.setIconTextGap(0);
			this.joinChannelButton.setHorizontalTextPosition(SwingConstants.CENTER);
			this.joinChannelButton.setIcon(new ImageIcon(Images.ADD_DARK));
			this.joinChannelButton.addActionListener(arg0 -> {
                if (this.joinChannelWizard != null && this.joinChannelWizard.isVisible()) {
                    return;
                }
				this.joinChannelWizard = new InputWizard(this, "Join a channel.", "Channel name", null, "Join", "Creates and joins a new Channel!",
                        input -> {
                            if (input != null) {
                                input = input.trim();
                                final Channel chan = new Channel(this.chatContainer, "#" + input);
								this.chatContainer.add(chan);
								this.chatContainer.setSelectedComponent(chan.getPanel());
                            } else {
                                System.out.println("Channel wizard cancelled.");
                            }
                        }, text -> {
                            text = text.trim();
                            if (text.isEmpty()) {
                                return new ValidationResult<>("Well, you actually have to type something...", false);
                            }
                            if (this.chatContainer.getChat("#" + text) != null) {
                                return new ValidationResult<>("You're already in this channel.", false);
                            }
                            for (final char c : text.toCharArray()) {
                                if (Character.isDigit(c) || Character.isLetter(c) || c == ' ') {
                                    continue;
                                }
                                return new ValidationResult<>("Only allowed letters and numbers!", false);
                            }
                            return new ValidationResult<>("Current name is acceptable.", true);
                        });
				this.joinChannelWizard.setVisible(true);
            });
			this.joinChannelButton.setFont(Fonts.GLOBAL);
			this.joinChannelButton.setToolTipText("Join a channel");
			this.joinChannelButton.setDoubleBuffered(true);
			this.joinChannelButton.setFocusable(false);
		}
		return this.joinChannelButton;
	}
	
	public JButton getFileTransferButton() {
		if (this.fileTransferButton == null) {
			this.fileTransferButton = new JButton();
			this.fileTransferButton.setHorizontalTextPosition(SwingConstants.CENTER);
			this.fileTransferButton.setIconTextGap(0);
			this.fileTransferButton.setIcon(new ImageIcon(Images.PAGE_GO));
			this.fileTransferButton.addActionListener(unused -> {
                if (Debug.NONE.compareTo(Application.DEBUG) < 0) {
                    final FileShare ft = Application.getInstance().getFileShare();
                    if (ft != null) {
                        ft.setVisible(true);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Feature not supported yet!", "Feature not supported.", JOptionPane.ERROR_MESSAGE);
                }
            });
			this.fileTransferButton.setFont(Fonts.GLOBAL);
			this.fileTransferButton.setToolTipText("File sharing");
			this.fileTransferButton.setDoubleBuffered(true);
			this.fileTransferButton.setFocusable(false);
		}
		return this.fileTransferButton;
	}
	
	public JToggleButton getAdminButton() {
		if (this.adminButton == null) {
			this.adminButton = new JToggleButton();
			this.adminButton.setHorizontalTextPosition(SwingConstants.CENTER);
			this.adminButton.setIconTextGap(0);
			this.adminButton.setIcon(new ImageIcon(Images.KEY));
			this.adminButton.addActionListener(unused -> {
                final Application application = Application.getInstance();
                if (application.getAdminControl().isLoginVisible()) {
					this.adminButton.setSelected(!this.adminButton.isSelected());
                    return;
                }
                if (!this.adminButton.isSelected()) { // logout
                    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to logout of admin controls?", "Logout?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        application.getAdminControl().setAdmin(false);
						this.adminButton.setToolTipText("Login as admin");
                    } else {
						this.adminButton.setSelected(true);
                    }
                } else { // login
                    if (application.getAdminControl().isReady()) {
                        application.getAdminControl().showLogin();
                    } else {
						this.adminButton.setSelected(false);
                        JOptionPane.showMessageDialog(application.getMainFrame(), "Due to network complications, passwords entered can't be validated.", "Admin control is currently unavailable.", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
			this.adminButton.setFont(Fonts.GLOBAL);
			this.adminButton.setToolTipText("Login as admin");
			this.adminButton.setDoubleBuffered(true);
			this.adminButton.setFocusable(false);
		}
		return this.adminButton;
	}
	
	public JButton getSettingsButton() {
		if (this.settingsButton == null) {
			this.settingsButton = new JButton();
			this.settingsButton.setHorizontalTextPosition(SwingConstants.CENTER);
			this.settingsButton.setIconTextGap(0);
			this.settingsButton.setIcon(new ImageIcon(Images.COG));
			this.settingsButton.addActionListener(unused -> JOptionPane.showMessageDialog(this, "Feature not supported yet!", "Feature not supported.", JOptionPane.ERROR_MESSAGE));
			this.settingsButton.setFont(Fonts.GLOBAL);
			this.settingsButton.setToolTipText("Settings");
			this.settingsButton.setDoubleBuffered(true);
			this.settingsButton.setFocusable(false);
		}
		return this.settingsButton;
	}
	
}