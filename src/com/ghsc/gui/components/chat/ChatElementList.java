package com.ghsc.gui.components.chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;

import javax.swing.JPanel;

import com.ghsc.common.Debug;
import com.ghsc.gui.Application;

public class ChatElementList extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final Chat container;
	private final ArrayList<ChatElement> elements;

	public ChatElementList(final Chat container) {
		super();
		this.container = container;
		this.elements = new ArrayList<>();
		this.setBackground(Color.WHITE);

		this.setLayout(new ChatLayout());
	}
	
	public Chat getChat() {
		return this.container;
	}
	
	public ChatElement get(final int index) {
		return this.elements.get(index);
	}
	
	public int getCount() {
		return this.elements.size();
	}
	
	public void addElement(final ChatElement element) {
		this.add(element);
		this.elements.add(element);
		this.repaint();
		this.revalidate();
	}
	
	public void clear() {
		this.removeAll();
		this.elements.clear();
		this.repaint();
	}
	
	public class ChatLayout implements LayoutManager {
		
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

			this.prefWidth = ChatElementList.this.container.getScrollPane().getViewport().getWidth();
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
			for (int i = 0; i < nComps; ++i) {
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