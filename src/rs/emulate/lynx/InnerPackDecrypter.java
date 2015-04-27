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
	 * Decrypts the {@code inner.pack.gz} archive using the AES cipher. The decrypted data is then de-gzipped and
	 * unpacked from the pack200 format, before finally being split into a {@link ByteBuffer} per class. The data is
	 * then returned as a {@link Map} of class names to byte buffers.
	 * 
	 * @return The Map of Class names to the ByteBuffers containing their data.
	 * @throws GeneralSecurityException If there is some sort of security error.
	 * @throws IOException If there is an error reading from or writing to any of the various streams used.
	 */
	public Map<String, ByteBuffer> decrypt() throws GeneralSecurityException, IOException {
		byte[] secretKey = (encodedSecret.length() == 0) ? EMPTY_KEY : decodeBase64(encodedSecret);
		byte[] initialisationVector = (encodedVector.length() == 0) ? EMPTY_KEY : decodeBase64(encodedVector);

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

		byte[] decrypted = cipher.doFinal(buffer, 0, read);
		ByteArrayOutputStream bos = new ByteArrayOutputStream(LynxConstants.BUFFER_SIZE);

		try (JarOutputStream jar = new JarOutputStream(bos);
				GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(decrypted))) {
			Pack200.newUnpacker().unpack(gzip, jar);
		}

		Map<String, ByteBuffer> classes = new HashMap<>();

		try (JarInputStream jar = new JarInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
			for (JarEntry entry = jar.getNextJarEntry(); entry != null; entry = jar.getNextJarEntry()) {
				String name = entry.getName();
				if (!name.endsWith(".class")) {
					System.out.println(name);
					continue;
				}

				read = in = 0;
				while (read < buffer.length && (in = jar.read(buffer, read, buffer.length - read)) != -1) {
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
	 * <p>
	 * Jagex use a slightly different variant of base 64, where '+' and '/' are replaced with '*' and '-', so we replace
	 * those before passing it to the decoder. This is similar to the <a
	 * href="https://tools.ietf.org/html/rfc4648#page-7">Base 64 Encoding with URL and Filename Safe Alphabet</a>
	 * variant of Base 64 (but uses '*' in place of '_').
	 * 
	 * @param string The String to decode.
	 * @return The key, as a byte array.
	 */
	private byte[] decodeBase64(String string) {
		String valid = string.replace('*', '+').replace('-', '/');

		Base64.Decoder base64 = Base64.getDecoder();
		return base64.decode(valid);
	}

}