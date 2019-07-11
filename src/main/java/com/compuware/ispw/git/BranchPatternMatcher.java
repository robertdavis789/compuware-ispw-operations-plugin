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
	private Map<Pattern, RefMap> patternMapping = new HashMap<Pattern, RefMap>();

	/**
	 * Constructor
	 * 
	 * @param repo
	 *            the GIT repo
	 * @param ispwDao
	 *            the ISPW DAO instance
	 */
	public BranchPatternMatcher(Map<String, RefMap> branchPatternToIspwLevel, PrintStream log)
	{
		if (branchPatternToIspwLevel != null)
		{
			branchPatternToIspwLevel.entrySet().stream().forEach(entry -> {
				String branchPattern = entry.getKey();
				RefMap refMap = entry.getValue();
				String regex = StringUtils.EMPTY;

				try
				{
					regex = BranchPatternMatcher.wildcardToRegex(branchPattern);
					Pattern compiled = Pattern.compile(regex);

					patternMapping.put(compiled, refMap);
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
	public RefMap match(String refId)
	{
		Optional<Pattern> optional = patternMapping.keySet().stream().filter(x -> x.matcher(refId).find()).findFirst();

		if (optional.isPresent())
		{
			Pattern pattern = optional.get();
			return patternMapping.get(pattern);
		}
		else
		{
			return null;
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
