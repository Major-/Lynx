package rs.emulate.lynx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

import rs.emulate.lynx.net.ClientVersionWorker;
import rs.emulate.lynx.net.Crawler;
import rs.emulate.lynx.net.Js5Constants;

/**
 * Retrieves and decrypts the Runescape game client.
 * 
 * @author Major
 */
public final class Lynx {

	/**
	 * The logger for this class.
	 */
	private static final Logger logger = Logger.getLogger(Lynx.class.getSimpleName());

	static {
		try {
			logger.fine("Creating directories: " + LynxConstants.SAVE_DIRECTORY);
			Files.createDirectories(LynxConstants.SAVE_DIRECTORY);
		} catch (IOException e) {
			throw new ExceptionInInitializerError("Could not create directories: " + e.getMessage());
		}
	}

	/**
	 * Main entry point for lynx.
	 * 
	 * @param args The program arguments.
	 */
	public static void main(String[] args) {
		boolean identifyVersion = (args.length == 0 || !args[0].equals("--noversion"));
		logger.fine("Starting lynx with" + (identifyVersion ? " " : "out ") + " version identification.");
		Lynx lynx = new Lynx(identifyVersion);

		try {
			lynx.run();
		} catch (FileAlreadyExistsException e) {
			System.err.println("\"data\" must be a directory - please correct.");
		} catch (SecurityException e) {
			System.err.println("Error creating directory - please ensure this application has write perms.");
		} catch (IOException e) {
			System.err.println("Error - please report, along with the below stack trace:");
			e.printStackTrace();
		}
	}

	/**
	 * Downloads the gamepack file, saving it in the {@link LynxConstants#SAVE_DIRECTORY}.
	 * 
	 * @param url The {@link URL} to download from.
	 * @param directory The directory to store the {@code gamepack} in.
	 * @return The path to the {@code gamepack} file.
	 * @throws IOException If there is an error downloading the {@code gamepack} file.
	 */
	private static Path downloadGamepack(URL url, Path directory) throws IOException {
		Path gamepack = directory.resolve("gamepack.jar");

		try (InputStream is = new BufferedInputStream(url.openStream());
				OutputStream os = new BufferedOutputStream(Files.newOutputStream(gamepack, StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE))) {

			int read;
			while ((read = is.read()) != -1) {
				os.write(read);
			}
		} catch (IOException e) {
			throw new IOException("Error retrieving gamepack - please report.", e);
		}

		return gamepack;
	}

	/**
	 * Gets the connection key (a 32-character string) from the {@link Map} of parameters.
	 * 
	 * @param parameters The map of parameter names to values.
	 * @return The key.
	 * @throws IllegalStateException If the map of parameters does not contain the connection key.
	 */
	private static String getConnectionKey(Map<String, String> parameters) {
		return parameters.values().stream().filter(value -> value.length() == 32).findAny()
				.orElseThrow(() -> new IllegalStateException("Parameters did not contain the connection key - please report."));
	}

	/**
	 * Attempts to identify the current client version.
	 * 
	 * @param key The 32-character key.
	 * @return The client version.
	 * @throws IOException If there was an error identifying the version.
	 */
	private static int identifyVersion(String key) throws IOException {
		try (ClientVersionWorker worker = new ClientVersionWorker(Js5Constants.HOST, key)) {
			worker.connect(Js5Constants.MAJOR_VERSION, Js5Constants.MINOR_VERSION);

			return worker.identifyVersion();
		} catch (Exception e) {
			throw new IllegalStateException("Error identifying the correct version - please report.", e);
		}
	}

