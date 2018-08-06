package ghsc.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

import ghsc.gui.Application;
import ghsc.net.encryption.AES;
import ghsc.util.Base64;
import ghsc.util.Tag;
import ghsc.util.Utilities;

/**
 * Provides access to the predefined hidden folder created by GHSC on any user's personal folder.
 * 128 bit encryption and base64 encoding.
 * XML-like settings (read and write).
 */
public abstract class FileStorage {
	
	protected static final int LOAD_BUFFER_SIZE = 8192;
	protected static final String FOLDER_NAME = "ghsc";
	protected static String HOMEPATH;
    protected static String DIRPATH;
	
	protected final String storagePath;
	protected final AES encryption;
	protected Node root;
	protected CopyOnWriteArraySet<Hook> saveHooks = new CopyOnWriteArraySet<>();
	protected boolean savable;
	
	protected FileStorage() {
		this.encryption = new AES(new byte[] { 78, 82, 47, 54, 94, 74, -22, 111, -78, -46, 19, 34, -102, 55, 55, 112 });
		if (HOMEPATH == null) {
			HOMEPATH = this.determineHome();
		}
		if (DIRPATH == null) {
			DIRPATH = HOMEPATH + File.separator + FOLDER_NAME;
			final File dirFile = new File(DIRPATH);
			if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
			if (!dirFile.isHidden()) {
                this.setHidden(dirFile);
            }
		}
		this.storagePath = DIRPATH + File.separator + this.getStorageName();
	}
	
	public abstract String getStorageName();
	
	public String getHome() {
		return HOMEPATH;
	}
	
	public String getStorageDirectory() {
		return DIRPATH;
	}
	
	public String getStoragePath() {
		return this.storagePath;
	}
	
	public boolean exists() {
		return new File(this.getStoragePath()).exists();
	}
	
	public boolean isSavable() {
		return this.savable;
	}
	
	public void setSavable(final boolean savable) {
		this.savable = savable;
	}
	
	/**
	 * @param path '/blah/blah/blah'
	 * @return traverses the node tree given the path.
	 */
	public Node search(final String path) {
		if (!path.startsWith("/")) {
            return null;
        }
		return this.root != null ? this.root.search(path) : null;
	}
	
	public boolean addHook(final Hook h) {
		return this.saveHooks.contains(h) || this.saveHooks.add(h);
	}
	
	public boolean removeHook(final Hook h) {
		return !this.saveHooks.contains(h) || this.saveHooks.remove(h);
	}
	
	/**
	 * Deletes the entire storage directory.
	 */
	public boolean deleteAll() {
		return this.deleteFile(new File(this.getStorageDirectory()));
	}
	
	/**
	 * Deletes this storage file.
	 */
	public boolean delete() {
		return this.deleteFile(new File(this.getStoragePath()));
	}
	
