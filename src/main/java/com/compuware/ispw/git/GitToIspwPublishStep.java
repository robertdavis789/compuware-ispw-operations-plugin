package com.compuware.ispw.git;

import java.io.File;
import java.io.PrintStream;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.IndexTreeList;
import org.mapdb.Serializer;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.ispw.cli.model.GitPushInfo;
import com.compuware.ispw.cli.model.IGitToIspwPublish;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * GIT to ISPW integration pipeline
 * 
 * @author Sam Zhou
 *
 */
public class GitToIspwPublishStep extends AbstractStepImpl implements IGitToIspwPublish
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
	private boolean clearFailures = DescriptorImpl.clearFailures;

	@DataBoundConstructor
	public GitToIspwPublishStep()
	{
	}

	public static final class Execution extends AbstractSynchronousNonBlockingStepExecution<Integer>
	{
		private static final long serialVersionUID = 1L;

		@Inject
		private transient GitToIspwPublishStep step;

		@StepContextParameter
		private transient Run<?, ?> run;
		@StepContextParameter
		private transient TaskListener listener;

		@Override
		protected Integer run() throws Exception
		{
			PrintStream logger = listener.getLogger();

			EnvVars envVars = getContext().get(hudson.EnvVars.class);
			GitToIspwUtils.trimEnvironmentVariables(envVars);
			String workspacePath = envVars.get("WORKSPACE");
			File workspaceFile = new File(workspacePath);
			workspaceFile.mkdirs();
			File failedCommitFile = new File(workspacePath, GitToIspwConstants.FAILED_COMMIT_FILE_NAME);
			logger.println("Previous push queue file = " + failedCommitFile.getAbsolutePath());
			DB mapDb = DBMaker.fileDB(failedCommitFile.getAbsolutePath()).transactionEnable().make();
			IndexTreeList<GitPushInfo> gitPushList = null;
			boolean success = true;
			try
			{
				if (mapDb != null)
				{
					gitPushList = (IndexTreeList<GitPushInfo>) mapDb.indexTreeList("pushList", Serializer.JAVA).createOrOpen();
				}

				if (step.clearFailures && gitPushList != null)
				{
					logger.println("Attempting to clear commit failures file " + failedCommitFile.getAbsolutePath());
					gitPushList.clear();
					mapDb.commit();
				}

				// Add the new push
				GitToIspwUtils.addNewPushToDb(logger, envVars, mapDb, gitPushList, step.branchMapping);

				CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
				Launcher launcher = getContext().get(Launcher.class);
				// Sync to ISPW
				success = GitToIspwUtils.callCli(launcher, run, logger, mapDb, gitPushList, envVars, step,
						workspacePath);

				// Post the results
				StandardUsernamePasswordCredentials gitCredentials = globalConfig.getLoginInformation(run.getParent(),
						step.gitCredentialsId);
				mapDb = DBMaker.fileDB(failedCommitFile.getAbsolutePath()).transactionEnable().make();
				GitToIspwUtils.logResultsAndNotifyBitbucket(logger, run, listener, mapDb, step.gitRepoUrl, gitCredentials);
				if( success)
					return 0;
				else
					throw new AbortException("An error occurred while synchronizing source to ISPW");

			}
			finally
			{
				if (mapDb != null && !mapDb.isClosed())
				{
					mapDb.commit();
					mapDb.close();
				}
				if( success)
					return 0;
				else
					throw new AbortException("An error occurred while synchronizing source to ISPW");

			}
		}
	}

	@Extension
	public static final class DescriptorImpl extends AbstractStepDescriptorImpl
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

		public static final boolean clearFailures = false;

		public DescriptorImpl()
		{
			super(Execution.class);
		}

		@Override
		public String getDisplayName()
		{
			return "Git to ISPW Integration";
		}

		@Override
		public String getFunctionName()
		{
			return "gitToIspwIntegration";
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

	/**
	 * @return the clearFailures
	 */
	public boolean isClearFailedCommits()
	{
		return clearFailures;
	}

	/**
	 * @param clearFailedCommits
	 *            the clearFailures to set
	 */
	@DataBoundSetter
	public void setClearFailedCommits(boolean clearFailedCommits)
	{
		this.clearFailures = clearFailedCommits;
	}

}
