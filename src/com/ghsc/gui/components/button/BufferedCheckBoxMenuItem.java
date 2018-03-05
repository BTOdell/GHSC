package com.ghsc.gui.components.button;

public class BufferedCheckBoxMenuItem extends EnhancedCheckBoxMenuItem {
	
	private static final long serialVersionUID = 1L;
	
	private boolean enableBuffer;

	public BufferedCheckBoxMenuItem() {
		super();
	}
	
	public BufferedCheckBoxMenuItem(final boolean stayOpen) {
		super(stayOpen);
	}
	
	public BufferedCheckBoxMenuItem(final String text) {
		super(text);
	}
	
	public BufferedCheckBoxMenuItem(final String text, final boolean stayOpen) {
		super(text, stayOpen);
	}

	public boolean getBuffer() {
		return this.enableBuffer;
	}
	
	public void setBuffer(final boolean enabled) {
        this.enableBuffer = enabled;
	}
	
	public void apply() {
		this.setSelected(this.enableBuffer);
	}
	
	public void setSelected(final boolean enabled, final boolean buffer) {
		this.setSelected(buffer ? this.enableBuffer = enabled : enabled);
	}
	
}