package com.ghsc.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

import com.ghsc.gui.Application;
import com.ghsc.net.encryption.SHA2;

public class DevPassHash {
	
	public static void main(String[] args) throws Exception {
		AdminControl control = new AdminControl();
		Scanner scan = new Scanner(System.in);
		String line;
		while (true) {
			System.out.print("What password would you like to generate: ");
			if ((line = scan.nextLine()).equals("exit")) {
				break;
			}
			if (line.equals("read")) {
				System.out.println("Your password is: " + control.refreshPassword());
			} else if (line.equals("validate")) {
				System.out.print("Password: ");
				String pass = scan.nextLine();
				System.out.println(control.validate(pass) ? "Password is correct!" : "Password is incorrect :(");
			} else {
				byte[] hash = SHA2.hash512Bytes(line);
				if (hash == null) {
					System.out.println("Hashing failed!");
					continue;
				}
				System.out.println("Result: " + new String(hash, Application.CHARSET));
				System.out.print("Would you like to commit (Y/N): ");
				String commitResult = scan.nextLine();
				if (commitResult.toLowerCase().charAt(0) == 'y') {
					String commitPath = "C://Users//Bradley//workspace//bin//p";
					FileOutputStream fos = null;
					try {
						File cF = new File(commitPath);
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
	}
	
}