package com.ghsc.util;

import java.lang.reflect.Array;

public class Utilities {
	
	@SafeVarargs
	public static <E> E[] merge(E[] array, E... es) {
		@SuppressWarnings("unchecked")
		final E[] alloc = (E[]) Array.newInstance(array.getClass().getComponentType(), array.length + es.length);
		System.arraycopy(array, 0, alloc, 0, array.length);
		System.arraycopy(es, 0, alloc, array.length, es.length);
		return alloc;
	}
	
	/**
	 * Checks to see if the list of elements, contains the given element.
	 * @param element - the element to look for.
	 * @param elements - the elements to search through.
	 * @return <code>true</code>, if the element was found, otherwise <code>false</code>.
	 */
	@SafeVarargs
	public static <E> boolean contains(E element, E... elements) {
		for (E e : elements) {
            if (element == null ? e == null : element.equals(e)) {
                return true;
            }
        }
		return false;
	}
	
	public static int noQuotesIndexOf(final CharSequence content, final CharSequence search) {
		boolean quotes = false;
		final int max = content.length() - search.length();
		o: for (int i = 0; i < max; i++) {
			if (quotes) {
				quotes &= content.charAt(i) != '"';
				continue;
			}
			for (int j = i, s = 0; s < search.length(); j++, s++) {
				char cc = content.charAt(j);
				if ((quotes |= (cc == '"')) || (cc != search.charAt(s))) {
                    continue o;
                }
			}
			return i;
		}
		return -1;
	}
	
	public static boolean startsWith(final CharSequence content, final CharSequence search) {
		final int contentLength = content.length(), searchLength = search.length();
		if (searchLength > contentLength) {
            return false;
        }
		for (int i = 0; i < searchLength; i++) {
			if (content.charAt(i) != search.charAt(i)) {
                return false;
            }
		}
		return true;
	}
	
	public static int countLength(int i) {
		return ((int) Math.log10(i)) + 1;
	}
	
	private static final String BOOLEAN_TRUE = "t", BOOLEAN_FALSE = "f";
	
	public static boolean resolveToBoolean(final Object o) {
		if (o == null) {
            return false;
        }
		if (o instanceof Boolean) {
            return (Boolean) o;
        }
		return o.toString().equals(BOOLEAN_TRUE);
	}
	
	public static String resolveToString(final boolean b) {
		return b ? BOOLEAN_TRUE : BOOLEAN_FALSE;
	}
	
}