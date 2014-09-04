package com.ghsc.gui.fileshare.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;

import com.ghsc.common.Fonts;
import com.ghsc.event.EventListener;
import com.ghsc.gui.components.autocomplete.AutoComplete;
import com.ghsc.gui.components.autocomplete.ObjectToStringConverter;
import com.ghsc.impl.InputVerifier;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public class VisibilityDialog<E> extends JDialog {
	
	private static final long serialVersionUID = 1L;
	
	private final PackageWizard wizard;
	private final Collection<E> available, contents;
	private final EventListener<ArrayList<E>> callback;
	private final InputVerifier<E> inputVerifier;
	private final ObjectToStringConverter converter;
	private final boolean strict;
	
	private JPanel canvas;
	private ACComboBox entryBox;
	private DefaultComboBoxModel comboModel;
	private JScrollPane scrollPane;
	private JList list;
	private DefaultListModel listModel;
	private JButton cancelButton;
	private JButton okButton;

	/**
	 * Create the dialog.
	 */
	public VisibilityDialog(PackageWizard wizard, Collection<E> available, Collection<E> contents, 
			EventListener<ArrayList<E>> callback, InputVerifier<E> inputVerifier, ObjectToStringConverter converter, boolean strict) {
		super(wizard);
		this.wizard = wizard;
		this.available = available;
		this.contents = contents;
		this.callback = callback;
		this.inputVerifier = inputVerifier;
		this.converter = converter;
		this.strict = strict;
		
		Iterator<E> it = this.contents.iterator();
		while (it.hasNext()) {
			getListModel().addElement(it.next());
		}
		it = this.available.iterator();
		while (it.hasNext()) {
			E e = it.next();
			if (this.contents.contains(e))
				continue;
			getComboModel().addElement(e);
		}
		
		initComponents();
	}
	
	private void close() {
		callback.eventReceived(null);
		dispose();
	}
	
	private void initComponents() {
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setTitle(wizard.getTitle() + " - Edit visibility arguments");
		setSize(325, 400);
		setMinimumSize(getSize());
		setLocationRelativeTo(wizard);
		
		canvas = new JPanel();
		GroupLayout groupLayout = new GroupLayout(canvas);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(getScrollPane(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 264, Short.MAX_VALUE)
						.addComponent(getEntryBox(), Alignment.LEADING, 0, 264, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(getOkButton(), GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(getCancelButton(), GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(getEntryBox(), GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(getScrollPane(), GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(getCancelButton())
						.addComponent(getOkButton()))
					.addContainerGap())
		);
		canvas.setLayout(groupLayout);
		setContentPane(canvas);
	}
	
	public ACComboBox getEntryBox() {
		if (entryBox == null) {
			entryBox = new ACComboBox(getComboModel());
		}
		return entryBox;
	}
	
	public DefaultComboBoxModel getComboModel() {
		if (comboModel == null) {
			comboModel = new DefaultComboBoxModel();
		}
		return comboModel;
	}
	
	public JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setViewportView(getList());
		}
		return scrollPane;
	}
	
	public JList getList() {
		if (list == null) {
			list = new JList(getListModel());
			list.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_DELETE) {
						@SuppressWarnings("unchecked")
						final E selected = (E) getList().getSelectedValue();
						if (selected != null) {
							if (getListModel().removeElement(selected) &&
									available.contains(selected)) {
								getComboModel().addElement(selected);
							}
						}
					}
				}
			});
		}
		return list;
	}
	
	public DefaultListModel getListModel() {
		if (listModel == null) {
			listModel = new DefaultListModel();
		}
		return listModel;
	}
	
	public JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton("OK");
			okButton.addActionListener(new ActionListener() {
				@SuppressWarnings("unchecked")
				public void actionPerformed(ActionEvent e) {
					final int size = getListModel().size();
					final ArrayList<E> a = new ArrayList<E>(size);
					for (int i = 0; i < size; i++) {
						a.add((E) getListModel().get(i));
					}
					
					callback.eventReceived(a);
					dispose();
				}
			});
		}
		return okButton;
	}
	
	public JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					close();
				}
			});
		}
		return cancelButton;
	}
	
	public class ACComboBox extends JComboBox {
		
		private static final long serialVersionUID = 1L;
		
		private ACComboBox(ComboBoxModel model) {
			super(model);
			initComponents();
			AutoComplete.enable(this, converter, strict);
		}

		private void initComponents() {
			getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
				@SuppressWarnings("unchecked")
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() != KeyEvent.VK_ENTER)
						return;
					final E selected = (E) ACComboBox.this.getSelectedItem();
					System.out.println("Selected: " + selected);
					final E verified = inputVerifier.verify(selected);
					System.out.println("Verified: " + selected);
					getComboModel().removeElement(verified);
					getListModel().addElement(verified);
					ACComboBox.this.setSelectedItem(null);
					getList().setSelectedValue(verified, true);
				}
			});
			setFont(Fonts.GLOBAL);
			setEditable(true);
			setDoubleBuffered(true);
		}
		
		@Override
		public void setModel(ComboBoxModel model) {
			super.setModel(model);
			if (!strict) {
				setSelectedIndex(-1);
			}
		}
		
	}
	
}