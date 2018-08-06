package com.ghsc.gui.components.status;

import javax.swing.*;

public class StatusLabel extends JLabel {
	
	private static final long serialVersionUID = 1L;
	
	private final Timer timer;

	public StatusLabel() {
		super();
		this.timer = new Timer(0, e -> this.setDefaultStatus());
		this.timer.setRepeats(false);
	}
	
	private void stopTimer() {
		if (this.timer.isRunning()) {
			this.timer.stop();
        }
	}
	
	private void restartTimer(final int delay) {
		this.stopTimer();
		this.timer.setInitialDelay(delay);
		this.timer.restart();
	}
	
	public void setStatus(final String status) {
		if (status == null) {
			this.setDefaultStatus();
		} else {
			this.setText("Status: " + status);
		}
	}
	
	public void setDefaultStatus() {
		this.setStatus("Idle");
	}
	
	public void submit(final String status, final int period) {
		this.setStatus(status);
		this.stopTimer();
		if (status != null) {
			if (period > 0) {
				this.restartTimer(period);
            }
		}
	}
	
}