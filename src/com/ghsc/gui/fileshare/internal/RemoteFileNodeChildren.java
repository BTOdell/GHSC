package com.ghsc.gui.fileshare.internal;

import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.util.Tag;

public class RemoteFileNodeChildren extends FileNodeChildren<RemoteFileNode> {
	
	private static final long serialVersionUID = 1L;
	
	private final String endTag;
	
	public RemoteFileNodeChildren(final RemoteFileNode parent) {
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
				this.add((RemoteFileNode) t);
			}
		}
	}

	@Override
	public EndTaggable createForTag(final Tag tag) {
		final String tagName = tag.getName();
		if (tagName != null && (tagName.equals(FileNode.TAGNAME_FILE) || tagName.equals(FileNode.TAGNAME_DIR))) {
			final String name = tag.getAttribute(FileNode.ATT_NAME);
			if (name == null) {
                return null;
            }
			final String path = RemoteFileNode.tracePath(this.getParent(), name);
			if (tagName.equals(FileNode.TAGNAME_FILE)) {
				final String size = tag.getAttribute(FileNode.ATT_SIZE);
				if (size != null) {
					return new RemoteFileNode(this, name, path, Long.parseLong(size));
				}
			} else {
				return new RemoteFileNode(this, name, path);
			}
		}
		return null;
	}
	
}