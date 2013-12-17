package com.ghsc.gui.components.autocomplete;

import static com.ghsc.gui.components.autocomplete.ObjectToStringConverter.DEFAULT;

import java.util.Comparator;

import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;

public class ACDocument implements Document {

	private static final Comparator<String> EQUALS_IGNORE_CASE = new Comparator<String>() {
		public int compare(String o1, String o2) {
			return o1.equalsIgnoreCase(o2) ? 0 : -1;
		}
	};
	private static final Comparator<String> STARTS_WITH_IGNORE_CASE = new Comparator<String>() {
		public int compare(String o1, String o2) {
			if (o1.length() < o2.length()) return -1;
			return o1.regionMatches(true, 0, o2, 0, o2.length()) ? 0 : -1;
		}
	};
	private static final Comparator<String> EQUALS = new Comparator<String>() {
		public int compare(String o1, String o2) {
			return o1.equals(o2) ? 0 : -1;
		}
	};
	private static final Comparator<String> STARTS_WITH = new Comparator<String>() {
		public int compare(String o1, String o2) {
			return o1.startsWith(o2) ? 0 : -1;
		}
	};
	
	protected final Document delegate;
	private final Handler handler;
	ACAdapter adapter;
	private ObjectToStringConverter converter;
	protected boolean strictMatching;
	boolean selecting = false;
	
	public ACDocument(ACAdapter adapter, boolean strictMatching, ObjectToStringConverter converter, Document delegate) {
		if (adapter == null) {
			throw new IllegalArgumentException("adapter cannot be null");
		}
		this.adapter = adapter;
		this.strictMatching = strictMatching;
		this.converter = converter == null ? DEFAULT : converter;
		this.delegate = delegate == null ? createDefaultDocument() : delegate;
		
		this.handler = new Handler();
		this.delegate.addDocumentListener(handler);
		if (isStrictMatching()) {
			// Handle initially selected object
			final Object selected = adapter.getSelectedItem();
			if (selected != null) {
				final String itemStr = this.converter.getPreferredStringForItem(selected);
				setText(itemStr);
				this.adapter.setSelectedItemString(itemStr);
			}
		}
		this.adapter.markAll();
	}
	
	public ACDocument(ACAdapter adapter, boolean strictMatching, ObjectToStringConverter stringConverter) {
		this(adapter, strictMatching, stringConverter, null);
	}
	
	public ACDocument(ACAdapter adapter, boolean strictMatching) {
		this(adapter, strictMatching, null);
	}
	
	Document getDelegate() {
		return delegate;
	}
	
	ObjectToStringConverter getConverter() {
		return converter;
	}

	protected Document createDefaultDocument() {
		return new PlainDocument();
	}

