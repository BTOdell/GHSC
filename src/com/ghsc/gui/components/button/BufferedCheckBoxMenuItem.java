package com.ghsc.gui.components.button;

public class BufferedCheckBoxMenuItem extends EnhancedCheckBoxMenuItem {
	
	private static final long serialVersionUID = 1L;
	
	private boolean enableBuffer = false;

	public BufferedCheckBoxMenuItem() {
		super();
	}
	
	public BufferedCheckBoxMenuItem(boolean stayOpen) {
		super(stayOpen);
	}
	
	public BufferedCheckBoxMenuItem(String text) {
		super(text);
	}
	
	public BufferedCheckBoxMenuItem(String text, boolean stayOpen) {
		super(text, stayOpen);
	}

	public boolean getBuffer() {
		return enableBuffer;
	}
	
	public void setBuffer(boolean enabled) {
		enableBuffer = enabled;
	}
	
	public void apply() {
		setSelected(enableBuffer);
	}
	
	public void setSelected(boolean enabled, boolean buffer) {
		setSelected(buffer ? enableBuffer = enabled : enabled);
	}
	
}