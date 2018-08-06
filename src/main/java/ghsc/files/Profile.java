package ghsc.files;

/**
 * Provides access to the profile file in the hidden GHSC folder.
 * @author Odell
 */
public class Profile extends FileStorage {
	
	private static Profile profileInstance;
	
	public static Profile getProfile() {
		if (profileInstance == null) {
			profileInstance = new Profile();
			if (profileInstance.exists()) {
				if (profileInstance.load()) {
					System.out.println("Profile loaded.");
				} else {
					System.out.println("Profile failed to load.");
					if (profileInstance.delete()) {
						System.out.println("Profile was deleted.");
					}
				}
			} else {
				System.out.println("No profile to load.");
			}
			configureStaticProfile();
		}
		return profileInstance;
	}
	
	private static void configureStaticProfile() {
		if (profileInstance != null) {
			
		}
	}
	
	private Profile() {
		super();
	}
	
	@Override
	public String getStorageName() {
		return "profile";
	}
	
}