/**
 * 
 */
package com.compuware.ispw.git;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.ispw.restapi.Constants;
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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
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
	private String containerDesc = DescriptorImpl.containerDesc;
	private String containerPref = DescriptorImpl.containerPref;
	
	@DataBoundConstructor
	public GitToIspwPublish() {
	}
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();

		EnvVars envVars = build.getEnvironment(listener);
		String hash = envVars.get("hash", "hash");
		String ref = envVars.get("ref", "ref");

		String buildTag = envVars.get("BUILD_TAG");

		logger.println("getting buildTag=" + buildTag);
		String debugMsg = ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		logger.println("debugMsg=" + debugMsg);
		logger.println("hash=" + hash + ", ref=" + ref);

		CpwrGlobalConfiguration globalConfig = CpwrGlobalConfiguration.get();
		
		assert launcher!=null;
		VirtualChannel vChannel = launcher.getChannel();
		
		assert vChannel!=null;
		Properties remoteProperties = vChannel.call(new RemoteSystemProperties());
		String remoteFileSeparator = remoteProperties.getProperty(CommonConstants.FILE_SEPARATOR_PROPERTY_KEY);
		String osFile = launcher.isUnix() ? Constants.SCM_DOWNLOADER_CLI_SH : Constants.SCM_DOWNLOADER_CLI_BAT;

		String cliScriptFile = globalConfig.getTopazCLILocation(launcher) + remoteFileSeparator + osFile;
		logger.println("cliScriptFile: " + cliScriptFile); //$NON-NLS-1$
		String cliScriptFileRemote = new FilePath(vChannel, cliScriptFile).getRemote();
		logger.println("cliScriptFileRemote: " + cliScriptFileRemote); //$NON-NLS-1$

		
		
		// server args
		HostConnection connection = globalConfig.getHostConnection(connectionId);
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String protocol = connection.getProtocol();
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());
		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(build.getParent(),
				credentialsId);
		String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
		String targetFolder = ArgumentUtils.escapeForScript(build.getWorkspace().getRemote());
		String topazCliWorkspace = build.getWorkspace().getRemote() + remoteFileSeparator + CommonConstants.TOPAZ_CLI_WORKSPACE;
		logger.println("TopazCliWorkspace: " + topazCliWorkspace); //$NON-NLS-1$
		logger.println("targetFolder: "+targetFolder);
		
		//logger.println("remoteProperties="+remoteProperties);
		
		logger.println("host="+host+", port="+port+", protocol="+protocol+", codePage="+codePage+", timeout="+timeout+", userId="+userId+", password="+password);
		
		StandardUsernamePasswordCredentials gitCredentials = globalConfig.getLoginInformation(build.getParent(),
				gitCredentialsId);
		String gitUserId = ArgumentUtils.escapeForScript(gitCredentials.getUsername());
		String gitPassword = ArgumentUtils.escapeForScript(gitCredentials.getPassword().getPlainText());
		
		logger.println("gitRepoUrl="+gitRepoUrl+", gitUserId="+gitUserId+", gitPassword="+gitPassword);
		
		ArgumentListBuilder args = new ArgumentListBuilder();
		// build the list of arguments to pass to the CLI
		
		args.add(cliScriptFileRemote);
		
		// operation
		args.add(Constants.ISPW_OPERATION_PARAM, "syncGitToIspw");
		
		// host connection
		args.add(CommonConstants.HOST_PARM, host);
		args.add(CommonConstants.PORT_PARM, port);
		args.add(CommonConstants.USERID_PARM, userId);
		args.add(CommonConstants.PW_PARM);
		args.add(password, true);
		
		if(StringUtils.isNotBlank(protocol)) {
			args.add(CommonConstants.PROTOCOL_PARM, protocol);
		}
		
		args.add(CommonConstants.CODE_PAGE_PARM, codePage);
		args.add(CommonConstants.TIMEOUT_PARM, timeout);
		args.add(CommonConstants.TARGET_FOLDER_PARM, targetFolder);
		args.add(CommonConstants.DATA_PARM, topazCliWorkspace);

		if (StringUtils.isNotBlank(runtimeConfig))
		{
			runtimeConfig = ArgumentUtils.escapeForScript(runtimeConfig);
			args.add(Constants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}

		// ispw
		args.add(Constants.ISPW_SERVER_STREAM_PARAM, stream);
		args.add(Constants.ISPW_SERVER_APP_PARAM, app);
		args.add(Constants.ISPW_SERVER_CHECKOUT_LEV_PARAM, "DEV1");

		// git
		args.add(Constants.GIT_USERID_PARAM, gitUserId);
		args.add(Constants.GIT_PW_PARAM);
		args.add(gitPassword, true);
		args.add(Constants.GIT_REPO_URL_PARAM, gitRepoUrl);
		args.add(Constants.GIT_REF_PARAM, ref);
		args.add(Constants.GIT_HASH_PARAM, hash);
		
		// create the CLI workspace (in case it doesn't already exist)
		EnvVars env = build.getEnvironment(listener);
		FilePath workDir = new FilePath(vChannel, build.getWorkspace().getRemote());
		workDir.mkdirs();

		logger.println("Batch script: " + args.toString());
		
		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(env).stdout(logger).pwd(workDir).join();
		if (exitValue != 0)
		{
			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
			return true;
		}

	}
	
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
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
		public static final String branchMapping = "#The following messages are commented out to show how to use the 'Branch Mapping' field.\n"
				+"#Click on the help button to the right of the screen for examples of how to populate this field\n"
				+"#\n"
				+"#*/dev1/ => DEV1, per-commit\n"
				+"#*/dev2/ => DEV2, per-branch\n"
				+"#*/dev3/ => DEV3, custom, description\n";
		public static final String containerDesc = StringUtils.EMPTY;
		public static final String containerPref = StringUtils.EMPTY;
		
		public DescriptorImpl() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "GIT to ISPW Integration";
		}
		
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			return true;
		}
		
		// GIT
		public ListBoxModel doFillGitCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String gitCredentialsId,
				@AncestorInPath Item project) {
			return RestApiUtils.buildStandardCredentialsIdItems(context, gitCredentialsId, project);
		}
		
		// ISPW
		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project)
		{
			return RestApiUtils.buildConnectionIdItems(context,  connectionId, project);
		}
		
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project) {
			return RestApiUtils.buildStandardCredentialsIdItems(context, credentialsId, project);
		}

		public ListBoxModel doFillContainerPrefItems(@AncestorInPath Jenkins context, @QueryParameter String containerPref,
				@AncestorInPath Item project)
		{
			return RestApiUtils.buildContainerPrefItems(context, credentialsId, project);
		}
		
	}

	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void xStreamCompatibility() {
	}
	
	/**
	 * @return the gitRepoUrl
	 */
	public String getGitRepoUrl()
	{
		return gitRepoUrl;
	}

	/**
	 * @param gitRepoUrl the gitRepoUrl to set
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
	 * @param gitCredentialsId the gitCredentialsId to set
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
	 * @param connectionId the connectionId to set
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
	 * @param credentialsId the credentialsId to set
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
	 * @param runtimeConfig the runtimeConfig to set
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
	 * @param stream the stream to set
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
	 * @param app the app to set
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
	 * @param branchMapping the branchMapping to set
	 */
	@DataBoundSetter
	public void setBranchMapping(String branchMapping)
	{
		this.branchMapping = branchMapping;
	}

	/**
	 * @return the containerDesc
	 */
	public String getContainerDesc()
	{
		return containerDesc;
	}

	/**
	 * @param containerDesc the containerDesc to set
	 */
	@DataBoundSetter
	public void setContainerDesc(String containerDesc)
	{
		this.containerDesc = containerDesc;
	}

	/**
	 * @return the containerPref
	 */
	public String getContainerPref()
	{
		return containerPref;
	}

	/**
	 * @param containerPref the containerPref to set
	 */
	@DataBoundSetter
	public void setContainerPref(String containerPref)
	{
		this.containerPref = containerPref;
	}
	
}
