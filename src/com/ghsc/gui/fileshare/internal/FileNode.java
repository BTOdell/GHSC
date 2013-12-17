package com.ghsc.gui.fileshare.internal;

import java.util.Enumeration;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import com.ghsc.impl.EndTaggable;

public abstract class FileNode implements MutableTreeNode, EndTaggable {
	
	public static final String TAGNAME_DIR = "d", TAGNAME_FILE = "f", 
			ATT_NAME = "n", ATT_SIZE = "s", ATT_PATH = "p";
	
	FileNode parent;
	FileNodeChildren<?> container, children;
	
	FileNode(final FileNodeChildren<?> container) {
		this.container = container;
		this.parent = container != null ? container.getParent() : null;
	}
	
	public FileNode getParent() {
		return parent;
	}
	
	public FileNodeChildren<?> getContainer() {
		return (FileNodeChildren<?>) container;
	}
	
	public void setContainer(FileNodeChildren<?> container) {
		this.container = container;
	}
	
	public FileNodeChildren<?> getChildren() {
		return children;
	}
	
	public void setChildren(final FileNodeChildren<?> nodes) {
		this.children = nodes;
	}
	
	public boolean isRoot() {
		return parent == null;
	}
	
	public boolean isLeaf() {
		return children == null || children.size() == 0;
	}
	
	public abstract long getFileCount();
	
	public abstract long getDirectoryCount();
	
	/**
	 * @return the size of this node including the size of all its subnodes.
	 */
	public abstract long getSize();
	
	/**
	 * @return the name of this file node.
	 */
	public abstract String getName();
	
	/**
	 * @return the path of this file node.
	 */
	public abstract String getPath();
	
	/**
	 * @return whether this file is a directory.
	 */
	public abstract boolean isDirectory();
	
	public abstract long getFileSize();
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FileNode))
			return false;
		final String thisPath = getPath();
		return thisPath == null ? o == null : thisPath.equals(((FileNode) o).getPath());
	}
	
	/*
	 * MutableTreeNode implementation
	 */
	
	@Override
	public Enumeration<Object> children() {
		return new Enumeration<Object>() {
			int index = 0;
			public boolean hasMoreElements() {
				return children != null && index < children.size();
			}
			public Object nextElement() {
				return children.get(index++);
			}
		};
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public TreeNode getChildAt(int index) {
		if (index < 0 || children == null || index >= children.size())
			return null;
		return (TreeNode) children.get(index);
	}

	@Override
	public int getChildCount() {
		return children != null ? children.size() : 0;
	}

	@Override
	public int getIndex(TreeNode node) {
		return children != null ? children.indexOf(node) : -1;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void insert(MutableTreeNode node, int index) {
		if (index >= 0 && children != null && index < children.size() + 1) {
			((FileNodeChildren<FileNode>) children).add(index, (FileNode) node);
			node.setParent(this);
		}
	}

	@Override
	public void remove(int index) {
		if (index >= 0 && children != null && index < children.size()) {
			children.remove(index);
		}
	}

	@Override
	public void remove(MutableTreeNode node) {
		remove(getIndex(node));
		node.setParent(null);
	}

	@Override
	public void removeFromParent() {
		if (parent != null) {
			parent.remove(this);
		}
	}

	@Override
	public void setParent(MutableTreeNode node) {
		if (node == null || node instanceof FileNode) {
			this.parent = (FileNode) node;
		}
	}

	@Override
	public void setUserObject(Object arg0) {}
	
}