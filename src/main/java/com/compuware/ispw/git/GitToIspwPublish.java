/**
 * 
 */
package com.compuware.ispw.git;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.compuware.ispw.cli.model.IGitToIspwPublish;
import com.compuware.ispw.restapi.util.RestApiUtils;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
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
public class GitToIspwPublish extends Builder implements IGitToIspwPublish
{
	// Git related
	String gitRepoUrl = StringUtils.EMPTY;
	String gitCredentialsId = StringUtils.EMPTY;

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

		AbstractProject<?, ?> project = build.getProject();
		SCM scm = project.getScm();
		if (scm instanceof GitSCM)
		{
			GitSCM gitSCM = (GitSCM) scm;
			List<UserRemoteConfig> userRemoteConfigs = gitSCM.getUserRemoteConfigs();
			gitRepoUrl = userRemoteConfigs.get(0).getUrl();
			gitCredentialsId = userRemoteConfigs.get(0).getCredentialsId();
		}
		else
		{
			if (scm instanceof NullSCM)
			{
				throw new AbortException(
						"Jenkins Git Plugin SCM is required along with selecting the Git option in the Source Code Management section and providing the Git repository URL and credentials."); //$NON-NLS-1$
			}
			else
			{
				throw new AbortException(
						"The Git option must be selected in the Jenkins project Source Code Management section along with providing the Git repository URL and credentials. The Source Code Management section selection type is " //$NON-NLS-1$
								+ scm.getType());
			}

		}

		EnvVars envVars = build.getEnvironment(listener);
		GitToIspwUtils.trimEnvironmentVariables(envVars);
		String workspacePath = envVars.get("WORKSPACE"); //$NON-NLS-1$
		File workspaceFile = new File(workspacePath);
		workspaceFile.mkdirs();
		Map<String, RefMap> map = GitToIspwUtils.parse(branchMapping);
		logger.println("map=" + map);
		String refId = envVars.get(GitToIspwConstants.VAR_REF_ID, null);
		BranchPatternMatcher matcher = new BranchPatternMatcher(map, logger);
		RefMap refMap = matcher.match(refId);

		// Sync to ISPW
		boolean success = GitToIspwUtils.callCli(launcher, build, logger, envVars, refMap, this, workspacePath);

		if (!success)
		{
			throw new AbortException("An error occurred while synchronizing source to ISPW");
		}
		return true;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
	{
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
	 * @return the gitCredentialsId
	 */
	public String getGitCredentialsId()
	{
		return gitCredentialsId;
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
