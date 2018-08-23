package ghsc.gui.fileshare.components;

import ghsc.common.Fonts;
import ghsc.common.Images;
import ghsc.gui.Application;
import ghsc.gui.MainFrame;
import ghsc.gui.components.autocomplete.ObjectToStringConverter;
import ghsc.gui.components.input.WizardListener;
import ghsc.gui.components.users.User;
import ghsc.gui.components.util.PromptHandler;
import ghsc.gui.fileshare.FileShareFrame;
import ghsc.gui.fileshare.internal.FilePackage.Visibility;
import ghsc.gui.fileshare.internal.LocalFileNode;
import ghsc.gui.fileshare.internal.LocalPackage;
import ghsc.impl.Identifiable;
import ghsc.impl.ObjectConverter;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * GUI for creating new packages.
 */
public class PackageWizard extends JDialog {

	private static final long serialVersionUID = 1L;

	private LocalPackage lPackage;
	private FileShareFileChooser fileChooser;
	private SwingWorker<LocalPackage, Object> packageWorker;
	private final WizardListener<LocalPackage> wizardListener;
	private List<String> visibilityChannelData = new ArrayList<>();
	private List<Identifiable> visibilityUserData = new ArrayList<>();
	private VisibilityDialog visibilityDialog;
	private final Font largerFont;

    private JLabel nameLabel;
	private JTextField nameField;
	private JToolBar filesToolbar;
	private JButton addFilesButton;
	private JButton deleteFilesButton;
	private JScrollPane fileScrollPane;
	private JTree fileTree;
	private FileNodeTreeModel<LocalFileNode> fileTreeModel;
	private JPanel visibilityPanel;
	private JComboBox<Visibility.Type> visibilityComboBox;
	private JButton visibilityEditButton;
	private JPanel passwordPanel;
	private JCheckBox passwordProtect;
	private JTextField passwordField;
	private JScrollPane descriptionScrollPane;
	private JTextPane descriptionPane;
	private JPanel controlsPanel;
	private JButton okButton;
	private JButton cancelButton;

	/**
	 * Creates a new PackageWizard GUI.
	 */
	public PackageWizard(final FileShareFrame frame, final WizardListener<LocalPackage> wizardListener) {
		this(frame, null, wizardListener);
	}

	PackageWizard(final FileShareFrame frame, final LocalPackage pack, final WizardListener<LocalPackage> wizardListener) {
		super(frame);
		this.largerFont = Fonts.GLOBAL.deriveFont(Fonts.GLOBAL.getSize() + 1.0F);
		this.lPackage = pack;
		this.wizardListener = wizardListener;

        this.initComponents();
	}

	private void toggleControls(final boolean enabled) {
        this.nameField.setEnabled(enabled);
        this.addFilesButton.setEnabled(enabled);
        this.deleteFilesButton.setEnabled(enabled);
        this.fileTree.setEnabled(enabled);
        this.visibilityComboBox.setEnabled(enabled);
        this.visibilityEditButton.setEnabled(enabled);
        this.passwordProtect.setEnabled(enabled);
        this.passwordField.setEnabled(enabled);
        this.descriptionPane.setEnabled(enabled);
        this.okButton.setEnabled(enabled);
        this.cancelButton.setEnabled(enabled);
	}

	private void setCursor(final int cursor) {
		final Cursor cur = Cursor.getPredefinedCursor(cursor);
		for (final Component comp : this.getComponents()) {
			if (comp != null) {
                comp.setCursor(cur);
            }
		}
        this.setCursor(cur);
	}

	private void deleteSelectedNodes() {
		final TreePath[] tps = this.fileTree.getSelectionPaths();
        if (tps != null) {
            for (final TreePath tp : tps) {
                final Object o = tp.getLastPathComponent();
                if (o instanceof LocalFileNode) {
                    final LocalFileNode node = (LocalFileNode) o;
                    this.fileTreeModel.removeNodeFromParent(node);
                }
            }
        }
    }

