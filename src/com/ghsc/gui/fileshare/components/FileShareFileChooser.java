package com.ghsc.gui.fileshare.components;

import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;

import com.ghsc.common.Fonts;

public class FileShareFileChooser extends JFileChooser {
	
	private static final long serialVersionUID = 1L;
	
	public FileShareFileChooser(final File startDirectory) {
		super(startDirectory);
        this.init();
	}
	
	private void init() {
        this.setApproveButtonMnemonic(KeyEvent.VK_ENTER);
        this.setApproveButtonText("Accept");
        this.setApproveButtonToolTipText("Accept the current selected files.");
        this.setDialogTitle("Select files to share");
        this.setDialogType(CUSTOM_DIALOG);
        this.setDoubleBuffered(true);
        this.setDragEnabled(false);
        this.setFileSelectionMode(FILES_AND_DIRECTORIES);
        this.setFont(Fonts.GLOBAL);
        this.setMultiSelectionEnabled(true);
	}

}