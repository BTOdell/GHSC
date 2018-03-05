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
	private int count;
	
	/**
	 * Creates a new FixedArrayQueue with the given maximum size.
	 * @param max
	 * 		the maximum number of elements this queue is allowed to hold at once.
	 * @throws IllegalArgumentException if max is less than or equal to 0.
	 */
	public FixedArrayQueue(final int max) {
		if (max <= 0) {
            throw new IllegalArgumentException("Max: can't be less than or equal to 0.");
        }
		this.data = new Object[max];
	}
	
	/**
	 * Adds the element to the end of this queue.
	 * @param element
	 * 		the element to add.
	 * @return if the queue is full, the element at the head will be removed and returned, otherwise null.
	 */
	public E add(final E element) {
		return this.insert(this.getCount(), element);
	}
	
	/**
	 * Clears the entire queue.
	 */
	public void clear() {
		while (this.count > 0) {
			this.data[--this.count] = null;
        }
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
		if (index < 0 || index >= this.getCount()) {
            throw new IndexOutOfBoundsException("The index given (" + index + ") was out of bounds: [0 to " + (this.getCount() - 1) + "]");
        }
		return (E) this.data[index];
	}
	
	/**
	 * @return how many elements are currently in this queue.
	 */
	public int getCount() {
		return this.count;
	}
	
	/**
	 * Determines whether this queue is full.
	 * @return <tt>true</tt> if the queue is full, <tt>false</tt> if it's not.
	 */
	public boolean isFull() {
		return this.getCount() >= this.size();
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
		if (this.isFull()) {
			temp = this.remove(0, index);
			index--;
		} else {
			int i = this.count;
			while (i > index) {
				this.data[i] = this.data[--i];
            }
			if (this.count < this.size()) {
				this.count++;
            }
		}
		this.data[index] = element;
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
		final E temp = this.get(index);
		this.data[index] = element;
		return temp;
	}
	
	/**
	 * Removes the element at the head of this queue.
	 * @return the removed element.
	 */
	public E remove() {
		return this.remove(0);
	}
	
	/**
	 * Removes the element at this index in the queue.
	 * @param index
	 * 		the index in the queue.
	 * @return the removed element.
	 */
	public E remove(int index) {
		return this.remove(index, this.getCount() - 1);
	}
	
	private E remove(int index, final int shiftIndex) {
		final E temp = this.get(index);
		this.data[index] = null;
		int max = Math.min(shiftIndex, this.getCount() - 1);
		while (index < max) {
			this.data[index] = this.data[++index];
			this.data[index] = null;
		}
		if (this.count > 0) {
			this.count--;
        }
		return temp;
	}
	
	/**
	 * Removes the first element in the queue that equals the given element.
	 * @param element
	 * 		the element to match.
	 * @return an instance that's equal to the given element.
	 */
	public E remove(E element) {
		for (int i = 0; i < this.size(); i++) {
			final E temp = this.get(i);
			if (element == null) {
				if (temp == null) {
					this.remove(i);
					return temp;
				}
			} else {
				if (element.equals(temp)) {
					this.remove(i);
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
		return this.data.length;
	}
	
	@Override
	public String toString() {
		final StringBuilder build = new StringBuilder().append("[");
		for (int i = 0; i < this.getCount(); i++) {
			build.append(this.data[i].toString());
			if (i + 1 < this.getCount()) {
				build.append(",");
			}
		}
		return build.append("]").toString();
	}
	
}