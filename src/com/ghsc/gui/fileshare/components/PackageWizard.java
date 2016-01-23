package com.ghsc.gui.fileshare.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.TreePath;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.event.EventListener;
import com.ghsc.gui.Application;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.autocomplete.ObjectToStringConverter;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.users.User;
import com.ghsc.gui.components.util.PromptHandler;
import com.ghsc.gui.fileshare.FileShareFrame;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility;
import com.ghsc.gui.fileshare.internal.LocalFileNode;
import com.ghsc.gui.fileshare.internal.LocalPackage;
import com.ghsc.impl.IObjectConverter;
import com.ghsc.impl.Identifiable;
import com.ghsc.impl.InputVerifier;
import com.ghsc.impl.ObjectConverter;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class PackageWizard extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private LocalPackage lPackage = null;
	private FileShareFileChooser fileChooser = null;
	private SwingWorker<LocalPackage, Object> packageWorker = null;
	private WizardListener<LocalPackage> wizardListener;
	private List<String> visibilityChannelData = new ArrayList<String>();
	private List<Identifiable> visibilityUserData = new ArrayList<Identifiable>();
	@SuppressWarnings("rawtypes")
	private VisibilityDialog visibilityDialog = null;
	private Font largerFont;
	
	private JPanel canvas;
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
	 * @wbp.parser.constructor
	 */
	public PackageWizard(final FileShareFrame frame, final WizardListener<LocalPackage> wizardListener) {
		this(frame, null, wizardListener);
	}
	
	public PackageWizard(final FileShareFrame frame, final LocalPackage pack, final WizardListener<LocalPackage> wizardListener) {
		super(frame);
		this.largerFont = Fonts.GLOBAL.deriveFont(Fonts.GLOBAL.getSize() + 1.0F);
		this.lPackage = pack;
		this.wizardListener = wizardListener;
		
		initComponents();
	}
	
	private void toggleControls(boolean enabled) {
		nameField.setEnabled(enabled);
		addFilesButton.setEnabled(enabled);
		deleteFilesButton.setEnabled(enabled);
		fileTree.setEnabled(enabled);
		visibilityComboBox.setEnabled(enabled);
		visibilityEditButton.setEnabled(enabled);
		passwordProtect.setEnabled(enabled);
		passwordField.setEnabled(enabled);
		descriptionPane.setEnabled(enabled);
		okButton.setEnabled(enabled);
		cancelButton.setEnabled(enabled);
	}
	
	private void setCursor(int cursor) {
		final Cursor cur = Cursor.getPredefinedCursor(cursor);
		for (Component comp : getComponents()) {
			if (comp != null)
				comp.setCursor(cur);
		}
		setCursor(cur);
	}
	
	private void deleteSelectedNodes() {
		TreePath[] tps = fileTree.getSelectionPaths();
		for (int i = 0; i < tps.length; i++) {
			TreePath tp = tps[i];
			Object o = tp.getLastPathComponent();
			if (o != null && o instanceof LocalFileNode) {
				LocalFileNode node = (LocalFileNode) o;
				fileTreeModel.removeNodeFromParent(node);
			}
		}
	}
	
	/**
	 * Notifies the wizard listener with a 'null' value and disposes of this dialog window.
	 */
	private void close() {
		wizardListener.wizardFinished(null);
		dispose();
	}
	
	private void initComponents() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				close();
			}
		});
		setIconImage(Images.PACKAGE_ADD);
		setTitle("Package wizard");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setSize(400, 500);
		setMinimumSize(getSize());
		setLocationRelativeTo(null);
		
		canvas = new JPanel();
		canvas.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(canvas);
		GroupLayout gl_canvas = new GroupLayout(canvas);
		gl_canvas.setHorizontalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addComponent(getControlsPanel(), GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(getNameLabel())
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(getNameField(), GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
					.addContainerGap())
				.addComponent(getPasswordPanel(), Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(getFileScrollPane(), GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
					.addContainerGap())
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(getFilesToolbar(), GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
					.addContainerGap())
				.addComponent(getVisibilityPanel(), GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addComponent(getDescriptionScrollPane(), GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
					.addContainerGap())
		);
		gl_canvas.setVerticalGroup(
			gl_canvas.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_canvas.createParallelGroup(Alignment.BASELINE)
						.addComponent(getNameLabel())
						.addComponent(getNameField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getFilesToolbar(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(getFileScrollPane(), GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getVisibilityPanel(), GroupLayout.PREFERRED_SIZE, 52, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getPasswordPanel(), GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getDescriptionScrollPane(), GroupLayout.PREFERRED_SIZE, 94, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getControlsPanel(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		canvas.setLayout(gl_canvas);
	}
	
	public JLabel getNameLabel() {
		if (nameLabel == null) {
			nameLabel = new JLabel("Name:");
			nameLabel.setFont(largerFont);
		}
		return nameLabel;
	}
	
	public JTextField getNameField() {
		if (nameField == null) {
			nameField = new JTextField();
			nameField.setColumns(10);
			nameField.setFont(Fonts.GLOBAL);
			if (lPackage != null) {
				nameField.setText(lPackage.getName());
			}
		}
		return nameField;
	}
	
	public JToolBar getFilesToolbar() {
		if (filesToolbar == null) {
			filesToolbar = new JToolBar();
			filesToolbar.setFloatable(false);
			filesToolbar.add(getAddFilesButton());
			filesToolbar.add(getDeleteFilesButton());
		}
		return filesToolbar;
	}
	
	public JButton getAddFilesButton() {
		if (addFilesButton == null) {
			addFilesButton = new JButton("Add files");
			addFilesButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (fileChooser == null) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								fileChooser = new FileShareFileChooser(Application.LAST_DIRECTORY);
								try {
									int result = fileChooser.showDialog(PackageWizard.this, null);
									if (result == FileShareFileChooser.APPROVE_OPTION) {
										final File[] selected = fileChooser.getSelectedFiles();
										for (int i = 0; i < selected.length; i++) {
											final File select = selected[i];
											final LocalFileNode root = LocalFileNode.generateRoot(select);
											if (root == null)
												continue;
											fileTreeModel.addRoot(root);
										}
									}
								} finally {
									Application.LAST_DIRECTORY = fileChooser.getCurrentDirectory();
									fileChooser = null;
								}
							}
						});
					}
				}
			});
			addFilesButton.setFocusable(false);
			addFilesButton.setFont(Fonts.GLOBAL);
			addFilesButton.setIcon(new ImageIcon(Images.PAGE_ADD));
		}
		return addFilesButton;
	}
	
	public JButton getDeleteFilesButton() {
		if (deleteFilesButton == null) {
			deleteFilesButton = new JButton("Delete");
			deleteFilesButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					deleteSelectedNodes();
				}
			});
			deleteFilesButton.setFocusable(false);
			deleteFilesButton.setFont(Fonts.GLOBAL);
			deleteFilesButton.setIcon(new ImageIcon(Images.PAGE_DELETE));
		}
		return deleteFilesButton;
	}
	
	public JScrollPane getFileScrollPane() {
		if (fileScrollPane == null) {
			fileScrollPane = new JScrollPane();
			fileScrollPane.setViewportView(getFileTree());
		}
		return fileScrollPane;
	}
	
	public JTree getFileTree() {
		if (fileTree == null) {
			fileTree = new JTree();
			this.fileTreeModel = new FileNodeTreeModel<LocalFileNode>(fileTree);
			if (lPackage != null) {
				for (LocalFileNode n : lPackage.getRoots()) {
					this.fileTreeModel.addRoot(n.clone());
				}
			}
			fileTree.setModel(fileTreeModel);
			fileTree.setShowsRootHandles(true);
			fileTree.setRootVisible(false);
			fileTree.setDoubleBuffered(true);
		}
		return fileTree;
	}
	
	public JPanel getVisibilityPanel() {
		if (visibilityPanel == null) {
			visibilityPanel = new JPanel();
			visibilityPanel.setBorder(new TitledBorder(null, "Visibility", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			GroupLayout gl_visibilityPanel = new GroupLayout(visibilityPanel);
			gl_visibilityPanel.setHorizontalGroup(
				gl_visibilityPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_visibilityPanel.createSequentialGroup()
						.addGap(5)
						.addComponent(getVisibilityComboBox(), GroupLayout.PREFERRED_SIZE, 150, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(getVisibilityEditButton(), GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
						.addContainerGap(164, Short.MAX_VALUE))
			);
			gl_visibilityPanel.setVerticalGroup(
				gl_visibilityPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_visibilityPanel.createSequentialGroup()
						.addGap(3)
						.addGroup(gl_visibilityPanel.createParallelGroup(Alignment.BASELINE)
							.addComponent(getVisibilityComboBox(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addComponent(getVisibilityEditButton(), GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE))
						.addContainerGap())
			);
			visibilityPanel.setLayout(gl_visibilityPanel);
		}
		return visibilityPanel;
	}
	
	public JComboBox<Visibility.Type> getVisibilityComboBox() {
		if (visibilityComboBox == null) {
			visibilityComboBox = new JComboBox<Visibility.Type>();
			visibilityComboBox.setFont(Fonts.GLOBAL);
			visibilityComboBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					switch ((Visibility.Type) visibilityComboBox.getSelectedItem()) {
						case CHANNEL:
						case USER:
							getVisibilityEditButton().setVisible(true);
							break;
						default:
							getVisibilityEditButton().setVisible(false);
					}
					getVisibilityPanel().revalidate();
				}
			});
			visibilityComboBox.setFocusable(false);
			visibilityComboBox.setModel(new DefaultComboBoxModel<Visibility.Type>(Visibility.Type.values()));
			if (lPackage != null) {
				visibilityComboBox.setSelectedItem(lPackage.getVisibility().getType());
			}
		}
		return visibilityComboBox;
	}
	
	public JButton getVisibilityEditButton() {
		if (visibilityEditButton == null) {
			visibilityEditButton = new JButton("...");
			visibilityEditButton.setVisible(false);
			visibilityEditButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (visibilityDialog == null) {
						final MainFrame mainFrame = Application.getInstance().getMainFrame();
						switch ((Visibility.Type) visibilityComboBox.getSelectedItem()) {
							case CHANNEL:
								visibilityDialog = new VisibilityDialog<String>(PackageWizard.this, Arrays.asList(mainFrame.getChatContainer().getAllAsStrings()), visibilityChannelData, 
								new EventListener<ArrayList<String>>() {
									public void eventReceived(ArrayList<String> event) {
										if (event != null) {
											visibilityChannelData = event;
										}
										visibilityDialog = null;
									}
								}, new InputVerifier<String>() {
									public String verify(String str) {
										if (str == null)
											return null;
										str = str.trim();
										return str.isEmpty() || str.startsWith("#") ? str : "#" + str;
									}
								}, new ObjectToStringConverter() {
									public String getPreferredStringForItem(Object item) {
										if (item == null)
											return null;
										String str = item.toString();
										if (str == null)
											return null;
										str = str.trim();
										return str.isEmpty() || str.startsWith("#") ? str : "#" + str;
									}
									@Override
									public String[] getPossibleStringsForItem(Object item) {
										if (item == null)
											return new String[0];
										String str = item.toString();
										if (str == null)
											return new String[0];
										str = str.trim();
										if (str.isEmpty())
											return new String[0];
										return new String[] { str, str.startsWith("#") ? 
												str.substring(1, str.length()) : "#" + str };
									}
								}, false);
								break;
							case USER:
								final IObjectConverter<Identifiable> converter = new IObjectConverter<Identifiable>() {
									public String convert(Identifiable obj) {
										if (obj == null)
											return null;
										return obj.getNick();
									}
								};
								visibilityDialog = new VisibilityDialog<ObjectConverter<Identifiable>>(PackageWizard.this, 
										ObjectConverter.wrap(converter, new ArrayList<Identifiable>(mainFrame.getUsers().getUserCollection())), 
										ObjectConverter.wrap(converter, visibilityUserData), 
								new EventListener<ArrayList<ObjectConverter<Identifiable>>>() {
									public void eventReceived(ArrayList<ObjectConverter<Identifiable>> event) {
										if (event != null) {
											visibilityUserData = ObjectConverter.unwrap(event);
										}
										visibilityDialog = null;
									}
								}, new InputVerifier<ObjectConverter<Identifiable>>() {
									public ObjectConverter<Identifiable> verify(ObjectConverter<Identifiable> i) {
										return i;
									}
								}, null, true);
								break;
							default: break;
						}
						if (visibilityDialog != null) {
							visibilityDialog.setVisible(true);
						}
					}
				}
			});
			visibilityEditButton.setToolTipText("Edit visibility arguments");
			if (lPackage != null) {
				// load visibility arguments from package
				final Object data = lPackage.getVisibility().getData();
				if (data != null) {
					String[] split = data.toString().split(Pattern.quote(","));
					for (String s : split) {
						switch (lPackage.getVisibility().getType()) {
							case CHANNEL:
								visibilityChannelData.add(s);
								break;
							case USER:
								int index = s.indexOf('|');
								if (index < 0)
									break;
								final String[] vD = new String[] { s.substring(0, index), s.substring(index + 1, s.length()) };
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
									visibilityUserData.add(user);
								} else {
									final Identifiable i = new Identifiable() {
										public String getNick() {
											return vD[0];
										}
										public UUID getID() {
											return uuid;
										}
										@Override
										public boolean equals(Object o) {
											if (o == null || !(o instanceof Identifiable))
												return false;
											return this == o || getID().equals(((Identifiable) o).getID());
										}
									};
									visibilityUserData.add(i);
								}
								break;
							default: break;
						}
					}
				}
			}
		}
		return visibilityEditButton;
	}
	
	public JPanel getPasswordPanel() {
		if (passwordPanel == null) {
			passwordPanel = new JPanel();
			passwordPanel.setBorder(new TitledBorder(null, "Password protection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			GroupLayout gl_passwordPanel = new GroupLayout(passwordPanel);
			gl_passwordPanel.setHorizontalGroup(
				gl_passwordPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_passwordPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(getPasswordProtect())
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addComponent(getPasswordField())
						.addContainerGap())
			);
			gl_passwordPanel.setVerticalGroup(
				gl_passwordPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_passwordPanel.createSequentialGroup()
						.addGroup(gl_passwordPanel.createParallelGroup(Alignment.BASELINE)
							.addComponent(getPasswordProtect())
							.addComponent(getPasswordField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
			);
			passwordPanel.setLayout(gl_passwordPanel);
		}
		return passwordPanel;
	}
	
	public JCheckBox getPasswordProtect() {
		if (passwordProtect == null) {
			passwordProtect = new JCheckBox("");
			passwordProtect.setFocusable(false);
			passwordProtect.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getPasswordField().setVisible(passwordProtect.isSelected());
					getPasswordPanel().revalidate();
				}
			});
			passwordProtect.setFont(Fonts.GLOBAL);
			if (lPackage != null) {
				passwordProtect.setSelected(lPackage.isPasswordProtected());
			}
		}
		return passwordProtect;
	}
	
	public JTextField getPasswordField() {
		if (passwordField == null) {
			passwordField = new JTextField();
			passwordField.setVisible(false);
			passwordField.setFont(Fonts.GLOBAL);
			passwordField.setColumns(10);
			if (lPackage != null && lPackage.isPasswordProtected()) {
				passwordField.setVisible(true);
				passwordField.setText(lPackage.getPassword());
			}
		}
		return passwordField;
	}
	
	public JScrollPane getDescriptionScrollPane() {
		if (descriptionScrollPane == null) {
			descriptionScrollPane = new JScrollPane();
			descriptionScrollPane.setDoubleBuffered(true);
			descriptionScrollPane.setViewportView(getDescriptionPane());
		}
		return descriptionScrollPane;
	}
	
	public JTextPane getDescriptionPane() {
		if (descriptionPane == null) {
			descriptionPane = new JTextPane();
			descriptionPane.setDoubleBuffered(true);
			descriptionPane.setFont(Fonts.GLOBAL);
			if (lPackage != null) {
				descriptionPane.setText(lPackage.getDescription());
			}
			new PromptHandler(descriptionPane, "Write a description here.", Color.GRAY, JLabel.TOP);
		}
		return descriptionPane;
	}
	
	public JPanel getControlsPanel() {
		if (controlsPanel == null) {
			controlsPanel = new JPanel();
			controlsPanel.setBorder(new MatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
			GroupLayout gl_controlsPanel = new GroupLayout(controlsPanel);
			gl_controlsPanel.setHorizontalGroup(
				gl_controlsPanel.createParallelGroup(Alignment.TRAILING)
					.addGroup(gl_controlsPanel.createSequentialGroup()
						.addContainerGap(176, Short.MAX_VALUE)
						.addComponent(getOkButton(), GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(getCancelButton(), GroupLayout.PREFERRED_SIZE, 90, GroupLayout.PREFERRED_SIZE)
						.addContainerGap())
			);
			gl_controlsPanel.setVerticalGroup(
				gl_controlsPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_controlsPanel.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_controlsPanel.createParallelGroup(Alignment.BASELINE)
							.addComponent(getCancelButton())
							.addComponent(getOkButton()))
						.addContainerGap())
			);
			controlsPanel.setLayout(gl_controlsPanel);
		}
		return controlsPanel;
	}
	
	public JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton("OK");
			okButton.setFocusable(false);
			okButton.setFont(Fonts.GLOBAL);
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (packageWorker == null) {
						toggleControls(false);
						setCursor(Cursor.WAIT_CURSOR);
						final LocalFileNode[] nodes = fileTreeModel.getRoots(new LocalFileNode[fileTreeModel.getRootCount()]);
						packageWorker = new SwingWorker<LocalPackage, Object>() {
							protected LocalPackage doInBackground() throws Exception {
								if (lPackage != null) {
									// edit package data
									lPackage.setName(getNameField().getText());
									lPackage.setDescription(getDescriptionPane().getText());
									lPackage.setPassword(getPasswordProtect().isSelected() && getPasswordField().getText().length() > 0 ? getPasswordField().getText() : null);
									lPackage.setRoots(nodes);
								} else {
									// create new package
									lPackage = new LocalPackage(getNameField().getText(), getDescriptionPane().getText(), Calendar.getInstance(), null, 
											getPasswordProtect().isSelected() && getPasswordField().getText().length() > 0 ? getPasswordField().getText() : null, nodes);
								}
								final Visibility.Type vType = (Visibility.Type) getVisibilityComboBox().getSelectedItem();
								StringBuilder sb = vType != Visibility.Type.PUBLIC ? new StringBuilder() : null;
								switch (vType) {
									case CHANNEL:
										for (int i = 0; i < visibilityChannelData.size(); i++) {
											sb.append(visibilityChannelData.get(i));
											if (i + 1 < visibilityChannelData.size()) {
												sb.append(",");
											}
										}
									case USER:
										for (int i = 0; i < visibilityUserData.size(); i++) {
											final Identifiable u = visibilityUserData.get(i);
											sb.append(u.getNick());
											sb.append("|");
											sb.append(u.getID().toString());
											if (i + 1 < visibilityUserData.size()) {
												sb.append(",");
											}
										}
										break;
									case PRIVATE:
										sb.append(lPackage.getPrivateKey());
										break;
									default: break;
								}
								lPackage.setVisibility(new Visibility(vType, sb));
								return lPackage;
							}
							protected void done() {
								super.done();
								if (isCancelled()) {
									toggleControls(true);
									setCursor(Cursor.DEFAULT_CURSOR);
								} else {
									wizardListener.wizardFinished(lPackage);
									dispose();
								}
							}
						};
						packageWorker.execute();
					}
				}
			});
		}
		return okButton;
	}
	
	public JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
			cancelButton.setFocusable(false);
			cancelButton.setFont(Fonts.GLOBAL);
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					close();
				}
			});
		}
		return cancelButton;
	}
	
}