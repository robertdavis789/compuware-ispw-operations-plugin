package org.jenkinsci.plugins.stashNotifier;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.common.CertificateCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;

/**
 * A Bitbucket notifier based on Stash notifier
 * 
 * @author Sam Zhou
 *
 */
public class BitbucketNotifier
{
	public static final int MAX_FIELD_LENGTH = 255;
	public static final int MAX_URL_FIELD_LENGTH = 450;

	private PrintStream logger;
	private Run<?, ?> run;
	private TaskListener listener;

	public BitbucketNotifier(PrintStream logger, Run<?, ?> run, TaskListener listener)
	{
		this.logger = logger;
		this.run = run;
		this.listener = listener;
	}

	/**
	 * Notifies the configured Bitbucket server by posting the run results to the Bitbucket run API.
	 *
	 * @param hash
	 *            the SHA1 of the run commit
	 * @param state
	 *            the state of the build as defined by the Bitbucket API.
	 */
	public NotificationResult notifyStash(String bitbucketBaseUrl, UsernamePasswordCredentials usernamePasswordCredentials,
			String hash, StashBuildState state, CertificateCredentials certificateCredentials) throws Exception
	{
		String stashURL = expandStashURL(bitbucketBaseUrl);

		logger.println("Notifying Bitbucket at \"" + stashURL + "\"");
		logger.println("hash = " + hash);

		HttpEntity stashBuildNotificationEntity = newStashBuildNotificationEntity(state);
		
		HttpPost req = createRequest(stashBuildNotificationEntity, run.getParent(), hash, stashURL,
				usernamePasswordCredentials);
		try (CloseableHttpClient client = getHttpClient(stashURL, certificateCredentials))
		{
			HttpResponse res = client.execute(req);
			if (res.getStatusLine().getStatusCode() != 204)
			{
				return NotificationResult.newFailure(EntityUtils.toString(res.getEntity()));
			}
			else
			{
				return NotificationResult.newSuccess();
			}
		}
	}

	private String expandStashURL(String stashServerBaseUrl)
	{
		String url = stashServerBaseUrl;

		try
		{
			if (!(run instanceof AbstractBuild<?, ?>))
			{
				url = TokenMacro.expandAll(run, new FilePath(run.getRootDir()), listener, url);
			}
			else
			{
				url = TokenMacro.expandAll((AbstractBuild<?, ?>) run, listener, url);
			}

		}
		catch (IOException | InterruptedException | MacroEvaluationException ex)
		{
			PrintStream logger = listener.getLogger();
			logger.println("Unable to expand Bitbucker server URL");
			ex.printStackTrace(logger);
		}
		return url;
	}

