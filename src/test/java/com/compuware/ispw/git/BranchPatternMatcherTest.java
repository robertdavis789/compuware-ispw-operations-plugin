package com.compuware.ispw.git;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

@SuppressWarnings("nls")
public class BranchPatternMatcherTest
{

	@Test
	public void testWildcardToRegEx()
	{
		String[] wildcards = new String[]{"*", "PROJECT-*", "tags", "tags/**", "heads/**/master", "refs/**", "rep_1/**/b1",
				"rep_1/**/b2", "**/dev1"};
		String[] tests = new String[]{"rep_1/b1", "rep_1/abc/b1", "b1", "refs/heads/dev1"};
		for (String wildcard : wildcards)
		{
			String regEx = BranchPatternMatcher.wildcardToRegex(wildcard);
			Pattern compiled = Pattern.compile(regEx);

			for (String test : tests)
			{
				Matcher matcher = compiled.matcher(test);
				boolean matched = matcher.find();
				if (matched)
				{
					String msg = String.format("wildcard: %s, regex: %s, test: %s, match?: %s", wildcard, regEx, test, matched);
					System.out.println(msg);
				}
			}
		}
	}

}
