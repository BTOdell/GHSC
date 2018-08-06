package com.ghsc.common;

import java.awt.Image;
import java.awt.Toolkit;

public class Images {
	
	/*
	 * Application icons
	 */
	public static final Image ICON_16 = get("ghs16icon.png");
	public static final Image ICON_32 = get("ghsicon.png");
	public static final Image ICON_LARGE = get("ghslarge.png");
	
	/*
	 * Miscellaneous
	 */
	public static final Image ADD_DARK = get("add_dark.png");
	public static final Image COG = get("cog.png");
	public static final Image DELETE = get("delete.png");
	public static final Image FIND = get("find.png");
	public static final Image INFORMATION = get("information.png");
	public static final Image KEY = get("key.png");
	public static final Image PENCIL = get("pencil.png");
	public static final Image BULLET_ARROW_DOWN = get("bullet_arrow_down.png");
	public static final Image EYE = get("eye.png");
	public static final Image WORLD = get("world.png");
	public static final Image GROUP = get("group.png");
	
	/*
	 * User list
	 */
	public static final Image STATUS_IN = get("status_online.png");
	public static final Image STATUS_IN_AWAY = get("status_online_away.png");
	public static final Image STATUS_IN_BUSY = get("status_online_busy.png");
	public static final Image STATUS_OUT = get("status_offline.png");
	public static final Image STATUS_OUT_AWAY = get("status_offline_away.png");
	public static final Image STATUS_OUT_BUSY = get("status_offline_busy.png");
	
	/*
	 * File share
	 */
	public static final Image PAGE_ADD = get("page_add.png");
	public static final Image PAGE_DELETE = get("page_delete.png");
	public static final Image PAGE_GO = get("page_go.png");
	public static final Image PACKAGE = get("package.png");
	public static final Image PACKAGE_ADD = get("package_add.png");
	public static final Image PACKAGE_DELETE = get("package_delete.png");
	public static final Image PACKAGE_LARGE = get("package_large.png");
	
	/*
	 * InputWizard
	 */
	public static final Image X = get("X.png");
	public static final Image CHECK = get("check.png");
			
	private static Image get(final String path) {
		return Toolkit.getDefaultToolkit().getImage(Images.class.getResource("/images/" + path));
	}
	
}