package com.compuware.ispw.restapi;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.annotation.Nonnull;
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
import com.compuware.ispw.model.rest.SetInfoResponse;
import com.compuware.ispw.model.rest.TaskResponse;
import com.compuware.ispw.restapi.action.IAction;
import com.compuware.ispw.restapi.util.HttpRequestNameValuePair;
import com.compuware.ispw.restapi.util.ReflectUtils;
import com.compuware.ispw.restapi.util.RestApiUtils;
import com.compuware.jenkins.common.configuration.HostConnection;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * ISPW rest API pipeline build step
 * 
 * @author Martin d'Anjou
 * @author Sam Zhou
 */
public final class IspwRestApiRequestStep extends AbstractStepImpl {

	private @Nonnull String url = StringUtils.EMPTY;
	private boolean ignoreSslErrors = DescriptorImpl.ignoreSslErrors;
	private HttpMode httpMode = DescriptorImpl.httpMode;
	private String httpProxy = DescriptorImpl.httpProxy;
	private String validResponseCodes = DescriptorImpl.validResponseCodes;
	private String validResponseContent = DescriptorImpl.validResponseContent;
	private MimeType acceptType = DescriptorImpl.acceptType;
	private MimeType contentType = DescriptorImpl.contentType;
	private Integer timeout = DescriptorImpl.timeout;
	private String authentication = DescriptorImpl.authentication;
	private String requestBody = DescriptorImpl.requestBody;
	private List<HttpRequestNameValuePair> customHeaders = DescriptorImpl.customHeaders;
	private String outputFile = DescriptorImpl.outputFile;
	private ResponseHandle responseHandle = DescriptorImpl.responseHandle;
	private String token = DescriptorImpl.token; // modified by pmisvz0

	// ISPW
	private String connectionId = DescriptorImpl.connectionId;
	private String credentialsId = DescriptorImpl.credentialsId;
	private String ispwAction = DescriptorImpl.ispwAction;
	private String ispwRequestBody = DescriptorImpl.ispwRequestBody;
	private Boolean consoleLogResponseBody = DescriptorImpl.consoleLogResponseBody;
	
    @DataBoundConstructor
    public IspwRestApiRequestStep() {
    }

    public String getUrl() {
        return url;
    }
    
	public boolean isIgnoreSslErrors() {
		return ignoreSslErrors;
	}

    public HttpMode getHttpMode() {
        return httpMode;
    }

    // ispw begin
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

    public Integer getTimeout() {
        return timeout;
    }

    @DataBoundSetter
    public void setConsoleLogResponseBody(Boolean consoleLogResponseBody) {
        this.consoleLogResponseBody = consoleLogResponseBody;
    }

