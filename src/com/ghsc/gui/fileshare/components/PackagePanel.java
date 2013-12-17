package com.ghsc.gui.fileshare.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.event.EventListener;
import com.ghsc.event.EventProvider;
import com.ghsc.event.global.EventManager;
import com.ghsc.event.global.EventProviderListener;
import com.ghsc.gui.Application;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.popup.Popup;
import com.ghsc.gui.components.popup.PopupBuilder;
import com.ghsc.gui.components.popup.PopupManager;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.gui.fileshare.FileShare.SocketWorker;
import com.ghsc.gui.fileshare.FileShareFrame;
import com.ghsc.gui.fileshare.internal.FilePackage;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility;
import com.ghsc.gui.fileshare.internal.LocalPackage;
import com.ghsc.impl.Filter;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class PackagePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	//private final FileShareFrame frame;
	private final FilePackage pack;
	private boolean headerHovered = false, headerDown = false;
	private boolean expanded = false;
	
	/*
	 * Event variables
	 */
	private final EventProviderListener eventProviderListener = new EventProviderListener() {
		public void providerAdded(EventProvider<?>.Context context) {
			switch (context.getName()) {
				case Application.NICK_EVENTPROVIDER:
					if (pack != null && pack instanceof LocalPackage) {
						context.subscribe(usernameListener);
					}
					break;
			}
		}
		public void providerRemoved(EventProvider<?>.Context context) {
			switch (context.getName()) {
				case Application.NICK_EVENTPROVIDER:
					context.unsubscribe(usernameListener);
					break;
			}
		}
	};
	private final EventListener<String> usernameListener = new EventListener<String>() {
		public void eventReceived(String username) {
			sync();
		}
	};
	
	private JLabel packageIconLabel;
	private JLabel packageNameLabel;
	private JLabel packageOwnerLabel;
	private JLabel packageCreatedLabel;
	private JPanel infoPanel;
	private JLabel sizeLabel;
	private JLabel fileCountLabel;
	private JLabel folderCountLabel;
	private JLabel downloadsLabel;
	private JLabel visibilityLabel;
	private JLabel passwordRequiredLabel;
	private JScrollPane descriptionScrollPane;
	private JTextPane descriptionPane;

	/**
	 * Create the panel.
	 */
	public PackagePanel(final FileShareFrame frame, final FilePackage pack) {
		super();
		//this.frame = frame;
		this.pack = pack;
		
		EventManager.getEventManager().addListener(eventProviderListener);
		
		this.addMouseListener(new MouseAdapter() {
			public void mouseExited(MouseEvent me) {
				headerHovered = headerDown = false;
				repaint();
			}
			public void mousePressed(MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					headerDown = new Rectangle(0, 0, getWidth(), 60).contains(me.getPoint());
					if (headerDown) {
						setExpanded(!isExpanded());
					}
					repaint();
				}
			}
			public void mouseReleased(MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					headerDown = false;
					repaint();
				}
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent me) {
				headerHovered = new Rectangle(0, 0, getWidth(), 60).contains(me.getPoint());
				repaint();
			}
		});
		
		Application.getApplication().getPopupManager().submit(new PopupBuilder() {
			public boolean build(Popup menu, PopupManager popupManager, Component sender, int x, int y) {
				if (pack != null && new Rectangle(0, 0, sender.getWidth(), 60).contains(x, y)) {
					final JMenuItem detailsMenuItem = menu.createItem("Details", new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							// open details frame
							
						}
					});
					detailsMenuItem.setIcon(new ImageIcon(Images.INFORMATION));
					menu.add(detailsMenuItem);
					if (pack instanceof LocalPackage) {
						final JMenuItem editMenuItem = menu.createItem("Edit", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								// edit existing local packages...
								if (frame.packageWizard == null) {
									frame.packageWizard = new PackageWizard(frame, (LocalPackage) pack, new WizardListener<LocalPackage>() {
										public void wizardFinished(LocalPackage lPackage) {
											if (lPackage != null) {
												// TODO: send edit message
												
												sync();
											}
											frame.packageWizard = null;
										}
									});
									frame.packageWizard.setVisible(true);
								}
							}
						});
						editMenuItem.setIcon(new ImageIcon(Images.PENCIL));
						menu.add(editMenuItem);
						final JMenuItem deleteMenuItem = menu.createItem("Delete", new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to permanently delete this package?\nAny users currently downloading package contents will be disconnected.", 
												"Package warning!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, new ImageIcon(Images.PACKAGE_LARGE)) == JOptionPane.YES_OPTION) {
											final FileShare fs = frame.getFileShare();
											if (fs.removePackages(pack)) {
												// TODO: message users about package deletion.
												
												frame.getFileShare().closeAll(new Filter<SocketWorker>() {
													public boolean accept(SocketWorker sw) {
														return sw.isActivePackage(pack);
													}
												});
											}
										}
									}
								});
							}
						});
						deleteMenuItem.setIcon(new ImageIcon(Images.DELETE));
						menu.add(deleteMenuItem);
					}
					return true;
				}
				return false;
			}
		}, this);
		
		initComponents();
		sync();
	}
	
	public FilePackage getPackage() {
		return pack;
	}
	
	/**
	 * Sync the panel with the package data.
	 */
	public void sync() {
		if (pack != null) {
			getPackageNameLabel().setText("Name: " + pack.getName());
			getPackageOwnerLabel().setText("Owner: " + pack.getOwner());
			getPackageCreatedLabel().setText("Created: " + pack.getCreationDateString());
			getDescriptionPane().setText(pack.getDescription());
			getSizeLabel().setText("Size: " + FileShare.toHumanReadable(pack.getSize(), true));
			getFileCountLabel().setText("Files: " + pack.getFileCount());
			getFolderCountLabel().setText("Folders: " + pack.getDirectoryCount());
			getDownloadsLabel().setText("Downloads: " + pack.getDownloadCount());
			
			StringBuilder sb = new StringBuilder();
			Visibility vis = pack.getVisibility();
			sb.append(vis.getType());
			if (pack instanceof LocalPackage &&
					vis.getType() == Visibility.Type.PRIVATE) {
				sb.append(" [");
				sb.append(vis.getData());
				sb.append("]");
			}
			getVisibilityLabel().setText("Visibility: " + sb.toString());
			
			getPasswordRequiredLabel().setText("Password required: " + (pack.isPasswordProtected() ? "yes" : "no"));
			
		}
	}
	
	public boolean isExpanded() {
		return expanded;
	}
	
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
		this.getParent().revalidate();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o != null) {
			if (o instanceof FilePackage) {
				return this.pack == o;
			} else if (o instanceof PackagePanel) {
				return this == o;
			}
		}
		return false;
	}
	
	@Override
	public void paintComponent(Graphics g1) {
		super.paintComponent(g1);
		if (headerHovered) {
			Graphics2D g = (Graphics2D) g1;
			final int width = getWidth(), height = 60;
			final Color c = headerDown ? Color.LIGHT_GRAY : Color.WHITE;
			GradientPaint gp = new GradientPaint(0, 0, c, 0, height, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
			g.setPaint(gp);
			g.fillRect(0, 0, width, height);
			g.setPaint(null);
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension p = super.getPreferredSize();
		if (!expanded)
			p.height = 60;
		return p;
	}
	
	private void initComponents() {
		setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(getInfoPanel(), Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(getPackageIconLabel(), GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(getPackageOwnerLabel(), GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE)
						.addComponent(getPackageCreatedLabel(), GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE)
						.addComponent(getPackageNameLabel(), GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(getPackageIconLabel(), GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(9)
							.addComponent(getPackageNameLabel())
							.addComponent(getPackageOwnerLabel())
							.addComponent(getPackageCreatedLabel())))
					.addComponent(getInfoPanel(), GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE))
		);
		setLayout(groupLayout);
	}
	
	public JLabel getPackageIconLabel() {
		if (packageIconLabel == null) {
			packageIconLabel = new JLabel();
			packageIconLabel.setIcon(new ImageIcon(Images.PACKAGE_LARGE));
		}
		return packageIconLabel;
	}
	
	public JLabel getPackageNameLabel() {
		if (packageNameLabel == null) {
			packageNameLabel = new JLabel("Name: 2000 pictures for the yearbook");
			packageNameLabel.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD));
		}
		return packageNameLabel;
	}
	
	public JLabel getPackageOwnerLabel() {
		if (packageOwnerLabel == null) {
			packageOwnerLabel = new JLabel("Owner: Bradley Odell");
			packageOwnerLabel.setFont(Fonts.GLOBAL);
		}
		return packageOwnerLabel;
	}
	
	public JLabel getPackageCreatedLabel() {
		if (packageCreatedLabel == null) {
			packageCreatedLabel = new JLabel("Created: 00/00/00 00:00 PM");
			packageCreatedLabel.setFont(Fonts.GLOBAL);
		}
		return packageCreatedLabel;
	}
	
	public JPanel getInfoPanel() {
		if (infoPanel == null) {
			infoPanel = new JPanel();
			infoPanel.setBorder(new MatteBorder(1, 1, 0, 1, (Color) new Color(192, 192, 192)));
			GroupLayout gl_infoPanel = new GroupLayout(infoPanel);
			gl_infoPanel.setHorizontalGroup(
				gl_infoPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_infoPanel.createSequentialGroup()
						.addGap(15)
						.addGroup(gl_infoPanel.createParallelGroup(Alignment.TRAILING)
							.addComponent(getSizeLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(getFileCountLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(getFolderCountLabel(), GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(getVisibilityLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(getPasswordRequiredLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(getDownloadsLabel(), GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE))
						.addGap(15))
					.addGroup(gl_infoPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(getDescriptionScrollPane(), GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
						.addContainerGap())
			);
			gl_infoPanel.setVerticalGroup(
				gl_infoPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_infoPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(getSizeLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(getFileCountLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(getFolderCountLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(getDownloadsLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(getVisibilityLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(getPasswordRequiredLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(getDescriptionScrollPane(), GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
						.addContainerGap())
			);
			infoPanel.setLayout(gl_infoPanel);
		}
		return infoPanel;
	}
	
	public JLabel getSizeLabel() {
		if (sizeLabel == null) {
			sizeLabel = new JLabel("Size: 300MB");
			sizeLabel.setFont(Fonts.GLOBAL);
		}
		return sizeLabel;
	}
	
	public JLabel getFileCountLabel() {
		if (fileCountLabel == null) {
			fileCountLabel = new JLabel("Files: 52");
			fileCountLabel.setFont(Fonts.GLOBAL);
		}
		return fileCountLabel;
	}
	
	public JLabel getFolderCountLabel() {
		if (folderCountLabel == null) {
			folderCountLabel = new JLabel("Folders: 3");
			folderCountLabel.setFont(Fonts.GLOBAL);
		}
		return folderCountLabel;
	}
	
	public JLabel getDownloadsLabel() {
		if (downloadsLabel == null) {
			downloadsLabel = new JLabel("Downloads: 487");
			downloadsLabel.setFont(Fonts.GLOBAL);
		}
		return downloadsLabel;
	}
	
	public JLabel getVisibilityLabel() {
		if (visibilityLabel == null) {
			visibilityLabel = new JLabel("Visibility: Public");
			visibilityLabel.setFont(Fonts.GLOBAL);
		}
		return visibilityLabel;
	}
	
	public JLabel getPasswordRequiredLabel() {
		if (passwordRequiredLabel == null) {
			passwordRequiredLabel = new JLabel("Password required: no");
			passwordRequiredLabel.setFont(Fonts.GLOBAL);
		}
		return passwordRequiredLabel;
	}
	
	public JScrollPane getDescriptionScrollPane() {
		if (descriptionScrollPane == null) {
			descriptionScrollPane = new JScrollPane();
			descriptionScrollPane.setBorder(new TitledBorder(null, "Description", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			descriptionScrollPane.setViewportView(getDescriptionPane());
		}
		return descriptionScrollPane;
	}
	
	public JTextPane getDescriptionPane() {
		if (descriptionPane == null) {
			descriptionPane = new JTextPane();
			descriptionPane.setMargin(new Insets(0, 5, 3, 3));
			descriptionPane.setDoubleBuffered(true);
			descriptionPane.setMinimumSize(new Dimension(0, 0));
			descriptionPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			descriptionPane.setEditable(false);
			descriptionPane.setFont(new Font("Tahoma", Font.PLAIN, 11));
			descriptionPane.setOpaque(false);
			descriptionPane.setText("Hello, what is your name? My name is Josh.");
		}
		return descriptionPane;
	}
	
}