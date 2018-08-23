package ghsc.gui.fileshare.internal;

import java.util.Enumeration;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import ghsc.impl.EndTaggable;

public abstract class FileNode implements MutableTreeNode, EndTaggable {
	
	public static final String TAGNAME_DIR = "d";
	public static final String TAGNAME_FILE = "f";
	public static final String ATT_NAME = "n";
	public static final String ATT_SIZE = "s";
	public static final String ATT_PATH = "p";
	
	FileNode parent;
	FileNodeChildren<?> container;
	FileNodeChildren<?> children;
	
	FileNode(final FileNodeChildren<?> container) {
		this.container = container;
		this.parent = container != null ? container.getParent() : null;
	}
	
	public FileNode getParent() {
		return this.parent;
	}
	
	public FileNodeChildren<?> getContainer() {
		return this.container;
	}
	
	public void setContainer(final FileNodeChildren<?> container) {
		this.container = container;
	}
	
	public FileNodeChildren<?> getChildren() {
		return this.children;
	}
	
	public void setChildren(final FileNodeChildren<?> nodes) {
		this.children = nodes;
	}
	
	public boolean isRoot() {
		return this.parent == null;
	}
	
	public boolean isLeaf() {
		return this.children == null || this.children.isEmpty();
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
	public boolean equals(final Object o) {
		if (!(o instanceof FileNode)) {
            return false;
        }
		final String thisPath = this.getPath();
		return thisPath != null && thisPath.equals(((FileNode) o).getPath());
	}
	
	/*
	 * MutableTreeNode implementation
	 */
	
	@Override
	public Enumeration<? extends TreeNode> children() {
		return new Enumeration<TreeNode>() {
			int index;
			public boolean hasMoreElements() {
				return FileNode.this.children != null && this.index < FileNode.this.children.size();
			}
			public TreeNode nextElement() {
				return FileNode.this.children.get(this.index++);
			}
		};
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public TreeNode getChildAt(final int index) {
		if (index < 0 || this.children == null || index >= this.children.size()) {
            return null;
        }
		return this.children.get(index);
	}

	@Override
	public int getChildCount() {
		return this.children != null ? this.children.size() : 0;
	}

	@Override
	public int getIndex(final TreeNode node) {
		return this.children != null ? this.children.indexOf(node) : -1;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void insert(final MutableTreeNode node, final int index) {
		if (index >= 0 && this.children != null && index < this.children.size() + 1) {
			((FileNodeChildren<FileNode>) this.children).add(index, (FileNode) node);
			node.setParent(this);
		}
	}

	@Override
	public void remove(final int index) {
		if (index >= 0 && this.children != null && index < this.children.size()) {
			this.children.remove(index);
		}
	}

	@Override
	public void remove(final MutableTreeNode node) {
		this.remove(this.getIndex(node));
		node.setParent(null);
	}

	@Override
	public void removeFromParent() {
		if (this.parent != null) {
			this.parent.remove(this);
		}
	}

	@Override
	public void setParent(final MutableTreeNode node) {
		if (node == null || node instanceof FileNode) {
			this.parent = (FileNode) node;
		}
	}

	@Override
	public void setUserObject(final Object arg0) {}
	
}