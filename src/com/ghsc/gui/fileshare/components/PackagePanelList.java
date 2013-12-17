package com.ghsc.gui.fileshare.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JPanel;

import com.ghsc.common.Debug;
import com.ghsc.gui.Application;
import com.ghsc.gui.fileshare.FileShareFrame;
import com.ghsc.gui.fileshare.internal.FilePackage;
import com.ghsc.impl.ObjectProcessor;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class PackagePanelList extends JPanel {
	
	private static final long serialVersionUID = 1L;

	private final FileShareFrame container;
	private final ObjectProcessor<Object, LinkedList<FilePackage>> packageProvider;
	
	private ArrayList<PackagePanel> panels = new ArrayList<PackagePanel>();
	private Comparator<PackagePanel> comparator = null;
	
	public PackagePanelList(final FileShareFrame container, final ObjectProcessor<Object, LinkedList<FilePackage>> packageProvider) {
		super();
		this.container = container;
		this.packageProvider = packageProvider;
		
		this.setBackground(Color.WHITE);
		this.setLayout(new PackagePanelLayout());
	}

	public FileShareFrame getFileShareFrame() {
		return container;
	}
	
	public void setComparator(Comparator<PackagePanel> comp) {
		this.comparator = comp;
		refresh();
	}
	
	/**
	 * Sync from the given package provider.
	 * Also calls {@link PackagePanelList#refreshI()}
	 * Warning: Expensive operation.
	 */
	public void syncProvider() {
		final LinkedList<FilePackage> packages = packageProvider.process(null);
		synchronized (panels) {
			final LinkedList<PackagePanel> panelsCopy = new LinkedList<PackagePanel>(panels);
			final Iterator<FilePackage> it = packages.iterator();
			while (it.hasNext()) {
				final FilePackage test = it.next();
				if (test == null)
					continue;
				boolean found = false;
				final Iterator<PackagePanel> itp = panelsCopy.iterator();
				while (itp.hasNext()) {
					final PackagePanel pp = itp.next();
					final FilePackage pack = pp.getPackage();
					if (pack == test) {
						itp.remove();
						it.remove();
						found = true;
						break;
					}
				}
				if (!found) {
					panels.add(new PackagePanel(container, test));
				}
			}
			panels.removeAll(panelsCopy);
			refresh();
		}
	}
	
	/**
	 * Sync panels with package data using {@link PackagePanel#sync()}
	 */
	public void syncPanels() {
		synchronized (panels) {
			for (int i = 0; i < panels.size(); i++) {
				final PackagePanel pp = panels.get(i);
				if (pp == null)
					continue;
				pp.sync();
			}
			refresh();
		}
	}
	
	public void refresh() {
		synchronized (panels) {
			if (comparator != null) {
				Collections.sort(panels, comparator);
			}
			removeAll();
			for (int i = 0; i < panels.size(); i++) {
				PackagePanel pp = panels.get(i);
				if (pp == null)
					continue;
				add(pp);
			}
			repaint();
			revalidate();
		}
	}
	
	public PackagePanel get(int index) {
		return panels.get(index);
	}
	
	public int getCount() {
		return panels.size();
	}
	
	public void addPanels(PackagePanel... panels) {
		addPanelsSilently(panels);
		refresh();
	}
	
	public void addPanelsSilently(PackagePanel... panels) {
		this.panels.addAll(Arrays.asList(panels));
	}
	
	public void removePanels(PackagePanel... panels) {
		removePanelsSilently(panels);
		refresh();
	}
	
	public void removePanelsSilently(PackagePanel... panels) {
		this.panels.removeAll(Arrays.asList(panels));
	}
	
	public void removePanels(FilePackage... packages) {
		removePanelsSilently(packages);
		refresh();
	}
	
	public void removePanelsSilently(FilePackage... packages) {
		this.panels.removeAll(Arrays.asList(packages));
	}
	
	public void clear() {
		removeAll();
		panels.clear();
		repaint();
		revalidate();
	}
	
	public class PackagePanelLayout implements LayoutManager {
		
		private int prefWidth, prefHeight, minWidth, minHeight;

		@Override
		public void addLayoutComponent(String name, Component comp) {}

		private void setSizes(Container parent) {
			int nComps = parent.getComponentCount();
			Dimension d = null;
			
			// reset widths and heights
			prefWidth = prefHeight = minWidth = minHeight = 0;
			
			Insets insets = parent.getInsets();
			
			prefWidth = container.getPackageScrollPane().getViewport().getWidth();
			if (Debug.MAJOR.compareTo(Application.DEBUG) < 0)
				System.out.println("Viewport width: " + prefWidth);
			
			for (int i = 0; i < nComps; i++) {
				Component c = parent.getComponent(i);
				if (c.isVisible()) {
					d = c.getPreferredSize();
					if (d == null) 
						d = c.getMinimumSize();
					prefHeight += d.height;
				}
			}
			
			prefHeight += (insets.top + insets.bottom);
			minHeight = prefHeight;
			minWidth = prefWidth;
		}

		@Override
		public void layoutContainer(Container parent) {
			setSizes(parent);
			
			Insets insets = parent.getInsets();
			int y = insets.top;
			
			int nComps = parent.getComponentCount();
			for (int i = 0; i < nComps; i++) {
				Component comp = parent.getComponent(i);
				
				Dimension d = comp.getPreferredSize();
				if (d == null)
					d = comp.getMinimumSize();
				
				int h = (d != null) ? d.height : 0;
				comp.setBounds(insets.left, y, prefWidth - insets.left - insets.right, h);
				y += h;
			}
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			setSizes(parent);
			return new Dimension(minWidth, minHeight);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			setSizes(parent);
			return new Dimension(prefWidth, prefHeight);
		}

		@Override
		public void removeLayoutComponent(Component comp) {}
	}
	
}