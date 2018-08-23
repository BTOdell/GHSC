package ghsc.gui.fileshare.components;

import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import ghsc.gui.fileshare.internal.FileNode;
import ghsc.gui.fileshare.internal.FileNodeChildren;

/**
 * A tree model to display file nodes for file sharing.
 */
public class FileNodeTreeModel<N extends FileNode> implements TreeModel {
	
	private final JTree tree;
	private final Vector<TreeModelListener> treeModelListeners = new Vector<>();
	private final DefaultMutableTreeNode invisibleRoot;
	private final ArrayList<N> roots;
	
	public FileNodeTreeModel(final JTree tree) {
		this.tree = tree;
		this.invisibleRoot = new DefaultMutableTreeNode("Root");
		this.roots = new ArrayList<>();
	}
	
	public int getRootCount() {
		return this.roots.size();
	}
	
	public boolean addRoot(final N root) {
		if (this.roots.add(root)) {
			this.insertNodeInto(root, this.invisibleRoot, this.invisibleRoot.getChildCount());
			if (this.invisibleRoot.getChildCount() > 0) {
				this.tree.expandPath(new TreePath(this.invisibleRoot.getPath()));
			}
			return true;
		}
		return false;
	}
	
	public boolean removeRoot(final N root) {
		final int index = this.roots.indexOf(root);
		if (index < 0) {
            return false;
        }
		this.roots.remove(index);
		this.invisibleRoot.remove(index);
		this.nodesWereRemoved(this.invisibleRoot, new int[] { index }, new Object[] { root });
		return true;
	}

	public void insertNodeInto(final N root, MutableTreeNode parent, final int index) {
		if (parent == null) {
            parent = this.invisibleRoot;
        }
		parent.insert(root, index);
		this.nodesWereInserted(parent, new int[] { index });
	}

	public void removeNodeFromParent(final N ln) {
		final FileNode parent = ln.getParent();
		if (parent == null) {
			this.removeRoot(ln);
			return;
		}
		final int[] indicies = { parent.getIndex(ln) };
		parent.remove(ln);
		this.nodesWereRemoved(parent, indicies, new Object[] { ln });
	}

	public void nodesWereInserted(MutableTreeNode parent, final int[] indicies) {
		if (indicies != null && indicies.length > 0) {
			if (parent == null) {
                parent = this.invisibleRoot;
            }
			final Object[] objs = new Object[indicies.length];
			for (int i = 0; i < indicies.length; i++) {
                objs[i] = parent.getChildAt(indicies[i]);
            }
			this.fireTreeNodesInserted(this, this.getPathToRoot(parent), indicies, objs);
		}
	}

	public void nodesWereRemoved(MutableTreeNode parent, final int[] indices, final Object[] objs) {
		if (indices != null) {
			if (parent == null) {
                parent = this.invisibleRoot;
            }
			this.fireTreeNodesRemoved(this, this.getPathToRoot(parent), indices, objs);
		}
	}

	public void nodesChanged(final MutableTreeNode node, final int[] indicies) {
		if (node != null) {
			if (indicies != null) {
				if (indicies.length > 0) {
					final Object[] objs = new Object[indicies.length];
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

	public void nodeStructureChanged(final MutableTreeNode node) {
		if (node != null) {
			this.fireTreeStructureChanged(this, this.getPathToRoot(node), null, null);
		}
	}

	public Object[] getPathToRoot(final MutableTreeNode node) {
		return this.getPathToRoot(node, 0);
	}

	protected Object[] getPathToRoot(MutableTreeNode node, int i) {
		i++;
		final Object[] path;
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

	protected void fireTreeNodesInserted(final Object source, final Object[] parentPath, final int[] indices, final Object[] objs) {
		TreeModelEvent e = null;
		for (final TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeNodesInserted(e);
		}
	}
	
	protected void fireTreeNodesRemoved(final Object source, final Object[] parentPath, final int[] indices, final Object[] objs) {
		TreeModelEvent e = null;
		for (final TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeNodesRemoved(e);
		}
	}
	
	protected void fireTreeNodesChanged(final Object source, final Object[] parentPath, final int[] indices, final Object[] objs) {
		TreeModelEvent e = null;
		for (final TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeNodesChanged(e);
		}
	}
	
	protected void fireTreeStructureChanged(final Object source, final Object[] parentPath, final int[] indices, final Object[] objs) {
		TreeModelEvent e = null;
		for (final TreeModelListener tml : this.treeModelListeners) {
			if (e == null) {
                e = new TreeModelEvent(source, parentPath, indices, objs);
            }
			tml.treeStructureChanged(e);
		}
	}
	
	public N[] getRoots(final N[] array) {
		return this.roots.toArray(array);
	}

	@Override
	public Object getChild(final Object parent, final int index) {
		if (parent == null) {
            return null;
        }
		if (parent instanceof DefaultMutableTreeNode) {
			return this.roots.get(index);
		} else if (parent instanceof FileNode) {
			final FileNode node = (FileNode) parent;
			final FileNodeChildren<?> c = node.getChildren();
			if (c != null && index >= 0 && index < c.size()) {
				return c.get(index);
			}
		}
		return null;
	}

	@Override
	public int getChildCount(final Object parent) {
		if (parent == null) {
            return 0;
        }
		if (parent instanceof DefaultMutableTreeNode) {
			return this.roots.size();
		} else if (parent instanceof FileNode) {
			final FileNode node = (FileNode) parent;
			final FileNodeChildren<?> c = node.getChildren();
			if (c != null) {
				return c.size();
			}
		}
		return 0;
	}

	@Override
	public int getIndexOfChild(final Object parent, final Object child) {
		if (parent == null || child == null) {
            return -1;
        }
		if (parent instanceof DefaultMutableTreeNode) {
			return this.roots.indexOf(child);
		} else if (parent instanceof FileNode) {
			final FileNode node = (FileNode) parent;
			return node.getIndex((MutableTreeNode) child);
		}
		return -1;
	}

	@Override
	public boolean isLeaf(final Object node) {
		return this.getChildCount(node) <= 0;
	}
	
	@Override
	public Object getRoot() {
		return this.invisibleRoot;
	}
	
	@Override
	public void addTreeModelListener(final TreeModelListener l) {
		this.treeModelListeners.add(l);
	}

	@Override
	public void removeTreeModelListener(final TreeModelListener l) {
		this.treeModelListeners.remove(l);
	}

	@Override
	public void valueForPathChanged(final TreePath path, final Object newValue) {}
	
}