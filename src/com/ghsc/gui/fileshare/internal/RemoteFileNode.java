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
		this.endTag = new StringBuilder("</").append(this.getTagName()).append(">").toString();
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
		return (RemoteFileNode) this.parent;
	}
	
	public RemoteFileNodeChildren getContainer() {
		return (RemoteFileNodeChildren) this.container;
	}
	
	public void setContainer(RemoteFileNodeChildren container) {
		this.container = container;
	}
	
	public RemoteFileNodeChildren getChildren() {
		return (RemoteFileNodeChildren) this.children;
	}
	
	public void setChildren(final RemoteFileNodeChildren children) {
		this.children = children;
	}
	
	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public String getPath() {
		return this.path;
	}
	
	@Override
	public boolean isDirectory() {
		return this.directory;
	}
	
	@Override
	public long getFileSize() {
		return this.size;
	}
	
	@Override
	public long getSize() {
		long temp = this.getFileSize();
		if (!this.isLeaf()) {
			for (RemoteFileNode node : this.getChildren()) {
                temp += node.getSize();
            }
		}
		return temp;
	}
	
	@Override
	public long getFileCount() {
		long temp = this.isDirectory() ? 0 : 1;
		if (!this.isLeaf()) {
			for (RemoteFileNode node : this.getChildren()) {
                temp += node.getFileCount();
            }
		}
		return temp;
	}

	@Override
	public long getDirectoryCount() {
		long temp = this.isDirectory() ? 1 : 0;
		if (!this.isLeaf()) {
			for (RemoteFileNode node : this.getChildren()) {
                temp += node.getDirectoryCount();
            }
		}
		return temp;
	}
	
	/*
	 * Taggable implementation
	 */
	
	private final String endTag;
	
	@Override
	public String getTagName() {
		return this.directory ? TAGNAME_DIR : TAGNAME_FILE;
	}
	
	@Override
	public String getEndTag() {
		return this.endTag;
	}
	
	@Override
	public void receive(Object o) {
		if (o instanceof Taggable) {
			Taggable t = (Taggable) o;
			String tName = t.getTagName();
			if (tName != null && (tName.equals(FileNode.TAGNAME_FILE) || tName.equals(FileNode.TAGNAME_DIR))) {
				if (this.children == null) {
					this.setChildren(new RemoteFileNodeChildren(this));
				}
				this.getChildren().add((RemoteFileNode) t);
			}
		}
	}

	@Override
	public EndTaggable createForTag(Tag tag) {
		final String tagName = tag.getName();
		if (tagName != null && (tagName.equals(FileNode.TAGNAME_FILE) || tagName.equals(FileNode.TAGNAME_DIR))) {
			final String name = tag.getAttribute(FileNode.ATT_NAME);
			if (name == null) {
                return null;
            }
			final String path = this.tracePath(name);
			if (tagName.equals(FileNode.TAGNAME_FILE)) {
				final String size = tag.getAttribute(FileNode.ATT_SIZE);
				if (size != null) {
					if (this.children == null) {
						this.setChildren(new RemoteFileNodeChildren(this));
                    }
					return new RemoteFileNode(this.getChildren(), name, path, Long.parseLong(size));
				}
			} else {
				if (this.children == null) {
					this.setChildren(new RemoteFileNodeChildren(this));
                }
				return new RemoteFileNode(this.getChildren(), name, path);
			}
		}
		return null;
	}
	
	String tracePath() {
		return this.tracePath(this.getName());
	}
	
	String tracePath(final String name) {
		return tracePath(this.getParent(), name);
	}
	
	static String tracePath(final RemoteFileNode parent, final String name) {
		final StringBuilder build = new StringBuilder();
		if (parent != null) {
            build.append(parent.tracePath());
        }
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
		return this.isRoot() ? this.getPath() : this.getName();
	}
	
}