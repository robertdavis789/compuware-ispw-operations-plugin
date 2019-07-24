package com.compuware.ispw.git;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.stashNotifier.BitbucketNotifier;
import org.jenkinsci.plugins.stashNotifier.StashBuildState;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.CpwrGlobalConfiguration;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.compuware.jenkins.common.utils.ArgumentUtils;
import com.compuware.jenkins.common.utils.CommonConstants;
import com.squareup.tape2.ObjectQueue;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

/**
 * 
 * A reusable CLI executor for both freestyle and pipeline Jenkins build
 * 
 * @author Sam Zhou
 *
 */
public class CliExecutor
{
	private PrintStream logger;
	private Run<?, ?> run;
	private TaskListener listener;
	private EnvVars envVars;
	private Launcher launcher;

	private CpwrGlobalConfiguration globalConfig;

	private String targetFolder;
	private String topazCliWorkspace;

	private String cliScriptFileRemote;
	private FilePath workDir;

	private ObjectQueue<GitInfo> objectQueue;

	public CliExecutor(PrintStream logger, Run<?, ?> run, TaskListener listener, Launcher launcher, EnvVars envVars,
			String targetFolder, String topazCliWorkspace, CpwrGlobalConfiguration globalConfig, String cliScriptFileRemote,
			FilePath workDir, ObjectQueue<GitInfo> objectQueue)
	{
		this.logger = logger;
		this.run = run;
		this.listener = listener;
		this.envVars = envVars;
		this.launcher = launcher;

		this.globalConfig = globalConfig;

		this.targetFolder = targetFolder;
		this.topazCliWorkspace = topazCliWorkspace;

		this.cliScriptFileRemote = cliScriptFileRemote;

		this.workDir = workDir;

		this.objectQueue = objectQueue;

	}

	public boolean execute(boolean bitbucketNotify, String connectionId, String credentialsId, String runtimeConfig,
			String stream, String app, String ispwLevel, String gitRepoUrl, String gitCredentialsId, String ref, String refId,
			String hash) throws InterruptedException, IOException
	{
		HostConnection connection = globalConfig.getHostConnection(connectionId);
		String host = ArgumentUtils.escapeForScript(connection.getHost());
		String port = ArgumentUtils.escapeForScript(connection.getPort());
		String protocol = connection.getProtocol();
		String codePage = connection.getCodePage();
		String timeout = ArgumentUtils.escapeForScript(connection.getTimeout());

		StandardUsernamePasswordCredentials credentials = globalConfig.getLoginInformation(run.getParent(), credentialsId);
		String userId = ArgumentUtils.escapeForScript(credentials.getUsername());
		String password = ArgumentUtils.escapeForScript(credentials.getPassword().getPlainText());
		if (RestApiUtils.isIspwDebugMode())
		{
			logger.println("host=" + host + ", port=" + port + ", protocol=" + protocol + ", codePage=" + codePage
					+ ", timeout=" + timeout + ", userId=" + userId + ", password=" + password);
		}

		StandardUsernamePasswordCredentials gitCredentials = globalConfig.getLoginInformation(run.getParent(),
				gitCredentialsId);
		String gitUserId = ArgumentUtils.escapeForScript(gitCredentials.getUsername());
		String gitPassword = ArgumentUtils.escapeForScript(gitCredentials.getPassword().getPlainText());
		if (RestApiUtils.isIspwDebugMode())
		{
			logger.println("gitRepoUrl=" + gitRepoUrl + ", gitUserId=" + gitUserId + ", gitPassword=" + gitPassword);
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

		if (StringUtils.isNotBlank(runtimeConfig))
		{
			args.add(GitToIspwConstants.ISPW_SERVER_CONFIG_PARAM, runtimeConfig);
		}

		// ispw
		args.add(GitToIspwConstants.ISPW_SERVER_STREAM_PARAM, stream);
		args.add(GitToIspwConstants.ISPW_SERVER_APP_PARAM, app);
		args.add(GitToIspwConstants.ISPW_SERVER_CHECKOUT_LEV_PARAM, ispwLevel);

		// git
		args.add(GitToIspwConstants.GIT_USERID_PARAM, gitUserId);
		args.add(GitToIspwConstants.GIT_PW_PARAM);
		args.add(gitPassword, true);
		args.add(GitToIspwConstants.GIT_REPO_URL_PARAM, ArgumentUtils.escapeForScript(gitRepoUrl));
		args.add(GitToIspwConstants.GIT_REF_PARAM, ref);
		args.add(GitToIspwConstants.GIT_HASH_PARAM, hash);

		workDir.mkdirs();
		logger.println("Shell script: " + args.toString());

		// invoke the CLI (execute the batch/shell script)
		int exitValue = launcher.launch().cmds(args).envs(envVars).stdout(logger).pwd(workDir).join();

		BitbucketNotifier notifier = new BitbucketNotifier(logger, run, listener);
		URL url = new URL(gitRepoUrl);
		String baseUrl = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort();
		if (gitRepoUrl.contains("/bitbucket/"))
		{ // handle test environment
			baseUrl += "/bitbucket";
		}

		String osFile = launcher.isUnix()
				? GitToIspwConstants.SCM_DOWNLOADER_CLI_SH
				: GitToIspwConstants.SCM_DOWNLOADER_CLI_BAT;

		if (exitValue != 0)
		{
			if (bitbucketNotify)
			{
				try
				{
					logger.println("Notify bitbucket success at: " + baseUrl);
					notifier.notifyStash(baseUrl, gitCredentials, hash, StashBuildState.FAILED, null);
				}
				catch (Exception e)
				{
					e.printStackTrace(logger);
				}
			}

			GitInfo newGitInfo = new GitInfo(ref, refId, hash);
			if (objectQueue != null && !objectQueue.asList().contains(newGitInfo))
			{
				objectQueue.add(newGitInfo);
			}

			List<GitInfo> gitInfos = objectQueue.asList();
			logger.println("Current queue - gitInfos = " + gitInfos);

			throw new AbortException("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else
		{
			logger.println("Call " + osFile + " exited with value = " + exitValue); //$NON-NLS-1$ //$NON-NLS-2$

			if (bitbucketNotify)
			{
				try
				{
					logger.println("Notify bitbucket success at: " + baseUrl);

					notifier.notifyStash(baseUrl, gitCredentials, hash, StashBuildState.SUCCESSFUL, null);
				}
				catch (Exception e)
				{
					e.printStackTrace(logger);
				}
			}

			return true;
		}
	}
}