	/**
	 * Helper in place to allow us to define out HttpClient SSL context
	 */
	private SSLContext buildSslContext(boolean ignoreUnverifiedSSL, Credentials credentials)
			throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		SSLContextBuilder contextBuilder = SSLContexts.custom();
		contextBuilder.setProtocol("TLS");
		if (credentials instanceof CertificateCredentials)
		{
			contextBuilder.loadKeyMaterial(((CertificateCredentials) credentials).getKeyStore(),
					((CertificateCredentials) credentials).getPassword().getPlainText().toCharArray());
		}
		if (ignoreUnverifiedSSL)
		{
			contextBuilder.loadTrustMaterial(null, TrustAllStrategy.INSTANCE);
		}
		return contextBuilder.build();
	}

	private void configureProxy(HttpClientBuilder builder, URL url)
	{
		Jenkins jenkins = Jenkins.getInstance();
		ProxyConfiguration proxyConfig = jenkins.proxy;
		if (proxyConfig == null)
		{
			return;
		}

		Proxy proxy = proxyConfig.createProxy(url.getHost());
		if (proxy == null || proxy.type() != Proxy.Type.HTTP)
		{
			return;
		}

		SocketAddress addr = proxy.address();
		if (addr == null || !(addr instanceof InetSocketAddress))
		{
			return;
		}

		InetSocketAddress proxyAddr = (InetSocketAddress) addr;
		HttpHost proxyHost = new HttpHost(proxyAddr.getAddress().getHostAddress(), proxyAddr.getPort());
		builder.setProxy(proxyHost);

		String proxyUser = proxyConfig.getUserName();
		if (proxyUser != null)
		{
			String proxyPass = proxyConfig.getPassword();
			BasicCredentialsProvider cred = new BasicCredentialsProvider();
			cred.setCredentials(new AuthScope(proxyHost),
					new org.apache.http.auth.UsernamePasswordCredentials(proxyUser, proxyPass));
			builder.setDefaultCredentialsProvider(cred).setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
		}
	}

	/**
	 * Returns the HttpClient through which the REST call is made. Uses an unsafe TrustStrategy in case the user specified a
	 * HTTPS URL and set the ignoreUnverifiedSSLPeer flag.
	 */
	public CloseableHttpClient getHttpClient(String stashServer, CertificateCredentials certificateCredentials) throws Exception
	{

		RequestConfig.Builder requestBuilder = RequestConfig.custom();
		requestBuilder.setSocketTimeout(60_000);

		HttpClientBuilder clientBuilder = HttpClients.custom();
		clientBuilder.setDefaultRequestConfig(requestBuilder.build());

		URL url = new URL(stashServer);
		boolean ignoreUnverifiedSSL = true;

		if (url.getProtocol().equals("https") && ignoreUnverifiedSSL)
		{
			// add unsafe trust manager to avoid thrown SSLPeerUnverifiedException
			try
			{
				SSLContext sslContext = buildSslContext(ignoreUnverifiedSSL, certificateCredentials);
				SSLConnectionSocketFactory sslConnSocketFactory = new SSLConnectionSocketFactory(sslContext,
						new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
				clientBuilder.setSSLSocketFactory(sslConnSocketFactory);

				Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
						.register("https", sslConnSocketFactory).register("http", PlainConnectionSocketFactory.INSTANCE)
						.build();

				HttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(registry);
				clientBuilder.setConnectionManager(connectionManager);
			}
			catch (NoSuchAlgorithmException nsae)
			{
				logger.println("Couldn't establish SSL context:");
				nsae.printStackTrace(logger);
			}
			catch (KeyManagementException | KeyStoreException e)
			{
				logger.println("Couldn't initialize SSL context:");
				e.printStackTrace(logger);
			}
		}

		// Configure the proxy, if needed
		// Using the Jenkins methods handles the noProxyHost settings
		configureProxy(clientBuilder, url);

		return clientBuilder.build();
	}

	/**
	 * Returns the HTTP POST request ready to be sent to the Bitbucket build API for the given run and change set.
	 *
	 * @param stashBuildNotificationEntity
	 *            a entity containing the parameters for Bitbucket
	 * @param hash
	 *            the SHA1 of the commit that was built
	 * @param url
	 * @return the HTTP POST request to the Bitbucket build API
	 */
	public HttpPost createRequest(HttpEntity stashBuildNotificationEntity, Item project, String hash, String url,
			UsernamePasswordCredentials usernamePasswordCredentials) throws AuthenticationException
	{

		HttpPost req = new HttpPost(url + "/rest/build-status/1.0/commits/" + hash);

		if (usernamePasswordCredentials != null)
		{
			req.addHeader(new BasicScheme().authenticate(
					new org.apache.http.auth.UsernamePasswordCredentials(usernamePasswordCredentials.getUsername(),
							usernamePasswordCredentials.getPassword().getPlainText()),
					req, null));
		}

		req.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
		req.setEntity(stashBuildNotificationEntity);

		return req;
	}

	/**
	 * Returns the HTTP POST entity body with the JSON representation of the run result to be sent to the Bitbucket build API.
	 *
	 * @param run
	 *            the run to notify Bitbucket of
	 * @return HTTP entity body for POST to Bitbucket build API
	 */
	public HttpEntity newStashBuildNotificationEntity(StashBuildState state) throws UnsupportedEncodingException
	{

		JSONObject json = new JSONObject();

		json.put("state", state.name());

		json.put("key", abbreviate(getBuildKey(), MAX_FIELD_LENGTH));

		json.put("name", abbreviate(run.getFullDisplayName(), MAX_FIELD_LENGTH));

		json.put("description", abbreviate(getBuildDescription(state), MAX_FIELD_LENGTH));
		json.put("url", abbreviate(DisplayURLProvider.get().getRunURL(run), MAX_URL_FIELD_LENGTH));

		logger.println("json = "+json.toString());
		
		return new StringEntity(json.toString(), "UTF-8");
	}

	private String abbreviate(String text, int maxWidth)
	{
		if (text == null)
		{
			return null;
		}
		if (maxWidth < 4)
		{
			throw new IllegalArgumentException("Minimum abbreviation width is 4");
		}
		if (text.length() <= maxWidth)
		{
			return text;
		}
		return text.substring(0, maxWidth - 3) + "...";
	}

	/**
	 * Returns the description of the run used for the Bitbucket notification. Uses the run description provided by the Jenkins
	 * job, if available.
	 *
	 * @param run
	 *            the run to be described
	 * @param state
	 *            the state of the run
	 * @return the description of the run
	 */
	private String getBuildDescription(StashBuildState state)
	{

		if (run.getDescription() != null && run.getDescription().trim().length() > 0)
		{

			return run.getDescription();
		}
		else
		{
			switch (state)
			{
				case INPROGRESS :
					return "building on Jenkins @ " + getRootUrl();
				default :
					return "built by Jenkins @ " + getRootUrl();
			}
		}
	}

	/**
	 * Returns the run key used in the Bitbucket notification. Includes the run number depending on the user setting.
	 *
	 * @param run
	 *            the run to notify Bitbucket of
	 * @return the run key for the Bitbucket notification
	 */
	private String getBuildKey()
	{

		StringBuilder key = new StringBuilder();
		key.append(getDefaultBuildKey());

		return StringEscapeUtils.escapeJavaScript(key.toString());
	}

	/**
	 * Return the old-fashion build key
	 *
	 * @param run
	 *            the run to notify Bitbucket of
	 * @return default build key
	 */
	private String getDefaultBuildKey()
	{
		StringBuilder key = new StringBuilder();

		key.append(run.getParent().getName());
		key.append('-').append(run.getNumber());
		key.append('-').append(getRootUrl());

		return key.toString();
	}

	/**
	 * Provide a fallback for getting the instance's root URL
	 *
	 * @return Root URL contained in the global config
	 */
	private String getRootUrl()
	{
		Jenkins instance = Jenkins.getInstance();

		return (instance.getRootUrl() != null) ? instance.getRootUrl() : JenkinsLocationConfiguration.get().getUrl();
	}

}
