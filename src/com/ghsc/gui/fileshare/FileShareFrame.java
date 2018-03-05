package com.ghsc.gui.fileshare;

import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.ghsc.common.Fonts;
import com.ghsc.common.Images;
import com.ghsc.gui.components.input.ValidationResult;
import com.ghsc.gui.components.input.WizardListener;
import com.ghsc.gui.components.input.WizardValidator;
import com.ghsc.gui.fileshare.components.FindPackageDialog;
import com.ghsc.gui.fileshare.components.PackagePanel;
import com.ghsc.gui.fileshare.components.PackagePanelList;
import com.ghsc.gui.fileshare.components.PackageWizard;
import com.ghsc.gui.fileshare.components.SortByPopup;
import com.ghsc.gui.fileshare.internal.FilePackage;
import com.ghsc.gui.fileshare.internal.FilePackage.Visibility;
import com.ghsc.gui.fileshare.internal.LocalPackage;
import com.ghsc.gui.fileshare.internal.RemotePackage;
import com.ghsc.impl.ObjectProcessor;
import com.ghsc.util.SnapAdapter;
import com.ghsc.util.SnapAdapter.Magnet;
import com.ghsc.util.SnapAdapter.Side;

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
				public void windowGainedFocus(WindowEvent we) {
					if (!FileShareFrame.this.equals(we.getOppositeWindow())) {
						FileShareFrame.this.requestFocus(true);
						owner.requestFocus();
					}
				}
				public void windowClosing(WindowEvent e) {
					FileShareFrame.this.setVisible(false, false);
				}
			};
			owner.addWindowFocusListener(wa);
			owner.addWindowListener(wa);
			owner.addComponentListener(new ComponentAdapter() {
				public void componentShown(ComponentEvent arg0) {
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
	public void setVisible(boolean enabled) {
		super.setVisible(this.visibleBuffer = enabled);
	}
	
	public void setVisible(boolean enabled, boolean buffer) {
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
			this.packageWizardButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (FileShareFrame.this.packageWizard == null) {
						FileShareFrame.this.packageWizard = new PackageWizard(FileShareFrame.this, new WizardListener<LocalPackage>() {
							public void wizardFinished(LocalPackage lPackage) {
								if (lPackage != null) {
									FileShareFrame.this.fileShare.addPackages(lPackage);
									// TODO: message users about new package
								}
								FileShareFrame.this.packageWizard = null;
							}
						});
						FileShareFrame.this.packageWizard.setVisible(true);
					}
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
			this.findPackageButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (FileShareFrame.this.findPackageDialog == null) {
						FileShareFrame.this.findPackageDialog = new FindPackageDialog(FileShareFrame.this, new WizardListener<RemotePackage[]>() {
							public void wizardFinished(RemotePackage[] packages) {
								if (packages != null) {
									for (RemotePackage rp : packages) {
										rp.getVisibility().setDiscovered(true);
									}
								}
								FileShareFrame.this.findPackageDialog = null;
							}
						}, new WizardValidator<String, RemotePackage[], Boolean>() {
							public ValidationResult<RemotePackage[], Boolean> validate(String key) {
								if (key == null) {
									return new ValidationResult<RemotePackage[], Boolean>(null, false);
								}
								final LinkedList<RemotePackage> remotePackages = new LinkedList<RemotePackage>();
								final Collection<FilePackage> packages = FileShareFrame.this.fileShare.packages.values();
								synchronized (FileShareFrame.this.fileShare.packages) {
									for (final FilePackage pack : packages) {
										if (pack == null || !(pack instanceof RemotePackage)) {
											continue;
										}
										final RemotePackage rp = (RemotePackage) pack;
										final Visibility v = rp.getVisibility();
										if (!v.isDiscovered() && key.equals(v.getData().toString())) {
											remotePackages.add(rp);
										}
									}
								}
								int size = remotePackages.size();
								return new ValidationResult<RemotePackage[], Boolean>(remotePackages.toArray(new RemotePackage[size]), size > 0);
							}
						});
						FileShareFrame.this.findPackageDialog.setVisible(true);
					}
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
				public void mouseReleased(MouseEvent e) {
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
				public void popupMenuCanceled(PopupMenuEvent pme) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
					System.out.println("Sorting packages.");
					final Set<Comparator<PackagePanel>> comps = FileShareFrame.this.sortByPopup.getSortingComparators();
					final Comparator<PackagePanel> comp = new Comparator<PackagePanel>() {
						public int compare(PackagePanel p, PackagePanel pp) {
							for (Comparator<PackagePanel> c : comps) {
								int cp = c.compare(p, pp);
								if (cp != 0) {
									return cp;
								}
							}
							return 0;
						}
					};
					FileShareFrame.this.getPackagePanels().setComparator(comp);
				}
				public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {}
			});
			this.sortByPopup.setDoubleBuffered(true);
		}
		return this.sortByPopup;
	}
	
	public JScrollPane getPackageScrollPane() {
		if (this.packageScrollPane == null) {
			this.packageScrollPane = new JScrollPane();
			this.packageScrollPane.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
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
			this.packagePanels = new PackagePanelList(this, new ObjectProcessor<Object, LinkedList<FilePackage>>() {
				public LinkedList<FilePackage> process(Object input) {
					return new LinkedList<FilePackage>(FileShareFrame.this.fileShare.packages.values());
				}
			});
		}
		return this.packagePanels;
	}
	
}