	@Override
	public void remove(int offs, int len) throws BadLocationException {
		// return immediately when selecting an item
		if (selecting) return;
		getDelegate().remove(offs, len);
		if (!strictMatching) {
			setSelectedItem(getText(0, getLength()), getText(0, getLength()));
			adapter.getTextComponent().setCaretPosition(offs);
		}
	}

	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
		// return immediately when selecting an item
		if (selecting) return;
		// insert the string into the document
		getDelegate().insertString(offs, str, a);
		// lookup and select a matching item
		LookupResult lookupResult;
		String pattern = getText(0, getLength());
		if (pattern == null || pattern.length() == 0) {
			lookupResult = new LookupResult(null, "");
			setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
		} else {
			lookupResult = lookupItem(pattern);
		}
		if (lookupResult.matchingItem != null) {
			setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
		} else {
			if (strictMatching) {
				// keep old item selected if there is no match
				lookupResult.matchingItem = adapter.getSelectedItem();
				lookupResult.matchingString = adapter.getSelectedItemString();
				// imitate no insert (later on offs will be incremented by
				// str.length(): selection won't move forward)
				offs = str == null ? offs : offs - str.length();
				if (str != null && !str.isEmpty()) {
					// provide feedback to the user that his input has been received but can not be accepted
					UIManager.getLookAndFeel().provideErrorFeedback(adapter.getTextComponent());
				}
			} else {
				// no item matches => use the current input as selected item
				lookupResult.matchingItem = getText(0, getLength());
				lookupResult.matchingString = getText(0, getLength());
				setSelectedItem(lookupResult.matchingItem, lookupResult.matchingString);
			}
		}
		setText(lookupResult.matchingString);
		// select the completed part
		int len = str == null ? 0 : str.length();
		offs = lookupResult.matchingString == null ? 0 : offs + len;
		adapter.markFrom(offs);
	}
	
	private void setText(String text) {
		try {
			// remove all text and insert the completed string
			getDelegate().remove(0, getLength());
			getDelegate().insertString(0, text, null);
		} catch (BadLocationException e) {
			throw new RuntimeException(e.toString());
		}
	}
	
	private void setSelectedItem(Object item, String itemAsString) {
		selecting = true;
		adapter.setSelectedItem(item);
		adapter.setSelectedItemString(itemAsString);
		selecting = false;
	}
	
	private LookupResult lookupItem(String pattern) {
		Object selectedItem = adapter.getSelectedItem();
		LookupResult lookupResult;
		// first try: case sensitive
		lookupResult = lookupItem(pattern, EQUALS);
		if (lookupResult != null) return lookupResult;
		lookupResult = lookupOneItem(selectedItem, pattern, STARTS_WITH);
		if (lookupResult != null) return lookupResult;
		lookupResult = lookupItem(pattern, STARTS_WITH);
		if (lookupResult != null) return lookupResult;
		// second try: ignore case
		lookupResult = lookupItem(pattern, EQUALS_IGNORE_CASE);
		if (lookupResult != null) return lookupResult;
		lookupResult = lookupOneItem(selectedItem, pattern, STARTS_WITH_IGNORE_CASE);
		if (lookupResult != null) return lookupResult;
		lookupResult = lookupItem(pattern, STARTS_WITH_IGNORE_CASE);
		if (lookupResult != null) return lookupResult;
		// no item starts with the pattern => return null
		return new LookupResult(null, "");
	}

	private LookupResult lookupOneItem(Object item, String pattern, Comparator<String> comparator) {
		String[] possibleStrings = converter.getPossibleStringsForItem(item);
		if (possibleStrings != null) {
			for (int j = 0; j < possibleStrings.length; j++) {
				if (comparator.compare(possibleStrings[j], pattern) == 0) {
					return new LookupResult(item, possibleStrings[j]);
				}
			}
		}
		return null;
	}

	private LookupResult lookupItem(String pattern, Comparator<String> comparator) {
		// iterate over all items and return first match
		for (int i = 0, n = adapter.getItemCount(); i < n; i++) {
			Object currentItem = adapter.getItem(i);
			LookupResult result = lookupOneItem(currentItem, pattern, comparator);
			if (result != null) return result;
		}
		return null;
	}
	
	@Override
	public void addDocumentListener(DocumentListener listener) {
		handler.addDocumentListener(listener);
	}
	
	@Override
	public void addUndoableEditListener(UndoableEditListener listener) {
		handler.addUndoableEditListener(listener);
	}
	
	@Override
	public Position createPosition(int offs) throws BadLocationException {
		return delegate.createPosition(offs);
	}
	
	@Override
	public Element getDefaultRootElement() {
		return getDelegate().getDefaultRootElement();
	}
	
	@Override
	public Position getEndPosition() {
		return getDelegate().getEndPosition();
	}
	
	@Override
	public int getLength() {
		return getDelegate().getLength();
	}
	
	@Override
	public Object getProperty(Object key) {
		return getDelegate().getProperty(key);
	}
	
	@Override
	public Element[] getRootElements() {
		return getDelegate().getRootElements();
	}
	
	@Override
	public Position getStartPosition() {
		return getDelegate().getStartPosition();
	}
	
	@Override
	public String getText(int offset, int length) throws BadLocationException {
		return getDelegate().getText(offset, length);
	}
	
	@Override
	public void getText(int offset, int length, Segment txt) throws BadLocationException {
		getDelegate().getText(offset, length, txt);
	}
	
	@Override
	public void putProperty(Object key, Object value) {
		getDelegate().putProperty(key, value);
	}
	
	@Override
	public void removeDocumentListener(DocumentListener listener) {
		handler.removeDocumentListener(listener);
	}
	
	@Override
	public void removeUndoableEditListener(UndoableEditListener listener) {
		handler.removeUndoableEditListener(listener);
	}
	
	@Override
	public void render(Runnable r) {
		getDelegate().render(r);
	}
	
	public boolean isStrictMatching() {
		return strictMatching;
	}
	
	private static class LookupResult {
		Object matchingItem;
		String matchingString;

		public LookupResult(Object matchingItem, String matchingString) {
			this.matchingItem = matchingItem;
			this.matchingString = matchingString;
		}
	}
	
	private class Handler implements DocumentListener, UndoableEditListener {
		
		private final EventListenerList listenerList = new EventListenerList();

		public void addDocumentListener(DocumentListener listener) {
			listenerList.add(DocumentListener.class, listener);
		}

		public void addUndoableEditListener(UndoableEditListener listener) {
			listenerList.add(UndoableEditListener.class, listener);
		}
		
		public void removeDocumentListener(DocumentListener listener) {
			listenerList.remove(DocumentListener.class, listener);
		}
		
		public void removeUndoableEditListener(UndoableEditListener listener) {
			listenerList.remove(UndoableEditListener.class, listener);
		}
		
		@Override
		public void changedUpdate(DocumentEvent e) {
			e = new ACDocumentEvent(ACDocument.this, e);
			// Guaranteed to return a non-null array
			Object[] listeners = listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DocumentListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((DocumentListener) listeners[i + 1]).changedUpdate(e);
				}
			}
		}
		
		@Override
		public void insertUpdate(DocumentEvent e) {
			e = new ACDocumentEvent(ACDocument.this, e);
			// Guaranteed to return a non-null array
			Object[] listeners = listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DocumentListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((DocumentListener) listeners[i + 1]).insertUpdate(e);
				}
			}
		}
		
		@Override
		public void removeUpdate(DocumentEvent e) {
			e = new ACDocumentEvent(ACDocument.this, e);
			// Guaranteed to return a non-null array
			Object[] listeners = listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == DocumentListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((DocumentListener) listeners[i + 1]).removeUpdate(e);
				}
			}
		}
		
		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			e = new UndoableEditEvent(ACDocument.this, e.getEdit());
			// Guaranteed to return a non-null array
			Object[] listeners = listenerList.getListenerList();
			// Process the listeners last to first, notifying
			// those that are interested in this event
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == UndoableEditListener.class) {
					// Lazily create the event:
					// if (e == null)
					// e = new ListSelectionEvent(this, firstIndex, lastIndex);
					((UndoableEditListener) listeners[i + 1]).undoableEditHappened(e);
				}
			}
		}
		
	}
	
}