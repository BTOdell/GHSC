package com.ghsc.admin.commands.flash;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class FlashFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private Flasher flasher = null;
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
		initComponents();
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		flasher = new Flasher(canvas);
		flasher.setSleep(10);
	}
	
	@Override
	public void dispose() {
		if (flasher != null) 
			flasher.run = false;
		super.dispose();
	}
	
	private void initComponents() {
		if (keyListener) {
			addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent arg0) {
					if (arg0.getKeyCode() == KeyEvent.VK_ESCAPE) {
						dispose();
					}
				}
			});
		}
		setAlwaysOnTop(true);
		setResizable(false);
		setUndecorated(true);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		canvas = new JPanel();
		canvas.setDoubleBuffered(true);
		setContentPane(canvas);
	}
	
}