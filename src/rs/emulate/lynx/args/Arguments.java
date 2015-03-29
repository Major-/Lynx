package rs.emulate.lynx.args;

/**
 * Contains instances of application {@link Argument}s.
 *
 * @author Major
 */
public final class Arguments {

	/**
	 * The Argument specifying which gamepack should be downloaded.
	 */
	public static final Argument<ClientSource> GAMEPACK_SOURCE = new Argument<>("source");

	/**
	 * The Argument specifying that the current client version should be identified (if supported).
	 */
	public static final Argument<Boolean> IDENTIFY_VERSION = new Argument<>("identify");

	/**
	 * Sole private constructor to prevent instantiation.
	 */
	private Arguments() {

	}

}