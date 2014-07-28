package rs.emulate.lynx.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crawls the provided {@link URL} to parse and return parameters that match specified patterns.
 * 
 * @author Major
 */
public final class Crawler {

	/**
	 * The pattern used to match the gamepack archive location.
	 */
	private static final Pattern ARCHIVE_PATTERN = Pattern.compile("(?<=archive=)(.*)(?=  )");

	/**
	 * The pattern used to patch parameter names.
	 */
	private static final Pattern NAME_PATTERN = Pattern.compile("(?<=name=\")(.*)(?=\" )");

	/**
	 * The pattern used to match the parameters.
	 */
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<=<param)(.*)(?=>)");

	/**
	 * The pattern used to patch parameter values.
	 */
	private static final Pattern VALUE_PATTERN = Pattern.compile("(?<=value=\")(.*)(?=\")");

	/**
	 * The url to crawl.
	 */
	private final URL url;

	/**
	 * Creates the crawler.
	 * 
	 * @param url The url.
	 */
	public Crawler(URL url) {
		this.url = url;
	}

	/**
	 * Gets the parameters matching the pre-defined patterns, returning them as a {@link Map} of names to values.
	 * 
	 * @return The map of parameters.
	 * @throws IOException If there was an error reading from the {@link URL}.
	 */
	public Map<String, String> fetchParameters() throws IOException {
		Map<String, String> parameters = new HashMap<>();

		for (String string : readPage()) {
			Matcher matcher = ARCHIVE_PATTERN.matcher(string);
			if (matcher.find()) {
				parameters.put("gamepack", matcher.group(1).trim());
			}

			matcher = PARAMETER_PATTERN.matcher(string);
			if (!matcher.find()) {
				continue;
			}

			String found = matcher.group(1);
			parseParameter(found, parameters);
		}
		return parameters;
	}

	/**
	 * Parses a single parameter into its name and value, and adds it to the {@link Map}.
	 * 
	 * @param parameter The string containing the parameter.
	 * @param parameters The map of parameter names to values.
	 */
	private void parseParameter(String parameter, Map<String, String> parameters) {
		Matcher matcher = NAME_PATTERN.matcher(parameter);
		if (!matcher.find()) {
			throw new IllegalStateException("Found parameter " + parameter + " with no name pattern - please report.");
		}
		String name = matcher.group(1).trim();

		matcher = VALUE_PATTERN.matcher(parameter);
		if (!matcher.find()) {
			throw new IllegalStateException("Found parameter " + parameter + " with no value pattern - please report.");
		}

		String value = matcher.group(1).trim();
		parameters.put(name, value);
	}

	/**
	 * Reads the page, returning it as a {@link List} of strings (one string per line).
	 * 
	 * @return The list of strings.
	 * @throws IOException If there was an error reading from the {@link URL}.
	 */
	private List<String> readPage() throws IOException {
		List<String> lines = new ArrayList<>();

		try (InputStream is = new BufferedInputStream(url.openStream());
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}

		return lines;
	}

}