	/**
	 * Notifies the wizard listener with a 'null' value and disposes of this dialog window.
	 */
	private void close() {
        this.wizardListener.wizardFinished(null);
        this.dispose();
	}

	private void initComponents() {
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.addWindowListener(new WindowAdapter() {
			public void windowClosing(final WindowEvent we) {
                PackageWizard.this.close();
			}
		});
        this.setIconImage(Images.PACKAGE_ADD);
        this.setTitle("Package wizard");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setSize(400, 500);
        this.setMinimumSize(this.getSize());
        this.setLocationRelativeTo(null);

        final JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.setContentPane(contentPane);
		final GroupLayout gl_canvas = new GroupLayout(contentPane);
		gl_canvas.setHorizontalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addComponent(this.getControlsPanel(), GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(this.getNameLabel())
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(this.getNameField(), GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
					.addContainerGap())
				.addComponent(this.getPasswordPanel(), Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(this.getFileScrollPane(), GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
					.addContainerGap())
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(this.getFilesToolbar(), GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
					.addContainerGap())
				.addComponent(this.getVisibilityPanel(), GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(this.getDescriptionScrollPane(), GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
					.addContainerGap())
		);
		gl_canvas.setVerticalGroup(
			gl_canvas.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_canvas.createParallelGroup(Alignment.BASELINE)
						.addComponent(this.getNameLabel())
						.addComponent(this.getNameField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getFilesToolbar(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(this.getFileScrollPane(), GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getVisibilityPanel(), GroupLayout.PREFERRED_SIZE, 52, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getPasswordPanel(), GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getDescriptionScrollPane(), GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(this.getControlsPanel(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
        contentPane.setLayout(gl_canvas);
	}

	public JLabel getNameLabel() {
		if (this.nameLabel == null) {
            this.nameLabel = new JLabel("Name:");
            this.nameLabel.setFont(this.largerFont);
		}
		return this.nameLabel;
	}

	public JTextField getNameField() {
		if (this.nameField == null) {
            this.nameField = new JTextField();
            this.nameField.setColumns(10);
            this.nameField.setFont(Fonts.GLOBAL);
			if (this.lPackage != null) {
                this.nameField.setText(this.lPackage.getName());
			}
		}
		return this.nameField;
	}

	public JToolBar getFilesToolbar() {
		if (this.filesToolbar == null) {
            this.filesToolbar = new JToolBar();
            this.filesToolbar.setFloatable(false);
            this.filesToolbar.add(this.getAddFilesButton());
            this.filesToolbar.add(this.getDeleteFilesButton());
		}
		return this.filesToolbar;
	}

	public JButton getAddFilesButton() {
		if (this.addFilesButton == null) {
            this.addFilesButton = new JButton("Add files");
            this.addFilesButton.addActionListener(unused -> {
                if (this.fileChooser == null) {
                    SwingUtilities.invokeLater(() -> {
                        this.fileChooser = new FileShareFileChooser(Application.LAST_DIRECTORY);
                        try {
                            final int result = this.fileChooser.showDialog(this, null);
                            if (result == FileShareFileChooser.APPROVE_OPTION) {
                                final File[] selected = this.fileChooser.getSelectedFiles();
                                for (final File select : selected) {
                                    final LocalFileNode root = LocalFileNode.generateRoot(select);
                                    if (root == null) {
                                        continue;
                                    }
                                    this.fileTreeModel.addRoot(root);
                                }
                            }
                        } finally {
                            Application.LAST_DIRECTORY = this.fileChooser.getCurrentDirectory();
                            this.fileChooser = null;
                        }
                    });
                }
            });
            this.addFilesButton.setFocusable(false);
            this.addFilesButton.setFont(Fonts.GLOBAL);
            this.addFilesButton.setIcon(new ImageIcon(Images.PAGE_ADD));
		}
		return this.addFilesButton;
	}

	public JButton getDeleteFilesButton() {
		if (this.deleteFilesButton == null) {
            this.deleteFilesButton = new JButton("Delete");
            this.deleteFilesButton.addActionListener(ae -> this.deleteSelectedNodes());
            this.deleteFilesButton.setFocusable(false);
            this.deleteFilesButton.setFont(Fonts.GLOBAL);
            this.deleteFilesButton.setIcon(new ImageIcon(Images.PAGE_DELETE));
		}
		return this.deleteFilesButton;
	}

	public JScrollPane getFileScrollPane() {
		if (this.fileScrollPane == null) {
            this.fileScrollPane = new JScrollPane();
            this.fileScrollPane.setViewportView(this.getFileTree());
		}
		return this.fileScrollPane;
	}

	public JTree getFileTree() {
		if (this.fileTree == null) {
            this.fileTree = new JTree();
			this.fileTreeModel = new FileNodeTreeModel<>(this.fileTree);
			if (this.lPackage != null) {
				for (final LocalFileNode n : this.lPackage.getRoots()) {
					this.fileTreeModel.addRoot(n.clone());
				}
			}
            this.fileTree.setModel(this.fileTreeModel);
            this.fileTree.setShowsRootHandles(true);
            this.fileTree.setRootVisible(false);
            this.fileTree.setDoubleBuffered(true);
		}
		return this.fileTree;
	}

	public JPanel getVisibilityPanel() {
		if (this.visibilityPanel == null) {
            this.visibilityPanel = new JPanel();
            this.visibilityPanel.setBorder(new TitledBorder(null, "Visibility", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			final GroupLayout gl_visibilityPanel = new GroupLayout(this.visibilityPanel);
			gl_visibilityPanel.setHorizontalGroup(
				gl_visibilityPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_visibilityPanel.createSequentialGroup()
						.addGap(5)
						.addComponent(this.getVisibilityComboBox(), GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(this.getVisibilityEditButton(), GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(164, Short.MAX_VALUE))
			);
			gl_visibilityPanel.setVerticalGroup(
				gl_visibilityPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_visibilityPanel.createSequentialGroup()
						.addGap(3)
						.addGroup(gl_visibilityPanel.createParallelGroup(Alignment.BASELINE)
							.addComponent(this.getVisibilityComboBox(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(this.getVisibilityEditButton(), GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE))
						.addContainerGap())
			);
            this.visibilityPanel.setLayout(gl_visibilityPanel);
		}
		return this.visibilityPanel;
	}

	public JComboBox<Visibility.Type> getVisibilityComboBox() {
		if (this.visibilityComboBox == null) {
            this.visibilityComboBox = new JComboBox<>();
            this.visibilityComboBox.setFont(Fonts.GLOBAL);
            this.visibilityComboBox.addItemListener(ie -> {
                final Visibility.Type vType = (Visibility.Type) this.visibilityComboBox.getSelectedItem();
                if (vType == null) {
                    throw new IllegalStateException("Visibility type is null.");
                }
                switch (vType) {
                    case CHANNEL:
                    case USER:
                        this.getVisibilityEditButton().setVisible(true);
                        break;
                    default:
                        this.getVisibilityEditButton().setVisible(false);
                }
                this.getVisibilityPanel().revalidate();
            });
            this.visibilityComboBox.setFocusable(false);
            this.visibilityComboBox.setModel(new DefaultComboBoxModel<>(Visibility.Type.values()));
            if (this.lPackage != null) {
                this.visibilityComboBox.setSelectedItem(this.lPackage.getVisibility().getType());
            }
        }
		return this.visibilityComboBox;
	}

	public JButton getVisibilityEditButton() {
		if (this.visibilityEditButton == null) {
            this.visibilityEditButton = new JButton("...");
            this.visibilityEditButton.setVisible(false);
            this.visibilityEditButton.addActionListener(e -> {
                if (this.visibilityDialog == null) {
                    final MainFrame mainFrame = Application.getInstance().getMainFrame();
                    final Visibility.Type vType = (Visibility.Type) this.visibilityComboBox.getSelectedItem();
                    if (vType == null) {
                        throw new IllegalStateException("Visibility type is null.");
                    }
                    switch (vType) {
                        case CHANNEL:
                            this.visibilityDialog = new VisibilityDialog<>(
                                    this,
                                    Arrays.asList(mainFrame.getChatContainer().getAllAsStrings()),
                                    this.visibilityChannelData,
                                    event -> {
                                        if (event != null) {
                                            this.visibilityChannelData = event;
                                        }
                                        this.visibilityDialog = null;
                                    }, str -> {
                                        if (str == null) {
                                            return null;
                                        }
                                        str = str.trim();
                                        return str.isEmpty() || str.startsWith("#") ? str : "#" + str;
                                    }, new ObjectToStringConverter() {
                                        public String getPreferredStringForItem(final Object item) {
                                            if (item == null) {
                                                return null;
                                            }
                                            String str = item.toString();
                                            if (str == null) {
                                                return null;
                                            }
                                            str = str.trim();
                                            return str.isEmpty() || str.startsWith("#") ? str : "#" + str;
                                        }
                                        public String[] getPossibleStringsForItem(final Object item) {
                                            if (item == null) {
                                                return new String[0];
                                            }
                                            String str = item.toString();
                                            if (str == null) {
                                                return new String[0];
                                            }
                                            str = str.trim();
                                            if (str.isEmpty()) {
                                                return new String[0];
                                            }
                                            return new String[]{str, str.startsWith("#") ?
                                                    str.substring(1) : "#" + str};
                                        }
                                    }, false);
                            break;
                        case USER:
                            final Function<Identifiable, String> converter = obj -> {
                                if (obj == null) {
                                    return null;
                                }
                                return obj.getNick();
                            };
                            this.visibilityDialog = new VisibilityDialog<>(this,
                                    ObjectConverter.wrap(converter, new ArrayList<>(mainFrame.getUsers().getUserCollection())),
                                    ObjectConverter.wrap(converter, this.visibilityUserData),
                                    event -> {
                                        if (event != null) {
                                            this.visibilityUserData = ObjectConverter.unwrap(event);
                                        }
                                        this.visibilityDialog = null;
                                    }, UnaryOperator.identity(), null, true);
                            break;
                        default:
                            break;
                    }
                    if (this.visibilityDialog != null) {
                        this.visibilityDialog.setVisible(true);
                    }
                }
            });
            this.visibilityEditButton.setToolTipText("Edit visibility arguments");
			if (this.lPackage != null) {
				// load visibility arguments from package
				final Object data = this.lPackage.getVisibility().getData();
				if (data != null) {
					final String[] split = data.toString().split(Pattern.quote(","));
					for (final String s : split) {
						switch (this.lPackage.getVisibility().getType()) {
							case CHANNEL:
                                this.visibilityChannelData.add(s);
								break;
							case USER:
								final int index = s.indexOf('|');
								if (index < 0) {
                                    break;
                                }
								final String[] vD = { s.substring(0, index), s.substring(index + 1) };
								final UUID uuid = UUID.fromString(vD[1]);
								final User user = Application.getInstance().getMainFrame().getUsers().findUser(uuid);
								if (user != null) {
									/*
									 * find some way to get the visibility data to auto-update when users change their name...
									user.subscribe(new EventListener<User>() {
										public void eventReceived(User event) {
											if (visibilityDialog != null) {
												visibilityDialog.getEntryBox().invalidate();
												visibilityDialog.getEntryBox().validate();
												visibilityDialog.getList().invalidate();
												visibilityDialog.getList().validate();
											}
										}
									});
									*/
                                    this.visibilityUserData.add(user);
								} else {
									final Identifiable i = new Identifiable() {
										public String getNick() {
											return vD[0];
										}
										public UUID getID() {
											return uuid;
										}
										@Override
										public boolean equals(final Object o) {
                                            return o instanceof Identifiable && (this == o || this.getID().equals(((Identifiable) o).getID()));
                                        }
									};
                                    this.visibilityUserData.add(i);
								}
								break;
							default: break;
						}
					}
				}
			}
		}
		return this.visibilityEditButton;
	}

	public JPanel getPasswordPanel() {
		if (this.passwordPanel == null) {
            this.passwordPanel = new JPanel();
            this.passwordPanel.setBorder(new TitledBorder(null, "Password protection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			final GroupLayout gl_passwordPanel = new GroupLayout(this.passwordPanel);
			gl_passwordPanel.setHorizontalGroup(
				gl_passwordPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_passwordPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(this.getPasswordProtect())
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(this.getPasswordField())
						.addContainerGap())
			);
			gl_passwordPanel.setVerticalGroup(
				gl_passwordPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_passwordPanel.createSequentialGroup()
						.addGroup(gl_passwordPanel.createParallelGroup(Alignment.BASELINE)
							.addComponent(this.getPasswordProtect())
							.addComponent(this.getPasswordField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
			);
            this.passwordPanel.setLayout(gl_passwordPanel);
		}
		return this.passwordPanel;
	}

	public JCheckBox getPasswordProtect() {
		if (this.passwordProtect == null) {
            this.passwordProtect = new JCheckBox("");
            this.passwordProtect.setFocusable(false);
            this.passwordProtect.addActionListener(e -> {
                this.getPasswordField().setVisible(this.passwordProtect.isSelected());
                this.getPasswordPanel().revalidate();
            });
            this.passwordProtect.setFont(Fonts.GLOBAL);
			if (this.lPackage != null) {
                this.passwordProtect.setSelected(this.lPackage.isPasswordProtected());
			}
		}
		return this.passwordProtect;
	}

	public JTextField getPasswordField() {
		if (this.passwordField == null) {
            this.passwordField = new JTextField();
            this.passwordField.setVisible(false);
            this.passwordField.setFont(Fonts.GLOBAL);
            this.passwordField.setColumns(10);
			if (this.lPackage != null && this.lPackage.isPasswordProtected()) {
                this.passwordField.setVisible(true);
                this.passwordField.setText(this.lPackage.getPassword());
			}
		}
		return this.passwordField;
	}

	public JScrollPane getDescriptionScrollPane() {
		if (this.descriptionScrollPane == null) {
            this.descriptionScrollPane = new JScrollPane();
            this.descriptionScrollPane.setDoubleBuffered(true);
            this.descriptionScrollPane.setViewportView(this.getDescriptionPane());
		}
		return this.descriptionScrollPane;
	}

	public JTextPane getDescriptionPane() {
		if (this.descriptionPane == null) {
            this.descriptionPane = new JTextPane();
            this.descriptionPane.setDoubleBuffered(true);
            this.descriptionPane.setFont(Fonts.GLOBAL);
			if (this.lPackage != null) {
                this.descriptionPane.setText(this.lPackage.getDescription());
			}
			new PromptHandler(this.descriptionPane, "Write a description here.", Color.GRAY, JLabel.TOP);
		}
		return this.descriptionPane;
	}

	public JPanel getControlsPanel() {
		if (this.controlsPanel == null) {
            this.controlsPanel = new JPanel();
            this.controlsPanel.setBorder(new MatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
			final GroupLayout gl_controlsPanel = new GroupLayout(this.controlsPanel);
			gl_controlsPanel.setHorizontalGroup(
				gl_controlsPanel.createParallelGroup(Alignment.TRAILING)
					.addGroup(gl_controlsPanel.createSequentialGroup()
						.addContainerGap(176, Short.MAX_VALUE)
						.addComponent(this.getOkButton(), GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(this.getCancelButton(), GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
						.addContainerGap())
			);
			gl_controlsPanel.setVerticalGroup(
				gl_controlsPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_controlsPanel.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_controlsPanel.createParallelGroup(Alignment.BASELINE)
							.addComponent(this.getCancelButton())
							.addComponent(this.getOkButton()))
						.addContainerGap())
			);
            this.controlsPanel.setLayout(gl_controlsPanel);
		}
		return this.controlsPanel;
	}

	public JButton getOkButton() {
		if (this.okButton == null) {
            this.okButton = new JButton("OK");
            this.okButton.setFocusable(false);
            this.okButton.setFont(Fonts.GLOBAL);
            this.okButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent ae) {
					if (PackageWizard.this.packageWorker == null) {
                        PackageWizard.this.toggleControls(false);
                        PackageWizard.this.setCursor(Cursor.WAIT_CURSOR);
						final LocalFileNode[] nodes = PackageWizard.this.fileTreeModel.getRoots(new LocalFileNode[PackageWizard.this.fileTreeModel.getRootCount()]);
                        PackageWizard.this.packageWorker = new SwingWorker<LocalPackage, Object>() {
							protected LocalPackage doInBackground() {
							    if (PackageWizard.this.lPackage == null) {
                                    // Create new package
                                    PackageWizard.this.lPackage = new LocalPackage(
                                            PackageWizard.this.getNameField().getText(),
                                            PackageWizard.this.getDescriptionPane().getText(),
                                            Calendar.getInstance(),
                                            null);
                                } else {
							        // Edit required package data
                                    PackageWizard.this.lPackage.setName(PackageWizard.this.getNameField().getText());
                                    PackageWizard.this.lPackage.setDescription(PackageWizard.this.getDescriptionPane().getText());
                                }
                                // Edit package data
                                PackageWizard.this.lPackage.setPassword(PackageWizard.this.getPasswordProtect().isSelected() && !PackageWizard.this.getPasswordField().getText().isEmpty() ? PackageWizard.this.getPasswordField().getText() : null);
                                PackageWizard.this.lPackage.setRoots(nodes);
                                // Configure visibility of local package
								final Visibility.Type vType = (Visibility.Type) PackageWizard.this.getVisibilityComboBox().getSelectedItem();
								if (vType == null) {
								    throw new IllegalStateException("Visibility type is null.");
                                }
                                final StringBuilder sb = vType != Visibility.Type.PUBLIC ? new StringBuilder() : null;
								switch (vType) {
									case CHANNEL:
										for (int i = 0; i < PackageWizard.this.visibilityChannelData.size(); i++) {
											sb.append(PackageWizard.this.visibilityChannelData.get(i));
											if (i + 1 < PackageWizard.this.visibilityChannelData.size()) {
												sb.append(",");
											}
										}
									case USER:
										for (int i = 0; i < PackageWizard.this.visibilityUserData.size(); i++) {
											final Identifiable u = PackageWizard.this.visibilityUserData.get(i);
											sb.append(u.getNick());
											sb.append("|");
											sb.append(u.getID().toString());
											if (i + 1 < PackageWizard.this.visibilityUserData.size()) {
												sb.append(",");
											}
										}
										break;
									case PRIVATE:
										sb.append(PackageWizard.this.lPackage.getPrivateKey());
										break;
									default:
									    break;
								}
                                PackageWizard.this.lPackage.setVisibility(new Visibility(vType, sb));
								return PackageWizard.this.lPackage;
							}
							protected void done() {
								super.done();
								if (this.isCancelled()) {
                                    PackageWizard.this.toggleControls(true);
                                    PackageWizard.this.setCursor(Cursor.DEFAULT_CURSOR);
								} else {
                                    PackageWizard.this.wizardListener.wizardFinished(PackageWizard.this.lPackage);
                                    PackageWizard.this.dispose();
								}
							}
						};
                        PackageWizard.this.packageWorker.execute();
					}
				}
			});
		}
		return this.okButton;
	}

	public JButton getCancelButton() {
		if (this.cancelButton == null) {
            this.cancelButton = new JButton("Cancel");
            this.cancelButton.setFocusable(false);
            this.cancelButton.setFont(Fonts.GLOBAL);
            this.cancelButton.addActionListener(ae -> this.close());
		}
		return this.cancelButton;
	}

}