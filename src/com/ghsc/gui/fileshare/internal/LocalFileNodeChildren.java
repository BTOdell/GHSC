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
		this.endTag = new StringBuilder("</").append(getTagName()).append(">").toString();
	}

	@Override
	public String getTagName() {
		return TAGNAME;
	}
	
	@Override
	public String getEndTag() {
		return endTag;
	}

	@Override
	public void receive(Object o) {
		if (o instanceof Taggable) {
			Taggable t = (Taggable) o;
			switch (t.getTagName()) {
				case FileNode.TAGNAME_FILE:
				case FileNode.TAGNAME_DIR:
					add((LocalFileNode) t);
					break;
			}
		}
	}

	@Override
	public EndTaggable createForTag(Tag tag) {
		switch (tag.getName()) {
			case FileNode.TAGNAME_FILE:
			case FileNode.TAGNAME_DIR:
				final String path = tag.getAttribute(FileNode.ATT_PATH);
				if (path != null) {
					final File file = new File(path);
					return new LocalFileNode(this, file.exists() ? file : null);
				}
		}
		return null;
	}
	
}