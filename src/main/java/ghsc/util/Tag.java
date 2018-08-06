package ghsc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A custom made XML-like markup tag.<br>
 * Features Base64 encoding for the values.<br>
 * <br>
 * Format:<br>{@code <name key="value[base64]" key="value[base64]">post }
 */
public class Tag {
	
	protected String raw;
	protected String raw_encoded;
	protected String name;
	protected String post;
	protected String tag;
	protected String tag_encoded;
	protected HashMap<String, Base64Value> attributes;
	protected int length;
	protected int length_encoded;
	protected boolean parsed;
	
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
		return this.name;
	}
	
	/**
	 * @return the count of attributes of this tag.
	 */
	public final int getCount() {
		return this.attributes != null ? this.attributes.size() : 0;
	}
	
	/**
	 * Finds the attribute of this tag from the key.
	 * @param key
	 * 		the key mapped to the value.
	 * @return the attribute retrieved from the key.
	 */
	public final String getAttribute(final String key) {
		if (this.attributes == null) {
            return null;
        }
		final Base64Value v = this.attributes.get(key);
		return v != null ? v.getValue() : null;
	}
	
	/**
	 * @return the length of this tag not including the post data.
	 */
	public final int getLength() {
		return this.length;
	}
	
	public final int getEncodedLength() {
		return this.length_encoded;
	}
	
	public String getEncodedString() {
		return this.raw_encoded;
	}
	
	@Override
	public String toString() {
		return this.raw;
	}
	
	/**
	 * @return raw tag string excluding post data.
	 */
	public final String getTag() {
		return this.tag;
	}
	
	public final String getEncodedTag() {
		return this.tag_encoded;
	}
	
	/**
	 * Some Tags have data which is after the attributes, such as Tags that contain message info.</br>
	 * This method provides access to any post data which exists in this Tag.
	 * @return any post data that exists in this Tag, may return null.
	 */
	public final String getPost() {
		return this.post;
	}
	
	/**
	 * Sets the post data of this tag to null.
	 * Also modifies the raw strings.
	 */
	public void clearPost() {
		this.post = null;
		this.raw = this.raw.substring(0, this.getLength());
		this.raw_encoded = this.raw_encoded.substring(0, this.getEncodedLength());
	}
	
	public void reset() {
		this.parsed = false;
		this.resetI();
	}
	
	private void resetI() {
		this.attributes = null;
		this.name = this.post = this.tag = this.tag_encoded = null;
		this.length = this.length_encoded = 0;
	}
	
	/**
	 * @return whether this tag has been parsed.
	 */
	public boolean isParsed() {
		return this.parsed;
	}
	
	/**
	 * Parses this tag using the raw data.
	 * @return this tag after parsing.
	 */
	public Tag parse() {
		return this.parse(false);
	}
	
	/**
	 * Parses this tag using the raw (encoded) data.
	 * @param force whether to force parsing even if this tag is already parsed.
	 * @return this tag after parsing.
	 */
	public Tag parse(final boolean force) {
		if (this.parsed && !force) {
            return this;
        }
		if (this.raw_encoded.charAt(0) != '<') {
            return null;
        }
		this.resetI();
		final StringBuilder build = new StringBuilder();
		boolean noQuote = true;
		int m = 1;
		while (true) {
			if (m >= this.raw_encoded.length()) {
                return null;
            }
			final char curr = this.raw_encoded.charAt(m++);
			if (curr == '"') {
				noQuote = !noQuote;
			} else if (noQuote) {
				if (curr == ' ') {
					this.resolveBuilder(build);
					build.delete(0, build.length());
					continue;
				} else if (curr == '>') {
					this.resolveBuilder(build);
					this.length_encoded = m;
					break;
				}
			}
			build.append(curr);
		}
		this.tag_encoded = this.raw_encoded.substring(0, this.length_encoded);
		if (this.length_encoded < this.raw_encoded.length()) {
			this.post = this.raw_encoded.substring(this.length_encoded);
        }
		final StringBuilder rawBuilder = new StringBuilder();
		rawBuilder.append('<').append(this.name);
		if (this.attributes != null) {
			final Set<Map.Entry<String, Base64Value>> mapSet = this.attributes.entrySet();
			for (final Map.Entry<String, Base64Value> e : mapSet) {
				rawBuilder.append(' ').append(e.getKey()).append('=').append('"').append(e.getValue().getValue()).append('"');
			}
		}
		rawBuilder.append('>');
		this.tag = rawBuilder.toString();
		this.length = rawBuilder.length();
		if (this.post != null) {
            rawBuilder.append(this.post);
        }
		this.raw = rawBuilder.toString();
		this.parsed = true;
		return this;
	}
	
	/**
	 * Parses the tag with only encoded values valid.
	 * @return a tag with basic parsing.
	 */
	public Tag parseBasic() {
		return this.parseBasic(false);
	}
	
	public Tag parseBasic(final boolean force) {
		if (this.parsed && !force) {
            return this;
        }
		if (this.raw_encoded.charAt(0) != '<') {
            return null;
        }
		this.resetI();
		final StringBuilder build = new StringBuilder();
		boolean noQuote = true;
		int m = 1;
		while (true) {
			if (m >= this.raw_encoded.length()) {
                return null;
            }
			final char curr = this.raw_encoded.charAt(m++);
			if (curr == '"') {
				noQuote = !noQuote;
				continue;
			} else if (noQuote) {
				if (curr == ' ') {
					this.resolveBuilder(build);
					build.delete(0, build.length());
					continue;
				} else if (curr == '>') {
					this.resolveBuilder(build);
					this.length_encoded = m;
					break;
				}
			}
			build.append(curr);
		}
		this.tag_encoded = this.raw_encoded.substring(0, this.length_encoded);
		this.parsed = true;
		return this;
	}
	
	private void resolveBuilder(final StringBuilder build) {
		final int equalsIndex = build.indexOf("=");
		if (equalsIndex < 0) {
			this.name = build.toString();
		} else {
			final String data0 = build.substring(0, equalsIndex);
			final String data1 = build.substring(equalsIndex + 2, build.length() - 1);
			if (this.attributes == null) {
				this.attributes = new HashMap<>();
            }
			this.attributes.put(data0, new Base64Value(Base64.decode(data1), data1));
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
	public static Tag construct(final Object name, final Object... attributes) {
		int aLength = attributes.length;
		final boolean odd = (aLength & 1) == 1;
		if (odd) {
            aLength--;
        }
		final StringBuilder build = new StringBuilder();
		final StringBuilder buildE = new StringBuilder();
		build.append('<').append(name.toString());
		buildE.append('<').append(name.toString());
		final HashMap<String, Base64Value> att = aLength > 0 ? new HashMap<>() : null;
		int i = 0;
		while (i < aLength) {
			final Object key = attributes[i++];
			final Object value = attributes[i++];
			if (key == null || value == null) {
                continue;
            }
			final String keyString = key.toString();
			final String valueString = value.toString();
			final Base64Value bv = new Base64Value(valueString, Base64.encode(valueString));
			att.put(keyString, bv);
			build.append(' ').append(keyString).append("=\"").append(valueString).append('"');
			buildE.append(' ').append(keyString).append("=\"").append(bv.getBase64()).append('"');
		}
		build.append('>');
		buildE.append('>');
		final String tagString = build.toString();
		final String tagEString = buildE.toString();
		if (odd) {
			build.append(attributes[aLength]);
			buildE.append(attributes[aLength]);
		}
		return new Tag(build.toString(), buildE.toString(), name.toString(), odd ? attributes[aLength].toString() : null, 
				tagString, tagEString, att, tagString.length(), tagEString.length());
	}
	
	private static class Base64Value {
		
		private final String value;
		private final String base64;
		
		private Base64Value(final String value, final String base64) {
			this.value = value;
			this.base64 = base64;
		}
		
		public String getValue() {
			return this.value;
		}
		
		public String getBase64() {
			return this.base64;
		}
		
	}
	
}