package com.ghsc.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A custom made XML-like markup tag.<br>
 * Features Base64 encoding for the values.
 * Format: {@code <name key="value[base64]" key="value[base64]">post }
 * @author Odell
 */
public class Tag {
	
	protected String raw, raw_encoded, name, post, tag, tag_encoded;
	protected HashMap<String, Base64Value> attributes = null;
	protected int length, length_encoded;
	protected boolean parsed = false;
	
	public Tag(final Object raw) {
		this.raw_encoded = raw.toString();
	}
	
	private Tag(final String raw, final String raw_encoded, final String name, final String post, final String tag, final String tag_encoded, final HashMap<String, Base64Value> att, final int length, final int length_encoded) {
		this.raw = raw;
		this.raw_encoded = raw_encoded;
		this.name = name;
		this.post = post;
		this.tag = tag;
		this.tag_encoded = tag_encoded;
		this.attributes = att;
		this.length = length;
		this.length_encoded = length_encoded;
		this.parsed = true;
	}
	
	/**
	 * @return the name of this tag.
	 */
	public final String getName() {
		return name;
	}
	
	/**
	 * @return the count of attributes of this tag.
	 */
	public final int getCount() {
		return attributes != null ? attributes.size() : 0;
	}
	
	/**
	 * Finds the attribute of this tag from the key.
	 * @param key
	 * 		the key mapped to the value.
	 * @return the attribute retrieved from the key.
	 */
	public final String getAttribute(final String key) {
		if (attributes == null)
			return null;
		final Base64Value v = attributes.get(key);
		return v != null ? v.getValue() : null;
	}
	
	/**
	 * @return the length of this tag not including the post data.
	 */
	public final int getLength() {
		return length;
	}
	
	public final int getEncodedLength() {
		return length_encoded;
	}
	
	public String getEncodedString() {
		return raw_encoded;
	}
	
	@Override
	public String toString() {
		return raw;
	}
	
	/**
	 * @return raw tag string excluding post data.
	 */
	public final String getTag() {
		return tag;
	}
	
	public final String getEncodedTag() {
		return tag_encoded;
	}
	
	/**
	 * Some Tags have data which is after the attributes, such as Tags that contain message info.</br>
	 * This method provides access to any post data which exists in this Tag.
	 * @return any post data that exists in this Tag, may return null.
	 */
	public final String getPost() {
		return post;
	}
	
	/**
	 * Sets the post data of this tag to null.
	 * Also modifies the raw strings.
	 */
	public void clearPost() {
		this.post = null;
		this.raw = this.raw.substring(0, getLength());
		this.raw_encoded = this.raw_encoded.substring(0, getEncodedLength());
	}
	
	public void reset() {
		parsed = false;
		resetI();
	}
	
	private void resetI() {
		attributes = null;
		name = post = tag = tag_encoded = null;
		length = length_encoded = 0;
	}
	
	/**
	 * @return whether this tag has been parsed.
	 */
	public boolean isParsed() {
		return parsed;
	}
	
	/**
	 * Parses this tag using the raw data.
	 * @return this tag after parsing.
	 */
	public Tag parse() {
		return parse(false);
	}
	
	/**
	 * Parses this tag using the raw (encoded) data.
	 * @param force whether to force parsing even if this tag is already parsed.
	 * @return this tag after parsing.
	 */
	public Tag parse(boolean force) {
		if (parsed && !force)
			return this;
		if (raw_encoded.charAt(0) != '<')
			return null;
		resetI();
		final StringBuilder build = new StringBuilder();
		boolean noQuote = true;
		int m = 1;
		while (true) {
			if (m >= raw_encoded.length())
				return null;
			final char curr = raw_encoded.charAt(m++);
			if (curr == '"') {
				noQuote = !noQuote;
			} else if (noQuote) {
				if (curr == ' ') {
					resolveBuilder(build);
					build.delete(0, build.length());
					continue;
				} else if (curr == '>') {
					resolveBuilder(build);
					length_encoded = m;
					break;
				}
			}
			build.append(curr);
		}
		tag_encoded = raw_encoded.substring(0, length_encoded);
		if (length_encoded < raw_encoded.length())
			post = raw_encoded.substring(length_encoded);
		final StringBuilder rawBuilder = new StringBuilder();
		rawBuilder.append('<').append(name);
		if (attributes != null) {
			final Set<Map.Entry<String, Base64Value>> mapSet = attributes.entrySet();
			final Iterator<Map.Entry<String, Base64Value>> it = mapSet.iterator();
			while (it.hasNext()) {
				Map.Entry<String, Base64Value> e = it.next();
				rawBuilder.append(' ').append(e.getKey()).append('=').append('"').append(e.getValue().getValue()).append('"');
			}
		}
		rawBuilder.append('>');
		tag = rawBuilder.toString();
		length = rawBuilder.length();
		if (post != null)
			rawBuilder.append(post);
		raw = rawBuilder.toString();
		parsed = true;
		return this;
	}
	
