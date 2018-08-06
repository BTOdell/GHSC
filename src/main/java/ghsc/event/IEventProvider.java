package ghsc.event;

/**
 * A read-only interface for an event provider.
 */
public interface IEventProvider<E> {

	/**
	 * Adds an event listener to the event provider.
	 * @param listener The listener to add.
	 * @return Whether the listener was newly added.
	 */
	boolean subscribe(final EventListener<E> listener);

	/**
	 * Removes the given event listener from the event provider.
	 * @param listener The listener to remove.
	 * @return Whether the listener was removed.
	 */
	boolean unsubscribe(final EventListener<E> listener);
	
}