package ghsc.files;

import java.io.File;

import ghsc.gui.Application;
import ghsc.util.Tag;

/**
 * Provides access to the settings file in the hidden GHSC folder.
 */
public class Settings extends FileStorage {
	
	private static Settings settingsInstance;
	
	public static Settings getSettings() {
		if (settingsInstance == null) {
			settingsInstance = new Settings();
			if (settingsInstance.exists()) {
				if (settingsInstance.load()) {
					System.out.println("Settings loaded.");
				} else {
					System.out.println("Settings failed to load.");
					if (settingsInstance.delete()) {
						System.out.println("Settings were deleted.");
					}
				}
			} else {
				System.out.println("No settings to load.");
			}
			configureStaticSettings();
		}
		return settingsInstance;
	}
	
	private static void configureStaticSettings() {
		if (settingsInstance != null) {
			// Application last directory for FileChooser
			settingsInstance.addHook(() -> Application.LAST_DIRECTORY != null ?
					new Node(Tag.construct("application_lastdir"), Application.LAST_DIRECTORY.getPath()) : null);
			final Node lastDirNode = settingsInstance.search("/application_lastdir");
			if (lastDirNode != null) {
				final String lastDirString = lastDirNode.getData();
				if (lastDirString != null) {
					final File lastDirFile = new File(lastDirString);
					if (lastDirFile.exists()) {
						Application.LAST_DIRECTORY = lastDirFile;
					}
				}
			}
		}
	}
	
	private Settings() {
		super();
	}
	
	@Override
	public String getStorageName() {
		return "settings";
	}
	
}