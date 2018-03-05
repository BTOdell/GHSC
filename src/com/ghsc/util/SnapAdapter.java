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
	private boolean snapped;
	
	public SnapAdapter(final Window to, final Window win, final Magnet[][] mags) {
		this.parent = to;
		this.win = win;
		this.mags = mags;
		Point tmp = this.parent.getLocation();
		this.pLastX = tmp.x;
		this.pLastY = tmp.y;
		tmp = this.win.getLocation();
		this.wLastX = tmp.x;
		this.wLastY = tmp.y;
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
		if (magIndex < 0 || magIndex >= this.mags.length) {
            throw new IndexOutOfBoundsException("magIndex (" + magIndex + ") is out of bounds: 0 to " + (this.mags.length - 1));
        }
		Rectangle parentRect = this.parent.getBounds(), compRect = this.win.getBounds();
		switch (this.mags[magIndex][0].side.type) { // parent
			case TOP: // finished!
				switch (this.mags[magIndex][1].side.type) { // comp
					case TOP:
						this.win.setLocation(align ? this.mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, parentRect.y);
						return this.snapped = true;
					case BOTTOM:
						this.win.setLocation(align ? this.mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, parentRect.y + compRect.height);
						return this.snapped = true;
					case LEFT:
						break;
					case RIGHT:
						break;
					default:
						break;
				}
				return this.snapped = false;
			case RIGHT: // finished!
				switch (this.mags[magIndex][1].side.type) {
					case LEFT:
						this.win.setLocation(parentRect.x + parentRect.width, align ? this.mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return this.snapped = true;
					case RIGHT:
						this.win.setLocation((parentRect.x + parentRect.width) - compRect.width, align ? this.mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return this.snapped = true;
					case BOTTOM:
						break;
					case TOP:
						break;
					default:
						break;
				}
				return this.snapped = false;
			case BOTTOM: // finished!
				switch (this.mags[magIndex][1].side.type) { // comp
					case TOP:
						this.win.setLocation(align ? this.mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, parentRect.y + parentRect.height);
						return this.snapped = true;
					case BOTTOM:
						this.win.setLocation(align ? this.mags[magIndex][1].side.alignX(parentRect, compRect) : compRect.x, (parentRect.y + parentRect.height) - compRect.height);
						return this.snapped = true;
					case LEFT:
						break;
					case RIGHT:
						break;
					default:
						break;
				}
				return this.snapped = false;
			case LEFT: // finished!
				switch (this.mags[magIndex][1].side.type) {
					case LEFT:
						this.win.setLocation(parentRect.x, align ? this.mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return this.snapped = true;
					case RIGHT:
						this.win.setLocation(parentRect.x - compRect.width, align ? this.mags[magIndex][1].side.alignY(parentRect, compRect) : compRect.y);
						return this.snapped = true;
					case BOTTOM:
						break;
					case TOP:
						break;
					default:
						break;
				}
				return this.snapped = false;
		}
		return this.snapped = false;
	}
	
	@Override
	public void componentMoved(ComponentEvent e) {
		final Component source = e.getComponent();
		if (source != null) {
			final Rectangle rect = source.getBounds();
			//System.out.println("Component: " + comp.hasFocus());
			//System.out.println("Parent: " + parent.hasFocus());
			if (source.equals(this.win)) {
				if (!this.win.isFocused()) {
                    return;
                }
				// component moved
				final Rectangle parentRect = this.parent.getBounds();
				Rectangle parentHitRect;
				boolean tempSnap = false;
				loop: for (int i = 0; i < this.mags.length; i++) {
					/*
					 * If magnet succeeds break from for loop.
					 */
					final Magnet[] pair = this.mags[i];
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
									tempSnap = this.snap(i, reg != null && reg.contains(rect.getLocation()));
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
									tempSnap = this.snap(i, reg != null && reg.contains(rect.getLocation()));
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
									tempSnap = this.snap(i, reg != null && reg.contains(rect.getLocation()));
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
									tempSnap = this.snap(i, reg != null && reg.contains(rect.getLocation()));
									break loop;
								}
							}
							break;
					}
				}
				this.snapped = tempSnap;
				this.wLastX = rect.x;
				this.wLastY = rect.y;
				//System.out.println("Component moved: {" + rect.x + ", " + rect.y + "}");
			} else if (source.equals(this.parent)) {
				// parent moved
				if (this.snapped) {
					this.win.setLocation(this.win.getX() + (rect.x - this.pLastX), this.win.getY() + (rect.y - this.pLastY));
                }
				this.pLastX = rect.x;
				this.pLastY = rect.y;
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
			switch (this.type) {
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
			switch (this.type) {
				case TOP:
				case BOTTOM:
					final int alignX = this.alignX(parentRect, compRect);
					return new Region() {
						public boolean contains(final Point orig) {
							return Side.this.alignDist > Math.abs(orig.x - alignX);
						}
					};
				case LEFT:
				case RIGHT:
					final int alignY = this.alignY(parentRect, compRect);
					return new Region() {
						public boolean contains(final Point orig) {
							return Side.this.alignDist > Math.abs(orig.y - alignY);
						}
					};
			}
			return null;
		}
		
		private int alignX(final Rectangle parentRect, final Rectangle compRect) {
			switch (this.align) {
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
			switch (this.align) {
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
		boolean contains(Point p);
	}
	
}