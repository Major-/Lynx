package rs.emulate.lynx.args;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A parser for the application arguments.
 *
 * @author Major
 */
public final class ArgumentParser {

	/**
	 * The Map of argument names to aliases.
	 */
	private static final Map<String, String> ALIASES;

	/**
	 * The map of argument names to default values.
	 */
	private static final ArgumentMap DEFAULT_VALUES;

	/**
	 * The List of Strings containing the help text.
	 */
	private static final List<String> HELP_TEXT;

	/**
	 * The Pattern that matches any String starting with two dash characters ('-').
	 */
	private static final Pattern STARTING_DASH_PATTERN = Pattern.compile("^--");

	static {
		List<String> help = new ArrayList<>();
		help.add("Lynx arguments:");
		help.add("--h  --help    Prints this message.");
		help.add("--r  --runescape | --rs3    Specifies that the RS3 client should be downloaded.");
		help.add("--c  --classic    Specifies that the classic client should be downloaded.");
		help.add("--o  --oldschool    Specifies that the oldschool client should be downloaded.");
		help.add("--i  --identify <boolean>    Specifies whether or not the current client version should be identified (if supported). Defaults to true.");
		HELP_TEXT = Collections.unmodifiableList(help);

		ArgumentMap defaults = new ArgumentMap(2);
		defaults.put(Arguments.GAMEPACK_SOURCE, ClientSource.RUNESCAPE);
		defaults.put(Arguments.IDENTIFY_VERSION, true);
		DEFAULT_VALUES = defaults.freeze();

		Map<String, String> aliases = new HashMap<>(4);
		aliases.put("r", "runescape");
		aliases.put("c", "classic");
		aliases.put("o", "oldschool");
		aliases.put("i", "identify");
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	/**
	 * The application arguments to parse.
	 */
	private final String[] arguments;

	/**
	 * A Map containing the argument name and value pairs.
	 */
	private final ArgumentMap pairs = new ArgumentMap();

	/**
	 * Creates the ArgumentParser.
	 * 
	 * @param arguments The application arguments to parse. Must not be {@code null}.
	 */
	public ArgumentParser(String... arguments) {
		this.arguments = Objects.requireNonNull(arguments, "Arguments must not be null.");
	}

	/**
	 * Gets the value passed to this application with the specified argument name, as an {@link Optional}. If no
	 * argument with the specified name was passed to the application, this method returns {@link Optional#empty}.
	 * <p>
	 * This method infers the wrapped type from the caller, and as such will throw a {@code ClassCastException} at
	 * runtime if the call site is incorrect.
	 * 
	 * @param name The name of the argument.
	 * @return The Optional containing the value (if one was passed to the application), or {@link Optional#empty}.
	 */
	public <T> Optional<T> get(Argument<T> name) {
		return Optional.ofNullable(pairs.get(name));
	}

	/**
	 * Gets the default value for the specified argument.
	 * 
	 * @param name The name of the argument.
	 * @return The default value.
	 */
	public <T> T getDefault(Argument<T> name) {
		return DEFAULT_VALUES.get(name);
	}

	/**
	 * Gets the value passed to this application with the specified argument, returning the default value if the
	 * argument was not passed.
	 * 
	 * @param name The name of the argument.
	 * @return The value.
	 */
	public <T> T getOrDefault(Argument<T> name) {
		T value = pairs.get(name);
		return (value == null) ? getDefault(name) : value;
	}

	/**
	 * Parses the arguments, placing them into the {@link Map}.
	 */
	public void parse() {
		for (int index = 0; index < arguments.length; index++) {
			String argument = arguments[index].trim();
			argument = STARTING_DASH_PATTERN.matcher(argument).replaceFirst("");

			if (ALIASES.containsKey(argument)) {
				argument = ALIASES.get(argument);
			} else if (argument.equals("help")) {
				printHelpText();
				System.exit(0);
			}

			switch (argument) {
				case "oldschool":
					pairs.put(Arguments.GAMEPACK_SOURCE, ClientSource.OLDSCHOOL);
					break;
				case "classic":
					pairs.put(Arguments.GAMEPACK_SOURCE, ClientSource.CLASSIC);
					break;
				case "runescape":
				case "rs3":
					pairs.put(Arguments.GAMEPACK_SOURCE, ClientSource.RUNESCAPE);
					break;
				case "identify":
					pairs.put(Arguments.IDENTIFY_VERSION, Boolean.parseBoolean(arguments[index++]));
					break;
				default:
					throw new IllegalArgumentException("Undefined Argument " + argument + ".");
			}
		}
	}

	/**
	 * Prints the help text.
	 */
	public void printHelpText() {
		HELP_TEXT.forEach(System.out::println);
	}

}