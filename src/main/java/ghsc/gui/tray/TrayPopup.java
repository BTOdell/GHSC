package ghsc.gui.tray;

import ghsc.files.Settings;
import ghsc.gui.Application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Builds the default tray popup.
 */
class TrayPopup extends PopupMenu {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Initializes a new TrayPopup.
	 */
	TrayPopup() {
		this.addItem("Open", e -> Application.getInstance().getMainFrame().setVisible(true));
		this.addItem("Change nick", e -> Application.getInstance().showNickWizard());
		this.addItem("Clear settings", arg0 -> {
            final Application application = Application.getInstance();
            if (JOptionPane.showConfirmDialog(application.getMainFrame(),
					"By proceeding, you acknowledge that all your settings will be permanently erased.\nYour profile will not be erased.",
					"Clear all settings.",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                Settings.getSettings().delete();
                if (Application.isJar()) {
                    switch (JOptionPane.showOptionDialog(application.getMainFrame(),
							"Settings successfully cleared.\nHowever, the live application settings haven't been modified. Would you like to restart?",
							"Restart",
							JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							new String[] { "Restart", "Exit", "Cancel" },
							"Restart")) {
                        case 0:
                            Application.restart();
                            break;
                        case 1:
                            System.exit(0);
                            break;
                    }
                } else {
                    if (JOptionPane.showConfirmDialog(application.getMainFrame(),
							"Settings successfully cleared.\nWould you like to exit?",
							"Exit",
							JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
                        System.exit(0);
                    }
                }
            }
        });
		this.addItem("Restart", e -> {
            final Application application = Application.getInstance();
            if (Application.isJar()) {
                if (JOptionPane.showConfirmDialog(application.getMainFrame(),
						"Are you sure that you'd like to restart?",
						"Restart",
						JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
                    Application.restart();
                }
            } else {
                JOptionPane.showMessageDialog(application.getMainFrame(),
						"This application can't restart since it's not running from a jar file.",
						"Error with restart!",
						JOptionPane.ERROR_MESSAGE);
            }
        });
		this.addItem("Exit", e -> System.exit(0));
	}
	
	/**
	 * Creates a new MenuItem with the given title and ActionListener.
	 * @param title - the text to assign the MenuItem.
	 * @param listener - the listener to add to the MenuItem.
	 * @return the created and configured MenuItem.
	 */
	private MenuItem createItem(final String title, final ActionListener listener) {
		final MenuItem item = new MenuItem(title);
		item.addActionListener(listener);
		return item;
	}
	
	/**
	 * Uses {@link #createItem(String, ActionListener)} to create a MenuItem,</br>
	 * then automatically adds the item to this PopupMenu.
	 * @param title - the title to pass to {@link #createItem(String, ActionListener)}.
	 * @param listener - the listener to pass to {@link #createItem(String, ActionListener)}.
	 */
	private void addItem(final String title, final ActionListener listener) {
		this.add(this.createItem(title, listener));
	}
	
}