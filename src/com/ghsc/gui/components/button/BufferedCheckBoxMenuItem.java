package com.ghsc.gui.components.button;

public class BufferedCheckBoxMenuItem extends EnhancedCheckBoxMenuItem {
	
	private static final long serialVersionUID = 1L;
	
	private boolean enableBuffer;

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
		return this.enableBuffer;
	}
	
	public void setBuffer(boolean enabled) {
        this.enableBuffer = enabled;
	}
	
	public void apply() {
		this.setSelected(this.enableBuffer);
	}
	
	public void setSelected(boolean enabled, boolean buffer) {
		this.setSelected(buffer ? this.enableBuffer = enabled : enabled);
	}
	
}