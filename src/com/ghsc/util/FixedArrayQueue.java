package com.ghsc.util;

/**
 * An array-based queue with a fixed element array.<br>
 * If an element is added to this queue and the queue is full, the head element is discarded.
 * @author Odell
 * @param <E>
 * 		the type of object this queue contains.
 */
public class FixedArrayQueue<E> {

	private final Object[] data;
	private int count = 0;
	
	/**
	 * Creates a new FixedArrayQueue with the given maximum size.
	 * @param max
	 * 		the maximum number of elements this queue is allowed to hold at once.
	 * @throws IllegalArgumentException if max is less than or equal to 0.
	 */
	public FixedArrayQueue(final int max) {
		if (max <= 0) 
			throw new IllegalArgumentException("Max: can't be less than or equal to 0.");
		data = new Object[max];
	}
	
	/**
	 * Adds the element to the end of this queue.
	 * @param element
	 * 		the element to add.
	 * @return if the queue is full, the element at the head will be removed and returned, otherwise null.
	 */
	public E add(final E element) {
		return insert(getCount(), element);
	}
	
	/**
	 * Clears the entire queue.
	 */
	public void clear() {
		while (count > 0)
			data[--count] = null;
	}
	
	/**
	 * Fetches the element at the given index.
	 * @param index
	 * 		the index to fetch the element at.
	 * @return the element at the given index.
	 * @throws IndexOutOfBoundsException if the index was out of the array bounds.
	 */
	@SuppressWarnings("unchecked")
	public E get(int index) {
		if (index < 0 || index >= getCount())
			throw new IndexOutOfBoundsException("The index given (" + index + ") was out of bounds: [0 to " + (getCount() - 1) + "]");
		return (E) data[index];
	}
	
	/**
	 * @return how many elements are currently in this queue.
	 */
	public int getCount() {
		return count;
	}
	
	/**
	 * Determines whether this queue is full.
	 * @return <tt>true</tt> if the queue is full, <tt>false</tt> if it's not.
	 */
	public boolean isFull() {
		return getCount() >= size();
	}
	
	/**
	 * Inserts the element into the queue at the given index.
	 * @param index
	 * 		the index to insert the element.
	 * @param element
	 * 		the element to insert.
	 * @return if the queue is full, the element at the head will be removed and returned, otherwise null.
	 */
	public E insert(int index, final E element) {
		E temp = null;
		if (isFull()) {
			temp = remove(0, index);
			index--;
		} else {
			int i = count;
			while (i > index)
				data[i] = data[--i];
			if (count < size())
				count++;
		}
		data[index] = element;
		return temp;
	}
	
	/**
	 * Assigns a new element for the index in the array.
	 * @param index
	 * 		the index to set the new element.
	 * @param element
	 * 		the element to set at the given index.
	 * @return the previous element at the given index, or null if none existed.
	 */
	public E set(final int index, final E element) {
		final E temp = get(index);
		data[index] = element;
		return temp;
	}
	
	/**
	 * Removes the element at the head of this queue.
	 * @return the removed element.
	 */
	public E remove() {
		return remove(0);
	}
	
	/**
	 * Removes the element at this index in the queue.
	 * @param index
	 * 		the index in the queue.
	 * @return the removed element.
	 */
	public E remove(int index) {
		return remove(index, getCount() - 1);
	}
	
	private E remove(int index, final int shiftIndex) {
		final E temp = get(index);
		data[index] = null;
		int max = Math.min(shiftIndex, getCount() - 1);
		while (index < max) {
			data[index] = data[++index];
			data[index] = null;
		}
		if (count > 0)
			count--;
		return temp;
	}
	
	/**
	 * Removes the first element in the queue that equals the given element.
	 * @param element
	 * 		the element to match.
	 * @return an instance that's equal to the given element.
	 */
	public E remove(E element) {
		for (int i = 0; i < size(); i++) {
			final E temp = get(i);
			if (element == null) {
				if (temp == null) {
					remove(i);
					return temp;
				}
			} else {
				if (element.equals(temp)) {
					remove(i);
					return temp;
				}
			}
		}
		return null;
	}
	
	/**
	 * @return the maximum size of this fixed queue.
	 */
	public int size() {
		return data.length;
	}
	
	@Override
	public String toString() {
		final StringBuilder build = new StringBuilder().append("[");
		for (int i = 0; i < getCount(); i++) {
			build.append(data[i].toString());
			if (i + 1 < getCount()) {
				build.append(",");
			}
		}
		return build.append("]").toString();
	}
	
}