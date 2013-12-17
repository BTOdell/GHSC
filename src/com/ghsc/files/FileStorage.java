package com.ghsc.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ghsc.gui.Application;
import com.ghsc.net.encryption.AES;
import com.ghsc.util.Base64;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

/**
 * Provides access to the predefined hidden folder created by GHSC on any user's personal folder.
 * 128 bit encryption and base64 encoding.
 * XML-like settings (read and write).
 * @author Odell
 */
public abstract class FileStorage {
	
	protected static final int LOAD_BUFFER_SIZE = 8192;
	protected static final String FOLDER_NAME = "ghsc";
	protected static String HOMEPATH = null, DIRPATH = null;
	
	protected final String storagePath;
	protected final AES encryption;
	protected Node root = null;
	protected CopyOnWriteArraySet<Hook> saveHooks = new CopyOnWriteArraySet<Hook>();
	protected boolean savable = false;
	
	protected FileStorage() {
		encryption = new AES(new byte[] { 78, 82, 47, 54, 94, 74, -22, 111, -78, -46, 19, 34, -102, 55, 55, 112 });
		if (HOMEPATH == null) {
			HOMEPATH = determineHome();
		}
		if (DIRPATH == null) {
			DIRPATH = HOMEPATH + File.separator + FOLDER_NAME;
			File dirFile = new File(DIRPATH);
			if (!dirFile.exists())
				dirFile.mkdirs();
			if (!dirFile.isHidden())
				setHidden(dirFile);
		}
		storagePath = DIRPATH + File.separator + getStorageName();
	}
	
	public abstract String getStorageName();
	
	public String getHome() {
		return HOMEPATH;
	}
	
	public String getStorageDirectory() {
		return DIRPATH;
	}
	
	public String getStoragePath() {
		return storagePath;
	}
	
	public boolean exists() {
		return new File(getStoragePath()).exists();
	}
	
	public boolean isSavable() {
		return savable;
	}
	
	public void setSavable(final boolean savable) {
		this.savable = savable;
	}
	
	/**
	 * @param path '/blah/blah/blah'
	 * @return traverses the node tree given the path.
	 */
	public Node search(String path) {
		if (!path.startsWith("/"))
			return null;
		return root != null ? root.search(path) : null;
	}
	
	public boolean addHook(Hook h) {
		return saveHooks.contains(h) || saveHooks.add(h);
	}
	
	public boolean removeHook(Hook h) {
		return !saveHooks.contains(h) || saveHooks.remove(h);
	}
	
	/**
	 * Deletes the entire storage directory.
	 */
	public boolean deleteAll() {
		return deleteFile(new File(getStorageDirectory()));
	}
	
	/**
	 * Deletes this storage file.
	 */
	public boolean delete() {
		return deleteFile(new File(getStoragePath()));
	}
	
