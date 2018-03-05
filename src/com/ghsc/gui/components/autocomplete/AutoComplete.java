package com.ghsc.gui.components.autocomplete;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.UIResource;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.TextAction;

import com.ghsc.gui.components.autocomplete.workarounds.MacOSXPopupLocationFix;

@SuppressWarnings({"serial", "rawtypes"})
public final class AutoComplete {
	
	private static final List<String> COMBO_BOX_ACTIONS = Collections.unmodifiableList(Arrays.asList("selectNext", "selectNext2", "selectPrevious", "selectPrevious2", "pageDownPassThrough", "pageUpPassThrough", "homePassThrough", "endPassThrough"));
	
	private static final Object errorFeedbackAction = new TextAction("provide-error-feedback") {
		public void actionPerformed(ActionEvent e) {
			UIManager.getLookAndFeel().provideErrorFeedback(this.getTextComponent(e));
		}
	};
	
	private AutoComplete() {
		// prevent instantiation
	}
	
	public static void enable(JComboBox comboBox, boolean strict) {
		enable(comboBox, null, strict);
	}
	
	public static void enable(JComboBox comboBox, ObjectToStringConverter converter, boolean strict) {
		disable(comboBox);
		comboBox.setEditable(true);
		// fix the popup location
		MacOSXPopupLocationFix.install(comboBox);
		
		JTextComponent editorComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
		final ACAdapter adapter = new ComboBoxAdapter(comboBox);
		final ACDocument document = createACDocument(adapter, strict, converter, editorComponent.getDocument());
		enable(editorComponent, document, adapter);
		editorComponent.addKeyListener(new KeyAdapter(comboBox));
		// set before adding the listener for the editor
		comboBox.setEditor(new ACComboBoxEditor(comboBox.getEditor(), document.getConverter()));
		// Changing the l&f can change the combobox' editor which in turn
		// would not be autocompletion-enabled. The new editor needs to be set-up.
		PropertyChangeListener pcl = new PropertyChangeListener(comboBox);
		comboBox.addPropertyChangeListener("editor", pcl);
		comboBox.addPropertyChangeListener("enabled", pcl);
		
		if (!strict) {
			ActionMap map = comboBox.getActionMap();
			for (String key : COMBO_BOX_ACTIONS) {
				Action a = map.get(key);
				map.put(key, new SelectionAction(a));
			}
		}
	}
	
	public static void enable(JList list, JTextComponent textComponent) {
		enable(list, textComponent, null);
	}
	
	public static void enable(JList list, JTextComponent textComponent, ObjectToStringConverter stringConverter) {
		disable(list);
		ACAdapter adapter = new ListAdapter(list, textComponent, stringConverter);
		ACDocument document = createACDocument(adapter, true, stringConverter, textComponent.getDocument());
		enable(textComponent, document, adapter);
	}
	
	public static void enable(JTextComponent textComponent, List<?> items, boolean strictMatching) {
		enable(textComponent, items, strictMatching, null);
	}
	
	public static void enable(JTextComponent textComponent, List<?> items, boolean strictMatching, ObjectToStringConverter stringConverter) {
		ACAdapter adapter = new TextComponentAdapter(textComponent, items);
		ACDocument document = createACDocument(adapter, strictMatching, stringConverter, textComponent.getDocument());
		enable(textComponent, document, adapter);
	}
	
	public static void enable(JTextComponent textComponent, ACDocument document, ACAdapter adapter) {
		disable(textComponent);
		textComponent.setDocument(document);
		// mark entire text when the text component gains focus
		// otherwise the last mark would have been retained which is quiet confusing
		textComponent.addFocusListener(new AutoComplete.FocusAdapter(adapter));
		// Tweak some key bindings
		javax.swing.InputMap editorInputMap = textComponent.getInputMap();
		while (editorInputMap != null) {
			javax.swing.InputMap parent = editorInputMap.getParent();
			if (parent instanceof UIResource) {
				installMap(editorInputMap, document.isStrictMatching());
				break;
			}
			editorInputMap = parent;
		}
		ActionMap editorActionMap = textComponent.getActionMap();
		editorActionMap.put("nonstrict-backspace", new NonStrictBackspaceAction(editorActionMap.get(DefaultEditorKit.deletePrevCharAction), editorActionMap.get(DefaultEditorKit.selectionBackwardAction), adapter));
	}

