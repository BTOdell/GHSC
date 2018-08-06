package com.ghsc.admin.commands.flash;

import java.awt.Color;

import javax.swing.JPanel;

public class Flasher extends Thread {
	
	private final JPanel contentPane;
	public boolean run = true;
	private final Color[] colors = { Color.BLUE, Color.RED, Color.YELLOW, Color.ORANGE, Color.BLACK, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.WHITE, Color.PINK };
	private int colorIndex;
	private int sleep = 50;
	
	public Flasher(final JPanel contentPane) {
		this.contentPane = contentPane;
		this.start();
	}
	
	public Color getNext() {
		return this.colorIndex >= this.colors.length - 1 ? this.colors[this.colorIndex = 0] : this.colors[this.colorIndex++];
	}
	
	public void setSleep(final int sleep) {
		this.sleep = sleep;
	}
	
	@Override
	public void run() {
		while (this.run) {
            this.contentPane.setBackground(this.getNext());
			try {
				Thread.sleep(this.sleep);
			} catch (final InterruptedException ignored) {}
		}
	}
	
}