package com.ghsc.util;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Line2D;

import com.ghsc.util.SnapAdapter.Side.Align;
import com.ghsc.util.SnapAdapter.Side.Type;

public class SnapAdapter extends ComponentAdapter {
		
	private final Window parent, win;
	private final Magnet[][] mags;
	@SuppressWarnings("unused")
	private int pLastX, pLastY, wLastX, wLastY;
	private boolean snapped = false;
	
	public SnapAdapter(final Window to, final Window win, final Magnet[][] mags) {
		this.parent = to;
		this.win = win;
		this.mags = mags;
		Point tmp = this.parent.getLocation();
		pLastX = tmp.x;
		pLastY = tmp.y;
		tmp = this.win.getLocation();
		wLastX = tmp.x;
		wLastY = tmp.y;
	}
	
	public void setEnabled(boolean enabled) {
		if (enabled) {
			this.parent.addComponentListener(this);
			this.win.addComponentListener(this);
		} else {
			this.parent.removeComponentListener(this);
			this.win.removeComponentListener(this);
		}
	}
	
	public boolean snap(final int magIndex, final boolean align) {
		if (magIndex < 0 || magIndex >= mags.length)
			throw new IndexOutOfBoundsException("magIndex (" + magIndex + ") is out of bounds: 0 to " + (mags.length - 1));
		Rectangle parentRect = parent.getBounds(), compRect = win.getBounds();
		switch (mags[magIndex][0].side.type) { // parent
			case TOP: // finished!
				switch (mags[magIndex][1].side.type) { // comp
					case TOP:
						win.setLocation(align ? mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, parentRect.y);
						return snapped = true;
					case BOTTOM:
						win.setLocation(align ? mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, parentRect.y + compRect.height);
						return snapped = true;
					case LEFT:
						break;
					case RIGHT:
						break;
					default:
						break;
				}
				return snapped = false;
			case RIGHT: // finished!
				switch (mags[magIndex][1].side.type) {
					case LEFT:
						win.setLocation(parentRect.x + parentRect.width, align ? mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return snapped = true;
					case RIGHT:
						win.setLocation((parentRect.x + parentRect.width) - compRect.width, align ? mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return snapped = true;
					case BOTTOM:
						break;
					case TOP:
						break;
					default:
						break;
				}
				return snapped = false;
			case BOTTOM: // finished!
				switch (mags[magIndex][1].side.type) { // comp
					case TOP:
						win.setLocation(align ? mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, parentRect.y + parentRect.height);
						return snapped = true;
					case BOTTOM:
						win.setLocation(align ? mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, (parentRect.y + parentRect.height) - compRect.height);
						return snapped = true;
					case LEFT:
						break;
					case RIGHT:
						break;
					default:
						break;
				}
				return snapped = false;
			case LEFT: // finished!
				switch (mags[magIndex][1].side.type) {
					case LEFT:
						win.setLocation(parentRect.x, align ? mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return snapped = true;
					case RIGHT:
						win.setLocation(parentRect.x - compRect.width, align ? mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return snapped = true;
					case BOTTOM:
						break;
					case TOP:
						break;
					default:
						break;
				}
				return snapped = false;
		}
		return snapped = false;
	}
	
	@Override
	public void componentMoved(ComponentEvent e) {
		final Component source = e.getComponent();
		if (source != null) {
			final Rectangle rect = source.getBounds();
			//System.out.println("Component: " + comp.hasFocus());
			//System.out.println("Parent: " + parent.hasFocus());
			if (source.equals(win)) {
				if (!win.isFocused())
					return;
				// component moved
				final Rectangle parentRect = parent.getBounds();
				Rectangle parentHitRect;
				boolean tempSnap = false;
				loop: for (int i = 0; i < mags.length; i++) {
					/*
					 * If magnet succeeds break from for loop.
					 */
					final Magnet[] pair = mags[i];
					final Magnet parentMagnet = pair[0], compMagnet = pair[1];
					final Line2D compLine = compMagnet.side.createLine(rect);
					switch (parentMagnet.side.type) { // parent
						case TOP: // finished!
							if (compMagnet.side.type == Type.TOP ||
									compMagnet.side.type == Type.BOTTOM) {
								parentHitRect = new Rectangle(parentRect.x, parentRect.y - compMagnet.outer, parentRect.width, compMagnet.outer + compMagnet.inner);
								if (parentHitRect.intersectsLine(compLine) ||
										parentHitRect.contains(compLine.getP1())) {
									// check align distance
									final Region reg = compMagnet.side.createAlignmentRegion(parentRect, rect);
									tempSnap = snap(i, reg != null && reg.contains(rect.getLocation()));
									break loop;
								}
							}
							break;
						case RIGHT: // finished!
							if (compMagnet.side.type == Type.RIGHT ||
									compMagnet.side.type == Type.LEFT) {
								parentHitRect = new Rectangle((parentRect.x + parentRect.width) - compMagnet.inner, parentRect.y, compMagnet.inner + compMagnet.outer, parentRect.height);
								if (parentHitRect.intersectsLine(compLine) ||
										parentHitRect.contains(compLine.getP1())) {
									// check align distance
									final Region reg = compMagnet.side.createAlignmentRegion(parentRect, rect);
									tempSnap = snap(i, reg != null && reg.contains(rect.getLocation()));
									break loop;
								}
							}
							break;
						case BOTTOM: // finished!
							if (compMagnet.side.type == Type.BOTTOM ||
									compMagnet.side.type == Type.TOP) {
								parentHitRect = new Rectangle(parentRect.x, (parentRect.y + parentRect.height) - compMagnet.inner, parentRect.width, compMagnet.inner + compMagnet.outer);
								if (parentHitRect.intersectsLine(compLine) ||
										parentHitRect.contains(compLine.getP1())) {
									// check align distance
									final Region reg = compMagnet.side.createAlignmentRegion(parentRect, rect);
									tempSnap = snap(i, reg != null && reg.contains(rect.getLocation()));
									break loop;
								}
							}
							break;
						case LEFT: // finished!
							if (compMagnet.side.type == Type.LEFT ||
									compMagnet.side.type == Type.RIGHT) {
								parentHitRect = new Rectangle(parentRect.x - compMagnet.outer, parentRect.y, parentRect.width, compMagnet.outer + compMagnet.inner);
								if (parentHitRect.intersectsLine(compLine) ||
										parentHitRect.contains(compLine.getP1())) {
									// check align distance
									final Region reg = compMagnet.side.createAlignmentRegion(parentRect, rect);
									tempSnap = snap(i, reg != null && reg.contains(rect.getLocation()));
									break loop;
								}
							}
							break;
					}
				}
				snapped = tempSnap;
				wLastX = rect.x;
				wLastY = rect.y;
				//System.out.println("Component moved: {" + rect.x + ", " + rect.y + "}");
			} else if (source.equals(parent)) {
				// parent moved
				if (snapped)
					win.setLocation(win.getX() + (rect.x - pLastX), win.getY() + (rect.y - pLastY));
				pLastX = rect.x;
				pLastY = rect.y;
				//System.out.println("Parent moved: {" + rect.x + ", " + rect.y + "}");
			}
		}
	}
	
	public void componentResized(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentHidden(ComponentEvent e) {}
	
	public static Magnet[] createMagnets(final Side[] sides, final int distance) {
		return createMagnets(sides, distance, distance);
	}
	
	public static Magnet[] createMagnets(final Side[] sides, final int inner, final int outer) {
		return new Magnet[] {
			new Magnet(sides[0], inner, outer), new Magnet(sides[1], inner, outer)
		};
	}
	
	public static Side[] createSides(final Type parentType, final Type compType, final Align align) {
		return createSides(parentType, compType, align, -1);
	}
	
	public static Side[] createSides(final Type parentType, final Type compType, final Align align, final int alignDist) {
		return new Side[] {
			new Side(parentType, align, alignDist), new Side(compType, align, alignDist)
		};
	}
	
	public static final class Magnet {
		
		private final Side side;
		private final int inner, outer;
		
		private Magnet(final Side s, final int inner, final int outer) {
			this.side = s;
			this.inner = inner;
			this.outer = outer;
		}
		
	}
	
	public static final class Side {
		
		public enum Align { LEFT, CENTER, RIGHT, UP, DOWN }
		public enum Type { TOP, RIGHT, BOTTOM, LEFT }
		
		private final Type type;
		private final Align align;
		private final int alignDist;
		
		private Side(final Type type, final Align align, final int alignDist) {
			this.type = type;
			this.align = align;
			this.alignDist = alignDist;
		}
		
		private Line2D createLine(final Rectangle rect) {
			switch (type) {
				case TOP:
					return new Line2D.Double(rect.x, rect.y, rect.x + rect.width, rect.y);
				case RIGHT:
					return new Line2D.Double(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height);
				case BOTTOM:
					return new Line2D.Double(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height);
				case LEFT:
					return new Line2D.Double(rect.x, rect.y, rect.x, rect.y + rect.height);
			}
			return null;
		}
		
		// create function for alignment
		private Region createAlignmentRegion(final Rectangle parentRect, final Rectangle compRect) {
			switch (type) {
				case TOP:
				case BOTTOM:
					final int alignX = alignX(parentRect, compRect);
					return new Region() {
						public boolean contains(final Point orig) {
							return alignDist > Math.abs(orig.x - alignX);
						}
					};
				case LEFT:
				case RIGHT:
					final int alignY = alignY(parentRect, compRect);
					return new Region() {
						public boolean contains(final Point orig) {
							return alignDist > Math.abs(orig.y - alignY);
						}
					};
			}
			return null;
		}
		
		private int alignX(final Rectangle parentRect, final Rectangle compRect) {
			switch (align) {
				case LEFT:
					return parentRect.x;
				case CENTER:
					return parentRect.x + ((parentRect.width / 2) - (compRect.width / 2));
				case RIGHT:
					return (parentRect.x + parentRect.width) - compRect.width;
				case DOWN:
					break;
				case UP:
					break;
				default:
					break;
			}
			return compRect.x;
		}
		
		private int alignY(final Rectangle parentRect, final Rectangle compRect) {
			switch (align) {
				case UP:
					return parentRect.y;
				case CENTER:
					return parentRect.y + ((parentRect.height / 2) - (compRect.height / 2));
				case DOWN:
					return (parentRect.y + parentRect.height) - compRect.height;
				case LEFT:
					break;
				case RIGHT:
					break;
				default:
					break;
			}
			return compRect.y;
		}
		
	}
	
	private interface Region {
		public boolean contains(Point p);
	}
	
}