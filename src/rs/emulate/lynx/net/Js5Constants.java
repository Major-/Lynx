package rs.emulate.lynx.net;

/**
 * Contains constants used as part of the js5 protocol.
 * 
 * @author Major
 */
public final class Js5Constants {

	/**
	 * The amount of times to attempt a connection to the server (incrementing the major version each unsuccessful
	 * connection) before stopping.
	 */
	public static final int DEFAULT_ATTEMPT_COUNT = 100;

	/**
	 * The port the js5 worker should connect to.
	 */
	public static final int DEFAULT_PORT = 43594;

	/**
	 * The major version (i.e. client version).
	 */
	public static final int MAJOR_VERSION = 833;

	/**
	 * The minor version.
	 */
	public static final int MINOR_VERSION = 1;

	/**
	 * The host to connect to.
	 */
	public static final String HOST = "world2.runescape.com";

	/**
	 * Default private constructor to prevent instantiation.
	 */
	private Js5Constants() {

	}

}