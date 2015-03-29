package rs.emulate.lynx.args;

/**
 * A source of a client.
 *
 * @author Major
 */
public enum ClientSource {

	/**
	 * The classic game.
	 */
	CLASSIC("classic", ".runescape.com/", false),

	/**
	 * The oldschool game.
	 */
	OLDSCHOOL("oldschool", ".runescape.com/", false),

	/**
	 * The runescape game.
	 */
	RUNESCAPE("world", ".runescape.com/g=runescape/", true);

	/**
	 * The path that indicates the client has a valid Java installation.
	 */
	private static final String VALID_JAVA = "j0";

	/**
	 * Whether or not the client source code is encrypted.
	 */
	private final boolean encrypted;

	/**
	 * The prefix of this ClientSource, prepended before the world.
	 */
	private final String prefix;

	/**
	 * The suffix of this ClientSource, appended after the world (if applicable).
	 */
	private final String suffix;

	/**
	 * Creates the ClientSource.
	 *
	 * @param prefix The prefix of the ClientSource, prepended before the world.
	 * @param suffix The suffix of the ClientSource, appended after the world (if applicable).
	 * @param encrypted Whether or not the client source code is encrypted.
	 */
	private ClientSource(String prefix, String suffix, boolean encrypted) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.encrypted = encrypted;
	}

	/**
	 * Returns a full path to the applet page, to download the client (or gamepack).
	 * 
	 * @param protocol The protocol to use.
	 * @param world The world to use.
	 * @return The full path.
	 */
	public String forClient(String protocol, int world) {
		return protocol + prefix + world + suffix;
	}

	/**
	 * Returns a full path to the applet page, for a Crawler.
	 * 
	 * @param protocol The protocol to use.
	 * @param world The world to use.
	 * @return The full path.
	 */
	public String forCrawler(String protocol, int world) {
		return protocol + prefix + world + suffix + VALID_JAVA;
	}

	/**
	 * Gets the prefix of this ClientSource, prepended before the world.
	 * 
	 * @return The prefix.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Gets the name of this ClientSource, with correct capitalisation.
	 * 
	 * @return The name.
	 */
	public String getPrettyName() {
		String name = name();
		return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
	}

	/**
	 * Gets the suffix of this ClientSource, appended after the world (if applicable).
	 * 
	 * @return The suffix.
	 */
	public String getSuffix() {
		return suffix;
	}

	/**
	 * Returns whether or not the client source code is encrypted.
	 * 
	 * @return {@code true} if the client source is encrypted, {@code false} if not.
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

}