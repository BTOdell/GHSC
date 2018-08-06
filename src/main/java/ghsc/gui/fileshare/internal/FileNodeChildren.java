package ghsc.gui.fileshare.internal;

import java.util.ArrayList;

import ghsc.impl.EndTaggable;

public abstract class FileNodeChildren<E extends FileNode> extends ArrayList<E> implements EndTaggable {
	
	private static final long serialVersionUID = 1L;
	
	public static final String TAGNAME = "c";
	
	final E parent;
	
	public FileNodeChildren(final E parent) {
		this.parent = parent;
	}
	
	public E getParent() {
		return this.parent;
	}
	
}