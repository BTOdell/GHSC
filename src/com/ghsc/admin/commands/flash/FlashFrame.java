package com.ghsc.admin.commands.flash;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class FlashFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private Flasher flasher;
	private boolean keyListener;
	private JPanel canvas;
	
	/**
	 * Create the frame.
	 */
	public FlashFrame() {
		this(false);
	}
	
	public FlashFrame(boolean keyListener) {
		this.keyListener = keyListener;
		this.initComponents();
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.flasher = new Flasher(this.canvas);
		this.flasher.setSleep(10);
	}
	
	@Override
	public void dispose() {
		if (this.flasher != null) {
			this.flasher.run = false;
        }
		super.dispose();
	}
	
	private void initComponents() {
		if (this.keyListener) {
			this.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent arg0) {
					if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
						FlashFrame.this.dispose();
					}
				}
			});
		}
		this.setAlwaysOnTop(true);
		this.setResizable(false);
		this.setUndecorated(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.canvas = new JPanel();
		this.canvas.setDoubleBuffered(true);
		this.setContentPane(this.canvas);
	}
	
}