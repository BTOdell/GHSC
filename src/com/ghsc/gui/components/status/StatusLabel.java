package com.ghsc.gui.components.status;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.Timer;

public class StatusLabel extends JLabel {
	
	private static final long serialVersionUID = 1L;
	
	Timer timer = null;
	ActionListener event = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			setDefaultStatus();
		}
	};
	
	public StatusLabel(boolean initTimer) {
		super();
		if (initTimer) {
			timer = new Timer(0, event);
			timer.setRepeats(false);
		}
	}
	
	private void stopTimer() {
		if (timer.isRunning())
			timer.stop();
	}
	
	private void restartTimer(int delay) {
		stopTimer();
		timer.setInitialDelay(delay);
		timer.restart();
	}
	
	public void setStatus(String status) {
		if (status == null) {
			setDefaultStatus();
		} else {
			setText("Status: " + status);
		}
	}
	
	public void setDefaultStatus() {
		setStatus("Idle");
	}
	
	public void submit(String status, int period) {
		setStatus(status);
		stopTimer();
		if (status != null) {
			if (period > 0)
				restartTimer(period);
		}
	}
	
}