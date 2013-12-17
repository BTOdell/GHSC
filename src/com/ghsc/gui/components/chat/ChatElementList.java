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
	
	private Chat container;
	
	private ArrayList<ChatElement> elements;

	public ChatElementList(Chat container) {
		super();
		this.container = container;
		elements = new ArrayList<ChatElement>();
		setBackground(Color.WHITE);
		
		setLayout(new ChatLayout());
	}
	
	public Chat getChat() {
		return container;
	}
	
	public ChatElement get(int index) {
		return elements.get(index);
	}
	
	public int getCount() {
		return elements.size();
	}
	
	public void addElement(ChatElement element) {
		add(element);
		elements.add(element);
		repaint();
		revalidate();
	}
	
	public void clear() {
		removeAll();
		elements.clear();
		repaint();
	}
	
	public class ChatLayout implements LayoutManager {
		
		private int prefWidth, prefHeight, minWidth, minHeight;

		@Override
		public void addLayoutComponent(String name, Component comp) {}

		private void setSizes(Container parent) {
			int nComps = parent.getComponentCount();
			Dimension d = null;
			
			// reset widths and heights
			prefWidth = prefHeight = minWidth = minHeight = 0;
			
			Insets insets = parent.getInsets();
			
			prefWidth = container.getScrollPane().getViewport().getWidth();
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
			for (int i = 0; i < nComps; ++i) {
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