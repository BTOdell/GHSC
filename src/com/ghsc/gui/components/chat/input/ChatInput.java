package com.ghsc.gui.components.chat.input;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import com.ghsc.common.Fonts;
import com.ghsc.event.EventProvider;
import com.ghsc.event.global.EventManager;
import com.ghsc.gui.MainFrame;
import com.ghsc.gui.components.util.PromptHandler;

public class ChatInput extends JTextArea {
	
	private static final long serialVersionUID = 1L;
	
	public static final String SENDMESSAGE_EVENTPROVIDER = "chatinput.sendmessage";
	
	MainFrame frame;
	private final EventProvider<String> sendMessageProvider;
	
	public ChatInput() {
		super();
		this.sendMessageProvider = new EventProvider<String>(SENDMESSAGE_EVENTPROVIDER);
		EventManager.getEventManager().add(sendMessageProvider);
	}
	
	public ChatInput(MainFrame frame) {
		this();
		this.frame = frame;
		init();
	}
	
	private void init() {
		
		setTransferHandler(new TransferHandler());
		setDropMode(DropMode.INSERT);
		setEnabled(false);
		setFont(Fonts.GLOBAL.deriveFont(11));
		addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (e.isShiftDown()) {
						insert("\n", getCaretPosition());
					} else {
						sendMessage();
						e.consume();
					}
				}
			}
		});
		setDoubleBuffered(true);
		setColumns(10);
		new PromptHandler(this, "Type something...", Color.GRAY, JLabel.TOP);
		
		getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent arg0) {
				updateLineCount();
			}
			public void insertUpdate(DocumentEvent arg0) {
				updateLineCount();
			}
			public void removeUpdate(DocumentEvent arg0) {
				updateLineCount();
			}
			private void updateLineCount() {
                int lineCount = getLineCount();
                if (lineCount <= 5) {
                	setRows(lineCount);
                	frame.getChatPanel().revalidate();
                }
            }
		});
	}
	
	public void sendMessage() {
		sendMessageProvider.fireEvent(getText());
	}

	private class TransferHandler extends javax.swing.TransferHandler {
		
		private static final long serialVersionUID = 1L;

		public TransferHandler() {
			super();
		}
		
		public boolean canImport(TransferHandler.TransferSupport support) {
			//chatFocusGained();
			return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
		}
		
		public boolean importData(TransferHandler.TransferSupport support) {
			try {
				DataFlavor[] supportedFlavors = support.getDataFlavors();
				for (DataFlavor flavor : supportedFlavors) {
					System.out.println("Flavor: " + flavor);
					if (DataFlavor.stringFlavor.equals(flavor)) {
						BufferedReader read = new BufferedReader(flavor.getReaderForText(support.getTransferable()));
						StringBuilder build = new StringBuilder();
						String line;
						while ((line = read.readLine()) != null) {
							build.append(line);
						}
						String text = build.toString();
						JTextComponent.DropLocation dLoc = ChatInput.this.getDropLocation();
						if (dLoc != null) {
							int index = dLoc.getIndex();
							ChatInput.this.insert(text, index);
							ChatInput.this.setCaretPosition(index + text.length());
						} else {
							ChatInput.this.insert(text, ChatInput.this.getCaretPosition());
						}
						//check();
						break;
					} else if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
						// TODO: paste image!
					}
				}
				System.out.println();
				return super.importData(support);
			} catch (UnsupportedFlavorException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}
		
		public int getSourceActions(JComponent c) {
		    return COPY;
		}
		
	}
	
}