package com.ghsc.files;

import java.util.Map;
import java.util.Properties;

public class EnvironmentExplorer {

	/**
	 * Main entry point for the environment explorer.
	 * @param args The application's command line arguments.
	 */
	public static void main(final String[] args) {
		System.out.println("Start of property mapping:");
		final Properties prop = System.getProperties();
		for (final Map.Entry<Object, Object> e : prop.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}
		System.out.println("End of property mapping:");
		System.out.println("user.dir: " + System.getProperty("user.dir"));
		System.out.println("user.home: " + System.getProperty("user.home"));
		System.out.println();
		System.out.println("Start of environment mapping:");
		final Map<String, String> envs = System.getenv();
		for (final Map.Entry<String, String> e : envs.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
		}
		System.out.println("End of environment mapping:");
		System.out.println();
		System.out.println("HOMESHARE -> " + System.getenv("HOMESHARE"));
	}

}
