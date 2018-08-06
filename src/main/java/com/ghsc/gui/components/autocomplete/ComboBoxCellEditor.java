package com.ghsc.gui.components.autocomplete;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

@SuppressWarnings("rawtypes")
public class ComboBoxCellEditor extends DefaultCellEditor {
	
	private static final long serialVersionUID = 1L;
	
	public ComboBoxCellEditor(final JComboBox comboBox) {
		super(comboBox);
		comboBox.removeActionListener(this.delegate);
		this.delegate = new EditorDelegate() {
			private static final long serialVersionUID = 1L;
			
			public void setValue(final Object value) {
				comboBox.setSelectedItem(value);
			}
			
			public Object getCellEditorValue() {
				return comboBox.getSelectedItem();
			}
			
			public boolean shouldSelectCell(final EventObject anEvent) {
				if (anEvent instanceof MouseEvent) {
					final MouseEvent e = (MouseEvent) anEvent;
					return e.getID() != MouseEvent.MOUSE_DRAGGED;
				}
				return true;
			}
			
			public boolean stopCellEditing() {
				if (comboBox.isEditable()) {
					comboBox.actionPerformed(new ActionEvent(ComboBoxCellEditor.this, 0, ""));
				}
				return super.stopCellEditing();
			}
			
			public void actionPerformed(final ActionEvent e) {
				final JTextComponent editorComponent = (JTextComponent) comboBox.getEditor().getEditorComponent();
				if (editorComponent.getDocument() instanceof ACDocument) {
					final ACDocument document = (ACDocument) editorComponent.getDocument();
					if (!document.selecting) {
						ComboBoxCellEditor.this.stopCellEditing();
					}
				} else {
					ComboBoxCellEditor.this.stopCellEditing();
				}
			}
		};
		comboBox.addActionListener(this.delegate);
	}
	
}