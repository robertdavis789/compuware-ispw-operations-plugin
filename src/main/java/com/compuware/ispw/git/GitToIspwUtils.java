package com.compuware.ispw.git;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.compuware.ispw.cli.model.IGitToIspwPublish;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.utils.CommonConstants;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;

public class GitToIspwUtils
{

	public static ListBoxModel buildStandardCredentialsIdItems(@AncestorInPath Jenkins context,
			@QueryParameter String credentialsId, @AncestorInPath Item project)
	{
		List<StandardUsernamePasswordCredentials> creds = CredentialsProvider.lookupCredentials(
				StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList());

		StandardListBoxModel model = new StandardListBoxModel();

		model.add(new Option(StringUtils.EMPTY, StringUtils.EMPTY, false));

		for (StandardUsernamePasswordCredentials c : creds)
		{
			boolean isSelected = false;

			if (credentialsId != null)
			{
				isSelected = credentialsId.matches(c.getId());
			}

			String description = Util.fixEmptyAndTrim(c.getDescription());
			model.add(new Option(c.getUsername() + (description != null ? " (" + description + ")" : StringUtils.EMPTY), //$NON-NLS-1$ //$NON-NLS-2$
					c.getId(), isSelected));
		}

		return model;
	}

	public static ListBoxModel buildContainerPrefItems(@AncestorInPath Jenkins context, @QueryParameter String containerPref,
			@AncestorInPath Item project)
	{
		ListBoxModel model = new ListBoxModel();

		model.add(new Option(GitToIspwConstants.CONTAINER_PREF_PER_COMMIT, GitToIspwConstants.CONTAINER_PREF_PER_COMMIT));
		model.add(new Option(GitToIspwConstants.CONTAINER_PREF_PER_BRANCH, GitToIspwConstants.CONTAINER_PREF_PER_BRANCH));
		model.add(new Option(GitToIspwConstants.CONTAINER_PREF_CUSTOM, GitToIspwConstants.CONTAINER_PREF_CUSTOM));

		return model;
	}

	public static Map<String, RefMap> parse(String branchMapping)
	{
		Map<String, RefMap> map = new HashMap<String, RefMap>();

		String[] lines = branchMapping.split("\n"); //$NON-NLS-1$
		for (String line : lines)
		{
			line = StringUtils.trimToEmpty(line);

			if (line.startsWith("#")) //$NON-NLS-1$
			{
				continue;
			}

			int indexOfArrow = line.indexOf("=>"); //$NON-NLS-1$
			if (indexOfArrow != -1)
			{
				String pattern = StringUtils.trimToEmpty(line.substring(0, indexOfArrow));
				String ispwLevel = StringUtils.EMPTY;
				String containerPref = GitToIspwConstants.CONTAINER_PREF_PER_COMMIT;
				String containerDesc = StringUtils.EMPTY;

				String rest = line.substring(indexOfArrow + 2);
				StringTokenizer tokenizer = new StringTokenizer(rest, ",");
				if (tokenizer.hasMoreTokens())
				{
					ispwLevel = StringUtils.trimToEmpty(tokenizer.nextToken());
				}

				if (tokenizer.hasMoreElements())
				{
					containerPref = StringUtils.trimToEmpty(tokenizer.nextToken());
				}

				if(tokenizer.hasMoreElements()) {
					containerDesc = StringUtils.trimToEmpty(tokenizer.nextToken());
				}
				
				RefMap refMap = new RefMap(ispwLevel, containerPref, containerDesc);
				map.put(pattern, refMap);
			}
		}

		return map;
	}
	
	/**
	 * Gets the ref, refId, fromHash, and toHash environment variables and trims them to empty.
	 * 
	 * @param envVars
	 *            the EnvVars for Jenkins
	 */
	public static void trimEnvironmentVariables(EnvVars envVars)
	{
		String toHash = envVars.get(GitToIspwConstants.VAR_TO_HASH, null);
		String fromHash = envVars.get(GitToIspwConstants.VAR_FROM_HASH, null);
		String ref = envVars.get(GitToIspwConstants.VAR_REF, null);
		String refId = envVars.get(GitToIspwConstants.VAR_REF_ID, null);

		envVars.put(GitToIspwConstants.VAR_TO_HASH, StringUtils.trimToEmpty(toHash));
		envVars.put(GitToIspwConstants.VAR_FROM_HASH, StringUtils.trimToEmpty(fromHash));
		envVars.put(GitToIspwConstants.VAR_REF, StringUtils.trimToEmpty(ref));
		envVars.put(GitToIspwConstants.VAR_REF_ID, StringUtils.trimToEmpty(refId));
	}

