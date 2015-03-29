package rs.emulate.lynx.args;

import java.util.HashMap;
import java.util.Map;

/**
 * A type-safe Map of the form <Argument<T>, T>, which also supports 'freezing' - modification prevention.
 *
 * @author Major
 */
public final class ArgumentMap {

	/**
	 * The default capacity of an ArgumentMap.
	 */
	private static final int DEFAULT_CAPACITY = 16;

	/**
	 * The Map of Argument<T> to {@code T}s.
	 */
	private final Map<Argument<? extends Object>, Object> arguments;

	/**
	 * Whether or not this ArgumentMap is frozen.
	 */
	private boolean frozen;

	/**
	 * Creates the ArgumentMap with the default initial capacity.
	 */
	public ArgumentMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * Creates the ArgumentMap.
	 *
	 * @param capacity The initial capacity.
	 */
	public ArgumentMap(int capacity) {
		arguments = new HashMap<>(capacity);
	}

	/**
	 * Freezes this ArgumentMap.
	 * 
	 * @return This ArgumentMap.
	 */
	public ArgumentMap freeze() {
		frozen = true;
		return this;
	}

	/**
	 * Gets the value of the specified {@link Argument}.
	 * 
	 * @param argument The Argument.
	 * @return The value.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Argument<T> argument) {
		return (T) arguments.get(argument);
	}

	/**
	 * Places the specified pair into this Map.
	 * 
	 * @param argument The {@link Argument}.
	 * @param value The value.
	 */
	public <T> void put(Argument<T> argument, T value) {
		if (frozen) {
			throw new IllegalStateException("Cannot place values into a frozen ArgumentMap.");
		}

		arguments.put(argument, value);
	}

}