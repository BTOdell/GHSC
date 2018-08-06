package com.ghsc.net.update;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * This is a simple version class which contains a three number versioning system.<br>
 * Version format: { major.minor.revision }<br>
 * The status number corresponds to:<br>
 * 1 - indev
 * 2 - pre-alpha<br>
 * 3 - alpha<br>
 * 4 - beta<br>
 * 5 - preview<br>
 * 0 - release<br>
 * @author Odell
 */
public class Version implements Comparable<Version> {
	
	public static final String COMPATIBLE = "c";
	public static final String REQUIRED = "r";
	public static final String FORCED = "f";
	
	private final int[] version = new int[3];
	private String status;
	private final ArrayList<String> flags;
	
	private Version() {
		this.flags = new ArrayList<>();
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public void setStatus(final String status) {
		this.status = status;
	}
	
	public boolean hasFlag(final String flag) {
		return this.flags.contains(flag);
	}
	
	/**
	 * Creates a Version from the given String representation.
	 * @param from - the String representation to parse from.
	 * @return a version from the string.
	 */
	public static Version parse(final String from) {
		final Version v = new Version();
		final String[] split = from.split(Pattern.quote(" "));
		final String[] versionSplit = split[0].split(Pattern.quote("."), v.version.length);
		for (int i = 0; i < versionSplit.length; i++) {
			try {
				v.version[i] = Integer.parseInt(versionSplit[i]);
			} catch (final NumberFormatException nfe) {
				return null;
			}
		}
		//noinspection ManualArrayToCollectionCopy
		for (int i = 1; i < split.length; i++) {
			v.flags.add(split[i]);
		}
		return v;
	}
	
	public static Version create(final String parse, final String status) {
		final Version v = parse(parse);
		if (v == null) {
			return null;
		}
		v.setStatus(status);
		return v;
	}
	
	/**
	 * Format: #.#.# (status)</br>
	 * @return a detailed String representation of this version.
	 */
	public String getDetailed() {
		final StringBuilder sb = new StringBuilder(this.toString());
		final String statusStr = this.getStatus();
		if (statusStr != null) {
			sb.append(" (").append(statusStr).append(")");
		}
		return sb.toString();
	}
	
	/**
	 * Format: #.#.#
	 * @return a basic String representation of this version.
	 */
	public String toString() {
		final StringBuilder build = new StringBuilder();
		for (int i = 0; i < this.version.length; i++) {
			build.append(this.version[i]);
			if (i + 1 < this.version.length) {
				build.append('.');
			}
		}
		return build.toString();
	}

	@Override
	public int compareTo(final Version o) {
		for (int i = 0; i < this.version.length; i++) {
			final int diff = o.version[i] - this.version[i];
			if (diff == 0) {
				continue;
			}
			return diff;
		}
		return 0;
	}
	
	@Override
	public boolean equals(final Object o) {
		return o instanceof Version && this.compareTo((Version) o) == 0;
	}
	
	@Override
	public int hashCode() {
		return (this.version[0] << 2) + (this.version[1] << 1) + this.version[2];
	}
	
}