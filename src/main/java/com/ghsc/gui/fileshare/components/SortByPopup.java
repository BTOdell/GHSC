package com.ghsc.gui.fileshare.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.ghsc.files.FileStorage.Node;
import com.ghsc.files.Settings;
import com.ghsc.gui.fileshare.internal.FilePackage;

public class SortByPopup extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	
	private final ArrayList<SortByPanel> activePanels = new ArrayList<>();
	private final ArrayList<SortByPanel> nonActivePanels = new ArrayList<>();
	private final Object panelLock = new Object();
	
	private final JToggleButton sourceButton;
	
	private SortByPanel ownerAZ;
	private SortByPanel ownerZA;
	private SortByPanel newest;
	private SortByPanel oldest;
	private SortByPanel mPop;
	private SortByPanel lPop;
	
	public SortByPopup(final JToggleButton srcButton) {
		super();
		this.sourceButton = srcButton;

		this.initComponents();
	}
	
	public Set<Comparator<PackagePanel>> getSortingComparators() {
		final HashSet<Comparator<PackagePanel>> set = new HashSet<>(this.activePanels.size());
		for (final SortByPanel p : this.activePanels) {
			if (p == null) {
				continue;
			}
			set.add(p.getComparator());
		}
		return set;
	}
	
	private void applyPanels() {
		synchronized (this.panelLock) {
			this.applyPanelsSync();
		}
	}
	
	private void applyPanelsSync() {
		this.removeAll();
		for (final SortByPanel p : this.activePanels) {
			if (p == null) {
				continue;
			}
			this.add(p);
		}
		this.addSeparator();
		for (final SortByPanel p : this.nonActivePanels) {
			if (p == null) {
				continue;
			}
			this.add(p);
		}
	}
	
	private void initComponents() {
		this.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(final PopupMenuEvent pme) {
				SortByPopup.this.sourceButton.setSelected(false);
			}
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
				SortByPopup.this.sourceButton.setSelected(false);
			}
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {}
		});
		
		final SortByPanel.PropertyCallback pCall = (source, prop, obj) -> {
            if (prop == null) {
                return;
            }
            if ("active".equals(prop)) {
                final boolean active = (boolean)(Boolean) obj;
                synchronized (this.panelLock) {
                    if (active) {
                        this.nonActivePanels.remove(source);
                        this.activePanels.add(source);
                    } else {
                        this.activePanels.remove(source);
                        this.nonActivePanels.add(source);
                    }
                    this.applyPanelsSync();
                }
            } else if ("order".equals(prop)) {
                final boolean up = (boolean)(Boolean) obj;
                synchronized (this.panelLock) {
                    final int index = this.activePanels.indexOf(source);
                    if ((up && index <= 0) || (!up && index >= this.activePanels.size() - 1)) {
                        return;
                    }
                    this.activePanels.remove(index);
                    this.activePanels.add(index + (up ? -1 : 1), source);
                    this.applyPanelsSync();
                }
                this.revalidate();
            }
        };

		this.ownerAZ = new SortByPanel("Owner (A-Z)", (p, pp) -> {
            if (p == null || pp == null) {
                return 0;
            }
            final FilePackage pP = p.getPackage();
            final FilePackage ppP = pp.getPackage();
            if (pP == null || ppP == null) {
                return 0;
            }
            return pP.getOwner().compareTo(ppP.getOwner());
        }, pCall);
		this.ownerAZ.addPropertyCallback((source, prop, obj) -> {
            if ("active".equals(prop) && (Boolean) obj) {
                if (this.ownerZA.isActive()) {
                    this.ownerZA.setActive(false);
                }
            }
        });
		this.ownerZA = new SortByPanel("Owner (Z-A)", (p, pp) -> {
            if (p == null || pp == null) {
                return 0;
            }
            final FilePackage pP = p.getPackage();
            final FilePackage ppP = pp.getPackage();
            if (pP == null || ppP == null) {
                return 0;
            }
            return ppP.getOwner().compareTo(pP.getOwner());
        }, pCall);
		this.ownerZA.addPropertyCallback((source, prop, obj) -> {
            if ("active".equals(prop) && (Boolean) obj) {
                if (this.ownerAZ.isActive()) {
                    this.ownerAZ.setActive(false);
                }
            }
        });
		this.newest = new SortByPanel("Creation date (newest)", (p, pp) -> {
            if (p == null || pp == null) {
                return 0;
            }
            final FilePackage pP = p.getPackage();
            final FilePackage ppP = pp.getPackage();
            if (pP == null || ppP == null) {
                return 0;
            }
            return ppP.getCreationDate().compareTo(pP.getCreationDate());
        }, pCall);
		this.newest.addPropertyCallback((source, prop, obj) -> {
            if ("active".equals(prop) && (Boolean) obj) {
                if (this.oldest.isActive()) {
                    this.oldest.setActive(false);
                }
            }
        });
		this.oldest = new SortByPanel("Creation date (oldest)", (p, pp) -> {
            if (p == null || pp == null) {
                return 0;
            }
            final FilePackage pP = p.getPackage();
            final FilePackage ppP = pp.getPackage();
            if (pP == null || ppP == null) {
                return 0;
            }
            return pP.getCreationDate().compareTo(ppP.getCreationDate());
        }, pCall);
		this.oldest.addPropertyCallback((source, prop, obj) -> {
            if ("active".equals(prop) && (Boolean) obj) {
                if (this.newest.isActive()) {
                    this.newest.setActive(false);
                }
            }
        });
		this.mPop = new SortByPanel("Popularity (most)", (p, pp) -> {
            if (p == null || pp == null) {
                return 0;
            }
            final FilePackage pP = p.getPackage();
            final FilePackage ppP = pp.getPackage();
            if (pP == null || ppP == null) {
                return 0;
            }
            return (int)(pP.getDownloadCount() - ppP.getDownloadCount());
        }, pCall);
		this.mPop.addPropertyCallback((source, prop, obj) -> {
            if ("active".equals(prop) && (Boolean) obj) {
                if (this.lPop.isActive()) {
                    this.lPop.setActive(false);
                }
            }
        });
		this.lPop = new SortByPanel("Popularity (least)", (p, pp) -> {
            if (p == null || pp == null) {
                return 0;
            }
            final FilePackage pP = p.getPackage();
            final FilePackage ppP = pp.getPackage();
            if (pP == null || ppP == null) {
                return 0;
            }
            return (int)(ppP.getDownloadCount() - pP.getDownloadCount());
        }, pCall);
		this.lPop.addPropertyCallback((source, prop, obj) -> {
            if ("active".equals(prop) && (Boolean) obj) {
                if (this.mPop.isActive()) {
                    this.mPop.setActive(false);
                }
            }
        });

		this.nonActivePanels.add(this.ownerAZ);
		this.nonActivePanels.add(this.ownerZA);
		this.nonActivePanels.add(this.newest);
		this.nonActivePanels.add(this.oldest);
		this.nonActivePanels.add(this.mPop);
		this.nonActivePanels.add(this.lPop);
		final Node sortByNode = Settings.getSettings().search("/fileshare/sort");
		if (sortByNode != null) {
			final String sortByData = sortByNode.getData();
			if (sortByData != null) {
				final String[] sortBys = sortByData.split(Pattern.quote(","));
				final SortByPanel[] panels = this.nonActivePanels.toArray(new SortByPanel[0]);
				for (final String title : sortBys) {
					for (final SortByPanel p : panels) {
						if (title.equals(p.getTitle())) {
							p.setActive(true);
						}
					}
				}
			}
		}
		this.applyPanels();
	}
	
	public String printSort() {
		final StringBuilder build = new StringBuilder();
		synchronized (this.panelLock) {
			for (final SortByPanel p : this.activePanels) {
				build.append(p.getTitle());
				build.append(',');
			}
		}
		if (build.length() > 0) {
			build.deleteCharAt(build.length() - 1);
		}
		return build.toString();
	}
	
}