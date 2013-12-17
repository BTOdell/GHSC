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
		this.endTag = new StringBuilder("</").append(getTagName()).append(">").toString();
	}
	
	public final File getFile() {
		return file;
	}
	
	public LocalFileNode getParent() {
		return (LocalFileNode) parent;
	}
	
	public LocalFileNodeChildren getContainer() {
		return (LocalFileNodeChildren) container;
	}
	
	public void setContainer(LocalFileNodeChildren container) {
		this.container = container;
	}
	
	public LocalFileNodeChildren getChildren() {
		return (LocalFileNodeChildren) children;
	}
	
	public void setChildren(final LocalFileNodeChildren children) {
		this.children = children;
	}

	@Override
	public String getName() {
		return file != null ? file.getName() : null;
	}
	
	@Override
	public String getPath() {
		return file != null ? file.getPath() : null;
	}
	
	@Override
	public boolean isDirectory() {
		return file != null ? file.isDirectory() : false;
	}
	
	@Override
	public long getFileSize() {
		return file != null ? file.length() : 0;
	}
	
	@Override
	public long getSize() {
		long temp = getFileSize();
		if (!isLeaf()) {
			for (LocalFileNode node : getChildren())
				temp += node.getSize();
		}
		return temp;
	}
	
	@Override
	public long getFileCount() {
		long temp = isDirectory() ? 0 : 1;
		if (!isLeaf()) {
			for (LocalFileNode node : getChildren())
				temp += node.getFileCount();
		}
		return temp;
	}

	@Override
	public long getDirectoryCount() {
		long temp = isDirectory() ? 1 : 0;
		if (!isLeaf()) {
			for (LocalFileNode node : getChildren())
				temp += node.getDirectoryCount();
		}
		return temp;
	}
	
	protected String concat() {
		final StringBuilder build = new StringBuilder(toString());
		if (!isLeaf()) {
			for (final LocalFileNode node : getChildren())
				build.append(node.concat());
		}
		return build.toString();
	}
	
	public InputStream openInputStream() {
		if (file != null) {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException e) {}
		}
		return null;
	}
	
	/*
	 * Remote package related methods
	 */
	
	public String toRemoteMeta() {
		final StringBuilder build = new StringBuilder();
		{
			final LinkedList<Object> objs = new LinkedList<Object>();
			objs.add(FileNode.ATT_NAME);
			objs.add(getName());
			boolean isDir = isDirectory();
			if (!isDir) {
				objs.add(FileNode.ATT_SIZE);
				objs.add(getSize());
			}
			final String tagName = getTagName();
			build.append(Tag.construct(tagName, objs.toArray()).getEncodedString());
			if (isDir && !isLeaf()) {
				for (final LocalFileNode lc : getChildren())
					build.append(lc.toRemoteMeta());
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
			final LinkedList<Object> objs = new LinkedList<Object>();
			objs.add(FileNode.ATT_PATH);
			objs.add(getPath());
			final String tagName = getTagName();
			build.append(Tag.construct(tagName, objs.toArray()).getEncodedString());
			if (isDirectory() && !isLeaf()) {
				for (final LocalFileNode lc : getChildren())
					build.append(lc.toSaveMeta());
			}
			build.append("</").append(tagName).append(">");
		}
		return build.toString();
	}
	
	/**
	 * Finds the LocalFile given a path relative to the node.
	 */
	public LocalFileNode findFile(String relativePath) {
		return findFile(relativePath, getChildren());
	}
	
	/**
	 * Finds the LocalFile given a relative path and nodes.
	 * Example: \pictures\2012\image.png
	 */
	static LocalFileNode findFile(String relativePath, List<LocalFileNode> nodes) {
		relativePath = relativePath.substring(1, relativePath.length());
		int index = relativePath.indexOf(File.separatorChar);
		String nodeName = relativePath.substring(0, index >= 0 ? index : relativePath.length());
		String newRelativePath = relativePath.substring(index);
		for (LocalFileNode node : nodes) {
			if (nodeName.equals(node.getFile().getName())) {
				return node.findFile(newRelativePath);
			}
		}
		return null;
	}
	
	public static LocalFileNode generateRoot(File file) {
		return generate(null, file);
	}
	
	private static LocalFileNode generate(LocalFileNodeChildren container, File file) {
		final LocalFileNode node = new LocalFileNode(container, file);
		if (node.isDirectory()) {
			final File[] files = file.listFiles();
			if (files.length > 0) {
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
		return isDirectory() ? TAGNAME_DIR : TAGNAME_FILE;
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
					if (children == null)
						setChildren(new LocalFileNodeChildren(this));
					getChildren().add((LocalFileNode) t);
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
					if (children == null)
						setChildren(new LocalFileNodeChildren(this));
					final File file = new File(path);
					return new LocalFileNode(getChildren(), file.exists() ? file : null);
				}
				break;
		}
		return null;
	}
	
	/**
	 * Clones the entire node tree leaving this node as the root.
	 */
	@Override
	public LocalFileNode clone() {
		return clone(null);
	}
	
	/**
	 * Clones the entire node tree.
	 */
	public LocalFileNode clone(LocalFileNodeChildren container) {
		final LocalFileNode node = new LocalFileNode(container, file);
		if (!isLeaf()) {
			final LocalFileNodeChildren nodes = new LocalFileNodeChildren(node);
			for (final LocalFileNode n : getChildren()) {
				nodes.add(n.clone(nodes));
			}
			node.setChildren(nodes);
		}
		return node;
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