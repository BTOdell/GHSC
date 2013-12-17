package com.ghsc.gui.fileshare;

import java.awt.Rectangle;
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
import java.util.Iterator;
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
import com.ghsc.gui.MainFrame;
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
import com.ghsc.util.WindowUtils.SnapAdapter;
import com.ghsc.util.WindowUtils.SnapAdapter.Magnet;
import com.ghsc.util.WindowUtils.SnapAdapter.Side;

public class FileShareFrame extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private FileShare fileShare;
	private MainFrame mainFrame;
	public PackageWizard packageWizard = null;
	private FindPackageDialog findPackageDialog = null;
	
	private boolean visibleBuffer = false;
	
	private SnapAdapter snapAdapter = null;
	
	private JPanel canvas;
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
	public FileShareFrame(final FileShare ft) {
		this.fileShare = ft;
		this.mainFrame = ft.getApplication().getMainFrame();
		final WindowAdapter wa = new WindowAdapter() {
			public void windowGainedFocus(WindowEvent we) {
				if (!FileShareFrame.this.equals(we.getOppositeWindow())) {
					FileShareFrame.this.requestFocus(true);
					FileShareFrame.this.mainFrame.requestFocus();
				}
			}
			public void windowClosing(WindowEvent e) {
				setVisible(false, false);
			}
		};
		this.mainFrame.addWindowFocusListener(wa);
		this.mainFrame.addWindowListener(wa);
		this.mainFrame.addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent arg0) {
				applyVisible();
			}
		});
		
		initComponents();
	}
	
	@Override
	public void setVisible(boolean enabled) {
		super.setVisible(visibleBuffer = enabled);
	}
	
	public void setVisible(boolean enabled, boolean buffer) {
		super.setVisible(buffer ? visibleBuffer = enabled : enabled);
	}
	
	public void applyVisible() {
		super.setVisible(visibleBuffer);
	}
	
	public FileShare getFileShare() {
		return fileShare;
	}
	
	public MainFrame getMainFrame() {
		return mainFrame;
	}
	
	private void initComponents() {
		setTitle("GHSC - File sharing");
		setIconImage(Images.PAGE_GO);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(300, 450);
		setMinimumSize(getSize());
		
		getSnapAdapter().setEnabled(true);
		
		canvas = new JPanel();
		canvas.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(canvas);
		GroupLayout gl_canvas = new GroupLayout(canvas);
		gl_canvas.setHorizontalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addComponent(getTopBar(), GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
				.addComponent(getPackageScrollPane(), GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
		);
		gl_canvas.setVerticalGroup(
			gl_canvas.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_canvas.createSequentialGroup()
					.addComponent(getTopBar(), GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
					.addComponent(getPackageScrollPane(), GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE))
		);
		canvas.setLayout(gl_canvas);
	}
	
	public SnapAdapter getSnapAdapter() {
		if (snapAdapter == null) {
			snapAdapter = new SnapAdapter(mainFrame, this, new Magnet[][] {
				SnapAdapter.createMagnets(SnapAdapter.createSides(Side.Type.RIGHT, Side.Type.LEFT, Side.Align.UP, 8), 10)
			});
		}
		return snapAdapter;
	}
	
	public JToolBar getTopBar() {
		if (topBar == null) {
			topBar = new JToolBar();
			topBar.setDoubleBuffered(true);
			topBar.setFloatable(false);
			topBar.add(getPackageWizardButton());
			topBar.add(getFindPackageButton());
			topBar.add(Box.createHorizontalGlue());
			topBar.add(getSortByButton());
		}
		return topBar;
	}
	
	public JButton getPackageWizardButton() {
		if (packageWizardButton == null) {
			packageWizardButton = new JButton();
			packageWizardButton.setIconTextGap(0);
			packageWizardButton.setHorizontalTextPosition(SwingConstants.CENTER);
			packageWizardButton.setIcon(new ImageIcon(Images.PACKAGE_ADD));
			packageWizardButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (packageWizard == null) {
						packageWizard = new PackageWizard(FileShareFrame.this, new WizardListener<LocalPackage>() {
							public void wizardFinished(LocalPackage lPackage) {
								if (lPackage != null) {
									fileShare.addPackages(lPackage);
									// TODO: message users about new package
								}
								packageWizard = null;
							}
						});
						packageWizard.setVisible(true);
					}
				}
			});
			packageWizardButton.setFont(Fonts.GLOBAL);
			packageWizardButton.setToolTipText("New package");
			packageWizardButton.setDoubleBuffered(true);
			packageWizardButton.setFocusable(false);
		}
		return packageWizardButton;
	}
	
	public JButton getFindPackageButton() {
		if (findPackageButton == null) {
			findPackageButton = new JButton();
			findPackageButton.setIconTextGap(0);
			findPackageButton.setHorizontalTextPosition(SwingConstants.CENTER);
			findPackageButton.setIcon(new ImageIcon(Images.FIND));
			findPackageButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (findPackageDialog == null) {
						findPackageDialog = new FindPackageDialog(FileShareFrame.this, new WizardListener<RemotePackage[]>() {
							public void wizardFinished(RemotePackage[] packages) {
								if (packages != null) {
									for (RemotePackage rp : packages) {
										rp.getVisibility().setDiscovered(true);
									}
								}
								findPackageDialog = null;
							}
						}, new WizardValidator<String, RemotePackage[], Boolean>() {
							public ValidationResult<RemotePackage[], Boolean> validate(String key) {
								if (key == null)
									return new ValidationResult<RemotePackage[], Boolean>(null, false);
								final LinkedList<RemotePackage> remotePackages = new LinkedList<RemotePackage>();
								final Collection<FilePackage> packages = fileShare.packages.values();
								synchronized (fileShare.packages) {
									final Iterator<FilePackage> it = packages.iterator();
									while (it.hasNext()) {
										final FilePackage pack = it.next();
										if (pack == null || !(pack instanceof RemotePackage))
											continue;
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
						findPackageDialog.setVisible(true);
					}
				}
			});
			findPackageButton.setFont(Fonts.GLOBAL);
			findPackageButton.setToolTipText("Find private packages");
			findPackageButton.setDoubleBuffered(true);
			findPackageButton.setFocusable(false);
		}
		return findPackageButton;
	}
	
	public JToggleButton getSortByButton() {
		if (sortByButton == null) {
			sortByButton = new JToggleButton("Sort by");
			sortByButton.setIconTextGap(0);
			sortByButton.setHorizontalTextPosition(SwingConstants.LEADING);
			sortByButton.setIcon(new ImageIcon(Images.BULLET_ARROW_DOWN));
			sortByButton.addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					if (getSortByPopup().isShowing()) {
						getSortByPopup().setVisible(false);
					} else {
						final Rectangle bounds = e.getComponent().getBounds();
						getSortByPopup().show(e.getComponent(), bounds.width / 2, bounds.height / 2);
					}
				}
			});
			sortByButton.setFont(Fonts.GLOBAL);
			sortByButton.setToolTipText("Sort packages");
			sortByButton.setDoubleBuffered(true);
			sortByButton.setFocusable(false);
		}
		return sortByButton;
	}
	
	public SortByPopup getSortByPopup() {
		if (sortByPopup == null) {
			sortByPopup = new SortByPopup(sortByButton);
			sortByPopup.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent pme) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
					System.out.println("Sorting packages.");
					final Set<Comparator<PackagePanel>> comps = sortByPopup.getSortingComparators();
					final Comparator<PackagePanel> comp = new Comparator<PackagePanel>() {
						public int compare(PackagePanel p, PackagePanel pp) {
							for (Comparator<PackagePanel> c : comps) {
								int cp = c.compare(p, pp);
								if (cp != 0)
									return cp;
							}
							return 0;
						}
					};
					getPackagePanels().setComparator(comp);
				}
				public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {}
			});
			sortByPopup.setDoubleBuffered(true);
		}
		return sortByPopup;
	}
	
	public JScrollPane getPackageScrollPane() {
		if (packageScrollPane == null) {
			packageScrollPane = new JScrollPane();
			packageScrollPane.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
					getPackagePanels().invalidate();
				}
			});
			packageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			packageScrollPane.getVerticalScrollBar().setUnitIncrement(8);
			packageScrollPane.setViewportView(getPackagePanels());
		}
		return packageScrollPane;
	}
	
	public PackagePanelList getPackagePanels() {
		if (packagePanels == null) {
			packagePanels = new PackagePanelList(this, new ObjectProcessor<Object, LinkedList<FilePackage>>() {
				public LinkedList<FilePackage> process(Object input) {
					return new LinkedList<FilePackage>(fileShare.packages.values());
				}
			});
		}
		return packagePanels;
	}
	
}