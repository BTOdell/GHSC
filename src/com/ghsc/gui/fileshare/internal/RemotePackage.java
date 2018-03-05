package com.ghsc.gui.fileshare.internal;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.UUID;

import com.ghsc.gui.components.users.User;
import com.ghsc.impl.EndTaggable;
import com.ghsc.impl.Taggable;
import com.ghsc.util.Tag;
import com.ghsc.util.Utilities;

public class RemotePackage extends FilePackage {
	
	private final User host;
	private RemoteFileNode[] roots;
	private boolean passwordProtected;
	
	public RemotePackage(final User user, final String name, final String description, String creationDateS, final Visibility visibility, final boolean passwordProtected, final String uuid) {
		super(name, description, parseCalendar(new SimpleDateFormat(DATE_FORMAT), creationDateS), visibility);
		this.host = user;
		this.uuid = UUID.fromString(uuid);
		this.passwordProtected = passwordProtected;
	}
	
	@Override
	public UUID getUUID() {
		return this.uuid;
	}
	
	public User getHost() {
		return this.host;
	}
	
	@Override
	public String getOwner() {
		return this.host.getPreferredName();
	}
	
	@Override
	public boolean isPasswordProtected() {
		return this.passwordProtected;
	}
	
	public RemoteFileNode[] getRoots() {
		return this.roots;
	}
	
	public void setRoots(final RemoteFileNode[] roots) {
		this.roots = roots;
	}
	
	@Override
	public long getFileCount() {
		if (this.fileCount == null) {
			long temp = 0;
			for (final RemoteFileNode node : this.getRoots()) {
                temp += node.getFileCount();
            }
			this.fileCount = temp;
		}
		return this.fileCount;
	}

	@Override
	public long getDirectoryCount() {
		if (this.directoryCount == null) {
			long temp = 0;
			for (final RemoteFileNode node : this.getRoots()) {
                temp += node.getDirectoryCount();
            }
			this.directoryCount = temp;
		}
		return this.directoryCount;
	}
	
	@Override
	public long getSize() {
		if (this.size == null) {
			long temp = 0;
			for (final RemoteFileNode node : this.getRoots()) {
                temp += node.getSize();
            }
			this.size = temp;
		}
		return this.size;
	}
	
	public static RemotePackage parse(final User u, final String meta) {
		return parse(u, Tag.parse(meta));
	}
	
	public static RemotePackage parse(final User u, final Tag pTag) {
		// parse package info...
		if (pTag == null) {
            return null;
        }
		String name = null, uuid = null, vA = null, cD = null;
		if (!pTag.getName().equals(TAGNAME) || (name = pTag.getAttribute(ATT_NAME)) == null || 
			(uuid = pTag.getAttribute(ATT_UUID)) == null || (vA = pTag.getAttribute(ATT_VISIBILITY)) == null ||
			(cD = pTag.getAttribute(ATT_CREATIONDATE)) == null) {
            return null;
        }
		final int vIndex = vA.indexOf(':');
		final String[] vD = { vA.substring(0, vIndex), vA.substring(vIndex + 1, vA.length()) };
		final RemotePackage rp = new RemotePackage(u, name, pTag.getAttribute(ATT_DESCRIPTION), cD, new Visibility(Visibility.Type.match(vD[0]), vD.length > 1 ? vD[1] : null), Utilities.resolveToBoolean(pTag.getAttribute(ATT_PASSWORDPROTECTED)), uuid);
		rp.setDownloadCount(Long.parseLong(pTag.getAttribute(ATT_DOWNLOADCOUNT)));
		
		final RemoteFileNodeChildren rChildren = new RemoteFileNodeChildren(null);
		final LinkedList<EndTaggable> tagStack = new LinkedList<EndTaggable>();
		tagStack.push(rChildren); // initialize with RemoteNodeChildren.
		final StringBuilder build = new StringBuilder(pTag.getPost());
		EndTaggable peek = rChildren;
		while (build.length() > 0) {
			if (Utilities.startsWith(build, peek.getEndTag())) { // build starts with endTag
				build.delete(0, peek.getEndTag().length());
				final Taggable popT = tagStack.pop();
				peek = tagStack.peek();
				if (peek != null) {
					peek.receive(popT);
				} else {
                    break;
                }
			} else {
				final Tag newTag = new Tag(build).parseBasic();
				if (newTag != null) {
					final EndTaggable resolve = peek.createForTag(newTag);
					if (resolve != null) {
						tagStack.push(resolve);
						peek = resolve;
						build.delete(0, newTag.getEncodedLength());
						continue;
					}
				}
				return null;
			}
		}
		RemoteFileNode[] nodes = rChildren.toArray(new RemoteFileNode[rChildren.size()]);
		rp.setRoots(nodes);
		return rp;
	}
	
}