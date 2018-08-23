package ghsc.gui.fileshare.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import ghsc.gui.Application;
import ghsc.gui.components.chat.Chat;
import ghsc.gui.components.chat.channels.Channel;
import ghsc.gui.fileshare.components.PackagePanel;
import ghsc.util.Utilities;

/**
 *
 */
public abstract class FilePackage {
	
	public static final String TAGNAME = "p";
    public static final String ATT_NAME = "n";
    public static final String ATT_DESCRIPTION = "d";
    public static final String ATT_CREATIONDATE = "c";
    public static final String ATT_DOWNLOADCOUNT = "dc";
    public static final String ATT_ACTIVE = "a";
    public static final String ATT_PRIVATEKEY = "pk";
    public static final String ATT_PASSWORDPROTECTED = "p";
    public static final String ATT_VISIBILITY = "v";
    public static final String ATT_UUID = "u";

	protected static final String DATE_FORMAT = "MM/dd/yy hh:mm aa";

	private final UUID uuid;
	private final Calendar creationDate;
	private final String creationDateString;

	private String name;
	private String description;
	private Visibility visibility;
	private long downloadCount;
	private boolean active = true;

	Long size;
	Long fileCount;
	Long directoryCount;
	
	protected FilePackage(final UUID uuid, final String name, final String description, final Calendar creationDate, final Visibility visibility) {
		this.uuid = uuid;
		this.name = name;
		this.description = description;
		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		this.creationDate = creationDate;
		this.creationDateString = sdf.format(creationDate.getTime());
		this.visibility = visibility;
	}

	/**
	 * Gets the unique ID of the file package.
	 */
	public UUID getUUID() {
		return this.uuid;
	}

