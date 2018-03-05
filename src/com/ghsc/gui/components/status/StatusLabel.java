package com.ghsc.gui.components.status;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.Timer;

public class StatusLabel extends JLabel {
	
	private static final long serialVersionUID = 1L;
	
	Timer timer;
	ActionListener event = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			StatusLabel.this.setDefaultStatus();
		}
	};
	
	public StatusLabel(boolean initTimer) {
		super();
		if (initTimer) {
			this.timer = new Timer(0, this.event);
			this.timer.setRepeats(false);
		}
	}
	
	private void stopTimer() {
		if (this.timer.isRunning()) {
			this.timer.stop();
        }
	}
	
	private void restartTimer(int delay) {
		this.stopTimer();
		this.timer.setInitialDelay(delay);
		this.timer.restart();
	}
	
	public void setStatus(String status) {
		if (status == null) {
			this.setDefaultStatus();
		} else {
			this.setText("Status: " + status);
		}
	}
	
	public void setDefaultStatus() {
		this.setStatus("Idle");
	}
	
	public void submit(String status, int period) {
		this.setStatus(status);
		this.stopTimer();
		if (status != null) {
			if (period > 0) {
				this.restartTimer(period);
            }
		}
	}
	
}