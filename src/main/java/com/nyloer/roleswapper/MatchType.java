/*
BSD 2-Clause License

Copyright (c) 2021, geheur
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
		switch (this)
		{
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
		if (s == null)
		{
			return MatchType.IGNORE;
		}

		int star = s.indexOf('*');
		if (star == -1)
		{
			return MatchType.EQUALS;
		}
		if (star == 0)
		{
			if (s.length() == 1)
			{
				return MatchType.IGNORE;
			}
			star = s.indexOf('*', star + 1);
			if (star == -1)
			{
				return MatchType.ENDS_WITH;
			}
			if (star == s.length() - 1)
			{
				return MatchType.CONTAINS;
			}
		}
		else if (star == s.length() - 1)
		{
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