	/**
	 * Writes the classes to the specified directory.
	 * 
	 * @param classes The {@link Map} of class names to {@link ByteBuffer}s.
	 * @param directory The {@link Path} to the directory to store the classes in.
	 * @throws IOException If there is an error writing the classes to files.
	 */
	private static void writeClasses(Map<String, ByteBuffer> classes, Path directory) throws IOException {
		for (Entry<String, ByteBuffer> entry : classes.entrySet()) {
			String name = entry.getKey();
			Path path = directory.resolve(name);

			if (name.contains("/")) {
				Files.createDirectories(path.getParent());
			}

			try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
				ByteBuffer buffer = entry.getValue();
				channel.write(buffer);
			} catch (Exception e) {
				throw new IllegalStateException(
						"Error writing classes to file - please ensure this program has write permissions.", e);
			}
		}
	}

	/**
	 * Writes the class data to a jar file with the specified name.
	 * 
	 * @param classes The {@link Map} of class names to {@link ByteBuffer}s.
	 * @param directory The {@link Path} to the directory to store the jar file in.
	 * @param name The name of the jar file.
	 * @throws IOException If there is an error writing to the jar file.
	 */
	private static void writeJar(Map<String, ByteBuffer> classes, Path directory, String name) throws IOException {
		Path jar = directory.resolve(name);

		try (JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jar)))) {
			for (Entry<String, ByteBuffer> entry : classes.entrySet()) {
				ZipEntry zip = new ZipEntry(entry.getKey());

				jos.putNextEntry(zip);
				jos.write(entry.getValue().array());
			}
		} catch (Exception e) {
			throw new IllegalStateException("Error writing classes to jar - please ensure this program has write permissions.", e);
		}
	}

	/**
	 * Indicates whether to identify the current client version, or to use the current date instead.
	 */
	private final boolean identifyVersion;

	/**
	 * Creates the new lynx object.
	 * 
	 * @param identifyVersion Whether or not the current client version should be identified.
	 */
	public Lynx(boolean identifyVersion) {
		this.identifyVersion = identifyVersion;
	}

	/**
	 * Runs lynx, which downloads the gamepack, decrypts the {@code inner.pack.gz} file, and writes the class data.
	 * 
	 * @throws IOException If there is an I/O error.
	 */
	private void run() throws IOException {
		long start = System.currentTimeMillis();

		logger.fine("Creating a Crawler for the URL " + (LynxConstants.APPLET_URL + ",j0"));
		Crawler crawler = new Crawler(new URL(LynxConstants.APPLET_URL + ",j0"));
		Map<String, String> parameters = crawler.fetchParameters();

		logger.fine("Fetched parameters: " + parameters);

		if (!parameters.containsKey("gamepack")) {
			throw new IllegalStateException("Failed to parse parameters (no gamepack found) - please report.");
		}

		System.out.println("Successfully fetched parameters.");

		String suffix = Instant.now().toString().replace(':', '.');
		if (identifyVersion) {
			String key = getConnectionKey(parameters);
			int version = identifyVersion(key);

			suffix = Integer.toString(version);
		}

		Path directory = LynxConstants.SAVE_DIRECTORY.resolve(suffix);
		Files.createDirectories(directory);

		URL url = new URL(LynxConstants.APPLET_URL + parameters.get("gamepack"));
		logger.fine("Downloading gamepack from " + LynxConstants.APPLET_URL + parameters.get("gamepack"));

		Path gamepack = downloadGamepack(url, directory);
		logger.fine("Saving gamepack to " + gamepack + ".");

		String secret = parameters.get(LynxConstants.SECRET_PARAMETER_NAME);
		String vector = parameters.get(LynxConstants.VECTOR_PARAMETER_NAME);

		logger.fine("Secret parameter: " + secret);
		logger.fine("Vector parameter: " + vector);

		if (secret == null || vector == null) {
			throw new IllegalStateException("Failed to identify an AES parameter - please report.");
		}

		Map<String, ByteBuffer> classes;

		try (InnerPackDecrypter decrypter = new InnerPackDecrypter(gamepack, secret, vector)) {
			classes = decrypter.decrypt();
		} catch (Exception e) {
			throw new IllegalStateException("Error decrypting the inner archive - please report.", e);
		}

		Path client = directory.resolve("bin");
		Files.createDirectories(client);

		System.out.println("Writing class files.");

		writeJar(classes, directory, "client.jar");
		writeClasses(classes, client);

		System.out.println("Done, took " + (System.currentTimeMillis() - start) / 1_000 + " seconds.");
	}

}