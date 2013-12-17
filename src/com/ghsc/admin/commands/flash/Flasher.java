package com.ghsc.admin.commands.flash;

import java.awt.Color;

import javax.swing.JPanel;

public class Flasher extends Thread {
	
	final JPanel contentPane;
	public boolean run = true;
	private Color[] colors = { Color.BLUE, Color.RED, Color.YELLOW, Color.ORANGE, Color.BLACK, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.WHITE, Color.PINK };
	private int colorIndex = 0, sleep = 50;
	
	public Flasher(JPanel contentPane) {
		this.contentPane = contentPane;
		start();
	}
	
	public Color getNext() {
		return colorIndex >= colors.length - 1 ? colors[colorIndex = 0] : colors[colorIndex++];
	}
	
	public void setSleep(int sleep) {
		this.sleep = sleep;
	}
	
	@Override
	public void run() {
		while (run) {
			contentPane.setBackground(getNext());
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {}
		}
	}
	
}