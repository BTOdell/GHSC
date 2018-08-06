package com.ghsc.gui.fileshare.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	private final ArrayList<PackagePanel> panels = new ArrayList<>();
	private Comparator<PackagePanel> comparator;
	
	public PackagePanelList(final FileShareFrame container, final ObjectProcessor<Object, LinkedList<FilePackage>> packageProvider) {
		super();
		this.container = container;
		this.packageProvider = packageProvider;
		
		this.setBackground(Color.WHITE);
		this.setLayout(new PackagePanelLayout());
	}

	public FileShareFrame getFileShareFrame() {
		return this.container;
	}
	
	public void setComparator(final Comparator<PackagePanel> comp) {
		this.comparator = comp;
		this.refresh();
	}
	
	/**
	 * Sync from the given package provider.
	 * Also calls {@link PackagePanelList#refresh()}
	 * Warning: Expensive operation.
	 */
	public void syncProvider() {
		final LinkedList<FilePackage> packages = this.packageProvider.process(null);
		synchronized (this.panels) {
			final LinkedList<PackagePanel> panelsCopy = new LinkedList<>(this.panels);
			final Iterator<FilePackage> it = packages.iterator();
			while (it.hasNext()) {
				final FilePackage test = it.next();
				if (test == null) {
					continue;
				}
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
					this.panels.add(new PackagePanel(this.container, test));
				}
			}
			this.panels.removeAll(panelsCopy);
			this.refresh();
		}
	}
	
	/**
	 * Sync panels with package data using {@link PackagePanel#sync()}
	 */
	public void syncPanels() {
		synchronized (this.panels) {
			for (final PackagePanel pp : this.panels) {
				if (pp == null) {
					continue;
				}
				pp.sync();
			}
			this.refresh();
		}
	}
	
	public void refresh() {
		synchronized (this.panels) {
			if (this.comparator != null) {
				this.panels.sort(this.comparator);
			}
			this.removeAll();
			for (final PackagePanel pp : this.panels) {
				if (pp == null) {
					continue;
				}
				this.add(pp);
			}
			this.repaint();
			this.revalidate();
		}
	}
	
	public PackagePanel get(final int index) {
		return this.panels.get(index);
	}
	
	public int getCount() {
		return this.panels.size();
	}
	
	public void addPanels(final PackagePanel... panels) {
		this.addPanelsSilently(panels);
		this.refresh();
	}
	
	public void addPanelsSilently(final PackagePanel... panels) {
		this.panels.addAll(Arrays.asList(panels));
	}
	
	public void removePanels(final PackagePanel... panels) {
		this.removePanelsSilently(panels);
		this.refresh();
	}
	
	public void removePanelsSilently(final PackagePanel... panels) {
		this.panels.removeAll(Arrays.asList(panels));
	}
	
	public void removePanels(final FilePackage... packages) {
		this.removePanelsSilently(packages);
		this.refresh();
	}
	
	public void removePanelsSilently(final FilePackage... packages) {
		// TODO ERROR
		this.panels.removeAll(Arrays.asList(packages));
	}
	
	public void clear() {
		this.removeAll();
		this.panels.clear();
		this.repaint();
		this.revalidate();
	}
	
	public class PackagePanelLayout implements LayoutManager {
		
		private int prefWidth;
        private int prefHeight;
        private int minWidth;
        private int minHeight;

		@Override
		public void addLayoutComponent(final String name, final Component comp) {}

		private void setSizes(final Container parent) {
			final int nComps = parent.getComponentCount();

			// reset widths and heights
			this.prefWidth = this.prefHeight = this.minWidth = this.minHeight = 0;
			
			final Insets insets = parent.getInsets();

			this.prefWidth = PackagePanelList.this.container.getPackageScrollPane().getViewport().getWidth();
			if (Debug.MAJOR.compareTo(Application.DEBUG) < 0) {
				System.out.println("Viewport width: " + this.prefWidth);
			}
			
			for (int i = 0; i < nComps; i++) {
				final Component c = parent.getComponent(i);
				if (c.isVisible()) {
					Dimension d = c.getPreferredSize();
					if (d == null) {
						d = c.getMinimumSize();
					}
					this.prefHeight += d.height;
				}
			}

			this.prefHeight += (insets.top + insets.bottom);
			this.minHeight = this.prefHeight;
			this.minWidth = this.prefWidth;
		}

		@Override
		public void layoutContainer(final Container parent) {
			this.setSizes(parent);
			
			final Insets insets = parent.getInsets();
			int y = insets.top;
			
			final int nComps = parent.getComponentCount();
			for (int i = 0; i < nComps; i++) {
				final Component comp = parent.getComponent(i);
				
				Dimension d = comp.getPreferredSize();
				if (d == null) {
					d = comp.getMinimumSize();
				}

				final int h = (d != null) ? d.height : 0;
				comp.setBounds(insets.left, y, this.prefWidth - insets.left - insets.right, h);
				y += h;
			}
		}

		@Override
		public Dimension minimumLayoutSize(final Container parent) {
			this.setSizes(parent);
			return new Dimension(this.minWidth, this.minHeight);
		}

		@Override
		public Dimension preferredLayoutSize(final Container parent) {
			this.setSizes(parent);
			return new Dimension(this.prefWidth, this.prefHeight);
		}

		@Override
		public void removeLayoutComponent(final Component comp) {}
	}
	
}