	static void disable(JComboBox comboBox) {
		JTextComponent editorComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
		if (editorComponent.getDocument() instanceof ACDocument) {
			ACDocument doc = (ACDocument) editorComponent.getDocument();
			if (doc.isStrictMatching()) {
				ActionMap map = comboBox.getActionMap();
				for (String key : COMBO_BOX_ACTIONS) {
					map.put(key, null);
				}
			}
			// remove old property change listener
			for (java.beans.PropertyChangeListener l : comboBox.getPropertyChangeListeners("editor")) {
				if (l instanceof AutoComplete.PropertyChangeListener) {
					comboBox.removePropertyChangeListener("editor", l);
				}
			}
			for (java.beans.PropertyChangeListener l : comboBox.getPropertyChangeListeners("enabled")) {
				if (l instanceof AutoComplete.PropertyChangeListener) {
					comboBox.removePropertyChangeListener("enabled", l);
				}
			}
			ACComboBoxEditor editor = (ACComboBoxEditor) comboBox.getEditor();
			comboBox.setEditor(editor.wrapped);
			// remove old key listener
			for (KeyListener l : editorComponent.getKeyListeners()) {
				if (l instanceof AutoComplete.KeyAdapter) {
					editorComponent.removeKeyListener(l);
					break;
				}
			}
			disable(editorComponent);
			for (ActionListener l : comboBox.getActionListeners()) {
				if (l instanceof ComboBoxAdapter) {
					comboBox.removeActionListener(l);
					break;
				}
			}
			// TODO remove aqua fix
			// TODO reset editibility
		}
	}

	static void disable(JList list) {
		for (ListSelectionListener l : list.getListSelectionListeners()) {
			if (l instanceof ListAdapter) {
				list.removeListSelectionListener(l);
				break;
			}
		}
	}

	static void disable(JTextComponent textComponent) {
		Document doc = textComponent.getDocument();
		if (doc instanceof ACDocument) {
			// remove autocomplete key/action mappings
			javax.swing.InputMap map = textComponent.getInputMap();
			while (map.getParent() != null) {
				javax.swing.InputMap parent = map.getParent();
				if (parent instanceof AutoComplete.InputMap) {
					map.setParent(parent.getParent());
				}
				map = parent;
			}
			textComponent.getActionMap().put("nonstrict-backspace", null);
			// remove old focus listener
			for (FocusListener l : textComponent.getFocusListeners()) {
				if (l instanceof AutoComplete.FocusAdapter) {
					textComponent.removeFocusListener(l);
					break;
				}
			}
			// reset to original document
			textComponent.setDocument(((ACDocument) doc).getDelegate());
		}
	}
	
