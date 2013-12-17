package com.ghsc.gui.fileshare.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import com.ghsc.common.Fonts;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class SortByPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	public interface PropertyCallback {
		public void propertyChanged(SortByPanel source, String prop, Object obj);
	}
	
	private String title;
	private Comparator<PackagePanel> comparator;
	private Set<PropertyCallback> propCallbacks = new CopyOnWriteArraySet<PropertyCallback>();
	private boolean isActive = false;
	
	private JLabel upLabel;
	private JLabel downLabel;
	private JPanel canvas;
	private JLabel titleLabel;

	/**
	 * Create the panel.
	 */
	public SortByPanel(String title, Comparator<PackagePanel> comparator, PropertyCallback callback) {
		super();
		this.title = title;
		this.comparator = comparator;
		this.propCallbacks.add(callback);
		
		initComponents();
	}
	
	public String getTitle() {
		return title;
	}
	
	public Comparator<PackagePanel> getComparator() {
		return comparator;
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public void setActive(boolean active) {
		if (this.isActive != active) {
			for (PropertyCallback call : propCallbacks) {
				call.propertyChanged(this, "active", active);
			}
		}
		this.isActive = active;
		this.getUpLabel().setVisible(active);
		this.getDownLabel().setVisible(active);
	}
	
	public void addPropertyCallback(PropertyCallback call) {
		this.propCallbacks.add(call);
	}
	
	private void initComponents() {
		setMinimumSize(new Dimension(200, 30));
		setPreferredSize(new Dimension(200, 30));
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(getCanvas(), GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(getUpLabel(), Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(getDownLabel(), Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(getUpLabel())
					.addComponent(getDownLabel()))
				.addComponent(getCanvas(), GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
		);
		setLayout(groupLayout);
	}
	
	public JLabel getUpLabel() {
		if (upLabel == null) {
			upLabel = new JLabel("▲");
			upLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent me) {
					for (PropertyCallback call : propCallbacks) {
						call.propertyChanged(SortByPanel.this, "order", true);
					}
				}
				public void mouseEntered(MouseEvent me) {
					upLabel.setForeground(Color.BLACK);
				}
				public void mouseExited(MouseEvent me) {
					upLabel.setForeground(Color.GRAY);
				}
			});
			upLabel.setForeground(Color.GRAY);
			upLabel.setBorder(new MatteBorder(1, 0, 0, 1, (Color) new Color(192, 192, 192)));
			upLabel.setPreferredSize(new Dimension(15, 15));
			upLabel.setMinimumSize(new Dimension(15, 15));
			upLabel.setHorizontalAlignment(SwingConstants.CENTER);
			upLabel.setVisible(false);
		}
		return upLabel;
	}
	
	public JLabel getDownLabel() {
		if (downLabel == null) {
			downLabel = new JLabel("▼");
			downLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent me) {
					for (PropertyCallback call : propCallbacks) {
						call.propertyChanged(SortByPanel.this, "order", false);
					}
				}
				public void mouseEntered(MouseEvent me) {
					downLabel.setForeground(Color.BLACK);
				}
				public void mouseExited(MouseEvent me) {
					downLabel.setForeground(Color.GRAY);
				}
			});
			downLabel.setForeground(Color.GRAY);
			downLabel.setBorder(new MatteBorder(1, 0, 1, 1, (Color) new Color(192, 192, 192)));
			downLabel.setPreferredSize(new Dimension(15, 15));
			downLabel.setMinimumSize(new Dimension(15, 15));
			downLabel.setHorizontalAlignment(SwingConstants.CENTER);
			downLabel.setVisible(false);
		}
		return downLabel;
	}
	
	private void onCanvasPress() {
		System.out.println("Canvas pressed!");
		setActive(!isActive());
	}
	
	public JPanel getCanvas() {
		if (canvas == null) {
			canvas = new JPanel();
			canvas.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					onCanvasPress();
				}
			});
			canvas.setBorder(new MatteBorder(1, 1, 1, 1, (Color) new Color(192, 192, 192)));
			GroupLayout gl_canvas = new GroupLayout(canvas);
			gl_canvas.setHorizontalGroup(
				gl_canvas.createParallelGroup(Alignment.LEADING)
					.addGroup(Alignment.TRAILING, gl_canvas.createSequentialGroup()
						.addContainerGap()
						.addComponent(getTitleLabel(), GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE))
			);
			gl_canvas.setVerticalGroup(
				gl_canvas.createParallelGroup(Alignment.LEADING)
					.addComponent(getTitleLabel(), GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
			);
			canvas.setLayout(gl_canvas);
		}
		return canvas;
	}
	
	public JLabel getTitleLabel() {
		if (titleLabel == null) {
			titleLabel = new JLabel(title);
			titleLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					onCanvasPress();
				}
			});
			titleLabel.setFont(Fonts.GLOBAL);
		}
		return titleLabel;
	}
	
}