package com.ghsc.util;

import javax.xml.bind.DatatypeConverter;

import com.ghsc.gui.Application;

/**
 * @author Odell
 */
public class Base64 {
	
	public static String encode(final String orig) {
		if (orig == null) {
            return null;
        }
		return DatatypeConverter.printBase64Binary(orig.getBytes(Application.CHARSET));
	}
	
	public static String decode(final String base64) {
		if (base64 == null) {
            return null;
        }
		return new String(DatatypeConverter.parseBase64Binary(base64), Application.CHARSET);
	}
	
}