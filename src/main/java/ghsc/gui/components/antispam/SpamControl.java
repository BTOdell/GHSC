package ghsc.gui.components.antispam;

import java.util.HashMap;
import java.util.regex.Pattern;

import ghsc.common.Debug;
import ghsc.gui.Application;
import ghsc.util.FixedArrayQueue;

/**
 * Prevents users from spamming chat channels. (Doesn't protect PM spamming)
 */
public class SpamControl {
	
	private final Object spamSync = new Object();
	
	private final HashMap<String, SpamBan> banned;
	private final HashMap<String, Integer> offences;
	private final FixedArrayQueue<Long> times;

	private long lastOffence = -1;
	private String lastChannel;
	
	public SpamControl() {
		this.times = new FixedArrayQueue<>(3);
		this.banned = new HashMap<>();
		this.offences = new HashMap<>();
	}
	
	public SpamBan getChannelBan(final String channel) {
		synchronized (this.spamSync) {
			return this.getChannelBanI(channel);
		}
	}
	
	private SpamBan getChannelBanI(final String channel) {
		if (!channel.startsWith("#") || !this.banned.containsKey(channel)) {
            return null;
        }
		final SpamBan sb = this.banned.get(channel);
		if (sb.finished()) {
			this.banned.remove(channel);
			return null;
		}
		return sb;
	}
	
	public void reset() {
		synchronized (this.spamSync) {
			this.banned.clear();
			this.offences.clear();
			this.times.clear();
			this.lastOffence = -1;
			this.lastChannel = null;
		}
	}
	
	/**
	 * Goals:</br>
	 * - One string (not case-sensitive) can't be spammed 3 times or more times in one and a half seconds.</br>
	 * - More than 5 strings of text can't be spammed in less than 2 seconds.</br>
	 * User will be given a warning the first time, if it occurs a second time, they are locked out of the spammed channel for a minute, if it happens a third time, they are locked out for 5 minutes.</br>
	 * 5 minutes after an offense, all recorded offenses are removed.</br>
	 * @return 0 is no errors occur, 1 is a warning, 2 you violated the rules and have been restricted access, 3 you are already blocked from this channel.
	 */
	public int filter(final String message, final String channel) {
		synchronized (this.spamSync) {
			if (this.lastOffence > 0 && !this.offences.isEmpty() &&
					System.currentTimeMillis() - this.lastOffence > (this.offences.get(channel) == 1 ? 30000 : 300000)) {
				this.offences.clear();
			}
			if (this.getChannelBanI(channel) != null) {
                return 3;
            }
			if (this.lastChannel != null) {
				if (this.lastChannel.equals(channel)) {
					final long curr = System.currentTimeMillis();
					final Long firstL = this.times.add(curr);
					if (firstL == null) {
                        return 0;
                    }
					if (this.times.isFull()) {
						final long first = firstL;
						if (curr - first < 2400) { // 2000 - 3000
							this.lastOffence = curr;
							if (this.offences.containsKey(channel)) {
								final long time = 60000L * (long) Math.pow(3, this.offences.get(channel) - 1);
								if (Debug.MAJOR.compareTo(Application.DEBUG) < 0) {
                                    System.out.println("Time: " + time);
                                }
								this.banned.put(channel, new SpamBan(this.lastOffence, time));
								this.offences.put(channel, this.offences.get(channel) + 1);
								return 2;
							} else {
								this.offences.put(channel, 1);
								return 1;
							}
						}
					}
				} else {
					this.times.clear();
				}
			}
			this.lastChannel = channel;
			return 0;
		}
	}
	
	/**
	 * Loads the SpamControl with the given Settings data.
	 * @param data - the data to load the SpamControl with.
	 */
	public void load(final String[] data) {
		for (final String s : data) {
			final String[] mini = s.split(Pattern.quote("|"), 3);
			final SpamBan sb = new SpamBan(Long.parseLong(mini[1]), Long.parseLong(mini[2]));
			if (sb.finished()) {
                continue;
            }
			this.banned.put(mini[0], sb);
		}
	}
	
	/**
	 * @return CHANNEL|AT|TIME,CHANNEL|AT|TIME, | | | , ...
	 */
	public String printBanned() {
		final StringBuilder build = new StringBuilder();
		synchronized (this.spamSync) {
			for (final String channel : this.banned.keySet()) {
				final SpamBan sb = this.banned.get(channel);
				if (sb.finished()) {
					continue;
				}
				build.append(channel);
				build.append('|');
				build.append(sb.at);
				build.append('|');
				build.append(sb.time);
				build.append(',');
			}
		}
		if (build.length() > 0) {
            build.deleteCharAt(build.length() - 1);
        }
		return build.toString();
	}
	
	public class SpamBan {

	    final long at;
        final long time;
		
		private SpamBan(final long at, final long time) {
			this.at = at;
			this.time = time;
		}
		
		private long elapsed() {
			return System.currentTimeMillis() - this.at;
		}
		
		public boolean finished() {
			return this.remaining() <= 0;
		}
		
		public long duration() {
			return this.time;
		}
		
		public long remaining() {
			return this.time - this.elapsed();
		}
		
	}
	
}