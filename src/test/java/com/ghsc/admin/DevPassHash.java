package com.ghsc.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

import com.ghsc.gui.Application;
import com.ghsc.net.encryption.SHA2;

public class DevPassHash {
	
	public static void main(final String[] args) throws Exception {
		final AdminControl control = new AdminControl();
		final Scanner scan = new Scanner(System.in);
		while (true) {
			System.out.print("What password would you like to generate: ");
			final String line = scan.nextLine();
			if ("exit".equals(line)) {
				break;
			}
			if ("read".equals(line)) {
				System.out.println("Your password is: " + control.refreshPassword());
			} else if ("validate".equals(line)) {
				System.out.print("Password: ");
				final String pass = scan.nextLine();
				System.out.println(control.validate(pass) ? "Password is correct!" : "Password is incorrect :(");
			} else {
				final byte[] hash = SHA2.hash512Bytes(line);
				if (hash == null) {
					System.out.println("Hashing failed!");
					continue;
				}
				System.out.println("Result: " + new String(hash, Application.CHARSET));
				System.out.print("Would you like to commit (Y/N): ");
				final String commitResult = scan.nextLine();
				if (commitResult.toLowerCase().charAt(0) == 'y') {
					final String commitPath = "C://Users//Bradley//workspace//bin//p";
					FileOutputStream fos = null;
					try {
						final File cF = new File(commitPath);
						fos = new FileOutputStream(cF);
						fos.write(hash);
					} finally {
						if (fos != null) {
							fos.flush();
							fos.close();
						}
					}
					System.out.println("Hash saved to " + commitPath);
				}
			}
		}
		scan.close();
	}
}