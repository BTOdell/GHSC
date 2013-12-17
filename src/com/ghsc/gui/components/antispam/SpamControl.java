package com.ghsc.gui.components.antispam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.ghsc.common.Debug;
import com.ghsc.gui.Application;
import com.ghsc.util.FixedArrayQueue;

/**
 * Prevents users from spamming chat channels. (Doesn't protect PM spamming)
 * @author Odell
 */
public class SpamControl {
	
	private Object spamSync = new Object();
	
	private HashMap<String, SpamBan> banned;
	private HashMap<String, Integer> offences;
	private FixedArrayQueue<Long> times;
	private long lastOffence = -1;
	private String lastChannel = null;
	
	public SpamControl() {
		times = new FixedArrayQueue<Long>(3);
		banned = new HashMap<String, SpamBan>();
		offences = new HashMap<String, Integer>();
	}
	
	public SpamBan getChannelBan(String channel) {
		synchronized (spamSync) {
			return getChannelBanI(channel);
		}
	}
	
	private SpamBan getChannelBanI(String channel) {
		if (!channel.startsWith("#") || !banned.containsKey(channel))
			return null;
		SpamBan sb = banned.get(channel);
		if (sb.finished()) {
			banned.remove(channel);
			return null;
		}
		return sb;
	}
	
	public void reset() {
		synchronized (spamSync) {
			banned.clear();
			offences.clear();
			times.clear();
			lastOffence = -1;
			lastChannel = null;
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
	public int filter(String message, String channel) {
		synchronized (spamSync) {
			if (lastOffence > 0 && offences.size() > 0 && 
					System.currentTimeMillis() - lastOffence > (offences.get(channel) == 1 ? 30000 : 300000)) {
				offences.clear();
			}
			if (getChannelBanI(channel) != null)
				return 3;
			if (lastChannel != null) {
				if (lastChannel.equals(channel)) {
					long curr = System.currentTimeMillis();
					Long firstL = times.add(curr);
					if (firstL == null)
						return 0;
					if (times.isFull()) {
						long first = (long) firstL;
						if (curr - first < 2400) { // 2000 - 3000
							lastOffence = curr;
							if (offences.containsKey(channel)) {
								long time = 60000L * (long) Math.pow(3, offences.get(channel) - 1);
								if (Debug.MAJOR.compareTo(Application.DEBUG) < 0)
									System.out.println("Time: " + time);
								banned.put(channel, new SpamBan(lastOffence, time));
								offences.put(channel, offences.get(channel) + 1);
								return 2;
							} else {
								offences.put(channel, 1);
								return 1;
							}
						}
					}
				} else {
					times.clear();
				}
			}
			lastChannel = channel;
			return 0;
		}
	}
	
	/**
	 * Loads the SpamControl with the given Settings data.
	 * @param data - the data to load the SpamControl with.
	 */
	public void load(String[] data) {
		for (String s : data) {
			String[] mini = s.split(Pattern.quote("|"), 3);
			SpamBan sb = new SpamBan(Long.parseLong(mini[1]), Long.parseLong(mini[2]));
			if (sb.finished())
				continue;
			banned.put(mini[0], sb);
		}
	}
	
	/**
	 * @return CHANNEL|AT|TIME,CHANNEL|AT|TIME, | | | , ...
	 */
	public String printBanned() {
		StringBuilder build = new StringBuilder();
		synchronized (spamSync) {
			Iterator<String> it = banned.keySet().iterator();
			while (it.hasNext()) {
				String channel = it.next();
				SpamBan sb = banned.get(channel);
				if (sb.finished())
					continue;
				build.append(channel);
				build.append('|');
				build.append(sb.at);
				build.append('|');
				build.append(sb.time);
				build.append(',');
			}
		}
		if (build.length() > 0)
			build.deleteCharAt(build.length() - 1);
		return build.toString();
	}
	
	public class SpamBan {
		
		long at, time;
		
		private SpamBan(long at, long time) {
			this.at = at;
			this.time = time;
		}
		
		private long elapsed() {
			return System.currentTimeMillis() - at;
		}
		
		public boolean finished() {
			return remaining() <= 0;
		}
		
		public long duration() {
			return time;
		}
		
		public long remaining() {
			return time - elapsed();
		}
		
	}
	
}