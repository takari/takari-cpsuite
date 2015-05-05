package org.junit.extensions.cpsuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the Ant/Maven style classname matching algorithm.
 */
final class JavaStyleClassnameMatcher {
	private static final Pattern WILDCARDS = Pattern.compile("\\*{1,2}");

	private final Pattern pattern;

	JavaStyleClassnameMatcher(String pattern) {
		StringBuilder rx = new StringBuilder();
		rx.append("^");
		for (String part : splitIncludingSeparator(pattern)) {
			if (part.equals("**")) {
				rx.append(".*");
			} else if (part.equals("*")) {
				rx.append("[^\\.]*");
			} else {
				rx.append(Pattern.quote(part));
			}
		}
		rx.append("$");

		this.pattern = Pattern.compile(rx.toString());
	}

	private static Collection<String> splitIncludingSeparator(String input) {
		Collection<String> result = new ArrayList<String>();
		Matcher matcher = WILDCARDS.matcher(input);
		int start = 0;
		while (matcher.find()) {
			if (matcher.start() > start) {
				result.add(input.substring(start, matcher.start()));
			}
			result.add(matcher.group());
			start = matcher.end() + 1;
		}
		if (start < input.length()) {
			result.add(input.substring(start));
		}
		return result;
	}

	/**
	 * @param classname
	 *            the fully qualified classname to check for match.
	 * @return true if the classname matches the filter, false otherwise.
	 */
	boolean matches(String classname) {
		return pattern.matcher(classname).matches();
	}
}