    public Boolean getConsoleLogResponseBody() {
        return consoleLogResponseBody;
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

	public String getOutputFile() {
		return outputFile;
	}

	public ResponseHandle getResponseHandle() {
		return responseHandle;
	}

	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

	List<HttpRequestNameValuePair> resolveHeaders() {
		final List<HttpRequestNameValuePair> headers = new ArrayList<>();
		
		// ISPW
		headers.add(new HttpRequestNameValuePair("Content-type", MimeType.APPLICATION_JSON.toString()));
		headers.add(new HttpRequestNameValuePair("Authorization", getToken()));
		
		if (acceptType != null && acceptType != MimeType.NOT_SET) {
			headers.add(new HttpRequestNameValuePair("Accept", acceptType.getValue()));
		}
		for (HttpRequestNameValuePair header : customHeaders) {
			String headerName = header.getName();
			String headerValue = header.getValue();
			boolean maskValue = headerName.equalsIgnoreCase("Authorization") ||
					header.getMaskValue();

			headers.add(new HttpRequestNameValuePair(headerName, headerValue, maskValue));
		}
		return headers;
	}

	@Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
		public static final boolean ignoreSslErrors =
				IspwRestApiRequest.DescriptorImpl.ignoreSslErrors;
		public static final HttpMode httpMode = IspwRestApiRequest.DescriptorImpl.httpMode;
		public static final String httpProxy = IspwRestApiRequest.DescriptorImpl.httpProxy;
		public static final String validResponseCodes =
				IspwRestApiRequest.DescriptorImpl.validResponseCodes;
		public static final String validResponseContent =
				IspwRestApiRequest.DescriptorImpl.validResponseContent;
		public static final MimeType acceptType = IspwRestApiRequest.DescriptorImpl.acceptType;
		public static final MimeType contentType = IspwRestApiRequest.DescriptorImpl.contentType;
		public static final int timeout = IspwRestApiRequest.DescriptorImpl.timeout;
		public static final Boolean quiet = IspwRestApiRequest.DescriptorImpl.quiet;
		public static final String authentication =
				IspwRestApiRequest.DescriptorImpl.authentication;
		public static final String requestBody = IspwRestApiRequest.DescriptorImpl.requestBody;
		public static final String token = IspwRestApiRequest.DescriptorImpl.token;
		public static final List<HttpRequestNameValuePair> customHeaders = Collections
				.<HttpRequestNameValuePair> emptyList();
		public static final String outputFile = "";
		public static final ResponseHandle responseHandle = ResponseHandle.STRING;

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
		public static final Boolean consoleLogResponseBody =
				IspwRestApiRequest.DescriptorImpl.consoleLogResponseBody;
		
        public DescriptorImpl() {
            super(Execution.class);
        }

		@Override
		public String getFunctionName() {
			return "ispwOperation";
		}

		@Override
		public String getDisplayName() {
			return "Perform a Compuware ISPW Rest API Request and return a JSON object";
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
		
        public ListBoxModel doFillHttpModeItems() {
            return HttpMode.getFillItems();
        }
 
        public ListBoxModel doFillAcceptTypeItems() {
            return MimeType.getContentTypeFillItems();
        }

        public ListBoxModel doFillContentTypeItems() {
            return MimeType.getContentTypeFillItems();
        }

		public ListBoxModel doFillResponseHandleItems() {
			ListBoxModel items = new ListBoxModel();
			for (ResponseHandle responseHandle : ResponseHandle.values()) {
				items.add(responseHandle.name());
			}
			return items;
		}

        public ListBoxModel doFillAuthenticationItems(@AncestorInPath Item project,
													  @QueryParameter String url) {
            return IspwRestApiRequest.DescriptorImpl.fillAuthenticationItems(project, url);
        }

        public FormValidation doCheckValidResponseCodes(@QueryParameter String value) {
            return IspwRestApiRequest.DescriptorImpl.checkValidResponseCodes(value);
        }

	}

    public static final class Execution extends AbstractSynchronousNonBlockingStepExecution<ResponseContentSupplier> {

        @Inject
        private transient IspwRestApiRequestStep step;

		@StepContextParameter
		private transient Run<?, ?> run;
		@StepContextParameter
		private transient TaskListener listener;

		public ResponseContentSupplier runExec(HttpRequestExecution exec) throws InterruptedException, IOException {
			Launcher launcher = getContext().get(Launcher.class);
			ResponseContentSupplier supplier = null;
			if (launcher != null) {
				supplier = launcher.getChannel().call(exec);
			} else {
				supplier = exec.call();
			}

			return supplier;
		}
		
		@Override
		protected ResponseContentSupplier run() throws Exception {
			PrintStream logger = listener.getLogger();

			EnvVars envVars = getContext().get(hudson.EnvVars.class);

			String buildTag = envVars.get("BUILD_TAG");
			WebhookToken webhookToken = WebhookTokenManager.getInstance().get(buildTag);
			
			if(RestApiUtils.isIspwDebugMode())
				logger.println("...getting buildTag=" + buildTag + ", webhookToken=" + webhookToken);

			IAction action = ReflectUtils.createAction(step.ispwAction, logger);
			step.httpMode = action.getHttpMode();
			
			if (!ReflectUtils.isActionInstantiated(action))
			{
				String errorMsg =
						"Action:"
								+ step.ispwAction
								+ " is not implemented. Please make sure you have the correct ISPW action name.";
				logger.println(errorMsg);
				throw new IllegalStateException(new Exception(errorMsg));
			}
			
			if(RestApiUtils.isIspwDebugMode())
				logger.println("ispwAction=" + step.ispwAction + ", httpMode=" + step.httpMode);

			String cesUrl = StringUtils.EMPTY;
			String cesIspwHost = StringUtils.EMPTY;

			HostConnection hostConnection = RestApiUtils.getCesUrl(step.connectionId);
			if (hostConnection != null) {
				cesUrl = StringUtils.trimToEmpty(hostConnection.getCesUrl());

				if(!cesUrl.startsWith("http")) {
					String errorMsg = "Host connection does NOT contain a valid CES URL. Please re-configure in 'Manage Jenkins | Configure System | Compuware Configurations' section";
					logger.println(errorMsg);
					throw new IllegalStateException(new Exception(errorMsg));
				}
				
				String host = StringUtils.trimToEmpty(hostConnection.getHost());
				String port = StringUtils.trimToEmpty(hostConnection.getPort());
				cesIspwHost = host + "-" + port;
			}

			String cesIspwToken = RestApiUtils.getCesToken(step.credentialsId);

			if (RestApiUtils.isIspwDebugMode())
				logger.println("...ces.url=" + cesUrl + ", ces.ispw.host=" + cesIspwHost
						+ ", ces.ispw.token=" + cesIspwToken);

			IspwRequestBean ispwRequestBean =
					action.getIspwRequestBean(cesIspwHost, step.ispwRequestBody, webhookToken);

			if (RestApiUtils.isIspwDebugMode())
				logger.println("ispwRequestBean=" + ispwRequestBean);

			step.url = cesUrl + ispwRequestBean.getContextPath(); // CES URL
			step.requestBody = ispwRequestBean.getJsonRequest();
			step.token = cesIspwToken; // CES TOKEN

			// This is a generated code for Visual Studio Code - REST Client
			if (step.consoleLogResponseBody) {
				logger.println();
				logger.println();
				logger.println("### [" + step.ispwAction + "] - " + "RFC 2616");
				logger.println();
				logger.println(step.httpMode + " " + step.url + " HTTP/1.1");
				logger.println("Content-type: "
						+ MimeType.APPLICATION_JSON.getContentType().toString());
				logger.println("Authorization: " + RestApiUtils.maskToken(step.token));
				logger.println("");
				logger.println(step.requestBody);
				logger.println();
				logger.println("###");
				logger.println();
				logger.println();
			}

			logger.println("Starting ISPW Operations Plugin");
			action.startLog(logger, ispwRequestBean.getIspwContextPathBean(), ispwRequestBean.getJsonObject());

			HttpRequestExecution exec =
					HttpRequestExecution.from(step, listener, this);

			ResponseContentSupplier supplier = runExec(exec);
			
			String responseJson = supplier.getContent();
			if (RestApiUtils.isIspwDebugMode())
				logger.println("responseJson=" + responseJson);

			Object respObject = action.endLog(logger, ispwRequestBean, responseJson);
			logger.println("ISPW Operation Complete");
			
			// polling status if no webhook
			if (webhookToken == null) {
				if (respObject != null && respObject instanceof TaskResponse) {
					TaskResponse taskResp = (TaskResponse) respObject;
					String setId = taskResp.getSetId();

					HashSet<String> set = new HashSet<String>();

					int i = 0;
					for (; i < 60; i++) {
						Thread.sleep(Constants.POLLING_INTERVAL);
						HttpRequestExecution poller = HttpRequestExecution
								.createPoller(setId, step, listener, this);
						ResponseContentSupplier pollerSupplier = runExec(poller);
						String pollingJson = pollerSupplier.getContent();

						JsonProcessor jsonProcessor = new JsonProcessor();
						SetInfoResponse setInfoResp =
								jsonProcessor.parse(pollingJson, SetInfoResponse.class);
						String setState = StringUtils.trimToEmpty(setInfoResp.getState());
						if (!set.contains(setState)) {
							logger.println("Set " + setInfoResp.getSetid() + " status - "
									+ setState);
							set.add(setState);

							if (setState.equals(Constants.SET_STATE_CLOSED) || setState.equals(Constants.SET_STATE_COMPLETE)) {
								logger.println("Action " + step.ispwAction + " completed");
								break;
							}
						}
					}

					if (i == Constants.POLLING_COUNT) {
						logger.println("Warn - max timeout reached");
					}
				}
			}

			
			return supplier;
		}

        private static final long serialVersionUID = 1L;

		FilePath resolveOutputFile() {
			String outputFile = step.getOutputFile();
			if (outputFile == null || outputFile.trim().isEmpty()) {
				return null;
			}

			try {
				FilePath workspace = getContext().get(FilePath.class);
				if (workspace == null) {
					throw new IllegalStateException("Could not find workspace to save file outputFile: " + outputFile +
							". You should use it inside a 'node' block");
				}
				return workspace.child(outputFile);
			} catch (IOException | InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}

		public Item getProject() {
			return run.getParent();
		}

    }
}
