package ghsc.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by Eclipse IDE.
 * @author Odell
 */
public abstract class ObjectConverter<E> implements IObjectConverter<E> {
	
	private final E obj;
	
	public ObjectConverter(final E obj) {
		this.obj = obj;
	}
	
	public E getObject() {
		return this.obj;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
            return false;
        }
		if (obj instanceof ObjectConverter) {
			obj = ((ObjectConverter) obj).getObject();
		}
		return this.obj == null ? obj == null : this.obj.equals(obj);
	}
	
	@Override
	public String toString() {
		return this.convert(this.obj);
	}
	
	@SafeVarargs
	public static <E> List<ObjectConverter<E>> wrap(final IObjectConverter<E> converter, final E... data) {
		return wrap(converter, Arrays.asList(data));
	}
	
	public static <E> List<ObjectConverter<E>> wrap(final IObjectConverter<E> converter, final Collection<E> elements) {
		final ArrayList<ObjectConverter<E>> collection = new ArrayList<>(elements.size());
		for (final E element : elements) {
			collection.add(new ObjectConverter<E>(element) {
				public String convert(final E obj) {
					return converter.convert(obj);
				}
			});
		}
		return collection;
	}
	
	public static <E> List<E> unwrap(final Collection<ObjectConverter<E>> wrapped) {
		final ArrayList<E> collection = new ArrayList<>(wrapped.size());
		for (final ObjectConverter<E> aWrapped : wrapped) {
			collection.add(aWrapped.getObject());
		}
		return collection;
	}
	
}