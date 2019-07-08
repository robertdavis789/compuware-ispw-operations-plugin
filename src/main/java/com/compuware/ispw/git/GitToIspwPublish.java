/**
 * 
 */
package com.compuware.ispw.git;

import java.io.IOException;
import java.io.PrintStream;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.compuware.ispw.restapi.util.RestApiUtils;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
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

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();

		EnvVars envVars = build.getEnvironment(listener);

		String buildTag = envVars.get("BUILD_TAG");
		
		logger.println("...getting buildTag=" + buildTag);
		
		return true;
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

		public DescriptorImpl() {
			load();
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
	
}
