package com.ghsc.util;

import java.util.Calendar;

/**
 * Holds data to display a simple clock of HH:MM:SS.
 */
public class TimeStamp {
	
	public enum Style { Hour24, Hour12 }
	
	private final byte h;
	private final byte m;
	private final byte s;
	private boolean showSeconds = true;
	
	/**
	 * Initializes a new TimeStamp with the given hours, minutes and seconds.
	 */
	private TimeStamp(final byte h, final byte m, final byte s) {
		this.h = h;
		this.m = m;
		this.s = s;
	}
	
	/**
	 * Sets whether to display seconds on the end of the String from {@link #print(Style)}.
	 */
	public void setShowSeconds(final boolean show) {
		this.showSeconds = show;
	}
	
	/**
	 * Creates a String displaying the time in HH:MM:SS format.</br>
	 * Is {@link #showSeconds} is set to false, seconds will not display on the end of the String returned.
	 * @param style - The 12/24 hour format to use.
	 * @return a String representing the time of this TimeStamp.
	 */
	public String print(final Style style) {
		final StringBuilder build = new StringBuilder();
		byte h = this.h;
		if (style == Style.Hour12) {
			if (h > 12) {
                h -= 12;
            }
		}
		if (h < 10) {
            build.append("0");
        }
		build.append(h);
		build.append(":");
		if (this.m < 10) {
            build.append("0");
        }
		build.append(this.m);
		if (this.showSeconds) {
			build.append(":");
			if (this.s < 10) {
                build.append("0");
            }
			build.append(this.s);
		}
		return build.toString();
	}
	
	/**
	 * Creates a new TimeStamp representing the current time of this function call.
	 * @return the newly generated TimeStamp object.
	 */
	public static TimeStamp newInstance() {
		final Calendar c = Calendar.getInstance();
		return new TimeStamp((byte) c.get(Calendar.HOUR_OF_DAY), (byte) c.get(Calendar.MINUTE), (byte) c.get(Calendar.SECOND));
	}
	
}