package com.ghsc.gui.fileshare;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.fileshare.components.*;
import com.ghsc.gui.fileshare.internal.FilePackage;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility;
import com.ghsc.gui.fileshare.internal.RemotePackage;
import com.ghsc.util.SnapAdapter;
import com.ghsc.util.SnapAdapter.Magnet;
import com.ghsc.util.SnapAdapter.Side;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

public class FileShareFrame extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private final FileShare fileShare;
	public PackageWizard packageWizard;
	private FindPackageDialog findPackageDialog;
	
	private final SnapAdapter snapAdapter;
	
	private boolean visibleBuffer;

	private JToolBar topBar;
	private JButton packageWizardButton;
	private JButton findPackageButton;
	private JToggleButton sortByButton;
	private SortByPopup sortByPopup;
	private JScrollPane packageScrollPane;
	private PackagePanelList packagePanels;
	
	/**
	 * Create the frame.
	 */
	public FileShareFrame(final Window owner, final FileShare ft) {
		super(owner, ModalityType.MODELESS);
		this.fileShare = ft;
		
		if (owner != null) {
			
			final WindowAdapter wa = new WindowAdapter() {
				public void windowGainedFocus(final WindowEvent we) {
					if (!FileShareFrame.this.equals(we.getOppositeWindow())) {
						FileShareFrame.this.requestFocus(true);
						owner.requestFocus();
					}
				}
				public void windowClosing(final WindowEvent e) {
					FileShareFrame.this.setVisible(false, false);
				}
			};
			owner.addWindowFocusListener(wa);
			owner.addWindowListener(wa);
			owner.addComponentListener(new ComponentAdapter() {
				public void componentShown(final ComponentEvent arg0) {
					FileShareFrame.this.applyVisible();
				}
			});
			
			this.snapAdapter = new SnapAdapter(owner, this, new Magnet[][] {
				SnapAdapter.createMagnets(SnapAdapter.createSides(Side.Type.RIGHT, Side.Type.LEFT, Side.Align.UP, 8), 10)
			});
			this.snapAdapter.setEnabled(true);
			
		} else {
			this.snapAdapter = null;
		}

		this.initComponents();
	}
	
	@Override
	public void setVisible(final boolean enabled) {
		super.setVisible(this.visibleBuffer = enabled);
	}
	
	public void setVisible(final boolean enabled, final boolean buffer) {
		super.setVisible(buffer ? this.visibleBuffer = enabled : enabled);
	}
	
	public void applyVisible() {
		super.setVisible(this.visibleBuffer);
	}
	
	public FileShare getFileShare() {
		return this.fileShare;
	}
	
	public SnapAdapter getSnapAdapter() {
		return this.snapAdapter;
	}
	
	private void initComponents() {
		this.setTitle("GHSC - File sharing");
		this.setIconImage(Images.PAGE_GO);
		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		this.setSize(300, 450);
		this.setMinimumSize(this.getSize());

		final JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setContentPane(contentPane);
		final GroupLayout gl_canvas = new GroupLayout(contentPane);
		gl_canvas.setHorizontalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addComponent(this.getTopBar(), GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
				.addComponent(this.getPackageScrollPane(), GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
		);
		gl_canvas.setVerticalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addComponent(this.getTopBar(), GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
					.addComponent(this.getPackageScrollPane(), GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE))
		);
		contentPane.setLayout(gl_canvas);
	}
	
	public JToolBar getTopBar() {
		if (this.topBar == null) {
			this.topBar = new JToolBar();
			this.topBar.setDoubleBuffered(true);
			this.topBar.setFloatable(false);
			this.topBar.add(this.getPackageWizardButton());
			this.topBar.add(this.getFindPackageButton());
			this.topBar.add(Box.createHorizontalGlue());
			this.topBar.add(this.getSortByButton());
		}
		return this.topBar;
	}
	
	public JButton getPackageWizardButton() {
		if (this.packageWizardButton == null) {
			this.packageWizardButton = new JButton();
			this.packageWizardButton.setIconTextGap(0);
			this.packageWizardButton.setHorizontalTextPosition(SwingConstants.CENTER);
			this.packageWizardButton.setIcon(new ImageIcon(Images.PACKAGE_ADD));
			this.packageWizardButton.addActionListener(arg0 -> {
				if (this.packageWizard == null) {
					this.packageWizard = new PackageWizard(this, lPackage -> {
						if (lPackage != null) {
							this.fileShare.addPackages(lPackage);
							// TODO: message users about new package
						}
						this.packageWizard = null;
					});
					this.packageWizard.setVisible(true);
				}
			});
			this.packageWizardButton.setFont(Fonts.GLOBAL);
			this.packageWizardButton.setToolTipText("New package");
			this.packageWizardButton.setDoubleBuffered(true);
			this.packageWizardButton.setFocusable(false);
		}
		return this.packageWizardButton;
	}
	
	public JButton getFindPackageButton() {
		if (this.findPackageButton == null) {
			this.findPackageButton = new JButton();
			this.findPackageButton.setIconTextGap(0);
			this.findPackageButton.setHorizontalTextPosition(SwingConstants.CENTER);
            this.findPackageButton.setIcon(new ImageIcon(Images.FIND));
            this.findPackageButton.addActionListener(arg0 -> {
                if (this.findPackageDialog == null) {
                    this.findPackageDialog = new FindPackageDialog(this, packages -> {
                        if (packages != null) {
                            for (final RemotePackage rp : packages) {
                                rp.getVisibility().setDiscovered(true);
                            }
                        }
                        this.findPackageDialog = null;
                    }, key -> {
                        if (key == null) {
                            return new ValidationResult<>(null, false);
                        }
                        final LinkedList<RemotePackage> remotePackages = new LinkedList<>();
                        final Collection<FilePackage> packages = this.fileShare.packages.values();
                        synchronized (this.fileShare.packages) {
                            for (final FilePackage pack : packages) {
                                if (!(pack instanceof RemotePackage)) {
                                    continue;
                                }
                                final RemotePackage rp = (RemotePackage) pack;
                                final Visibility v = rp.getVisibility();
                                if (!v.isDiscovered() && key.equals(v.getData().toString())) {
                                    remotePackages.add(rp);
                                }
                            }
                        }
                        final int size = remotePackages.size();
                        return new ValidationResult<>(remotePackages.toArray(new RemotePackage[size]), size > 0);
                    });
                    this.findPackageDialog.setVisible(true);
                }
            });
            this.findPackageButton.setFont(Fonts.GLOBAL);
			this.findPackageButton.setToolTipText("Find private packages");
			this.findPackageButton.setDoubleBuffered(true);
			this.findPackageButton.setFocusable(false);
		}
		return this.findPackageButton;
	}
	
	public JToggleButton getSortByButton() {
		if (this.sortByButton == null) {
			this.sortByButton = new JToggleButton("Sort by");
			this.sortByButton.setIconTextGap(0);
			this.sortByButton.setHorizontalTextPosition(SwingConstants.LEADING);
			this.sortByButton.setIcon(new ImageIcon(Images.BULLET_ARROW_DOWN));
			this.sortByButton.addMouseListener(new MouseAdapter() {
				public void mouseReleased(final MouseEvent e) {
					if (FileShareFrame.this.getSortByPopup().isShowing()) {
						FileShareFrame.this.getSortByPopup().setVisible(false);
					} else {
						final Rectangle bounds = e.getComponent().getBounds();
						FileShareFrame.this.getSortByPopup().show(e.getComponent(), bounds.width / 2, bounds.height / 2);
					}
				}
			});
			this.sortByButton.setFont(Fonts.GLOBAL);
			this.sortByButton.setToolTipText("Sort packages");
			this.sortByButton.setDoubleBuffered(true);
			this.sortByButton.setFocusable(false);
		}
		return this.sortByButton;
	}
	
	public SortByPopup getSortByPopup() {
		if (this.sortByPopup == null) {
			this.sortByPopup = new SortByPopup(this.sortByButton);
			this.sortByPopup.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(final PopupMenuEvent pme) {}
				public void popupMenuWillBecomeInvisible(final PopupMenuEvent pme) {
					System.out.println("Sorting packages.");
					final Set<Comparator<PackagePanel>> comps = FileShareFrame.this.sortByPopup.getSortingComparators();
					final Comparator<PackagePanel> comp = (p, pp) -> {
                        for (final Comparator<PackagePanel> c : comps) {
                            final int cp = c.compare(p, pp);
                            if (cp != 0) {
                                return cp;
                            }
                        }
                        return 0;
                    };
					FileShareFrame.this.getPackagePanels().setComparator(comp);
				}
				public void popupMenuWillBecomeVisible(final PopupMenuEvent pme) {}
			});
			this.sortByPopup.setDoubleBuffered(true);
		}
		return this.sortByPopup;
	}
	
	public JScrollPane getPackageScrollPane() {
		if (this.packageScrollPane == null) {
			this.packageScrollPane = new JScrollPane();
			this.packageScrollPane.addComponentListener(new ComponentAdapter() {
				public void componentResized(final ComponentEvent ce) {
					FileShareFrame.this.getPackagePanels().invalidate();
				}
			});
			this.packageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			this.packageScrollPane.getVerticalScrollBar().setUnitIncrement(8);
			this.packageScrollPane.setViewportView(this.getPackagePanels());
		}
		return this.packageScrollPane;
	}
	
	public PackagePanelList getPackagePanels() {
		if (this.packagePanels == null) {
			this.packagePanels = new PackagePanelList(this, input -> new LinkedList<>(this.fileShare.packages.values()));
		}
		return this.packagePanels;
	}
	
}