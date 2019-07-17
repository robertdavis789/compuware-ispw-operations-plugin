/**
 * 
 */
package com.compuware.ispw.git;

import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * GIT to ISPW integration pipeline build
 * 
 * @author Sam Zhou
 *
 */
public class GitToIspwPublishStep extends AbstractStepImpl
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
	public GitToIspwPublishStep()
	{
	}

	public static final class Execution extends AbstractSynchronousNonBlockingStepExecution<Integer>
	{

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

			String hash = envVars.get(GitToIspwConstants.VAR_HASH, GitToIspwConstants.VAR_HASH);
			String ref = envVars.get(GitToIspwConstants.VAR_REF, GitToIspwConstants.VAR_REF);
			String refId = envVars.get(GitToIspwConstants.VAR_REF_ID, GitToIspwConstants.VAR_REF_ID);

			if (hash.equals(GitToIspwConstants.VAR_HASH) || ref.equals(GitToIspwConstants.VAR_REF)
					|| refId.equals(GitToIspwConstants.VAR_REF_ID))
			{
				logger.println("hash, ref, refId must be presented in order for the build to work");
				return -1;
			}
			else
			{
				logger.println("hash=" + hash + ", ref=" + ref + ", refId=" + refId);
			}

			Map<String, RefMap> map = GitToIspwUtils.parse(step.branchMapping);
			logger.println("map=" + map);

			BranchPatternMatcher matcher = new BranchPatternMatcher(map, logger);
			RefMap refMap = matcher.match(refId);

			if (refMap == null)
			{
				logger.println("branch mapping is not defined for refId: " + refId);
				return -1;
			}
			else
			{
				logger.println("mapping refId: " + refId + " to refMap=" + refMap.toString());
			}

			String ispwLevel = refMap.getIspwLevel();
			String containerPref = refMap.getContainerPref();

			if (RestApiUtils.isIspwDebugMode())
			{
				String buildTag = envVars.get("BUILD_TAG");
				logger.println("getting buildTag=" + buildTag);

				String debugMsg = ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
				logger.println("debugMsg=" + debugMsg);
			}

			CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();

			Launcher launcher = getContext().get(Launcher.class);
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

			// server args
			HostConnection connection = globalConfig.getHostConnection(step.connectionId);
			String host = ArgumentUtils.escapeForScript(connection.getHost());
			String port = ArgumentUtils.escapeForScript(connection.getPort());
			String protocol = connection.getProtocol();
			String codePage = connection.getCodePage();
			String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
			StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(),
					step.credentialsId);
			String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
			String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
			String targetFolder = ArgumentUtils.escapeForScript(run.getRootDir().toString());
			String topazCliWorkspace = run.getRootDir().toString() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE;
			logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
			logger.println("targetFolder: " + targetFolder);

			if (RestApiUtils.isIspwDebugMode())
			{
				logger.println("host=" + host + ", port=" + port + ", protocol=" + protocol + ", codePage=" + codePage
						+ ", timeout=" + timeout + ", userId=" + userId + ", password=" + password);
			}

			StandardUsernamePasswordCredentials gitCredentials = globalConfig.getLoginInformation(run.getParent(),
					step.gitCredentialsId);
			String gitUserId = ArgumentUtils.escapeForScript(gitCredentials.getUsername());
			String gitPassword = ArgumentUtils.escapeForScript(gitCredentials.getPassword().getPlainText());

			if (RestApiUtils.isIspwDebugMode())
			{
				logger.println("gitRepoUrl=" + step.gitRepoUrl + ", gitUserId=" + gitUserId + ", gitPassword=" + gitPassword);
			}

			ArgumentListBuilder args = new ArgumentListBuilder();
			// build the list of arguments to pass to the CLI

			args.add(cliScriptFileRemote);

			// operation
			args.add(GitToIspwConstants.ISPW_OPERATION_PARAM, "syncGitToIspw");

			// host connection
			args.add(CommonConstants.HOST_PARM, host);
			args.add(CommonConstants.PORT_PARM, port);
			args.add(CommonConstants.USERID_PARM, userId);
			args.add(CommonConstants.PW_PARM);
			args.add(password, true);

			if (StringUtils.isNotBlank(protocol))
			{
				args.add(CommonConstants.PROTOCOL_PARM, protocol);
			}

			args.add(CommonConstants.CODE_PAGE_PARM, codePage);
			args.add(CommonConstants.TIMEOUT_PARM, timeout);
			args.add(CommonConstants.TARGET_FOLDER_PARM, targetFolder);
			args.add(CommonConstants.DATA_PARM, topazCliWorkspace);

			if (StringUtils.isNotBlank(step.runtimeConfig))
			{
				args.add(GitToIspwConstants.ISPW_SERVER_CONFIG_PARAM, step.runtimeConfig);
			}

			// ispw
			args.add(GitToIspwConstants.ISPW_SERVER_STREAM_PARAM, step.stream);
			args.add(GitToIspwConstants.ISPW_SERVER_APP_PARAM, step.app);
			args.add(GitToIspwConstants.ISPW_SERVER_CHECKOUT_LEV_PARAM, ispwLevel);

			// git
			args.add(GitToIspwConstants.GIT_USERID_PARAM, gitUserId);
			args.add(GitToIspwConstants.GIT_PW_PARAM);
			args.add(gitPassword, true);
			args.add(GitToIspwConstants.GIT_REPO_URL_PARAM, ArgumentUtils.escapeForScript(step.gitRepoUrl));
			args.add(GitToIspwConstants.GIT_REF_PARAM, ref);
			args.add(GitToIspwConstants.GIT_HASH_PARAM, hash);

			// create the CLI workspace (in case it doesn't already exist)

			FilePath workDir = new FilePath(vChannel, run.getRootDir().toString());
			workDir.mkdirs();

			logger.println("Shell script: " + args.toString());

			// invoke the CLI (execute the batch/shell script)
			int exitValue = launcher.launch().cmds(args).envs(envVars).stdout(logger).pwd(workDir).join();
			if (exitValue != 0)
			{
				throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
				return exitValue;
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
		public static final String branchMapping = "#The following comments show how to use the 'Branch Mapping' field.\n"
				+ "#Click on the help button to the right of the screen for more details on how to populate this field\n" + "#\n"
				+ "#*/dev1/ => DEV1, per-commit\n" + "#*/dev2/ => DEV2, per-branch\n"
				+ "#*/dev3/ => DEV3, custom, a description\n";
		public static final String containerDesc = StringUtils.EMPTY;
		public static final String containerPref = StringUtils.EMPTY;

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
			return "gitToIspwItegration";
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
