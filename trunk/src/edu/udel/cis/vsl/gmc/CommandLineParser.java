package edu.udel.cis.vsl.gmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.udel.cis.vsl.gmc.Option.OptionType;

public class CommandLineParser {

	private Map<String, Option> optionMap = new LinkedHashMap<>();

	private Map<String, Option> mapOptionMap = new LinkedHashMap<>();

	private static Boolean trueBoolean = new Boolean(true);

	private static Boolean falseBoolean = new Boolean(false);

	public CommandLineParser(Collection<Option> options) {
		for (Option option : options) {
			String name = option.name();

			if (optionMap.put(name, option) != null)
				throw new IllegalArgumentException("Saw two options named "
						+ name);
			if (option.type() == OptionType.MAP)
				mapOptionMap.put(name, option);
		}
	}

	/**
	 * Interpretes the string valueString to determine its type and value as a
	 * command line parameter. The rules are:
	 * 
	 * If value is null or the empty string, the parameter will be interpreted
	 * as having boolean type with value true. This conforms with examples such
	 * as <code>-verbose</code>, which usually means parameter "verbose" is a
	 * boolean flag which should be set to true.
	 * 
	 * If value is surrounded by quotes, it will be interpreted as a String,
	 * with the quotes removed. Example: <code>-rep="~/civl/examples/gcd</code>
	 * on the command line yields a key "rep" with value
	 * <code>~/civl/examples/gcd</code>, i.e., the String in between those
	 * quotes.
	 * 
	 * If value is an integer, it will be interpred as an Integer. Example:
	 * <code>-depth=100</code>
	 * 
	 * If value can be interpreted as a floating point number, it will be
	 * interpreted as a Double. Example: <code>-inputPi=3.14</code>
	 * 
	 * If value is "true" or "false", it will be interpreted as a Boolean with
	 * the corresponding value. Example: <code>-verbose=false</code>
	 * 
	 * Otherwise value will be interpreted as a String. Example:
	 * <code>-dir=/usr/local</code>
	 * 
	 * @param key
	 *            the parameter name
	 * @param valueString
	 *            a string which will be interpreted to yield the parameter
	 *            value
	 * @return the value that results from interpreting the valueString
	 */
	public static Object interpretValue(String valueString) {
		if (valueString == null) {
			return trueBoolean;
		} else {
			int length = valueString.length();

			if (length == 0)
				return trueBoolean;
			if (length >= 2) {
				char firstChar = valueString.charAt(0), lastChar = valueString
						.charAt(length - 1);

				if (firstChar == '"' && lastChar == '"') {
					String result = valueString.substring(1, length - 1);

					result.replace("\\" + "n", "\n");
					result.replace("\\" + "t", "\t");
					result.replace("\\" + " ", " ");
					result.replace("\\" + "\"", "\"");
					result.replace("\\" + "'", "'");
					result.replace("\\" + "\\", "\\");
					return result;
				}
				if (firstChar == '\'' && lastChar == '\'') {
					return valueString.substring(1, length - 1);
				}
			}
			try {
				return new Integer(valueString);
			} catch (Exception e) {
				// proceed...
			}
			try {
				return new Double(valueString);
			} catch (Exception e) {
				// proceed...
			}
			if ("true".equals(valueString))
				return trueBoolean;
			if ("false".equals(valueString))
				return falseBoolean;
			return valueString;
		}
	}

	public static Object parseValue(Option option, String valueString) {
		OptionType type = option.type();
		String name = option.name();

		switch (type) {
		case BOOLEAN:
			if (valueString == null)
				return trueBoolean;
			if ("true".equals(valueString) || "".equals(valueString))
				return trueBoolean;
			if ("false".equals(valueString))
				return falseBoolean;
			throw new IllegalArgumentException("Option " + name
					+ ": expected boolean, saw " + valueString);
		case DOUBLE:
			try {
				return new Double(valueString);
			} catch (Exception e) {
				throw new IllegalArgumentException("Option " + name
						+ ": expected double, saw " + valueString);
			}
		case INTEGER:
			try {
				return new Integer(valueString);
			} catch (Exception e) {
				throw new IllegalArgumentException("Option " + name
						+ ": expected integer, saw " + valueString);
			}
		case MAP:
			throw new IllegalArgumentException("map should not be used here");
		case STRING: {
			int length = valueString.length();

			if (length >= 2) {
				char firstChar = valueString.charAt(0), lastChar = valueString
						.charAt(length - 1);

				if (firstChar == '"' && lastChar == '"') {
					String result = valueString.substring(1, length - 1);

					result.replace("\\" + "n", "\n");
					result.replace("\\" + "t", "\t");
					result.replace("\\" + " ", " ");
					result.replace("\\" + "\"", "\"");
					result.replace("\\" + "'", "'");
					result.replace("\\" + "\\", "\\");
					return result;
				}
				if (firstChar == '\'' && lastChar == '\'') {
					return valueString.substring(1, length - 1);
				}
			}
			return valueString;
		}
		default:
			throw new RuntimeException("unreachable");
		}
	}

	private void processArg(GMCConfiguration config, String arg)
			throws CommandLineException {
		int length = arg.length();

		if (arg.startsWith("-")) {
			int eqIndex = arg.indexOf('=');
			String optionName, valueString;
			Option option;
			Object value;

			if (eqIndex >= 0) {
				optionName = arg.substring(1, eqIndex);
				valueString = arg.substring(eqIndex + 1, length);
			} else {
				optionName = arg.substring(1);
				valueString = null;
			}
			// is it a map?
			for (String mapName : mapOptionMap.keySet()) {
				if (optionName.startsWith(mapName)) {
					String key = optionName.substring(mapName.length(),
							optionName.length());

					option = mapOptionMap.get(mapName);
					value = interpretValue(valueString);
					config.putMapEntry(option, key, value);
					return;
				}
			}
			option = optionMap.get(optionName);
			if (option == null)
				throw new CommandLineException("Unknown command line option "
						+ optionName);
			value = parseValue(option, valueString);
			config.setValue(option, value);
		} else {
			config.addFreeArg(arg);
		}
	}

	public GMCConfiguration parse(int startIndex, String[] args)
			throws CommandLineException {
		GMCConfiguration config = new GMCConfiguration(optionMap.values());

		for (int i = startIndex; i < args.length; i++) {
			processArg(config, args[i]);
		}
		return config;
	}

	public GMCConfiguration parse(Collection<String> args)
			throws CommandLineException {
		GMCConfiguration config = new GMCConfiguration(optionMap.values());

		for (String arg : args) {
			processArg(config, arg);
		}
		return config;
	}

	public GMCConfiguration parse(BufferedReader reader) throws IOException,
			CommandLineException {
		ArrayList<String> args = new ArrayList<>();

		while (true) {
			String arg = reader.readLine();

			if (arg == null)
				break;
			arg = arg.trim();
			args.add(arg);
		}
		return parse(args);
	}

	public void printUsage(PrintStream out) {
		for (Option option : optionMap.values()) {
			option.print(out);
		}
		out.flush();
	}

}
