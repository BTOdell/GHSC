package com.ghsc.gui.fileshare.components;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.gui.Application;
import com.ghsc.gui.fileshare.FileShare;
import com.ghsc.gui.fileshare.FileShareFrame;
import com.ghsc.gui.fileshare.internal.FilePackage;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility;
import com.ghsc.gui.fileshare.internal.LocalPackage;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class PackagePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	//private final FileShareFrame frame;
	private final FilePackage pack;
	private boolean headerHovered;
    private boolean headerDown;
	private boolean expanded;

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
		if (pack instanceof LocalPackage) {
			Application.getInstance().getNickEventProvider().subscribe((final String username) -> this.sync());
		}

		this.addMouseListener(new MouseAdapter() {
			public void mouseExited(final MouseEvent me) {
				PackagePanel.this.headerHovered = PackagePanel.this.headerDown = false;
				PackagePanel.this.repaint();
			}
			public void mousePressed(final MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					PackagePanel.this.headerDown = new Rectangle(0, 0, PackagePanel.this.getWidth(), 60).contains(me.getPoint());
					if (PackagePanel.this.headerDown) {
						PackagePanel.this.setExpanded(!PackagePanel.this.isExpanded());
					}
					PackagePanel.this.repaint();
				}
			}
			public void mouseReleased(final MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					PackagePanel.this.headerDown = false;
					PackagePanel.this.repaint();
				}
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(final MouseEvent me) {
				PackagePanel.this.headerHovered = new Rectangle(0, 0, PackagePanel.this.getWidth(), 60).contains(me.getPoint());
				PackagePanel.this.repaint();
			}
		});

        Application.getInstance().getPopupManager().submit((menu, popupManager, sender, x, y) -> {
            if (pack != null && new Rectangle(0, 0, sender.getWidth(), 60).contains(x, y)) {
                final JMenuItem detailsMenuItem = menu.createItem("Details", e -> {
                    // open details frame

                });
                detailsMenuItem.setIcon(new ImageIcon(Images.INFORMATION));
                menu.add(detailsMenuItem);
                if (pack instanceof LocalPackage) {
                    final JMenuItem editMenuItem = menu.createItem("Edit", e -> {
                        // edit existing local packages...
                        if (frame.packageWizard == null) {
                            frame.packageWizard = new PackageWizard(frame, (LocalPackage) pack, lPackage -> {
                                if (lPackage != null) {
                                    // TODO: send edit message

                                    this.sync();
                                }
                                frame.packageWizard = null;
                            });
                            frame.packageWizard.setVisible(true);
                        }
                    });
                    editMenuItem.setIcon(new ImageIcon(Images.PENCIL));
                    menu.add(editMenuItem);
                    final JMenuItem deleteMenuItem = menu.createItem("Delete", e -> {
                        if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to permanently delete this package?\nAny users currently downloading package contents will be disconnected.",
                                "Package warning!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, new ImageIcon(Images.PACKAGE_LARGE)) == JOptionPane.YES_OPTION) {
                            final FileShare fs = frame.getFileShare();
                            if (fs.removePackages(pack)) {
                                // TODO: message users about package deletion.

                                frame.getFileShare().closeAll(sw -> sw.isActivePackage(pack));
                            }
                        }
                    });
                    deleteMenuItem.setIcon(new ImageIcon(Images.DELETE));
                    menu.add(deleteMenuItem);
                }
                return true;
            }
            return false;
        }, this);

		this.initComponents();
		this.sync();
	}
	
	public FilePackage getPackage() {
		return this.pack;
	}
	
	/**
	 * Sync the panel with the package data.
	 */
	public void sync() {
		if (this.pack != null) {
			this.getPackageNameLabel().setText("Name: " + this.pack.getName());
			this.getPackageOwnerLabel().setText("Owner: " + this.pack.getOwner());
			this.getPackageCreatedLabel().setText("Created: " + this.pack.getCreationDateString());
			this.getDescriptionPane().setText(this.pack.getDescription());
			this.getSizeLabel().setText("Size: " + FileShare.toHumanReadable(this.pack.getSize(), true));
			this.getFileCountLabel().setText("Files: " + this.pack.getFileCount());
			this.getFolderCountLabel().setText("Folders: " + this.pack.getDirectoryCount());
			this.getDownloadsLabel().setText("Downloads: " + this.pack.getDownloadCount());
			
			final StringBuilder sb = new StringBuilder();
			final Visibility vis = this.pack.getVisibility();
			sb.append(vis.getType());
			if (this.pack instanceof LocalPackage &&
					vis.getType() == Visibility.Type.PRIVATE) {
				sb.append(" [");
				sb.append(vis.getData());
				sb.append("]");
			}
			this.getVisibilityLabel().setText("Visibility: " + sb.toString());

			this.getPasswordRequiredLabel().setText("Password required: " + (this.pack.isPasswordProtected() ? "yes" : "no"));
			
		}
	}
	
	public boolean isExpanded() {
		return this.expanded;
	}
	
	public void setExpanded(final boolean expanded) {
		this.expanded = expanded;
		final Container c = this.getParent();
		c.invalidate();
		c.validate();
	}
	
	@Override
	public boolean equals(final Object o) {
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
	public void paintComponent(final Graphics g1) {
		super.paintComponent(g1);
		if (this.headerHovered) {
			final Graphics2D g = (Graphics2D) g1;
			final int width = this.getWidth();
			final int height = 60;
			final Color c = this.headerDown ? Color.LIGHT_GRAY : Color.WHITE;
			final GradientPaint gp = new GradientPaint(0, 0, c, 0, height, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
			g.setPaint(gp);
			g.fillRect(0, 0, width, height);
			g.setPaint(null);
		}
	}
	
	@Override
	public Dimension getPreferredSize() {
		final Dimension p = super.getPreferredSize();
		if (!this.expanded) {
            p.height = 60;
        }
		return p;
	}
	
	private void initComponents() {
		this.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(this.getInfoPanel(), Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(this.getPackageIconLabel(), GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(this.getPackageOwnerLabel(), GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE)
						.addComponent(this.getPackageCreatedLabel(), GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE)
						.addComponent(this.getPackageNameLabel(), GroupLayout.PREFERRED_SIZE, 192, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(this.getPackageIconLabel(), GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(9)
							.addComponent(this.getPackageNameLabel())
							.addComponent(this.getPackageOwnerLabel())
							.addComponent(this.getPackageCreatedLabel())))
					.addComponent(this.getInfoPanel(), GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE))
		);
		this.setLayout(groupLayout);
	}
	
	public JLabel getPackageIconLabel() {
		if (this.packageIconLabel == null) {
			this.packageIconLabel = new JLabel();
			this.packageIconLabel.setIcon(new ImageIcon(Images.PACKAGE_LARGE));
		}
		return this.packageIconLabel;
	}
	
	public JLabel getPackageNameLabel() {
		if (this.packageNameLabel == null) {
			this.packageNameLabel = new JLabel("Name: 2000 pictures for the yearbook");
			this.packageNameLabel.setFont(Fonts.GLOBAL.deriveFont(Font.BOLD));
		}
		return this.packageNameLabel;
	}
	
	public JLabel getPackageOwnerLabel() {
		if (this.packageOwnerLabel == null) {
			this.packageOwnerLabel = new JLabel("Owner: Bradley Odell");
			this.packageOwnerLabel.setFont(Fonts.GLOBAL);
		}
		return this.packageOwnerLabel;
	}
	
	public JLabel getPackageCreatedLabel() {
		if (this.packageCreatedLabel == null) {
			this.packageCreatedLabel = new JLabel("Created: 00/00/00 00:00 PM");
			this.packageCreatedLabel.setFont(Fonts.GLOBAL);
		}
		return this.packageCreatedLabel;
	}
	
	public JPanel getInfoPanel() {
		if (this.infoPanel == null) {
			this.infoPanel = new JPanel();
			this.infoPanel.setBorder(new MatteBorder(1, 1, 0, 1, new Color(192, 192, 192)));
			final GroupLayout gl_infoPanel = new GroupLayout(this.infoPanel);
			gl_infoPanel.setHorizontalGroup(
				gl_infoPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_infoPanel.createSequentialGroup()
						.addGap(15)
						.addGroup(gl_infoPanel.createParallelGroup(Alignment.TRAILING)
							.addComponent(this.getSizeLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(this.getFileCountLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(this.getFolderCountLabel(), GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(this.getVisibilityLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(this.getPasswordRequiredLabel(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
							.addComponent(this.getDownloadsLabel(), GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE))
						.addGap(15))
					.addGroup(gl_infoPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(this.getDescriptionScrollPane(), GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
						.addContainerGap())
			);
			gl_infoPanel.setVerticalGroup(
				gl_infoPanel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_infoPanel.createSequentialGroup()
						.addContainerGap()
						.addComponent(this.getSizeLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(this.getFileCountLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(this.getFolderCountLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(this.getDownloadsLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(this.getVisibilityLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addComponent(this.getPasswordRequiredLabel(), GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(this.getDescriptionScrollPane(), GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
						.addContainerGap())
			);
			this.infoPanel.setLayout(gl_infoPanel);
		}
		return this.infoPanel;
	}
	
	public JLabel getSizeLabel() {
		if (this.sizeLabel == null) {
			this.sizeLabel = new JLabel("Size: 300MB");
			this.sizeLabel.setFont(Fonts.GLOBAL);
		}
		return this.sizeLabel;
	}
	
	public JLabel getFileCountLabel() {
		if (this.fileCountLabel == null) {
			this.fileCountLabel = new JLabel("Files: 52");
			this.fileCountLabel.setFont(Fonts.GLOBAL);
		}
		return this.fileCountLabel;
	}
	
	public JLabel getFolderCountLabel() {
		if (this.folderCountLabel == null) {
			this.folderCountLabel = new JLabel("Folders: 3");
			this.folderCountLabel.setFont(Fonts.GLOBAL);
		}
		return this.folderCountLabel;
	}
	
	public JLabel getDownloadsLabel() {
		if (this.downloadsLabel == null) {
			this.downloadsLabel = new JLabel("Downloads: 487");
			this.downloadsLabel.setFont(Fonts.GLOBAL);
		}
		return this.downloadsLabel;
	}
	
	public JLabel getVisibilityLabel() {
		if (this.visibilityLabel == null) {
			this.visibilityLabel = new JLabel("Visibility: Public");
			this.visibilityLabel.setFont(Fonts.GLOBAL);
		}
		return this.visibilityLabel;
	}
	
	public JLabel getPasswordRequiredLabel() {
		if (this.passwordRequiredLabel == null) {
			this.passwordRequiredLabel = new JLabel("Password required: no");
			this.passwordRequiredLabel.setFont(Fonts.GLOBAL);
		}
		return this.passwordRequiredLabel;
	}
	
	public JScrollPane getDescriptionScrollPane() {
		if (this.descriptionScrollPane == null) {
			this.descriptionScrollPane = new JScrollPane();
			this.descriptionScrollPane.setBorder(new TitledBorder(null, "Description", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			this.descriptionScrollPane.setViewportView(this.getDescriptionPane());
		}
		return this.descriptionScrollPane;
	}
	
	public JTextPane getDescriptionPane() {
		if (this.descriptionPane == null) {
			this.descriptionPane = new JTextPane();
			this.descriptionPane.setMargin(new Insets(0, 5, 3, 3));
			this.descriptionPane.setDoubleBuffered(true);
			this.descriptionPane.setMinimumSize(new Dimension(0, 0));
			this.descriptionPane.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			this.descriptionPane.setEditable(false);
			this.descriptionPane.setFont(new Font("Tahoma", Font.PLAIN, 11));
			this.descriptionPane.setOpaque(false);
			this.descriptionPane.setText("Hello, what is your name? My name is Josh.");
		}
		return this.descriptionPane;
	}
	
}