	/**
	 * Gets the display name of the file package.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of the file package.
	 * @param name The new name of the package.
	 */
	public void setName(final String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(final String d) {
		this.description = d;
	}
	
	public Calendar getCreationDate() {
		return this.creationDate;
	}
	
	/**
	 * MM/dd/yy hh:mm AM|PM
	 */
	public String getCreationDateString() {
		return this.creationDateString;
	}
	
	public Visibility getVisibility() {
		return this.visibility;
	}
	
	public void setVisibility(final Visibility vis) {
		this.visibility = vis;
	}
	
	public boolean isActive() {
		return this.active;
	}
	
	public void setActive(final boolean active) {
		this.active = active;
	}
	
	/**
	 * @return how many unique requests were made this this package.
	 */
	public long getDownloadCount() {
		return this.downloadCount;
	}
	
	public void setDownloadCount(final long downloads) {
		this.downloadCount = downloads;
	}
	
	public abstract String getOwner();
	
	/**
	 * @return whether this package is password protected.
	 */
	public abstract boolean isPasswordProtected();
	
	/**
	 * @return how many files are in this package.
	 */
	public abstract long getFileCount();
	
	/**
	 * @return how many directories are in this package.
	 */
	public abstract long getDirectoryCount();
	
	/**
	 * Calculates the size of all the files in this package.<br>
	 * Warning: This function can be a expensive operation if the package contains numerous files.
	 * @return the sum of the sizes of all the files.
	 */
	public abstract long getSize();
	
	@Override
	public boolean equals(final Object o) {
		if (o != null) {
			if (o instanceof FilePackage) {
				return this == o;
			} else if (o instanceof PackagePanel) {
				return this == ((PackagePanel) o).getPackage();
			}
		}
		return false;
	}

	static Calendar parseCalendar(final String cal) {
	    return parseCalendar(new SimpleDateFormat(DATE_FORMAT), cal);
    }

	static Calendar parseCalendar(final SimpleDateFormat sdf, final String cal) {
		final Calendar c = Calendar.getInstance();
		try {
			c.setTime(sdf.parse(cal));
		} catch (final ParseException e) {
			return null;
		}
		return c;
	}

	public static class Visibility implements Predicate<FilePackage> {

		public enum Type {
			PUBLIC("pu"), // visible to all users
			PRIVATE("pr"), // must be unlocked with "key"
			CHANNEL("c"), // only available to users in channel(s)
			USER("u"); // only available to select users (could be used for friends)

			private final String identifier;

			Type(final String identifier) {
				this.identifier = identifier;
			}

			public String getIdentifier() {
				return this.identifier;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder(super.toString().toLowerCase());
				sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
				return sb.toString();
			}

			public static Type match(final String identifier) {
				for (final Type v : values()) {
					if (v.getIdentifier().equals(identifier)) {
						return v;
					}
				}
				return null;
			}

		}

		private Type type;
		private Object data;
		private boolean privateDiscovered;

        /**
         * Creates a new Visibility object.
         * @param type The type of visibility.
         * @param data The related data.
         */
		public Visibility(final Type type, final Object data) {
			this.type = type;
			this.data = data;
		}

		public Type getType() {
			return this.type;
		}

		public void setType(final Type t) {
			this.type = t;
		}

		public Object getData() {
			return this.data;
		}

		public void setData(final Object data) {
			this.data = data;
		}

		/**
		 * @return whether or not this private package has been discovered. Will return true if package is not private.
		 */
		public boolean isDiscovered() {
			return this.type != Type.PRIVATE || this.privateDiscovered;
		}

		/**
		 * Sets whether this private package has been discovered. Only applies for private packages.
		 */
		public void setDiscovered(final boolean pd) {
			this.privateDiscovered = pd;
		}

		@Override
		public boolean test(final FilePackage p) {
			if (p instanceof LocalPackage || this.type == Type.PUBLIC || p.isActive()) { // handles public packages
				return true;
			}
			if (p instanceof RemotePackage) {
				final RemotePackage rp = (RemotePackage) p;
				if (!this.isDiscovered()) { // handles private packages
					return false;
				}
				final String[] dataStr = this.data.toString().split(Pattern.quote(","));
				switch (this.type) {
					case CHANNEL: // handles channel packages
						final Chat[] chats = rp.getHost().getContainer().getMainFrame().getChatContainer().getAll();
						for (final Chat chat : chats) {
							if (chat == null) {
								continue;
							}
							if (chat instanceof Channel && Utilities.contains(chat.getName(), dataStr)) {
								return true;
							}
						}

						// TODO: Is this fall through intentional !
						// adding break until this question is resolved.
						break;

					case USER: // handles user packages
						final ArrayList<String> uuids = new ArrayList<>();
						for (final String d : dataStr) {
							final int dI = d.indexOf('|');
							if (dI >= 0) {
								uuids.add(d.substring(dI + 1));
							}
						}
						return Utilities.contains(Application.getInstance().getID().toString(), uuids.toArray(new String[0]));
					case PRIVATE:
						// TODO: Not done yet.
						break;
					case PUBLIC:
						// TODO: Not done yet.
						break;
					default:
						break;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder(this.type.getIdentifier());
			if (this.data != null) {
				sb.append(":");
				sb.append(this.data);
			}
			return sb.toString();
		}

        /**
         * Parses a Visibility object from the given string.
         * @param raw Raw data to parse from.
         * @return A Visibility object, or <code>null</code> if failed to parse.
         */
		public static Visibility parse(final Object raw) {
            if (raw != null) {
                final String rawStr = raw.toString();
                if (rawStr != null) {
                    final String typeString;
                    final String dataString;
                    final int vIndex = rawStr.indexOf(':');
                    if (vIndex >= 0) {
                        typeString = rawStr.substring(0, vIndex);
                        dataString = rawStr.substring(vIndex + 1);
                    } else {
                        typeString = rawStr;
                        dataString = null;
                    }
                    final Visibility.Type vType = Visibility.Type.match(typeString);
                    if (vType != null) {
                        return new Visibility(vType, dataString);
                    }
                }
            }
            return null;
        }

	}
	
}