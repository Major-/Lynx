package rs.emulate.lynx;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts the {@code inner.pack.gz} archive in the {@code gamepack} jar file.
 * 
 * @author Major
 */
public final class InnerPackDecrypter implements Closeable {

	/**
	 * The key returned if an empty (i.e. {@code length == 0} string is decrypted.
	 */
	private static final byte[] EMPTY_KEY = new byte[0];

	/**
	 * The encoded secret key for the AES block cipher.
	 */
	private final String encodedSecret;

	/**
	 * The encoded initialisation vector for the AES block cipher.
	 */
	private final String encodedVector;

	/**
	 * The input stream to the {@code inner.pack.gz} file.
	 */
	private final InputStream input;

	/**
	 * The gamepack jar file.
	 */
	private final JarFile jar;

	/**
	 * Creates the inner pack decrypter.
	 * 
	 * @param gamepack The {@link Path} to the gamepack jar.
	 * @param secret The encoded secret key.
	 * @param vector The encoded initialisation vector.
	 * @throws IOException If the path to the gamepack is invalid.
	 */
	public InnerPackDecrypter(Path gamepack, String secret, String vector) throws IOException {
		this.encodedSecret = secret;
		this.encodedVector = vector;
		this.jar = new JarFile(gamepack.toFile());

		ZipEntry archive = jar.getEntry(LynxConstants.ENCRYPTED_ARCHIVE_NAME);
		this.input = new BufferedInputStream(jar.getInputStream(archive));
	}

	@Override
	public void close() throws IOException {
		input.close();
		jar.close();
	}

	/**
	 * Decrypts the {@code inner.pack.gz} archive using the AES cipher. The decrypted data is then un-gzipped and
	 * unpacked from the pack200 format, before finally being split into a {@link ByteBuffer} per class. The data is
	 * then returned as a {@link Map} of class names to byte buffers.
	 * 
	 * @return The map of class names to the byte buffers containing their data.
	 * @throws GeneralSecurityException If there is some sort of security error (e.g. can't find the algorithm, invalid
	 *             initialisation vector, etc).
	 * @throws IOException If there is an error reading from or writing to any of the various streams used.
	 */
	public Map<String, ByteBuffer> decrypt() throws GeneralSecurityException, IOException {
		int secretKeySize = getKeySize(encodedSecret.length());
		int vectorSize = getKeySize(encodedVector.length());

		byte[] secretKey = (secretKeySize == 0) ? EMPTY_KEY : decodeBase64(encodedSecret, secretKeySize);
		byte[] initialisationVector = (vectorSize == 0) ? EMPTY_KEY : decodeBase64(encodedVector, vectorSize);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secret = new SecretKeySpec(secretKey, "AES");
		IvParameterSpec vector = new IvParameterSpec(initialisationVector);

		cipher.init(Cipher.DECRYPT_MODE, secret, vector);

		byte[] buffer = new byte[LynxConstants.BUFFER_SIZE];
		int read = 0, in = 0;

		while (read < buffer.length && (in = input.read(buffer, read, buffer.length - read)) != -1) {
			read += in;
		}

		System.out.println("Decrypting the archive.");

		// Decrypt the inner.pack.gz file.
		byte[] decrypted = cipher.doFinal(buffer, 0, read);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(LynxConstants.BUFFER_SIZE);

		// Un-gzip and unpack the jar file contained in the archive, and write the decompressed data out.
		try (JarOutputStream jos = new JarOutputStream(bos);
				GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(decrypted))) {
			Pack200.newUnpacker().unpack(gzip, jos);
		}

		Map<String, ByteBuffer> classes = new HashMap<>();

		// Iterate through the jar entries from the stream, read and wrap them, and add them to the map.
		try (JarInputStream jis = new JarInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
			for (JarEntry entry = jis.getNextJarEntry(); entry != null; entry = jis.getNextJarEntry()) {
				String name = entry.getName();
				if (!name.endsWith(".class")) {
					System.out.println(name);
					continue;
				}

				read = in = 0;
				while (read < buffer.length && (in = jis.read(buffer, read, buffer.length - read)) != -1) {
					read += in;
				}

				ByteBuffer data = ByteBuffer.allocate(read);
				data.put(buffer, 0, read).flip();
				classes.put(name, data);
			}
		}

		return classes;
	}

	/**
	 * Decodes the base64 string into a valid secret key or initialisation vector.
	 * 
	 * @param string The string.
	 * @param size The size of the key, in bytes.
	 * @return The key, as a byte array.
	 */
	private static byte[] decodeBase64(String string, int size) {
		// JaGex's implementation uses * and - instead of + and /, so replace them.
		String valid = string.replaceAll("\\*", "\\+").replaceAll("-", "/");

		Base64.Decoder base64 = Base64.getDecoder();
		return base64.decode(valid);
	}

	/**
	 * Gets the key size for a string of the specified length.
	 * 
	 * @param length The length of the string.
	 * @return The key size.
	 */
	private static int getKeySize(int length) {
		if (length == 0) {
			return 0;
		}

		return 3 * (int) Math.floor((length - 1) / 4) + 1;
	}

}