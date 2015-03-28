package rs.emulate.lynx;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Contains lynx-related constants.
 * 
 * @author Major
 */
public final class LynxConstants {

	/**
	 * The id of the world the applet should use.
	 */
	public static final int APPLET_ID = 2;

	/**
	 * The URL to the page containing the game applet.
	 */
	public static final String APPLET_URL = "http://world" + APPLET_ID + ".runescape.com/g=runescape/";

	/**
	 * The size of the buffer used when decrypting the {@code inner.pack.gz} archive, in bytes.
	 */
	public static final int BUFFER_SIZE = 5 * 1024 * 1024;

	/**
	 * The name of the archive containing the client.
	 */
	public static final String ENCRYPTED_ARCHIVE_NAME = "inner.pack.gz";

	/**
	 * The path to the directory to save data in.
	 */
	public static final Path SAVE_DIRECTORY = Paths.get(".", "data");

	/**
	 * The parameter name for the encoded AES secret key.
	 */
	public static final String SECRET_PARAMETER_NAME = "0";

	/**
	 * The parameter name for the encoded AES initialisation vector.
	 */
	public static final String VECTOR_PARAMETER_NAME = "-1";

	/**
	 * Sole private constructor to prevent instantiation.
	 */
	private LynxConstants() {

	}

}