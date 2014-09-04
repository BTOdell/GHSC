package com.ghsc.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public abstract class ObjectConverter<E> implements IObjectConverter<E> {
	
	private final E obj;
	
	public ObjectConverter(E obj) {
		this.obj = obj;
	}
	
	public E getObject() {
		return obj;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof ObjectConverter) {
			obj = ((ObjectConverter) obj).getObject();
		}
		return this.obj == null ? obj == null : this.obj.equals(obj);
	}
	
	@Override
	public String toString() {
		return convert(obj);
	}
	
	public static <E> List<ObjectConverter<E>> wrap(final IObjectConverter<E> converter, E... data) {
		return wrap(converter, Arrays.asList(data));
	}
	
	public static <E> List<ObjectConverter<E>> wrap(final IObjectConverter<E> converter, Collection<E> elements) {
		final ArrayList<ObjectConverter<E>> collection = new ArrayList<ObjectConverter<E>>(elements.size());
		final Iterator<E> it = elements.iterator();
		while (it.hasNext()) {
			collection.add(new ObjectConverter<E>(it.next()) {
				public String convert(E obj) {
					return converter.convert(obj);
				}
			});
		}
		return collection;
	}
	
	public static <E> List<E> unwrap(Collection<ObjectConverter<E>> wrapped) {
		final ArrayList<E> collection = new ArrayList<E>(wrapped.size());
		final Iterator<ObjectConverter<E>> it = wrapped.iterator();
		while (it.hasNext()) {
			collection.add(it.next().getObject());
		}
		return collection;
	}
	
}