	/**
	 * Calls the IspwCLI and returns whether the execution was successful. Any exceptions thrown by the executor are caught and
	 * returned as a boolean.
	 * 
	 * @param launcher
	 *            the launcher
	 * @param build
	 *            the Jenkins Run
	 * @param logger
	 *            the logger
	 * @param mapDb
	 *            the database that store the Git push information.
	 * @param gitPushList
	 *            the list of GitPushInfos that is linked to the database. The IspwCLI will be called once for each push.
	 * @param envVars
	 *            the environment variables including ref, refId, fromHash, and toHash
	 * @param publishStep
	 *            The step that this CLI execution is being called from. The publish step is used to get the information input
	 *            into the Jenkins UI.
	 * @param workspacePath
	 *            The folder that holds the git repository cloned by the SCM step.
	 * @return a boolean indicating success.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static boolean callCli(Launcher launcher, Run<?, ?> build, PrintStream logger, EnvVars envVars, RefMap refMap, IGitToIspwPublish publishStep, String workspacePath)
			throws InterruptedException, IOException
	{
		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		if (launcher == null)
		{
			return false;
		}
		VirtualChannel vChannel = launcher.getChannel();

		if (vChannel == null)
		{
			return false;
		}
		
		String toHash = envVars.get(GitToIspwConstants.VAR_TO_HASH, null);
		String fromHash = envVars.get(GitToIspwConstants.VAR_FROM_HASH, null);
		String ref = envVars.get(GitToIspwConstants.VAR_REF, null);
		String refId = envVars.get(GitToIspwConstants.VAR_REF_ID, null);
		if (refMap == null)
		{
			logger.println("branch mapping is not defined for refId: " + refId);
			return false;
		}
		else
		{
			logger.println("mapping refId: " + refId + " to refMap=" + refMap.toString());
		}
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		String osFile = launcher.isUnix()
				? GitToIspwConstants.SCM_DOWNLOADER_CLI_SH
				: GitToIspwConstants.SCM_DOWNLOADER_CLI_BAT;
		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("CLI Script File: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("CLI Script File Remote: " + cliScriptFileRemote); //$NON-NLS-1$

		String topazCliWorkspace = workspacePath + CommonConstants.TOPAZ_CLI_WORKSPACE;
		logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$

		FilePath workDir = new FilePath(vChannel, build.getRootDir().toString());
		workDir.mkdirs();

		if (RestApiUtils.isIspwDebugMode())
		{
			String buildTag = envVars.get("BUILD_TAG"); //$NON-NLS-1$
			logger.println("Getting buildTag =" + buildTag);
		}
   
		boolean success = true;
			CliExecutor cliExecutor = new CliExecutor(logger, build, launcher, envVars, workspacePath, topazCliWorkspace,
					globalConfig, cliScriptFileRemote, workDir);
			try
			{
				success = cliExecutor.execute(true, publishStep.getConnectionId(), publishStep.getCredentialsId(),
						publishStep.getRuntimeConfig(), publishStep.getStream(), publishStep.getApp(), refMap.getIspwLevel(), refMap.getContainerPref(), refMap.getContainerDesc(), 
						publishStep.getGitRepoUrl(), publishStep.getGitCredentialsId(),ref, refId, fromHash, toHash);
			}
			catch (AbortException e)
			{
				success = false;
			}

		if (!success)
		{
			logger.println("Synchronization for push ending with commit " + toHash + " failed.");
		}
		else
		{
			logger.println("Synchronization for push ending with commit " + toHash + " was successful.");
		}
		return success;
	}

	/**
	 * Logs the results to the logger.
	 * 
	 * @param logger
	 *            the logger
	 * @param pushes
	 *            the list of GitPushInfos to acquire information from
	 */
/*	public static void logResults(PrintStream logger, List<GitPushInfo> pushes)
	{
		GitPushInfo metaPush = new GitPushInfo();
		if (pushes != null && !pushes.isEmpty())
		{
			metaPush.setFromHash(pushes.get(0).getFromHash());
			metaPush.setToHash(pushes.get(pushes.size() - 1).getToHash());

			for (GitPushInfo push : pushes)
			{
				metaPush.getSuccessfulCommits().addAll(push.getSuccessfulCommits());
				if (!metaPush.getFailedCommits().isEmpty())
				{
					metaPush.getFailedCommits().addAll(push.getFailedCommits());
					break; // once there's one failed commit, there's no point in looking at more pushes because they were not
							// attempted.
				}
			}
		}

		logger.println("***********************************************************");
		logger.println("*  Synchronization report for Git push                    *");
		logger.println("*  From hash " + metaPush.getFromHash() + "     *");
		logger.println("*  To hash " + metaPush.getToHash() + "       *");
		logger.println("*                                                         *");
		for (String commitId : metaPush.getSuccessfulCommits())
		{
			logger.println("*  " + commitId + "--- SUCCESSFUL *");
		}
		for (String commitId : metaPush.getFailedCommits())
		{
			logger.println("*  " + commitId + "------ FAILURE *");
		}

		boolean isAllCommitsAttempted = true;
		if (!(!metaPush.getSuccessfulCommits().isEmpty() && metaPush.getSuccessfulCommits()
				.get(metaPush.getSuccessfulCommits().size() - 1).equals(metaPush.getToHash())))
		{
			isAllCommitsAttempted = false;
		}
		if (!isAllCommitsAttempted && !(!metaPush.getFailedCommits().isEmpty()
				&& metaPush.getFailedCommits().get(metaPush.getFailedCommits().size() - 1).equals(metaPush.getToHash())))
		{
			// if the "toHash" does not match either the last successful commit or the last failed commit, then there were some
			// commits that were not attempted.
			logger.println("*  SYNCHRONIZATION NOT ATTEMPTED ON REMAINING COMMITS     *");
		}
		logger.println("***********************************************************");
	}*/
}