	/**
	 * Parses the tag with only encoded values valid.
	 * @return a tag with basic parsing.
	 */
	public Tag parseBasic() {
		return parseBasic(false);
	}
	
	public Tag parseBasic(boolean force) {
		if (parsed && !force)
			return this;
		if (raw_encoded.charAt(0) != '<')
			return null;
		resetI();
		final StringBuilder build = new StringBuilder();
		boolean noQuote = true;
		int m = 1;
		while (true) {
			if (m >= raw_encoded.length())
				return null;
			final char curr = raw_encoded.charAt(m++);
			if (curr == '"') {
				noQuote = !noQuote;
				continue;
			} else if (noQuote) {
				if (curr == ' ') {
					resolveBuilder(build);
					build.delete(0, build.length());
					continue;
				} else if (curr == '>') {
					resolveBuilder(build);
					length_encoded = m;
					break;
				}
			}
			build.append(curr);
		}
		tag_encoded = raw_encoded.substring(0, length_encoded);
		parsed = true;
		return this;
	}
	
	private void resolveBuilder(final StringBuilder build) {
		final int equalsIndex = build.indexOf("=");
		if (equalsIndex < 0) {
			name = build.toString();
		} else {
			final String data0 = build.substring(0, equalsIndex), 
					data1 = build.substring(equalsIndex + 2, build.length() - 1);
			if (attributes == null)
				attributes = new HashMap<String, Base64Value>();
			attributes.put(data0, new Base64Value(Base64.decode(data1), data1));
		}
	}
	
	/**
	 * Parses a new Tag from the given data.
	 * @param data
	 * 		the data to parse.
	 * @return a new parsed Tag object.
	 */
	public static Tag parse(final Object data) {
		return new Tag(data).parse();
	}
	
	/**
	 * Creates a Tag object with given name and attributes.</br>
	 * If the length of the attributes aren't even, then the last attributes is appended as post data.
	 * @return a Tag object from the arguments.
	 */
	public static Tag construct(Object name, Object... attributes) {
		int aLength = attributes.length;
		boolean odd = (aLength & 1) == 1;
		if (odd)
			aLength--;
		final StringBuilder build = new StringBuilder(), buildE = new StringBuilder();
		build.append('<').append(name.toString());
		buildE.append('<').append(name.toString());
		final HashMap<String, Base64Value> att = aLength > 0 ? new HashMap<String, Base64Value>() : null;
		int i = 0;
		while (i < aLength) {
			final Object key = attributes[i++], value = attributes[i++];
			if (key == null || value == null)
				continue;
			final String keyString = key.toString(), valueString = value.toString();
			final Base64Value bv = new Base64Value(valueString, Base64.encode(valueString));
			att.put(keyString, bv);
			build.append(' ').append(keyString).append("=\"").append(valueString).append('"');
			buildE.append(' ').append(keyString).append("=\"").append(bv.getBase64()).append('"');
		}
		build.append('>');
		buildE.append('>');
		final String tagString = build.toString(), tagEString = buildE.toString();
		if (odd)
			build.append(attributes[aLength]);
		return new Tag(build.toString(), buildE.toString(), name.toString(), odd ? attributes[aLength].toString() : null, 
				tagString, tagEString, att, tagString.length(), tagEString.length());
	}
	
	private static class Base64Value {
		
		private String value, base64;
		
		private Base64Value(final String value, final String base64) {
			this.value = value;
			this.base64 = base64;
		}
		
		public String getValue() {
			return value;
		}
		
		public String getBase64() {
			return base64;
		}
		
	}
	
}