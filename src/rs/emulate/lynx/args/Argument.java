package rs.emulate.lynx.args;

/**
 * An Argument that may be configured.
 *
 * @author Major
 *
 * @param <T> The type of the argument.
 */
public final class Argument<T> {

	/**
	 * The name of the argument.
	 */
	private final String name;

	/**
	 * Creates the Argument.
	 * 
	 * @param name The name of the Argument.
	 */
	public Argument(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of this Argument.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

}