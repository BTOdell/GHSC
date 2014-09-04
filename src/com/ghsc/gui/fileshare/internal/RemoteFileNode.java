package com.ghsc.gui.fileshare.internal;

import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.util.Tag;

public class RemoteFileNode extends FileNode {
	
	private final String name, path;
	private final long size;
	private final boolean directory;
	
	/**
	 * Creates a RemoteFile representing a file.
	 * @param name The name of the file on the remote system.
	 * @param path The path of the file on the remote system.
	 * @param size The size of the file on the remote system.
	 */
	public RemoteFileNode(final RemoteFileNodeChildren container, final String name, final String path, final long size) {
		super(container);
		this.name = name;
		this.path = path;
		this.size = Math.max(0, size);
		this.directory = size < 0;
		this.endTag = new StringBuilder("</").append(getTagName()).append(">").toString();
	}
	
	/**
	 * Creates a RemoteFile representing a directory.
	 * @param name The name of the file on the remote system.
	 * @param path The path of the file on the remote system.
	 */
	public RemoteFileNode(final RemoteFileNodeChildren container, final String name, final String path) {
		this(container, name, path, -1);
	}
	
	public RemoteFileNode getParent() {
		return (RemoteFileNode) parent;
	}
	
	public RemoteFileNodeChildren getContainer() {
		return (RemoteFileNodeChildren) container;
	}
	
	public void setContainer(RemoteFileNodeChildren container) {
		this.container = container;
	}
	
	public RemoteFileNodeChildren getChildren() {
		return (RemoteFileNodeChildren) children;
	}
	
	public void setChildren(final RemoteFileNodeChildren children) {
		this.children = children;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getPath() {
		return path;
	}
	
	@Override
	public boolean isDirectory() {
		return directory;
	}
	
	@Override
	public long getFileSize() {
		return size;
	}
	
	@Override
	public long getSize() {
		long temp = getFileSize();
		if (!isLeaf()) {
			for (RemoteFileNode node : getChildren())
				temp += node.getSize();
		}
		return temp;
	}
	
	@Override
	public long getFileCount() {
		long temp = isDirectory() ? 0 : 1;
		if (!isLeaf()) {
			for (RemoteFileNode node : getChildren())
				temp += node.getFileCount();
		}
		return temp;
	}

	@Override
	public long getDirectoryCount() {
		long temp = isDirectory() ? 1 : 0;
		if (!isLeaf()) {
			for (RemoteFileNode node : getChildren())
				temp += node.getDirectoryCount();
		}
		return temp;
	}
	
	/*
	 * Taggable implementation
	 */
	
	private final String endTag;
	
	@Override
	public String getTagName() {
		return directory ? TAGNAME_DIR : TAGNAME_FILE;
	}
	
	@Override
	public String getEndTag() {
		return endTag;
	}
	
	@Override
	public void receive(Object o) {
		if (o instanceof Taggable) {
			Taggable t = (Taggable) o;
			String tName = t.getTagName();
			if (tName != null && (tName.equals(FileNode.TAGNAME_FILE) || tName.equals(FileNode.TAGNAME_DIR))) {
				if (children == null) {
					setChildren(new RemoteFileNodeChildren(this));
				}
				getChildren().add((RemoteFileNode) t);
			}
		}
	}

	@Override
	public EndTaggable createForTag(Tag tag) {
		final String tagName = tag.getName();
		if (tagName != null && (tagName.equals(FileNode.TAGNAME_FILE) || tagName.equals(FileNode.TAGNAME_DIR))) {
			final String name = tag.getAttribute(FileNode.ATT_NAME);
			if (name == null)
				return null;
			final String path = tracePath(name);
			if (tagName.equals(FileNode.TAGNAME_FILE)) {
				final String size = tag.getAttribute(FileNode.ATT_SIZE);
				if (size != null) {
					if (children == null)
						setChildren(new RemoteFileNodeChildren(this));
					return new RemoteFileNode(getChildren(), name, path, Long.parseLong(size));
				}
			} else {
				if (children == null)
					setChildren(new RemoteFileNodeChildren(this));
				return new RemoteFileNode(getChildren(), name, path);
			}
		}
		return null;
	}
	
	String tracePath() {
		return tracePath(getName());
	}
	
	String tracePath(final String name) {
		return tracePath(getParent(), name);
	}
	
	static String tracePath(final RemoteFileNode parent, final String name) {
		final StringBuilder build = new StringBuilder();
		if (parent != null)
			build.append(parent.tracePath());
		build.append("\\"); // do not use File.separator
		build.append(name);
		return build.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		return this == o;
	}
	
	@Override
	public String toString() {
		return isRoot() ? getPath() : getName();
	}
	
}