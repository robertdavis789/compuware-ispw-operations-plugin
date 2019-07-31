/**
 * 
 */
package com.compuware.ispw.git;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import com.squareup.tape2.ObjectQueue;
import com.squareup.tape2.QueueFile;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * GIT to ISPW publisher
 * 
 * @author Sam Zhou
 *
 */
public class GitToIspwPublish extends Builder
{
	// GIT related
	private String gitRepoUrl = DescriptorImpl.gitRepoUrl;
	private String gitCredentialsId = DescriptorImpl.gitCredentialsId;

	// ISPW related
	private String connectionId = DescriptorImpl.connectionId;
	private String credentialsId = DescriptorImpl.credentialsId;
	private String runtimeConfig = DescriptorImpl.runtimeConfig;
	private String stream = DescriptorImpl.stream;
	private String app = DescriptorImpl.app;

	// Branch mapping
	private String branchMapping = DescriptorImpl.branchMapping;

	@DataBoundConstructor
	public GitToIspwPublish()
	{
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException
	{
		PrintStream logger = listener.getLogger();

		EnvVars envVars = build.getEnvironment(listener);

		String hash = envVars.get(GitToIspwConstants.VAR_HASH, GitToIspwConstants.VAR_HASH);
		String ref = envVars.get(GitToIspwConstants.VAR_REF, GitToIspwConstants.VAR_REF);
		String refId = envVars.get(GitToIspwConstants.VAR_REF_ID, GitToIspwConstants.VAR_REF_ID);

		File file = new File(build.getRootDir(), "../" + GitToIspwConstants.FILE_QUEUE);
		logger.println("commits queue file path = " + file.toString());

		QueueFile queueFile = new QueueFile.Builder(file).build();
		GitInfoConverter converter = new GitInfoConverter();
		ObjectQueue<GitInfo> objectQueue = ObjectQueue.create(queueFile, converter);

		boolean newCommit = true;
		List<GitInfo> gitInfos = new ArrayList<GitInfo>();
		if (hash.equals(GitToIspwConstants.VAR_HASH) || ref.equals(GitToIspwConstants.VAR_REF)
				|| refId.equals(GitToIspwConstants.VAR_REF_ID))
		{
			logger.println(
					"hash, ref, refId must be presented in order for the build to work, reading from file queue if any ...");

			GitInfo gitInfo = objectQueue.peek();
			if (gitInfo != null)
			{
				newCommit = false;
				gitInfos = objectQueue.asList();
				logger.println("Re-push failed commits to ISPW...");
			}
			else
			{
				logger.println("No failed commits, do nothing...");
				return true;
			}
		}
		else
		{
			logger.println("New commit - hash=" + hash + ", ref=" + ref + ", refId=" + refId);

			newCommit = true;
			gitInfos.add(new GitInfo(ref, refId, hash));
		}

		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

		assert launcher != null;
		VirtualChannel vChannel = launcher.getChannel();

		assert vChannel != null;
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		String osFile = launcher.isUnix()
				? GitToIspwConstants.SCM_DOWNLOADER_CLI_SH
				: GitToIspwConstants.SCM_DOWNLOADER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$

		String targetFolder = ArgumentUtils.escapeForScript(build.getWorkspace().getRemote());
		String topazCliWorkspace = build.getWorkspace().getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE;
		logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		logger.println("targetFolder: " + targetFolder);

		FilePath workDir = new FilePath(vChannel, build.getWorkspace().getRemote());

		for (GitInfo gitInfo : gitInfos)
		{
			logger.println("gitInfo = " + gitInfo);

			ref = gitInfo.getRef();
			refId = gitInfo.getRefId();
			hash = gitInfo.getHash();

			Map<String, RefMap> map = GitToIspwUtils.parse(branchMapping);
			logger.println("map=" + map);

			BranchPatternMatcher matcher = new BranchPatternMatcher(map, logger);
			RefMap refMap = matcher.match(refId);

			if (refMap == null)
			{
				logger.println("branch mapping is not defined for refId: " + refId);
				return false;
			}
			else
			{
				logger.println("mapping refId: " + refId + " to refMap=" + refMap.toString());
			}

			String ispwLevel = refMap.getIspwLevel();
			String containerPref = refMap.getContainerPref();
			String containerDesc = refMap.getContainerDesc();

			if (RestApiUtils.isIspwDebugMode())
			{
				String buildTag = envVars.get("BUILD_TAG");
				logger.println("getting buildTag=" + buildTag);

				String debugMsg = ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
				logger.println("debugMsg=" + debugMsg);
			}

			CliExecutor cliExecutor = new CliExecutor(logger, build, listener, launcher, envVars, targetFolder,
					topazCliWorkspace, globalConfig, cliScriptFileRemote, workDir, objectQueue);
			boolean success = cliExecutor.execute(true, connectionId, credentialsId, runtimeConfig, stream, app, ispwLevel,
					containerPref, containerDesc, gitRepoUrl, gitCredentialsId, ref, refId, hash);

			if (success)
			{
				if (!newCommit)
				{
					objectQueue.remove();
				}
			}
			else
			{
				return false;
			}
		}

		return true;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
	{

		// GIT related
		public static final String gitRepoUrl = StringUtils.EMPTY;
		public static final String gitCredentialsId = StringUtils.EMPTY;

		// ISPW related
		public static final String connectionId = StringUtils.EMPTY;
		public static final String credentialsId = StringUtils.EMPTY;
		public static final String runtimeConfig = StringUtils.EMPTY;
		public static final String stream = StringUtils.EMPTY;
		public static final String app = StringUtils.EMPTY;

		// Branch mapping
		public static final String branchMapping = GitToIspwConstants.BRANCH_MAPPING_DEFAULT;

		public static final String containerDesc = StringUtils.EMPTY;
		public static final String containerPref = StringUtils.EMPTY;

		public DescriptorImpl()
		{
			load();
		}

		@Override
		public String getDisplayName()
		{
			return "Git to ISPW Integration";
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			return true;
		}

		// GIT
		public ListBoxModel doFillGitCredentialsIdItems(@AncestorInPath Jenkins context,
				@QueryParameter String gitCredentialsId, @AncestorInPath Item project)
		{
			return GitToIspwUtils.buildStandardCredentialsIdItems(context, gitCredentialsId, project);
		}

		// ISPW
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project)
		{
			return RestApiUtils.buildConnectionIdItems(context, connectionId, project);
		}

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project)
		{
			return GitToIspwUtils.buildStandardCredentialsIdItems(context, credentialsId, project);
		}

	}

	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void xStreamCompatibility()
	{
	}

