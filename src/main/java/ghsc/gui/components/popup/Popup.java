package ghsc.gui.components.popup;

import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * An extension to a JPopupMenu, providing easier addition of items.
 */
public class Popup extends JPopupMenu {
	
	private static final long serialVersionUID = 1L;

	public Popup() {}
	
	/**
	 * Creates a new JMenuItem with the given title and ActionListener.
	 * @param title - the text to assign the JMenuItem.
	 * @param listener - the listener to add to the JMenuItem.
	 * @return the created and configured JMenuItem.
	 */
	public JMenuItem createItem(final String title, final ActionListener listener) {
		final JMenuItem item = new JMenuItem(title);
		if (listener != null) {
            item.addActionListener(listener);
        }
		return item;
	}
	
	/**
	 * Uses {@link #createItem(String, ActionListener)} to create a JMenuItem,</br>
	 * then automatically adds the item to this JPopupMenu.
	 * @param title - the title to pass to {@link #createItem(String, ActionListener)}.
	 * @param listener - the listener to pass to {@link #createItem(String, ActionListener)}.
	 */
	public void addItem(final String title, final ActionListener listener) {
        this.add(this.createItem(title, listener));
	}
	
	public JMenu createMenu(final String title) {
		return new JMenu(title);
	}
	
}