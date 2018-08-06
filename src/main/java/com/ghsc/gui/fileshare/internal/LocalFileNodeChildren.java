package com.ghsc.gui.fileshare.internal;

import java.io.File;

import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.util.Tag;

public class LocalFileNodeChildren extends FileNodeChildren<LocalFileNode> {
	
	private static final long serialVersionUID = 1L;
	
	private final String endTag;
	
	public LocalFileNodeChildren(final LocalFileNode parent) {
		super(parent);
		this.endTag = "</" + this.getTagName() + ">";
	}

	@Override
	public String getTagName() {
		return TAGNAME;
	}
	
	@Override
	public String getEndTag() {
		return this.endTag;
	}

	@Override
	public void receive(final Object o) {
		if (o instanceof Taggable) {
			final Taggable t = (Taggable) o;
			final String tName = t.getTagName();
			if (tName != null && (tName.equals(FileNode.TAGNAME_FILE) || tName.equals(FileNode.TAGNAME_DIR))) {
				this.add((LocalFileNode) t);
			}
		}
	}

	@Override
	public EndTaggable createForTag(final Tag tag) {
		final String tName = tag.getName();
		if (tName != null && (tName.equals(FileNode.TAGNAME_FILE) || tName.equals(FileNode.TAGNAME_DIR))) {
			final String path = tag.getAttribute(FileNode.ATT_PATH);
			if (path != null) {
				final File file = new File(path);
				return new LocalFileNode(this, file.exists() ? file : null);
			}
		}
		return null;
	}
	
}