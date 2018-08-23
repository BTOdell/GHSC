package ghsc.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * TODO
 */
public abstract class ObjectConverter<E> implements Function<E, String> {
	
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
		return this.apply(this.obj);
	}
	
	@SafeVarargs
	public static <E> List<ObjectConverter<E>> wrap(final Function<E, String> converter, final E... data) {
		return wrap(converter, Arrays.asList(data));
	}
	
	public static <E> List<ObjectConverter<E>> wrap(final Function<E, String> converter, final Collection<E> elements) {
		final ArrayList<ObjectConverter<E>> collection = new ArrayList<>(elements.size());
		for (final E element : elements) {
			collection.add(new ObjectConverter<E>(element) {
				public String apply(final E obj) {
					return converter.apply(obj);
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