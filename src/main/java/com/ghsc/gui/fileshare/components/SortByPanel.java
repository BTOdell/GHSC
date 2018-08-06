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
 * TODO
 */
public class SortByPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	public interface PropertyCallback {
		void propertyChanged(SortByPanel source, String prop, Object obj);
	}
	
	private final String title;
	private final Comparator<PackagePanel> comparator;
	private final Set<PropertyCallback> propCallbacks = new CopyOnWriteArraySet<>();
	private boolean isActive;
	
	private JLabel upLabel;
	private JLabel downLabel;
	private JPanel canvas;
	private JLabel titleLabel;

	/**
	 * Create the panel.
	 */
	public SortByPanel(final String title, final Comparator<PackagePanel> comparator, final PropertyCallback callback) {
		super();
		this.title = title;
		this.comparator = comparator;
		this.propCallbacks.add(callback);

		this.initComponents();
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public Comparator<PackagePanel> getComparator() {
		return this.comparator;
	}
	
	public boolean isActive() {
		return this.isActive;
	}
	
	public void setActive(final boolean active) {
		if (this.isActive != active) {
			for (final PropertyCallback call : this.propCallbacks) {
				call.propertyChanged(this, "active", active);
			}
		}
		this.isActive = active;
		this.getUpLabel().setVisible(active);
		this.getDownLabel().setVisible(active);
	}
	
	public void addPropertyCallback(final PropertyCallback call) {
		this.propCallbacks.add(call);
	}
	
	private void initComponents() {
		this.setMinimumSize(new Dimension(200, 30));
		this.setPreferredSize(new Dimension(200, 30));
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(this.getCanvas(), GroupLayout.DEFAULT_SIZE, 168, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(this.getUpLabel(), Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)
						.addComponent(this.getDownLabel(), Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 28, GroupLayout.PREFERRED_SIZE)))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(this.getUpLabel())
					.addComponent(this.getDownLabel()))
				.addComponent(this.getCanvas(), GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
		);
		this.setLayout(groupLayout);
	}
	
	public JLabel getUpLabel() {
		if (this.upLabel == null) {
            this.upLabel = new JLabel("▲");
            this.upLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(final MouseEvent me) {
					for (final PropertyCallback call : SortByPanel.this.propCallbacks) {
						call.propertyChanged(SortByPanel.this, "order", true);
					}
				}
				public void mouseEntered(final MouseEvent me) {
                    SortByPanel.this.upLabel.setForeground(Color.BLACK);
				}
				public void mouseExited(final MouseEvent me) {
                    SortByPanel.this.upLabel.setForeground(Color.GRAY);
				}
			});
            this.upLabel.setForeground(Color.GRAY);
            this.upLabel.setBorder(new MatteBorder(1, 0, 0, 1, new Color(192, 192, 192)));
            this.upLabel.setPreferredSize(new Dimension(15, 15));
            this.upLabel.setMinimumSize(new Dimension(15, 15));
            this.upLabel.setHorizontalAlignment(SwingConstants.CENTER);
            this.upLabel.setVisible(false);
		}
		return this.upLabel;
	}
	
	public JLabel getDownLabel() {
		if (this.downLabel == null) {
            this.downLabel = new JLabel("▼");
            this.downLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(final MouseEvent me) {
					for (final PropertyCallback call : SortByPanel.this.propCallbacks) {
						call.propertyChanged(SortByPanel.this, "order", false);
					}
				}
				public void mouseEntered(final MouseEvent me) {
                    SortByPanel.this.downLabel.setForeground(Color.BLACK);
				}
				public void mouseExited(final MouseEvent me) {
                    SortByPanel.this.downLabel.setForeground(Color.GRAY);
				}
			});
            this.downLabel.setForeground(Color.GRAY);
            this.downLabel.setBorder(new MatteBorder(1, 0, 1, 1, new Color(192, 192, 192)));
            this.downLabel.setPreferredSize(new Dimension(15, 15));
            this.downLabel.setMinimumSize(new Dimension(15, 15));
            this.downLabel.setHorizontalAlignment(SwingConstants.CENTER);
            this.downLabel.setVisible(false);
		}
		return this.downLabel;
	}
	
	private void onCanvasPress() {
		System.out.println("Canvas pressed!");
		this.setActive(!this.isActive());
	}
	
	public JPanel getCanvas() {
		if (this.canvas == null) {
            this.canvas = new JPanel();
            this.canvas.addMouseListener(new MouseAdapter() {
				public void mousePressed(final MouseEvent e) {
					SortByPanel.this.onCanvasPress();
				}
			});
            this.canvas.setBorder(new MatteBorder(1, 1, 1, 1, new Color(192, 192, 192)));
			final GroupLayout gl_canvas = new GroupLayout(this.canvas);
			gl_canvas.setHorizontalGroup(
				gl_canvas.createParallelGroup(Alignment.LEADING)
					.addGroup(Alignment.TRAILING, gl_canvas.createSequentialGroup()
						.addContainerGap()
						.addComponent(this.getTitleLabel(), GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE))
			);
			gl_canvas.setVerticalGroup(
				gl_canvas.createParallelGroup(Alignment.LEADING)
					.addComponent(this.getTitleLabel(), GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
			);
            this.canvas.setLayout(gl_canvas);
		}
		return this.canvas;
	}
	
	public JLabel getTitleLabel() {
		if (this.titleLabel == null) {
            this.titleLabel = new JLabel(this.title);
            this.titleLabel.addMouseListener(new MouseAdapter() {
				public void mousePressed(final MouseEvent e) {
					SortByPanel.this.onCanvasPress();
				}
			});
            this.titleLabel.setFont(Fonts.GLOBAL);
		}
		return this.titleLabel;
	}
	
}