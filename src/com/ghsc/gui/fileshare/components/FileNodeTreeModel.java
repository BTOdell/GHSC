package com.ghsc.gui.fileshare.components;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.ghsc.gui.fileshare.internal.FileNode;
import com.ghsc.gui.fileshare.internal.FileNodeChildren;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class FileNodeTreeModel<N extends FileNode> implements TreeModel {
	
	private final JTree tree;
	private final Vector<TreeModelListener> treeModelListeners = new Vector<TreeModelListener>();
	private final DefaultMutableTreeNode invisibleRoot;
	private final ArrayList<N> roots;
	
	public FileNodeTreeModel(final JTree tree) {
		this.tree = tree;
		this.invisibleRoot = new DefaultMutableTreeNode("Root");
		this.roots = new ArrayList<N>();
	}
	
	public int getRootCount() {
		return this.roots.size();
	}
	
	public boolean addRoot(N root) {
		if (this.roots.add(root)) {
			this.insertNodeInto(root, this.invisibleRoot, this.invisibleRoot.getChildCount());
			if (this.invisibleRoot.getChildCount() > 0) {
				this.tree.expandPath(new TreePath(this.invisibleRoot.getPath()));
			}
			return true;
		}
		return false;
	}
	
	public boolean removeRoot(N root) {
		final int index = this.roots.indexOf(root);
		if (index < 0) {
            return false;
        }
		this.roots.remove(index);
		this.invisibleRoot.remove(index);
		this.nodesWereRemoved(this.invisibleRoot, new int[] { index }, new Object[] { root });
		return true;
	}

	public void insertNodeInto(N root, MutableTreeNode parent, int index) {
		if (parent == null) {
            parent = this.invisibleRoot;
        }
		parent.insert(root, index);
		this.nodesWereInserted(parent, new int[] { index });
	}

	public void removeNodeFromParent(N ln) {
		FileNode parent = ln.getParent();
		if (parent == null) {
			this.removeRoot(ln);
			return;
		}
		int[] indicies = { parent.getIndex(ln) };
		parent.remove(ln);
		this.nodesWereRemoved(parent, indicies, new Object[] { ln });
	}

	public void nodesWereInserted(MutableTreeNode parent, int[] indicies) {
		if (this.treeModelListeners != null && indicies != null && indicies.length > 0) {
			if (parent == null) {
                parent = this.invisibleRoot;
            }
			Object[] objs = new Object[indicies.length];
			for (int i = 0; i < indicies.length; i++) {
                objs[i] = parent.getChildAt(indicies[i]);
            }
			this.fireTreeNodesInserted(this, this.getPathToRoot(parent), indicies, objs);
		}
	}

	public void nodesWereRemoved(MutableTreeNode parent, int[] indices, Object[] objs) {
		if (indices != null) {
			if (parent == null) {
                parent = this.invisibleRoot;
            }
			this.fireTreeNodesRemoved(this, this.getPathToRoot(parent), indices, objs);
		}
	}

	public void nodesChanged(MutableTreeNode node, int[] indicies) {
		if (node != null) {
			if (indicies != null) {
				if (indicies.length > 0) {
					Object[] objs = new Object[indicies.length];
					for (int i = 0; i < indicies.length; i++) {
                        objs[i] = node.getChildAt(indicies[i]);
                    }
					this.fireTreeNodesChanged(this, this.getPathToRoot(node), indicies, objs);
				}
			} else if (node == this.getRoot()) {
				this.fireTreeNodesChanged(this, this.getPathToRoot(node), null, null);
			}
		}
	}

	public void nodeStructureChanged(MutableTreeNode node) {
		if (node != null) {
			this.fireTreeStructureChanged(this, this.getPathToRoot(node), null, null);
		}
	}

	public Object[] getPathToRoot(MutableTreeNode node) {
		return this.getPathToRoot(node, 0);
	}

	protected Object[] getPathToRoot(MutableTreeNode node, int i) {
		i++;
		Object[] path;
		if (node == null) {
            node = this.invisibleRoot;
        }
		if (node == this.invisibleRoot) {
			path = new Object[i];
		} else {
			path = this.getPathToRoot((MutableTreeNode) node.getParent(), i);
		}
		path[path.length - i] = node;
		return path;
	}

	protected void fireTreeNodesInserted(Object source, Object[] parentPath, int[] indices, Object[] objs) {
		TreeModelEvent e = null;
		for (TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeNodesInserted(e);
		}
	}
	
	protected void fireTreeNodesRemoved(Object source, Object[] parentPath, int[] indices, Object[] objs) {
		TreeModelEvent e = null;
		for (TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeNodesRemoved(e);
		}
	}
	
	protected void fireTreeNodesChanged(Object source, Object[] parentPath, int[] indices, Object[] objs) {
		TreeModelEvent e = null;
		for (TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeNodesChanged(e);
		}
	}
	
	protected void fireTreeStructureChanged(final Object source, final Object[] parentPath, int[] indices, Object[] objs) {
		TreeModelEvent e = null;
		for (final TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeStructureChanged(e);
		}
	}
	
	public N[] getRoots(N[] array) {
		return this.roots.toArray(array);
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (parent == null) {
            return null;
        }
		if (parent instanceof DefaultMutableTreeNode) {
			return this.roots.get(index);
		} else if (parent instanceof FileNode) {
			FileNode node = (FileNode) parent;
			FileNodeChildren<?> c = node.getChildren();
			if (c != null && index >= 0 && index < c.size()) {
				return c.get(index);
			}
		}
		return null;
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent == null) {
            return 0;
        }
		if (parent instanceof DefaultMutableTreeNode) {
			return this.roots.size();
		} else if (parent instanceof FileNode) {
			FileNode node = (FileNode) parent;
			FileNodeChildren<?> c = node.getChildren();
			if (c != null) {
				return c.size();
			}
		}
		return 0;
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent == null || child == null) {
            return -1;
        }
		if (parent instanceof DefaultMutableTreeNode) {
			return this.roots.indexOf(child);
		} else if (parent instanceof FileNode) {
			FileNode node = (FileNode) parent;
			return node.getIndex((MutableTreeNode) child);
		}
		return -1;
	}

	@Override
	public boolean isLeaf(Object node) {
		return this.getChildCount(node) <= 0;
	}
	
	@Override
	public Object getRoot() {
		return this.invisibleRoot;
	}
	
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		this.treeModelListeners.add(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		this.treeModelListeners.remove(l);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {}
	
}