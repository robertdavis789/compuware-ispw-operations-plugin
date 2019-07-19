package com.compuware.ispw.restapi;

import static com.google.common.base.Preconditions.checkArgument;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.compuware.ispw.model.rest.SetInfoResponse;
import com.compuware.ispw.model.rest.TaskResponse;
import com.compuware.ispw.restapi.action.IAction;
import com.compuware.ispw.restapi.action.SetOperationAction;
import com.compuware.ispw.restapi.auth.BasicDigestAuthentication;
import com.compuware.ispw.restapi.auth.FormAuthentication;
import com.compuware.ispw.restapi.util.HttpClientUtil;
import com.compuware.ispw.restapi.util.HttpRequestNameValuePair;
import com.compuware.ispw.restapi.util.ReflectUtils;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.HostConnection;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
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
import hudson.model.Items;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;

/**
 * ISPW rest API free style builder
 * 
 * @author Janario Oliveira
 * @author Sam Zhou
 */
@SuppressWarnings("deprecation")
public class IspwRestApiRequest extends Builder {

	private @Nonnull String url = StringUtils.EMPTY;
	private Boolean ignoreSslErrors = DescriptorImpl.ignoreSslErrors;
	private HttpMode httpMode = DescriptorImpl.httpMode;
	private String httpProxy = DescriptorImpl.httpProxy;
	private Boolean passBuildParameters = DescriptorImpl.passBuildParameters;
	private String validResponseCodes = DescriptorImpl.validResponseCodes;
	private String validResponseContent = DescriptorImpl.validResponseContent;
	private MimeType acceptType = DescriptorImpl.acceptType;
	private MimeType contentType = DescriptorImpl.contentType;
	private String outputFile = DescriptorImpl.outputFile;
	private Integer timeout = DescriptorImpl.timeout;
	private String requestBody = DescriptorImpl.requestBody;
	private String authentication = DescriptorImpl.authentication;
	private String token = DescriptorImpl.token;
	private List<HttpRequestNameValuePair> customHeaders = DescriptorImpl.customHeaders;

	// ISPW
	private String connectionId = DescriptorImpl.connectionId;
	private String credentialsId = DescriptorImpl.credentialsId;
	private String ispwAction = DescriptorImpl.ispwAction;
	private String ispwRequestBody = DescriptorImpl.ispwRequestBody;
	private Boolean consoleLogResponseBody = DescriptorImpl.consoleLogResponseBody;
	private Boolean skipWaitingForSet = DescriptorImpl.skipWaitingForSet;

	@DataBoundConstructor
	public IspwRestApiRequest() {
	}

	@Nonnull
	public String getUrl() {
		return url;
	}

	public Boolean getIgnoreSslErrors() {
		return ignoreSslErrors;
	}

	public HttpMode getHttpMode() {
		return httpMode;
	}

	public String getIspwAction() {
		return ispwAction;
	}

	@DataBoundSetter
	public void setIspwAction(String ispwAction) {
		this.ispwAction = ispwAction;
	}

	public String getConnectionId() {
		return connectionId;
	}

	@DataBoundSetter
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public String getCredentialsId()
	{
		return credentialsId;
	}
	
	@DataBoundSetter
	public void setCredentialsId(String credentialsId) {
		this.credentialsId = credentialsId;
	}
	
	public String getIspwRequestBody() {
		return ispwRequestBody;
	}

	@DataBoundSetter
	public void setIspwRequestBody(String ispwRequestBody) {
		this.ispwRequestBody = ispwRequestBody;
	}

	public String getToken() {
		return token;
	}

	public String getHttpProxy() {
		return httpProxy;
	}

	public Boolean getPassBuildParameters() {
		return passBuildParameters;
	}

	@Nonnull
	public String getValidResponseCodes() {
		return validResponseCodes;
	}

	public String getValidResponseContent() {
		return validResponseContent;
	}

	public MimeType getAcceptType() {
		return acceptType;
	}

	public MimeType getContentType() {
		return contentType;
	}