	public boolean save() {
		if (!this.isSavable()) {
            return false;
        }
		FileOutputStream fos = null;
		try {
			final File storageFile = new File(this.getStoragePath());
			if (!storageFile.exists()) {
				try {
					storageFile.createNewFile();
				} catch (final IOException e) {
					return false;
				}
			}
			fos = new FileOutputStream(storageFile);
			final StringBuilder sb = new StringBuilder();
			sb.append("<").append(this.getStorageName()).append(">");
			for (final Hook h : this.saveHooks) {
				if (h == null) {
					continue;
				}
				final Node n = h.onSave();
				if (n == null) {
					continue;
				}
				sb.append(n.toString());
			}
			sb.append("</").append(this.getStorageName()).append(">");
			final byte[] bytes = this.encryption.encrypt(sb);
			if (bytes == null) {
				return false;
			}
			fos.write(bytes);
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	protected boolean load() {
		final File storageFile = new File(this.getStoragePath());
		if (!storageFile.exists()) {
            return false;
        }
		final byte[] buffer = new byte[(int) storageFile.length()];
		FileInputStream fis = null;
		int total = 0;
		try {
			fis = new FileInputStream(storageFile);
			int read;
			while ((read = fis.read(buffer, total, Math.min(LOAD_BUFFER_SIZE, buffer.length - total))) > 0) {
				total += read;
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		final byte[] decrypt = this.encryption.decrypt(buffer, 0, total);
		if (decrypt == null) {
            return false;
        }
		final StringBuilder sb = new StringBuilder(new String(decrypt, Application.CHARSET));
		final LinkedList<Node> nodeStack = new LinkedList<>();
		Node peek = null;
		while (sb.length() > 0) {
			if (peek != null) {
				if (Utilities.startsWith(sb, peek.getEndTag())) { // handle submitting tags to parent tags
					sb.delete(0, peek.getEndTag().length());
					final Node pop = nodeStack.pop();
					peek = nodeStack.peek();
					if (peek == null) {
						this.root = pop;
					} else {
						peek.submit(pop);
						continue;
					}
					break;
				}
			}
			final Tag newTag = new Tag(sb).parseBasic(false);
			if (newTag != null) { // handle new tags
				final Node n = new Node(newTag);
				nodeStack.push(n);
				sb.delete(0, newTag.getEncodedLength());
				peek = n;
				continue;
			} else if (peek != null) { // handle data between tags
				final int index = sb.indexOf(peek.getEndTag());
				if (index >= 0) {
					final String sub = sb.substring(0, index);
					if (!sub.isEmpty()) {
						peek.submit(sub);
					}
					sb.delete(0, index);
					continue;
				}
			}
			return false;
		}
		return true;
	}
	
	private String determineHome() {
		final String home = System.getenv("HOMESHARE");
		return home == null ? System.getProperty("user.home") : home;
	}
	
	/*
	 * Helper functions
	 */
	
	/**
	 * Deletes any file as well as recursively deleting a directory and any contents.
	 * @param f - the file/directory to delete from the file system.
	 * @return <tt>true</tt> if everything deleted properly, if error occurred <tt>false</tt>.
	 */
	protected boolean deleteFile(final File f) {
		if (f.exists()) {
			if (f.isDirectory()) {
				final File[] files = f.listFiles();
				if (files != null) {
					for (final File fs : files) {
						if (!this.deleteFile(fs)) {
							return false;
						}
					}
				}
			}
			return f.delete();
		}
		return true;
	}
	
	/**
	 * Only works for Windows.</br>
	 * Executes a command to make the given file hidden.
	 * @param file - the file to make hidden.
	 * @return <tt>true</tt> if the folder was hidden successfully, otherwise <tt>false</tt>.
	 */
	protected boolean setHidden(final File file) {
	    try {
			return Runtime.getRuntime().exec("attrib +H \"" + file.getPath() + "\"").waitFor() == 0;
		} catch (final Exception e) {
			return false;
		}
	}
	
	/*
	 * Helper classes
	 */
	
	public interface Hook {
		Node onSave();
	}
	
	public static class Node {
		
		private final Tag tag;
		private final String endTag;
		private ConcurrentLinkedQueue<Node> nodes;
		private String data;
		private String data_encoded;
		
		public Node(final Tag tag) {
			this.tag = tag.parse();
			this.endTag = "</" + tag.getName() + ">";
		}
		
		public Node(final Tag tag, final Object data) {
			this(tag);
			this.data = data.toString();
		}
		
		public Node(final Tag tag, final Node... nodes) {
			this(tag);
			this.nodes = new ConcurrentLinkedQueue<>();
			this.nodes.addAll(Arrays.asList(nodes));
		}
		
		public String getData() {
			return this.data;
		}
		
		private String getEncodedData() {
			if (this.data_encoded == null) {
				this.data_encoded = Base64.encode(this.data);
			}
			return this.data_encoded;
		}
		
		private String getTagName() {
			return this.tag != null ? this.tag.getName() : null;
		}
		
		private String getEndTag() {
			return this.endTag;
		}

		public void submit(final Object o) {
			if (o instanceof Node) {
				if (this.nodes == null) {
                    this.nodes = new ConcurrentLinkedQueue<>();
                }
				this.nodes.offer((Node) o);
			} else {
				this.data_encoded = o.toString();
				this.data = Base64.decode(this.data_encoded);
			}
		}
		
		public Node search(final CharSequence path) {
			return this.search(new StringBuilder(path));
		}
		
		private Node search(final StringBuilder sb) {
			if (sb.length() <= 0) {
                return this;
            }
			if (this.nodes != null) {
				if (sb.indexOf("/") != 0) {
                    return null;
                }
				sb.deleteCharAt(0);
				final int index = sb.indexOf("/");
				final boolean leaf = index < 0;
				final String name = leaf ? sb.toString() : sb.substring(0, index);
				for (final Node n : this.nodes) {
					if (n == null) {
						continue;
					}
					if (name.equals(n.getTagName())) {
						return n.search(sb.delete(0, name.length()));
					}
				}
			}
			return null;
		}
		
		public Node[] searchAll(final CharSequence path) {
			return this.searchAll(new StringBuilder(path));
		}
		
		private Node[] searchAll(final StringBuilder sb) {
			if (sb.indexOf("/") != 0) {
                return null;
            }
			sb.deleteCharAt(0);
			if (this.nodes != null) {
				final int index = sb.indexOf("/");
				final boolean leaf = index < 0;
				final String name = leaf ? sb.toString() : sb.substring(0, index);
				List<Node> searched = null;
				for (final Node n : this.nodes) {
					if (n == null) {
						continue;
					}
					if (name.equals(n.getTagName())) {
						if (leaf) {
							if (searched == null) {
								searched = new ArrayList<>(this.nodes.size());
							}
							searched.add(n);
						} else {
							return n.searchAll(sb.delete(0, name.length()));
						}
					}
				}
				return searched != null ? searched.toArray(new Node[0]) : new Node[0];
			}
			return null;
		}
		
		@Override
		public String toString() {
			if (this.tag == null) {
                return "";
            }
			final StringBuilder sb = new StringBuilder(this.tag.getEncodedTag());
			if (this.nodes != null) {
				for (final Node n : this.nodes) {
					if (n == null) {
						continue;
					}
					sb.append(n.toString());
				}
			}
			if (this.data != null || this.data_encoded != null) {
				sb.append(this.getEncodedData());
			}
			sb.append(this.getEndTag());
			return sb.toString();
		}
		
	}
	
}