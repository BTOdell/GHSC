package com.ghsc.gui.fileshare.components;

import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;

import com.ghsc.common.Fonts;

public class FileShareFileChooser extends JFileChooser {
	
	private static final long serialVersionUID = 1L;
	
	public FileShareFileChooser(File startDirectory) {
		super(startDirectory);
		init();
	}
	
	private void init() {
		setApproveButtonMnemonic(KeyEvent.VK_ENTER);
		setApproveButtonText("Accept");
		setApproveButtonToolTipText("Accept the current selected files.");
		setDialogTitle("Select files to share");
		setDialogType(CUSTOM_DIALOG);
		setDoubleBuffered(true);
		setDragEnabled(false);
		setFileSelectionMode(FILES_AND_DIRECTORIES);
		setFont(Fonts.GLOBAL);
		setMultiSelectionEnabled(true);
	}

}