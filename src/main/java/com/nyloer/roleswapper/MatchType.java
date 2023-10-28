package com.nyloer.roleswapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum MatchType
{
	EQUALS,
	STARTS_WITH,
	ENDS_WITH,
	CONTAINS,
	WILDCARD,
	IGNORE,
	;

	public boolean matches(String menuText, String swapText)
	{
		switch (this) {
			case IGNORE:
				return true;
			case EQUALS:
				return menuText.equals(swapText);
			case STARTS_WITH:
				return menuText.startsWith(swapText);
			case ENDS_WITH:
				return menuText.endsWith(swapText);
			case CONTAINS:
				return menuText.contains(swapText);
			case WILDCARD:
				return menuText.matches(swapText);
			default:
				// shouldn't happen.
				throw new IllegalStateException();
		}
	}

	public static MatchType getType(String s)
	{
		if (s == null) return MatchType.IGNORE;

		int star = s.indexOf('*');
		if (star == -1) return MatchType.EQUALS;
		if (star == 0) {
			if (s.length() == 1) return MatchType.IGNORE;
			star = s.indexOf('*', star + 1);
			if (star == -1) return MatchType.ENDS_WITH;
			if (star == s.length() - 1) return MatchType.CONTAINS;
		} else if (star == s.length() - 1) {
			return MatchType.STARTS_WITH;
		}

		return MatchType.WILDCARD;
	}

	public static String prepareMatch(String option, MatchType optionType)
	{
		return optionType == MatchType.WILDCARD ? generateWildcardMatcher(option) : removeStars(option);
	}

	private static String removeStars(String s)
	{
		return s == null ? s : s.replaceAll("\\*", "");
	}

	// copied from runelite.
	private static final Pattern WILDCARD_PATTERN = Pattern.compile("(?i)[^*]+|(\\*)");
	private static String generateWildcardMatcher(String pattern)
	{
		final Matcher matcher = WILDCARD_PATTERN.matcher(pattern);
		final StringBuffer buffer = new StringBuffer();

		buffer.append("(?i)");
		while (matcher.find())
		{
			if (matcher.group(1) != null)
			{
				matcher.appendReplacement(buffer, ".*");
			}
			else
			{
				matcher.appendReplacement(buffer, Matcher.quoteReplacement(Pattern.quote(matcher.group(0))));
			}
		}

		matcher.appendTail(buffer);
		return buffer.toString();
	}
}