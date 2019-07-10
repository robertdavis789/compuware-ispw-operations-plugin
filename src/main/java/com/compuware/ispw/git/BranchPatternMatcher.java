package com.compuware.ispw.git;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.lang.StringUtils;

/**
 * A branch pattern wildcard matcher
 * 
 * @author pmisvz0
 *
 */
public class BranchPatternMatcher
{
	private String defaultLevel = StringUtils.EMPTY;
	private Map<Pattern, String> patternToLevel = new HashMap<Pattern, String>();

	/**
	 * Constructor
	 * 
	 * @param repo
	 *            the GIT repo
	 * @param ispwDao
	 *            the ISPW DAO instance
	 */
	public BranchPatternMatcher(Map<String, String> branchPatternToIspwLevel, PrintStream log)
	{
		if (branchPatternToIspwLevel != null)
		{
			branchPatternToIspwLevel.entrySet().stream().forEach(entry -> {
				String branchPattern = entry.getKey();
				String ispwLevel = entry.getValue();
				String regex = StringUtils.EMPTY;

				try
				{
					regex = BranchPatternMatcher.wildcardToRegex(branchPattern);
					Pattern compiled = Pattern.compile(regex);

					patternToLevel.put(compiled, ispwLevel);
				}
				catch (PatternSyntaxException x)
				{
					String error = String.format("cannot compile wildcard: %s to regex pattern: %s, ignored!", branchPattern,
							regex);
					log.println(error);
				}
			});
		}
	}

	/**
	 * Match the branch to the branch pattern, only the first find get returned
	 * 
	 * @param refId
	 *            the ref ID
	 * @return the ISPW level matched
	 */
	public String match(String refId)
	{
		Optional<Pattern> optional = patternToLevel.keySet().stream().filter(x -> x.matcher(refId).find()).findFirst();

		if (optional.isPresent())
		{
			Pattern pattern = optional.get();
			return StringUtils.trimToEmpty(patternToLevel.get(pattern));
		}
		else
		{
			return StringUtils.trimToEmpty(defaultLevel);
		}
	}

	/**
	 * Convert wildcard to regular expression
	 * 
	 * @param wildcard
	 * @return
	 */
	public static String wildcardToRegex(String wildcard)
	{
		wildcard = wildcard.replaceAll("\\*{2}", "##");
		wildcard = wildcard.replaceAll("[*]", "[^/]*");
		wildcard = wildcard.replaceAll("[\\.]", "[.]");
		wildcard = wildcard.replaceAll("##", ".*");

		return ".*" + wildcard + ".*";
	}

}
