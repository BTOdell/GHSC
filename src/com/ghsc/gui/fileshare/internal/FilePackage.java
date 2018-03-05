package com.ghsc.gui.fileshare.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.regex.Pattern;

import com.ghsc.gui.Application;
import com.ghsc.gui.components.chat.Chat;
import com.ghsc.gui.components.chat.channels.Channel;
import com.ghsc.gui.fileshare.components.PackagePanel;
import com.ghsc.impl.Filter;
import com.ghsc.util.Utilities;

public abstract class FilePackage {
	
	public static class Visibility implements Filter<FilePackage> {
		
		public enum Type {
			PUBLIC("pu"), // visible to all users
			PRIVATE("pr"), // must be unlocked with "key"
			CHANNEL("c"), // only available to users in channel(s)
			USER("u"); // only available to select users (could be used for friends)
			
			private final String identifier;
			
			Type(String identifier) {
				this.identifier = identifier;
			}
			
			public String getIdentifier() {
				return this.identifier;
			}
			
			@Override
			public String toString() {
				StringBuilder sb = new StringBuilder(super.toString().toLowerCase());
				sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
				return sb.toString();
			}
			
			public static Type match(String identifier) {
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
		
		public Visibility(Object raw) {
			if (raw != null) {
				String rawStr = raw.toString();
				if (rawStr != null) {
					final int vIndex = rawStr.indexOf(':');
					if (vIndex >= 0) {
						this.data = rawStr.substring(vIndex + 1, rawStr.length());
						rawStr = rawStr.substring(0, vIndex);
					}
					this.type = Visibility.Type.match(rawStr);
					return;
				}
			}
			throw new IllegalArgumentException("Raw object data cannot be null!");
		}
		
		public Visibility(Type type, Object data) {
			this.type = type;
			this.data = data;
		}
		
		public Type getType() {
			return this.type;
		}
		
		public void setType(Type t) {
			this.type = t;
		}
		
		public Object getData() {
			return this.data;
		}
		
		public void setData(Object data) {
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
		public boolean accept(final FilePackage p) {
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
								uuids.add(d.substring(dI + 1, d.length()));
							}
						}
						return Utilities.contains(Application.getInstance().getID().toString(), uuids.toArray(new String[uuids.size()]));
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
	
	public static final String TAGNAME = "p", ATT_NAME = "n", ATT_DESCRIPTION = "d", ATT_CREATIONDATE = "c", 
			ATT_DOWNLOADCOUNT = "dc", ATT_ACTIVE = "a", ATT_PRIVATEKEY = "pk", ATT_PASSWORDPROTECTED = "p", ATT_VISIBILITY = "v", ATT_UUID = "u";
	protected static final String DATE_FORMAT = "MM/dd/yy hh:mm aa";
	
	String name, description;
	Calendar creationDate;
	String creationDateString;
	UUID uuid;
	Visibility visibility;
	Long size, fileCount, directoryCount, downloadCount = 0L;
	boolean active = true;
	
	FilePackage(final String name, final String description, final Calendar creationDate, final Visibility visibility) {
		this.name = name;
		this.description = description;
		final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		this.creationDate = creationDate;
		this.creationDateString = sdf.format(creationDate.getTime());
		this.visibility = visibility;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public void setDescription(String d) {
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
	
	public void setVisibility(Visibility vis) {
		this.visibility = vis;
	}
	
	public boolean isActive() {
		return this.active;
	}
	
	public void setActive(boolean active) {
		this.active = active;
	}
	
	/**
	 * @return how many unique requests were made this this package.
	 */
	public long getDownloadCount() {
		return this.downloadCount;
	}
	
	public void setDownloadCount(long downloads) {
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
	
	public abstract UUID getUUID();
	
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
	
}