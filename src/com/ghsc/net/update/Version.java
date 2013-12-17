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
	
	public static final String COMPATIBLE = "c", REQUIRED = "r", FORCED = "f";
	
	private final int[] version = new int[3];
	private String status = null;
	private final ArrayList<String> flags;
	
	private Version() {
		this.flags = new ArrayList<String>();
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(final String status) {
		this.status = status;
	}
	
	public boolean hasFlag(String flag) {
		return flags.contains(flag);
	}
	
	/**
	 * Creates a Version from the given String representation.
	 * @param from - the String representation to parse from.
	 * @return a version from the string.
	 */
	public static Version parse(String from) {
		Version v = new Version();
		String[] split = from.split(Pattern.quote(" "));
		String[] versionSplit = split[0].split(Pattern.quote("."), v.version.length);
		for (int i = 0; i < versionSplit.length; i++) {
			try {
				v.version[i] = Integer.parseInt(versionSplit[i]);
			} catch (NumberFormatException nfe) {
				return null;
			}
		}
		for (int i = 1; i < split.length; i++) {
			v.flags.add(split[i]);
		}
		return v;
	}
	
	public static Version create(String parse, String status) {
		final Version v = parse(parse);
		if (v == null)
			return null;
		v.setStatus(status);
		return v;
	}
	
	/**
	 * Format: #.#.# (status)</br>
	 * @return a detailed String representation of this version.
	 */
	public String getDetailed() {
		final StringBuilder sb = new StringBuilder(toString());
		final String statusStr = getStatus();
		if (statusStr != null)
			sb.append(" (").append(statusStr).append(")");
		return sb.toString();
	}
	
	/**
	 * Format: #.#.#
	 * @return a basic String representation of this version.
	 */
	public String toString() {
		StringBuilder build = new StringBuilder();
		for (int i = 0; i < version.length; i++) {
			build.append(version[i]);
			if (i + 1 < version.length) {
				build.append('.');
			}
		}
		return build.toString();
	}

	@Override
	public int compareTo(Version o) {
		for (int i = 0; i < version.length; i++) {
			int diff = o.version[i] - version[i];
			if (diff == 0)
				continue;
			return diff;
		}
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Version)
			return compareTo((Version) o) == 0;
		return false;
	}
	
	@Override
	public int hashCode() {
		return (version[0] << 2) + (version[1] << 1) + version[2];
	}
	
}