	public boolean save() {
		if (!isSavable())
			return false;
		FileOutputStream fos = null;
		try {
			File storageFile = new File(getStoragePath());
			if (!storageFile.exists()) {
				try {
					storageFile.createNewFile();
				} catch (IOException e) {
					return false;
				}
			}
			fos = new FileOutputStream(storageFile);
			StringBuilder sb = new StringBuilder();
			sb.append("<" + getStorageName() + ">");
			Iterator<Hook> it = saveHooks.iterator();
			while (it.hasNext()) {
				Hook h = it.next();
				if (h == null)
					continue;
				Node n = h.onSave();
				if (n == null)
					continue;
				sb.append(n.toString());
			}
			sb.append("</" + getStorageName() + ">");
			byte[] bytes = encryption.encrypt(sb);
			if (bytes == null)
				return false;
			fos.write(bytes);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	protected boolean load() {
		File storageFile = new File(getStoragePath());
		if (!storageFile.exists())
			return false;
		byte[] buffer = new byte[(int) storageFile.length()];
		FileInputStream fis = null;
		int total = 0;
		try {
			fis = new FileInputStream(storageFile);
			int read;
			while ((read = fis.read(buffer, total, Math.min(LOAD_BUFFER_SIZE, buffer.length - total))) > 0) {
				total += read;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		byte[] decrypt = encryption.decrypt(buffer, 0, total);
		if (decrypt == null)
			return false;
		StringBuilder sb = new StringBuilder(new String(decrypt, Application.CHARSET));
		final LinkedList<Node> nodeStack = new LinkedList<Node>();
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
				int index = sb.indexOf(peek.getEndTag());
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
		String home = System.getenv("HOMESHARE");
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
	protected boolean deleteFile(File f) {
		if (f.exists()) {
			if (f.isDirectory()) {
				for (File fs : f.listFiles()) {
					if (!deleteFile(fs)) {
						return false;
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
	protected boolean setHidden(File file) {
	    try {
			return Runtime.getRuntime().exec("attrib +H \"" + file.getPath() + "\"").waitFor() == 0;
		} catch (Exception e) {
			return false;
		}
	}
	
	/*
	 * Helper classes
	 */
	
	public interface Hook {
		public Node onSave();
	}
	
	public static class Node {
		
		private final Tag tag;
		private final String endTag;
		private ConcurrentLinkedQueue<Node> nodes = null;
		private String data = null, data_encoded = null;
		
		public Node(final Tag tag) {
			this.tag = tag.parse();
			this.endTag = new StringBuilder("</").append(tag.getName()).append(">").toString();
		}
		
		public Node(final Tag tag, Object data) {
			this(tag);
			this.data = data.toString();
		}
		
		public Node(final Tag tag, final Node... nodes) {
			this(tag);
			this.nodes = new ConcurrentLinkedQueue<Node>();
			this.nodes.addAll(Arrays.asList(nodes));
		}
		
		public String getData() {
			return data;
		}
		
		private String getEncodedData() {
			if (data_encoded == null) {
				data_encoded = Base64.encode(data);
			}
			return data_encoded;
		}
		
		private String getTagName() {
			return tag != null ? tag.getName() : null;
		}
		
		private String getEndTag() {
			return endTag;
		}

		public void submit(Object o) {
			if (o instanceof Node) {
				if (nodes == null)
					nodes = new ConcurrentLinkedQueue<Node>();
				nodes.offer((Node) o);
			} else {
				this.data_encoded = o.toString();
				this.data = Base64.decode(data_encoded);
			}
		}
		
		public Node search(final CharSequence path) {
			return search(new StringBuilder(path));
		}
		
		private Node search(StringBuilder sb) {
			if (sb.length() <= 0)
				return this;
			if (nodes != null) {
				if (sb.indexOf("/") != 0)
					return null;
				sb.deleteCharAt(0);
				final int index = sb.indexOf("/");
				final boolean leaf = index < 0;
				final String name = leaf ? sb.toString() : sb.substring(0, index);
				final Iterator<Node> it = nodes.iterator();
				while (it.hasNext()) {
					final Node n = it.next();
					if (n == null)
						continue;
					if (name.equals(n.getTagName())) {
						return n.search(sb.delete(0, name.length()));
					}
				}
			}
			return null;
		}
		
		public Node[] searchAll(final CharSequence path) {
			return searchAll(new StringBuilder(path));
		}
		
		private Node[] searchAll(StringBuilder sb) {
			if (sb.indexOf("/") != 0)
				return null;
			sb.deleteCharAt(0);
			if (nodes != null) {
				final int index = sb.indexOf("/");
				final boolean leaf = index < 0;
				final String name = leaf ? sb.toString() : sb.substring(0, index);
				final LinkedList<Node> searched = leaf ? new LinkedList<Node>() : null;
				final Iterator<Node> it = nodes.iterator();
				while (it.hasNext()) {
					final Node n = it.next();
					if (n == null)
						continue;
					if (name.equals(n.getTagName())) {
						if (leaf) {
							searched.add(n);
						} else {
							return n.searchAll(sb.delete(0, name.length()));
						}
					}
				}
				return searched.toArray(new Node[searched.size()]);
			}
			return null;
		}
		
		@Override
		public String toString() {
			if (tag == null)
				return "";
			final StringBuilder sb = new StringBuilder(tag.getEncodedTag());
			if (nodes != null) {
				final Iterator<Node> it = nodes.iterator();
				while (it.hasNext()) {
					final Node n = it.next();
					if (n == null)
						continue;
					sb.append(n.toString());
				}
			}
			if (data != null || data_encoded != null) {
				sb.append(getEncodedData());
			}
			sb.append(getEndTag());
			return sb.toString();
		}
		
	}
	
}