	private static void installMap(javax.swing.InputMap componentMap, boolean strict) {
		InputMap map = new AutoComplete.InputMap();
		if (strict) {
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), DefaultEditorKit.selectionBackwardAction);
			// ignore VK_DELETE and CTRL+VK_X and beep instead when strict matching
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), errorFeedbackAction);
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), errorFeedbackAction);
		} else {
			// VK_BACKSPACE will move the selection to the left if the selected item is in the list
			// it will delete the previous character otherwise
			map.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "nonstrict-backspace");
			// leave VK_DELETE and CTRL+VK_X as is
		}
		map.setParent(componentMap.getParent());
		componentMap.setParent(map);
	}
	
	static ACDocument createACDocument(ACAdapter adapter, boolean strictMatching, ObjectToStringConverter stringConverter, Document delegate) {
		if (delegate instanceof StyledDocument) {
			return new ACStyledDocument(adapter, strictMatching, stringConverter, (StyledDocument) delegate);
		}
		return new ACDocument(adapter, strictMatching, stringConverter, delegate);
	}

	static class NonStrictBackspaceAction extends TextAction {
		
		Action backspace, selectionBackward;
		ACAdapter adapter;

		public NonStrictBackspaceAction(Action backspace, Action selectionBackward, ACAdapter adapter) {
			super("nonstrict-backspace");
			this.backspace = backspace;
			this.selectionBackward = selectionBackward;
			this.adapter = adapter;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.adapter.listContainsSelectedItem()) {
                this.selectionBackward.actionPerformed(e);
			} else {
                this.backspace.actionPerformed(e);
			}
		}
	}
	
	static class InputMap extends javax.swing.InputMap {
		private static final long serialVersionUID = 1L;
	}
	
	static class FocusAdapter extends java.awt.event.FocusAdapter {
		private ACAdapter adapter;

		public FocusAdapter(ACAdapter adaptor) {
			this.adapter = adaptor;
		}

		@Override
		public void focusGained(FocusEvent e) {
            this.adapter.markAll();
		}
	}

	static class KeyAdapter extends java.awt.event.KeyAdapter {
		private JComboBox comboBox;

		public KeyAdapter(JComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// don't popup on action keys (cursor movements, etc...)
			if (e.isActionKey()) {
				return;
			}
			// don't popup if the combobox isn't visible anyway
			if (this.comboBox.isDisplayable() && !this.comboBox.isPopupVisible()) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_SHIFT: case KeyEvent.VK_CONTROL: case KeyEvent.VK_ALT:
					case KeyEvent.VK_ENTER: case KeyEvent.VK_ESCAPE:
						break;
					default:
                        this.comboBox.setPopupVisible(true);
				}
			}
		}
	}

	static class PropertyChangeListener implements java.beans.PropertyChangeListener {
		private JComboBox comboBox;

		public PropertyChangeListener(JComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if ("editor".equals(evt.getPropertyName())) {
				this.handleEditor(evt);
			} else if ("enabled".equals(evt.getPropertyName())) {
				this.handleEnabled(evt);
			}
		}

		private void handleEnabled(PropertyChangeEvent evt) {
			if (Boolean.TRUE.equals(evt.getNewValue())) {
                this.comboBox.setEditable(true);
			} else {
				JTextComponent textComponent = (JTextComponent) this.comboBox.getEditor().getEditorComponent();
				boolean strictMatching = ((ACDocument) textComponent.getDocument()).isStrictMatching();
                this.comboBox.setEditable(!strictMatching);
			}
		}

		private void handleEditor(PropertyChangeEvent evt) {
			if (evt.getNewValue() instanceof ACComboBoxEditor) {
				return;
			}
			ACComboBoxEditor acEditor = (ACComboBoxEditor) evt.getOldValue();
			boolean strictMatching = false;
			if (acEditor.getEditorComponent() != null) {
				JTextComponent textComponent = (JTextComponent) acEditor.getEditorComponent();
				strictMatching = ((ACDocument) textComponent.getDocument()).isStrictMatching();
				disable(textComponent);
				for (KeyListener l : textComponent.getKeyListeners()) {
					if (l instanceof KeyAdapter) {
						textComponent.removeKeyListener(l);
						break;
					}
				}
			}
			JTextComponent editorComponent = (JTextComponent) this.comboBox.getEditor().getEditorComponent();
			ACAdapter adapter = new ComboBoxAdapter(this.comboBox);
			ACDocument document = createACDocument(adapter, strictMatching, acEditor.converter, editorComponent.getDocument());
			enable(editorComponent, document, adapter);
			editorComponent.addKeyListener(new AutoComplete.KeyAdapter(this.comboBox));
			// set before adding the listener for the editor
            this.comboBox.setEditor(new ACComboBoxEditor(this.comboBox.getEditor(), document.getConverter()));
		}
	}

	static class SelectionAction implements Action {
		private Action delegate;

		public SelectionAction(Action delegate) {
			this.delegate = delegate;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JComboBox comboBox = (JComboBox) e.getSource();
			JTextComponent textComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
			ACDocument doc = (ACDocument) textComponent.getDocument();
			// doing this prevents the updating of the selected item to "" during the remove prior
			// to the insert in JTextComponent.setText
			doc.strictMatching = true;
			try {
                this.delegate.actionPerformed(e);
			} finally {
				doc.strictMatching = false;
			}
		}
		
		@Override
		public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
            this.delegate.addPropertyChangeListener(listener);
		}
		
		@Override
		public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
            this.delegate.removePropertyChangeListener(listener);
		}
		
		@Override
		public Object getValue(String key) {
			return this.delegate.getValue(key);
		}
		
		@Override
		public void putValue(String key, Object value) {
            this.delegate.putValue(key, value);
		}
		
		@Override
		public boolean isEnabled() {
			return this.delegate.isEnabled();
		}

		@Override
		public void setEnabled(boolean b) {
            this.delegate.setEnabled(b);
		}
	}
	
}