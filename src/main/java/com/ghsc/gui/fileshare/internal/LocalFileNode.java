package com.ghsc.gui.fileshare.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.util.Tag;

public class LocalFileNode extends FileNode {
	
	private final File file;
	
	/**
	 * Creates a LocalFile that represents a local file.
	 * @param file the file on the local file system.
	 */
	public LocalFileNode(final LocalFileNodeChildren container, final File file) {
		super(container);
		this.file = file;
		this.endTag = "</" + this.getTagName() + ">";
	}
	
	public final File getFile() {
		return this.file;
	}
	
	public LocalFileNode getParent() {
		return (LocalFileNode) this.parent;
	}
	
	public LocalFileNodeChildren getContainer() {
		return (LocalFileNodeChildren) this.container;
	}
	
	public void setContainer(final LocalFileNodeChildren container) {
		this.container = container;
	}
	
	public LocalFileNodeChildren getChildren() {
		return (LocalFileNodeChildren) this.children;
	}
	
	public void setChildren(final LocalFileNodeChildren children) {
		this.children = children;
	}

	@Override
	public String getName() {
		return this.file != null ? this.file.getName() : null;
	}
	
	@Override
	public String getPath() {
		return this.file != null ? this.file.getPath() : null;
	}
	
	@Override
	public boolean isDirectory() {
		return this.file != null && this.file.isDirectory();
	}
	
	@Override
	public long getFileSize() {
		return this.file != null ? this.file.length() : 0;
	}
	
	@Override
	public long getSize() {
		long temp = this.getFileSize();
		if (!this.isLeaf()) {
			for (final LocalFileNode node : this.getChildren()) {
				temp += node.getSize();
			}
		}
		return temp;
	}
	
	@Override
	public long getFileCount() {
		long temp = this.isDirectory() ? 0 : 1;
		if (!this.isLeaf()) {
			for (final LocalFileNode node : this.getChildren()) {
				temp += node.getFileCount();
			}
		}
		return temp;
	}

	@Override
	public long getDirectoryCount() {
		long temp = this.isDirectory() ? 1 : 0;
		if (!this.isLeaf()) {
			for (final LocalFileNode node : this.getChildren()) {
				temp += node.getDirectoryCount();
			}
		}
		return temp;
	}
	
	protected String concat() {
		final StringBuilder build = new StringBuilder(this.toString());
		if (!this.isLeaf()) {
			for (final LocalFileNode node : this.getChildren()) {
				build.append(node.concat());
			}
		}
		return build.toString();
	}
	
	public InputStream openInputStream() {
		if (this.file != null) {
			try {
				return new FileInputStream(this.file);
			} catch (final FileNotFoundException ignored) {}
		}
		return null;
	}
	
	/*
	 * Remote package related methods
	 */
	
	public String toRemoteMeta() {
		final StringBuilder build = new StringBuilder();
		{
			final LinkedList<Object> objs = new LinkedList<>();
			objs.add(FileNode.ATT_NAME);
			objs.add(this.getName());
			final boolean isDir = this.isDirectory();
			if (!isDir) {
				objs.add(FileNode.ATT_SIZE);
				objs.add(this.getSize());
			}
			final String tagName = this.getTagName();
			build.append(Tag.construct(tagName, objs.toArray()).getEncodedString());
			if (isDir && !this.isLeaf()) {
				for (final LocalFileNode lc : this.getChildren()) {
					build.append(lc.toRemoteMeta());
				}
			}
			build.append("</").append(tagName).append(">");
		}
		return build.toString();
	}
	
	/*
	 * Save related methods
	 */
	
	public String toSaveMeta() {
		final StringBuilder build = new StringBuilder();
		{
			final LinkedList<Object> objs = new LinkedList<>();
			objs.add(FileNode.ATT_PATH);
			objs.add(this.getPath());
			final String tagName = this.getTagName();
			build.append(Tag.construct(tagName, objs.toArray()).getEncodedString());
			if (this.isDirectory() && !this.isLeaf()) {
				for (final LocalFileNode lc : this.getChildren()) {
					build.append(lc.toSaveMeta());
				}
			}
			build.append("</").append(tagName).append(">");
		}
		return build.toString();
	}
	
	/**
	 * Finds the LocalFile given a path relative to the node.
	 */
	public LocalFileNode findFile(final String relativePath) {
		return findFile(relativePath, this.getChildren());
	}
	
	/**
	 * Finds the LocalFile given a relative path and nodes.
	 * Example: \pictures\2012\image.png
	 */
	static LocalFileNode findFile(String relativePath, final List<LocalFileNode> nodes) {
		relativePath = relativePath.substring(1);
		final int index = relativePath.indexOf(File.separatorChar);
		final String nodeName = relativePath.substring(0, index >= 0 ? index : relativePath.length());
		final String newRelativePath = relativePath.substring(index);
		for (final LocalFileNode node : nodes) {
			if (nodeName.equals(node.getFile().getName())) {
				return node.findFile(newRelativePath);
			}
		}
		return null;
	}
	
	public static LocalFileNode generateRoot(final File file) {
		return generate(null, file);
	}
	
	private static LocalFileNode generate(final LocalFileNodeChildren container, final File file) {
		final LocalFileNode node = new LocalFileNode(container, file);
		if (node.isDirectory()) {
			final File[] files = file.listFiles();
			if (files != null && files.length > 0) {
				final LocalFileNodeChildren nodes = new LocalFileNodeChildren(node);
				for (final File checkFile : files) {
					nodes.add(LocalFileNode.generate(nodes, checkFile));
				}
				node.setChildren(nodes);
			}
		}
		return node;
	}
	
	/*
	 * Taggable implementation
	 */
	
	private final String endTag;
	
	@Override
	public String getTagName() {
		return this.isDirectory() ? TAGNAME_DIR : TAGNAME_FILE;
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
				if (this.children == null) {
					this.setChildren(new LocalFileNodeChildren(this));
				}
				this.getChildren().add((LocalFileNode) t);
			}
		}
	}

	@Override
	public EndTaggable createForTag(final Tag tag) {
		final String tName = tag.getName();
		if (tName != null && (tName.equals(FileNode.TAGNAME_FILE) || tName.equals(FileNode.TAGNAME_DIR))) {
			final String path = tag.getAttribute(FileNode.ATT_PATH);
			if (path != null) {
				if (this.children == null) {
					this.setChildren(new LocalFileNodeChildren(this));
				}
				final File file = new File(path);
				return new LocalFileNode(this.getChildren(), file.exists() ? file : null);
			}
		}
		return null;
	}
	
	/**
	 * Clones the entire node tree leaving this node as the root.
	 */
	@Override
	public LocalFileNode clone() {
		return this.clone(null);
	}
	
	/**
	 * Clones the entire node tree.
	 */
	public LocalFileNode clone(final LocalFileNodeChildren container) {
		final LocalFileNode node = new LocalFileNode(container, this.file);
		if (!this.isLeaf()) {
			final LocalFileNodeChildren nodes = new LocalFileNodeChildren(node);
			for (final LocalFileNode n : this.getChildren()) {
				nodes.add(n.clone(nodes));
			}
			node.setChildren(nodes);
		}
		return node;
	}
	
	@Override
	public boolean equals(final Object o) {
		return this == o;
	}
	
	@Override
	public String toString() {
		return this.isRoot() ? this.getPath() : this.getName();
	}
	
}