	public String getOutputFile() {
		return outputFile;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public Boolean getSkipWaitingForSet() {
		return skipWaitingForSet;
	}

	@DataBoundSetter
	public void setSkipWaitingForSet(Boolean skipWaitingForSet) {
		this.skipWaitingForSet = skipWaitingForSet;
	}

	public Boolean getConsoleLogResponseBody() {
		return consoleLogResponseBody;
	}

	@DataBoundSetter
	public void setConsoleLogResponseBody(Boolean consoleLogResponseBody) {
		this.consoleLogResponseBody = consoleLogResponseBody;
	}

	public String getAuthentication() {
		return authentication;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public List<HttpRequestNameValuePair> getCustomHeaders() {
		return customHeaders;
	}

	@Initializer(before = InitMilestone.PLUGINS_STARTED)
	public static void xStreamCompatibility() {
		Items.XSTREAM2.aliasField("logResponseBody", IspwRestApiRequest.class,
				"consoleLogResponseBody");
		Items.XSTREAM2.aliasField("consoleLogResponseBody", IspwRestApiRequest.class,
				"consoleLogResponseBody");
		
		Items.XSTREAM2.aliasField("skipWaitingForSet", IspwRestApiRequest.class, "skipWaitingForSet");
		
		Items.XSTREAM2.alias("pair", HttpRequestNameValuePair.class);
		
	}

	protected Object readResolve() {
		if (customHeaders == null) {
			customHeaders = DescriptorImpl.customHeaders;
		}
		if (validResponseCodes == null || validResponseCodes.trim().isEmpty()) {
			validResponseCodes = DescriptorImpl.validResponseCodes;
		}
		if (ignoreSslErrors == null) {
			// default for new job false(DescriptorImpl.ignoreSslErrors) for old ones true to keep
			// same behavior
			ignoreSslErrors = true;
		}

		return this;
	}

	private List<HttpRequestNameValuePair> createParams(EnvVars envVars, AbstractBuild<?, ?> build,
			TaskListener listener) throws IOException {
		Map<String, String> buildVariables = build.getBuildVariables();
		if (buildVariables.isEmpty()) {
			return Collections.emptyList();
		}
		PrintStream logger = listener.getLogger();
		logger.println("Parameters: ");

		List<HttpRequestNameValuePair> l = new ArrayList<>();
		for (Map.Entry<String, String> entry : buildVariables.entrySet()) {
			String value = envVars.expand(entry.getValue());
			logger.println("  " + entry.getKey() + " = " + value);

			l.add(new HttpRequestNameValuePair(entry.getKey(), value));
		}
		return l;
	}

	String resolveUrl(EnvVars envVars, AbstractBuild<?, ?> build, TaskListener listener)
			throws IOException {
		String url = envVars.expand(getUrl());
		if (Boolean.TRUE.equals(getPassBuildParameters()) && getHttpMode() == HttpMode.GET) {
			List<HttpRequestNameValuePair> params = createParams(envVars, build, listener);
			if (!params.isEmpty()) {
				url = HttpClientUtil.appendParamsToUrl(url, params);
			}
		}
		return url;
	}

	List<HttpRequestNameValuePair> resolveHeaders(EnvVars envVars) {
		final List<HttpRequestNameValuePair> headers = new ArrayList<>();

		headers.add(new HttpRequestNameValuePair("Content-type", MimeType.APPLICATION_JSON
				.toString()));
		headers.add(new HttpRequestNameValuePair("Authorization", getToken()));

		if (acceptType != null && acceptType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Accept", acceptType.getValue()));
		}

		for (HttpRequestNameValuePair header : customHeaders) {
			String headerName = envVars.expand(header.getName());
			String headerValue = envVars.expand(header.getValue());
			boolean maskValue =
					headerName.equalsIgnoreCase("Authorization") || header.getMaskValue();

			headers.add(new HttpRequestNameValuePair(headerName, headerValue, maskValue));
		}
		return headers;
	}

	String resolveBody(EnvVars envVars, AbstractBuild<?, ?> build, TaskListener listener)
			throws IOException {
		String body = envVars.expand(getRequestBody());
		if (Strings.isNullOrEmpty(body) && Boolean.TRUE.equals(getPassBuildParameters())) {
			List<HttpRequestNameValuePair> params = createParams(envVars, build, listener);
			if (!params.isEmpty()) {
				body = HttpClientUtil.paramsToString(params);
			}
		}
		return body;
	}

	FilePath resolveOutputFile(EnvVars envVars, AbstractBuild<?, ?> build) {
		if (outputFile == null || outputFile.trim().isEmpty()) {
			return null;
		}
		String filePath = envVars.expand(outputFile);
		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
			throw new IllegalStateException("Could not find workspace to save file outputFile: "
					+ outputFile);
		}
		return workspace.child(filePath);
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();

		EnvVars envVars = build.getEnvironment(listener);

		String buildTag = envVars.get("BUILD_TAG");
		WebhookToken webhookToken = WebhookTokenManager.getInstance().get(buildTag);
		
		if (RestApiUtils.isIspwDebugMode())
			logger.println("...getting buildTag=" + buildTag + ", webhookToken=" + webhookToken);

		IAction action = ReflectUtils.createAction(ispwAction, logger);
		httpMode = action.getHttpMode();
		
		if (!ReflectUtils.isActionInstantiated(action))
		{
			logger.println("Action:" + ispwAction
					+ " is not implemented, please make sure you have the correct ISPW action name");
			return false;
		}

		if (RestApiUtils.isIspwDebugMode())
			logger.println("...ispwAction=" + ispwAction + ", httpMode=" + httpMode);

		String cesUrl = StringUtils.EMPTY;
		String cesIspwHost = StringUtils.EMPTY;

		HostConnection hostConnection = RestApiUtils.getCesUrl(connectionId);
		if (hostConnection != null) {
			cesUrl = StringUtils.trimToEmpty(hostConnection.getCesUrl());
			if(!cesUrl.startsWith("http")) {
				logger.println("Host connection does NOT contain a valid CES URL. Please re-configure in 'Manage Jenkins | Configure System | Compuware Configurations' section");
				return false;
			}
			
			
			String host = StringUtils.trimToEmpty(hostConnection.getHost());
			String port = StringUtils.trimToEmpty(hostConnection.getPort());
			cesIspwHost = host + "-" + port;
		}

		String cesIspwToken = RestApiUtils.getCesToken(credentialsId);

		if (RestApiUtils.isIspwDebugMode())
			logger.println("CES Url=" + cesUrl + ", ces.ispw.host=" + cesIspwHost
					+ ", ces.ispw.token=" + cesIspwToken);

		IspwRequestBean ispwRequestBean =
				action.getIspwRequestBean(cesIspwHost, ispwRequestBody, webhookToken);
		
		if (RestApiUtils.isIspwDebugMode())
			logger.println("...ispwRequestBean=" + ispwRequestBean);

		this.url = cesUrl + ispwRequestBean.getContextPath(); // CES URL
		this.requestBody = ispwRequestBean.getJsonRequest();
		this.token = cesIspwToken; // CES TOKEN

		// This is a generated code for Visual Studio Code - REST Client
		if (consoleLogResponseBody) {
			logger.println();
			logger.println();
			logger.println("### " + ispwAction + " - " + "RFC 2616");
			logger.println();
			logger.println(httpMode + " " + url + " HTTP/1.1");
			logger.println("Content-type: " + MimeType.APPLICATION_JSON.getContentType().toString());
			logger.println("Authorization: " + RestApiUtils.maskToken(token));
			logger.println("");
			logger.println(requestBody);
			logger.println();
			logger.println("###");
			logger.println();
			logger.println();
		}

		for (Map.Entry<String, String> e : build.getBuildVariables().entrySet()) {
			envVars.put(e.getKey(), e.getValue());
			
			if (RestApiUtils.isIspwDebugMode())
				logger.println("EnvVars: " + e.getKey() + "=" + e.getValue());
		}

		logger.println("Starting ISPW Operations Plugin");
		action.startLog(logger, ispwRequestBean.getIspwContextPathBean(), ispwRequestBean.getJsonObject());
		HttpRequestExecution exec =
				HttpRequestExecution.from(this, envVars, build, listener);
		
		ResponseContentSupplier supplier = launcher.getChannel().call(exec);
		
		String responseJson = supplier.getContent();
		if (RestApiUtils.isIspwDebugMode())
			logger.println("responseJson=" + responseJson);

		Object respObject = action.endLog(logger, ispwRequestBean, responseJson);
		logger.println("ISPW Operation Complete");
		
		if(skipWaitingForSet) {
			logger.println("Skip waiting for the completion of the set for this job...");
		}
		
		// polling status if no webhook
		if (webhookToken == null && !skipWaitingForSet) {
			if (respObject != null && respObject instanceof TaskResponse) {
				TaskResponse taskResp = (TaskResponse) respObject;
				String setId = taskResp.getSetId();

				HashSet<String> set = new HashSet<String>();

				int i = 0;
				for (; i < 60; i++) {
					Thread.sleep(Constants.POLLING_INTERVAL);
					HttpRequestExecution poller =
							HttpRequestExecution.createPoller(setId, this, envVars,
									build, listener);
					ResponseContentSupplier pollerSupplier = launcher.getChannel().call(poller);
					String pollingJson = pollerSupplier.getContent();

					JsonProcessor jsonProcessor = new JsonProcessor();
					SetInfoResponse setInfoResp =
							jsonProcessor.parse(pollingJson, SetInfoResponse.class);
					String setState = StringUtils.trimToEmpty(setInfoResp.getState());
					if (!set.contains(setState)) {
						logger.println("...set " + setInfoResp.getSetid() + " status - " + setState);
						set.add(setState);

						if (setState.equals(Constants.SET_STATE_CLOSED) || setState.equals(Constants.SET_STATE_COMPLETE)
								|| setState.equals(Constants.SET_STATE_WAITING_APPROVAL)) {
							logger.println("Action " + ispwAction + " completed");
							break;
						}
						else if (Constants.SET_STATE_FAILED.equalsIgnoreCase(setState))
						{
							logger.println("Set ID " + setId + " Failed for action "
									+ ispwRequestBean.getIspwContextPathBean().getAction());
							return false;
						}
						else if (Constants.SET_STATE_TERMINATED.equalsIgnoreCase(setState)
								&& SetOperationAction.SET_ACTION_TERMINATE.equalsIgnoreCase(ispwRequestBean.getIspwContextPathBean().getAction()))
						{
							logger.println("Set " + setId + " successfully terminated");
							break;
						}
						else if (Constants.SET_STATE_HELD.equalsIgnoreCase(setState)
								&& SetOperationAction.SET_ACTION_HOLD.equalsIgnoreCase(ispwRequestBean.getIspwContextPathBean().getAction()))
						{
							logger.println("Set " + setId + " successfully held");
							break;
						}
						else if (Constants.SET_STATE_HELD.equalsIgnoreCase(setState)
								&& SetOperationAction.SET_ACTION_UNLOCK.equalsIgnoreCase(ispwRequestBean.getIspwContextPathBean().getAction()))
						{
							logger.println("Set " + setId + " successfully unlocked.  Set is currently held.");
							break;
						}
						else if ((Constants.SET_STATE_RELEASED.equalsIgnoreCase(setState)
								|| Constants.SET_STATE_WAITING_LOCK.equalsIgnoreCase(setState))
								&& SetOperationAction.SET_ACTION_RELEASE.equalsIgnoreCase(ispwRequestBean.getIspwContextPathBean().getAction()))
						{
							logger.println("Set " + setId + " successfully released");
							break;
						}
					}
				}

				if (i == 60) {
					logger.println("Warn - max timeout reached");
				}
			}
		}
		
		return true;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		public static final boolean ignoreSslErrors = false;
		public static final HttpMode httpMode = HttpMode.POST;
		public static final String httpProxy = "";
		public static final Boolean passBuildParameters = false;
		public static final String validResponseCodes = "100:399";
		public static final String validResponseContent = "";
		public static final MimeType acceptType = MimeType.NOT_SET;
		public static final MimeType contentType = MimeType.NOT_SET;
		public static final String outputFile = "";
		public static final int timeout = 0;
		public static final Boolean quiet = false;
		public static final String authentication = "";
		public static final String requestBody = "";
		public static final String token = "";

		// ISPW related
		public static final String connectionId = StringUtils.EMPTY;
		public static final String credentialsId = StringUtils.EMPTY;
		public static final String ispwAction = StringUtils.EMPTY;
		public static final String ispwRequestBody = "#The following messages are commented out to show how to use the 'Request' field.\n"
				+"#Click on the help button to the right of the screen for examples of how to populate this field based on 'Action' type\n"
				+"#\n"
				+"#For example, if you select GenerateTasksInAssignment for 'Action' field,\n"
				+"# you may populate the following properties in 'Request' field.\n"
				+"# The property value should be based on your own container ID and level.\n"
				+"#\n"
				+"#assignmentId=PLAY000313\n"
				+"#level=STG2\n";
		public static final Boolean consoleLogResponseBody = false;
		public static final Boolean skipWaitingForSet = false;

		public static final List<HttpRequestNameValuePair> customHeaders = Collections
				.<HttpRequestNameValuePair> emptyList();

		public DescriptorImpl() {
			load();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Execute a Compuware ISPW Operation";
		}

		public ListBoxModel doFillHttpModeItems() {
			return HttpMode.getFillItems();
		}

		// ISPW
		public ListBoxModel doFillIspwActionItems(@AncestorInPath Jenkins context, @QueryParameter String ispwAction,
				@AncestorInPath Item project) {
			return RestApiUtils.buildIspwActionItems(context, ispwAction, project);
		}

		public ListBoxModel doFillConnectionIdItems(@AncestorInPath Jenkins context, @QueryParameter String connectionId,
				@AncestorInPath Item project)
		{
			return RestApiUtils.buildConnectionIdItems(context,  connectionId, project);
		}
		
		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context, @QueryParameter String credentialsId,
				@AncestorInPath Item project) {
			return RestApiUtils.buildCredentialsIdItems(context, credentialsId, project);
		}
		
		
		public ListBoxModel doFillAcceptTypeItems() {
			return MimeType.getContentTypeFillItems();
		}

		public ListBoxModel doFillContentTypeItems() {
			return MimeType.getContentTypeFillItems();
		}

		public ListBoxModel doFillAuthenticationItems(@AncestorInPath Item project,
				@QueryParameter String url) {
			return fillAuthenticationItems(project, url);
		}

		public static ListBoxModel fillAuthenticationItems(Item project, String url) {
			if (project == null || !project.hasPermission(Item.CONFIGURE)) {
				return new StandardListBoxModel();
			}

			List<Option> options = new ArrayList<>();
			for (BasicDigestAuthentication basic : HttpRequestGlobalConfig.get()
					.getBasicDigestAuthentications()) {
				options.add(new Option("(deprecated - use Jenkins Credentials) "
						+ basic.getKeyName(), basic.getKeyName()));
			}

			for (FormAuthentication formAuthentication : HttpRequestGlobalConfig.get()
					.getFormAuthentications()) {
				options.add(new Option(formAuthentication.getKeyName()));
			}

			AbstractIdCredentialsListBoxModel<StandardListBoxModel, StandardCredentials> items =
					new StandardListBoxModel().includeEmptyValue().includeAs(ACL.SYSTEM, project,
							StandardUsernamePasswordCredentials.class,
							URIRequirementBuilder.fromUri(url).build());
			items.addMissing(options);
			return items;
		}

		public static List<Range<Integer>> parseToRange(String value) {
			List<Range<Integer>> validRanges = new ArrayList<Range<Integer>>();

			String[] codes = value.split(",");
			for (String code : codes) {
				String[] fromTo = code.trim().split(":");
				checkArgument(fromTo.length <= 2,
						"Code %s should be an interval from:to or a single value", code);

				Integer from;
				try {
					from = Integer.parseInt(fromTo[0]);
				} catch (NumberFormatException nfe) {
					throw new IllegalArgumentException("Invalid number " + fromTo[0]);
				}

				Integer to = from;
				if (fromTo.length != 1) {
					try {
						to = Integer.parseInt(fromTo[1]);
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("Invalid number " + fromTo[1]);
					}
				}

				checkArgument(from <= to, "Interval %s should be FROM less than TO", code);
				validRanges.add(Range.closed(from, to));
			}

			return validRanges;
		}

		public FormValidation doCheckValidResponseCodes(@QueryParameter String value) {
			return checkValidResponseCodes(value);
		}

		public static FormValidation checkValidResponseCodes(String value) {
			if (value == null || value.trim().isEmpty()) {
				return FormValidation.ok();
			}

			try {
				parseToRange(value);
			} catch (IllegalArgumentException iae) {
				return FormValidation
						.error("Response codes expected is wrong. " + iae.getMessage());
			}
			return FormValidation.ok();

		}
	}

}