	/**
	 * @return the gitRepoUrl
	 */
	public String getGitRepoUrl()
	{
		return gitRepoUrl;
	}

	/**
	 * @param gitRepoUrl
	 *            the gitRepoUrl to set
	 */
	@DataBoundSetter
	public void setGitRepoUrl(String gitRepoUrl)
	{
		this.gitRepoUrl = gitRepoUrl;
	}

	/**
	 * @return the gitCredentialsId
	 */
	public String getGitCredentialsId()
	{
		return gitCredentialsId;
	}

	/**
	 * @param gitCredentialsId
	 *            the gitCredentialsId to set
	 */
	@DataBoundSetter
	public void setGitCredentialsId(String gitCredentialsId)
	{
		this.gitCredentialsId = gitCredentialsId;
	}

	/**
	 * @return the connectionId
	 */
	public String getConnectionId()
	{
		return connectionId;
	}

	/**
	 * @param connectionId
	 *            the connectionId to set
	 */
	@DataBoundSetter
	public void setConnectionId(String connectionId)
	{
		this.connectionId = connectionId;
	}

	/**
	 * @return the credentialsId
	 */
	public String getCredentialsId()
	{
		return credentialsId;
	}

	/**
	 * @param credentialsId
	 *            the credentialsId to set
	 */
	@DataBoundSetter
	public void setCredentialsId(String credentialsId)
	{
		this.credentialsId = credentialsId;
	}

	/**
	 * @return the runtimeConfig
	 */
	public String getRuntimeConfig()
	{
		return runtimeConfig;
	}

	/**
	 * @param runtimeConfig
	 *            the runtimeConfig to set
	 */
	@DataBoundSetter
	public void setRuntimeConfig(String runtimeConfig)
	{
		this.runtimeConfig = runtimeConfig;
	}

	/**
	 * @return the stream
	 */
	public String getStream()
	{
		return stream;
	}

	/**
	 * @param stream
	 *            the stream to set
	 */
	@DataBoundSetter
	public void setStream(String stream)
	{
		this.stream = stream;
	}

	/**
	 * @return the app
	 */
	public String getApp()
	{
		return app;
	}

	/**
	 * @param app
	 *            the app to set
	 */
	@DataBoundSetter
	public void setApp(String app)
	{
		this.app = app;
	}

	/**
	 * @return the branchMapping
	 */
	public String getBranchMapping()
	{
		return branchMapping;
	}

	/**
	 * @param branchMapping
	 *            the branchMapping to set
	 */
	@DataBoundSetter
	public void setBranchMapping(String branchMapping)
	{
		this.branchMapping = branchMapping;
	}

}
