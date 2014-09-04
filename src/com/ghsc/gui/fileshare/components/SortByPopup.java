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
	
	private ArrayList<SortByPanel> activePanels = new ArrayList<SortByPanel>();
	private ArrayList<SortByPanel> nonActivePanels = new ArrayList<SortByPanel>();
	private Object panelLock = new Object();
	
	private final JToggleButton sourceButton;
	
	private SortByPanel ownerAZ, ownerZA, newest, oldest, mPop, lPop;
	
	public SortByPopup(final JToggleButton srcButton) {
		super();
		this.sourceButton = srcButton;
		
		initComponents();
	}
	
	public Set<Comparator<PackagePanel>> getSortingComparators() {
		final HashSet<Comparator<PackagePanel>> set = new HashSet<Comparator<PackagePanel>>(activePanels.size());
		for (int i = 0; i < activePanels.size(); i++) {
			final SortByPanel p = activePanels.get(i);
			if (p == null)
				continue;
			set.add(p.getComparator());
		}
		return set;
	}
	
	public void applyPanels() {
		synchronized (panelLock) {
			applyPanelsI();
		}
	}
	
	void applyPanelsI() {
		removeAll();
		for (int i = 0; i < activePanels.size(); i++) {
			SortByPanel p = activePanels.get(i);
			if (p == null)
				continue;
			add(p);
		}
		addSeparator();
		for (int i = 0; i < nonActivePanels.size(); i++) {
			SortByPanel p = nonActivePanels.get(i);
			if (p == null)
				continue;
			add(p);
		}
	}
	
	private void initComponents() {
		addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent pme) {
				sourceButton.setSelected(false);
			}
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				sourceButton.setSelected(false);
			}
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
		});
		
		final SortByPanel.PropertyCallback pCall = new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if (prop == null)
					return;
				if (prop.equals("active")) {
					boolean active = (boolean)(Boolean) obj;
					synchronized (panelLock) {
						if (active) {
							nonActivePanels.remove(source);
							activePanels.add(source);
						} else {
							activePanels.remove(source);
							nonActivePanels.add(source);
						}
						applyPanelsI();
					}
				} else if (prop.equals("order")) {
					boolean up = (boolean)(Boolean) obj;
					synchronized (panelLock) {
						int index = activePanels.indexOf(source);
						if ((up && index <= 0) || (!up && index >= activePanels.size() - 1))
							return;
						activePanels.remove(index);
						activePanels.add(index + (up ? -1 : 1), source);
						applyPanelsI();
					}
					revalidate();
				}
			}
		};
		
		ownerAZ = new SortByPanel("Owner (A-Z)", new Comparator<PackagePanel>() {
			public int compare(PackagePanel p, PackagePanel pp) {
				if (p == null || pp == null)
					return 0;
				FilePackage pP = p.getPackage(), ppP = pp.getPackage();
				if (pP == null || ppP == null)
					return 0;
				return pP.getOwner().compareTo(ppP.getOwner());
			}
		}, pCall);
		ownerAZ.addPropertyCallback(new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if ("active".equals(prop) && (boolean)(Boolean) obj) {
					if (ownerZA.isActive())
						ownerZA.setActive(false);
				}
			}
		});
		ownerZA = new SortByPanel("Owner (Z-A)", new Comparator<PackagePanel>() {
			public int compare(PackagePanel p, PackagePanel pp) {
				if (p == null || pp == null)
					return 0;
				FilePackage pP = p.getPackage(), ppP = pp.getPackage();
				if (pP == null || ppP == null)
					return 0;
				return ppP.getOwner().compareTo(pP.getOwner());
			}
		}, pCall);
		ownerZA.addPropertyCallback(new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if ("active".equals(prop) && (boolean)(Boolean) obj) {
					if (ownerAZ.isActive())
						ownerAZ.setActive(false);
				}
			}
		});
		newest = new SortByPanel("Creation date (newest)", new Comparator<PackagePanel>() {
			public int compare(PackagePanel p, PackagePanel pp) {
				if (p == null || pp == null)
					return 0;
				FilePackage pP = p.getPackage(), ppP = pp.getPackage();
				if (pP == null || ppP == null)
					return 0;
				return ppP.getCreationDate().compareTo(pP.getCreationDate());
			}
		}, pCall);
		newest.addPropertyCallback(new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if ("active".equals(prop) && (boolean)(Boolean) obj) {
					if (oldest.isActive())
						oldest.setActive(false);
				}
			}
		});
		oldest = new SortByPanel("Creation date (oldest)", new Comparator<PackagePanel>() {
			public int compare(PackagePanel p, PackagePanel pp) {
				if (p == null || pp == null)
					return 0;
				FilePackage pP = p.getPackage(), ppP = pp.getPackage();
				if (pP == null || ppP == null)
					return 0;
				return pP.getCreationDate().compareTo(ppP.getCreationDate());
			}
		}, pCall);
		oldest.addPropertyCallback(new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if ("active".equals(prop) && (boolean)(Boolean) obj) {
					if (newest.isActive())
						newest.setActive(false);
				}
			}
		});
		mPop = new SortByPanel("Popularity (most)", new Comparator<PackagePanel>() {
			public int compare(PackagePanel p, PackagePanel pp) {
				if (p == null || pp == null)
					return 0;
				FilePackage pP = p.getPackage(), ppP = pp.getPackage();
				if (pP == null || ppP == null)
					return 0;
				return (int)(pP.getDownloadCount() - ppP.getDownloadCount());
			}
		}, pCall);
		mPop.addPropertyCallback(new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if ("active".equals(prop) && (boolean)(Boolean) obj) {
					if (lPop.isActive())
						lPop.setActive(false);
				}
			}
		});
		lPop = new SortByPanel("Popularity (least)", new Comparator<PackagePanel>() {
			public int compare(PackagePanel p, PackagePanel pp) {
				if (p == null || pp == null)
					return 0;
				FilePackage pP = p.getPackage(), ppP = pp.getPackage();
				if (pP == null || ppP == null)
					return 0;
				return (int)(ppP.getDownloadCount() - pP.getDownloadCount());
			}
		}, pCall);
		lPop.addPropertyCallback(new SortByPanel.PropertyCallback() {
			public void propertyChanged(SortByPanel source, String prop, Object obj) {
				if ("active".equals(prop) && (boolean)(Boolean) obj) {
					if (mPop.isActive()) {
						mPop.setActive(false);
					}
				}
			}
		});
		
		nonActivePanels.add(ownerAZ);
		nonActivePanels.add(ownerZA);
		nonActivePanels.add(newest);
		nonActivePanels.add(oldest);
		nonActivePanels.add(mPop);
		nonActivePanels.add(lPop);
		Node sortByNode = Settings.getSettings().search("/fileshare/sort");
		if (sortByNode != null) {
			String sortByData = sortByNode.getData();
			if (sortByData != null) {
				String[] sortBys = sortByData.split(Pattern.quote(","));
				SortByPanel[] panels = nonActivePanels.toArray(new SortByPanel[nonActivePanels.size()]);
				for (String title : sortBys) {
					for (SortByPanel p : panels) {
						if (title.equals(p.getTitle())) {
							p.setActive(true);
						}
					}
				}
			}
		}
		applyPanels();
	}
	
	public String printSort() {
		StringBuilder build = new StringBuilder();
		synchronized (panelLock) {
			for (SortByPanel p : activePanels) {
				build.append(p.getTitle());
				build.append(',');
			}
		}
		if (build.length() > 0)
			build.deleteCharAt(build.length() - 1);
		return build.toString();
	}
	
}