package ghsc.gui.fileshare.components;

import ghsc.gui.components.button.BufferedCheckBoxMenuItem;

import java.util.function.Predicate;

@SuppressWarnings("rawtypes")
public class FilterCheckBoxMenuItem<E extends Predicate> extends BufferedCheckBoxMenuItem {
	
	private static final long serialVersionUID = 1L;

	private final E filter;
	
	public FilterCheckBoxMenuItem(final E filter) {
		super();
		this.filter = filter;
	}
	
	public FilterCheckBoxMenuItem(final boolean stayOpen, final E filter) {
		super(stayOpen);
		this.filter = filter;
	}
	
	public FilterCheckBoxMenuItem(final String text, final E filter) {
		super(text);
		this.filter = filter;
	}
	
	public FilterCheckBoxMenuItem(final String text, final boolean stayOpen, final E filter) {
		super(text, stayOpen);
		this.filter = filter;
	}
	
	public final E getFilter() {
		return this.filter;
	}
	
}