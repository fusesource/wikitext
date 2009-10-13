/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.commons.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.Proxy.Type;
import java.text.ParseException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.text.html.HTML.Tag;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.util.IdleConnectionTimeoutThread;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.mylyn.commons.net.HtmlStreamTokenizer.Token;
import org.eclipse.mylyn.internal.commons.net.AuthenticatedProxy;
import org.eclipse.mylyn.internal.commons.net.CloneableHostConfiguration;
import org.eclipse.mylyn.internal.commons.net.CommonsNetPlugin;
import org.eclipse.mylyn.internal.commons.net.MonitoredRequest;
import org.eclipse.mylyn.internal.commons.net.PollingInputStream;
import org.eclipse.mylyn.internal.commons.net.PollingProtocolSocketFactory;
import org.eclipse.mylyn.internal.commons.net.PollingSslProtocolSocketFactory;
import org.eclipse.mylyn.internal.commons.net.TimeoutInputStream;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 * @author Rob Elves
 * @since 3.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class WebUtil {

	// FIXME remove this again
	private static final boolean TEST_MODE;

	static {
		String application = System.getProperty("eclipse.application", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (application.length() > 0) {
			TEST_MODE = application.endsWith("testapplication"); //$NON-NLS-1$
		} else {
			// eclipse 3.3 does not the eclipse.application property
			String commands = System.getProperty("eclipse.commands", ""); //$NON-NLS-1$ //$NON-NLS-2$
			TEST_MODE = commands.contains("testapplication\n"); //$NON-NLS-1$
		}
	}

	/**
	 * like Mylyn/2.1.0 (Rally Connector 1.0) Eclipse/3.3.0 (JBuilder 2007) HttpClient/3.0.1 Java/1.5.0_11 (Sun)
	 * Linux/2.6.20-16-lowlatency (i386; en)
	 */
	private static final String USER_AGENT;

	private static final int CONNNECT_TIMEOUT = 60 * 1000;

	private static final int SOCKET_TIMEOUT = 3 * 60 * 1000;

	private static final int HTTP_PORT = 80;

	private static final int HTTPS_PORT = 443;

	private static final int POLL_INTERVAL = 500;

	private static final int POLL_ATTEMPTS = SOCKET_TIMEOUT / POLL_INTERVAL;

	private static final String USER_AGENT_PREFIX;

	private static final String USER_AGENT_POSTFIX;

	private static final int BUFFER_SIZE = 4096;

	/**
	 * Do not block.
	 */
	private static final long CLOSE_TIMEOUT = -1;

	/**
	 * @see IdleConnectionTimeoutThread#setTimeoutInterval(long)
	 */
	private static final long CONNECTION_TIMEOUT_INTERVAL = 30 * 1000;

	static {
		initCommonsLoggingSettings();

		StringBuilder sb = new StringBuilder();
		sb.append("Mylyn"); //$NON-NLS-1$
		sb.append(getBundleVersion(CommonsNetPlugin.getDefault()));

		USER_AGENT_PREFIX = sb.toString();
		sb.setLength(0);

		if (System.getProperty("org.osgi.framework.vendor") != null) { //$NON-NLS-1$
			sb.append(" "); //$NON-NLS-1$
			sb.append(System.getProperty("org.osgi.framework.vendor")); //$NON-NLS-1$
			sb.append(stripQualifier(System.getProperty("osgi.framework.version"))); //$NON-NLS-1$

			if (System.getProperty("eclipse.product") != null) { //$NON-NLS-1$
				sb.append(" ("); //$NON-NLS-1$
				sb.append(System.getProperty("eclipse.product")); //$NON-NLS-1$
				sb.append(")"); //$NON-NLS-1$
			}
		}

		sb.append(" "); //$NON-NLS-1$
		sb.append(DefaultHttpParams.getDefaultParams().getParameter(HttpMethodParams.USER_AGENT).toString().split("-")[1]); //$NON-NLS-1$

		sb.append(" Java/"); //$NON-NLS-1$
		sb.append(System.getProperty("java.version")); //$NON-NLS-1$
		sb.append(" ("); //$NON-NLS-1$
		sb.append(System.getProperty("java.vendor").split(" ")[0]); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(") "); //$NON-NLS-1$

		sb.append(System.getProperty("os.name")); //$NON-NLS-1$
		sb.append("/"); //$NON-NLS-1$
		sb.append(System.getProperty("os.version")); //$NON-NLS-1$
		sb.append(" ("); //$NON-NLS-1$
		sb.append(System.getProperty("os.arch")); //$NON-NLS-1$
		if (System.getProperty("osgi.nl") != null) { //$NON-NLS-1$
			sb.append("; "); //$NON-NLS-1$
			sb.append(System.getProperty("osgi.nl")); //$NON-NLS-1$
		}
		sb.append(")"); //$NON-NLS-1$

		USER_AGENT_POSTFIX = sb.toString();

		USER_AGENT = USER_AGENT_PREFIX + USER_AGENT_POSTFIX;
	}

	private static IdleConnectionTimeoutThread idleConnectionTimeoutThread;

	private static MultiThreadedHttpConnectionManager connectionManager;

	private static ProtocolSocketFactory sslSocketFactory = new PollingSslProtocolSocketFactory();

	private static PollingProtocolSocketFactory socketFactory = new PollingProtocolSocketFactory();

	/**
	 * @since 3.0
	 */
	public static void configureHttpClient(HttpClient client, String userAgent) {
		client.getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
		client.getParams().setParameter(HttpMethodParams.USER_AGENT, getUserAgent(userAgent));
		client.getParams().setConnectionManagerTimeout(CONNECTION_TIMEOUT_INTERVAL);
		// TODO consider setting this as the default
		//client.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
		configureHttpClientConnectionManager(client);
	}

	private static void configureHttpClientConnectionManager(HttpClient client) {
		client.getHttpConnectionManager().getParams().setSoTimeout(WebUtil.SOCKET_TIMEOUT);
		client.getHttpConnectionManager().getParams().setConnectionTimeout(WebUtil.CONNNECT_TIMEOUT);
		// FIXME fix connection leaks
		if (TEST_MODE) {
			client.getHttpConnectionManager().getParams().setMaxConnectionsPerHost(
					HostConfiguration.ANY_HOST_CONFIGURATION, 2);
		} else {
			client.getHttpConnectionManager().getParams().setMaxConnectionsPerHost(
					HostConfiguration.ANY_HOST_CONFIGURATION, 100);
			client.getHttpConnectionManager().getParams().setMaxTotalConnections(1000);
		}
	}

	private static void configureHttpClientProxy(HttpClient client, HostConfiguration hostConfiguration,
			AbstractWebLocation location) {
		String host = WebUtil.getHost(location.getUrl());

		Proxy proxy;
		if (WebUtil.isRepositoryHttps(location.getUrl())) {
			proxy = location.getProxyForHost(host, IProxyData.HTTPS_PROXY_TYPE);
		} else {
			proxy = location.getProxyForHost(host, IProxyData.HTTP_PROXY_TYPE);
		}

		if (proxy != null && !Proxy.NO_PROXY.equals(proxy)) {
			InetSocketAddress address = (InetSocketAddress) proxy.address();
			hostConfiguration.setProxy(address.getHostName(), address.getPort());
			if (proxy instanceof AuthenticatedProxy) {
				AuthenticatedProxy authProxy = (AuthenticatedProxy) proxy;
				Credentials credentials = getCredentials(authProxy.getUserName(), authProxy.getPassword(),
						address.getAddress());
				AuthScope proxyAuthScope = new AuthScope(address.getHostName(), address.getPort(), AuthScope.ANY_REALM);
				client.getState().setProxyCredentials(proxyAuthScope, credentials);
			}
		} else {
			hostConfiguration.setProxyHost(null);
		}
	}

	/**
	 * @since 3.0
	 */
	public static void connect(final Socket socket, final InetSocketAddress address, final int timeout,
			IProgressMonitor monitor) throws IOException {
		Assert.isNotNull(socket);

		WebRequest<?> executor = new WebRequest<Object>() {
			@Override
			public void abort() {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
				}
			}

			public Object call() throws Exception {
				socket.connect(address, timeout);
				return null;
			}
		};
		executeInternal(monitor, executor);
	}

	/**
	 * @since 3.0
	 */
	public static HostConfiguration createHostConfiguration(HttpClient client, AbstractWebLocation location,
			IProgressMonitor monitor) {
		Assert.isNotNull(client);
		Assert.isNotNull(location);

		String url = location.getUrl();
		String host = WebUtil.getHost(url);
		int port = WebUtil.getPort(url);

		configureHttpClientConnectionManager(client);

		HostConfiguration hostConfiguration = new CloneableHostConfiguration();
		configureHttpClientProxy(client, hostConfiguration, location);

		AuthenticationCredentials credentials = location.getCredentials(AuthenticationType.HTTP);
		if (credentials != null) {
			AuthScope authScope = new AuthScope(host, port, AuthScope.ANY_REALM);
			client.getState().setCredentials(authScope, getHttpClientCredentials(credentials, host));
		}

		if (WebUtil.isRepositoryHttps(url)) {
			Protocol protocol = new Protocol("https", sslSocketFactory, HTTPS_PORT); //$NON-NLS-1$
			hostConfiguration.setHost(host, port, protocol);
		} else {
			Protocol protocol = new Protocol("http", socketFactory, HTTP_PORT); //$NON-NLS-1$
			hostConfiguration.setHost(host, port, protocol);
		}

		return hostConfiguration;
	}

	/**
	 * @since 3.0
	 */
	public static int execute(final HttpClient client, final HostConfiguration hostConfiguration,
			final HttpMethod method, IProgressMonitor monitor) throws IOException {
		return execute(client, hostConfiguration, method, null, monitor);
	}

	/**
	 * @since 3.1
	 */
	public static int execute(final HttpClient client, final HostConfiguration hostConfiguration,
			final HttpMethod method, final HttpState state, IProgressMonitor monitor) throws IOException {
		Assert.isNotNull(client);
		Assert.isNotNull(method);

		monitor = Policy.monitorFor(monitor);

		MonitoredRequest<Integer> executor = new MonitoredRequest<Integer>(monitor) {
			@Override
			public void abort() {
				super.abort();
				method.abort();
			}

			@Override
			public Integer execute() throws Exception {
				return client.executeMethod(hostConfiguration, method, state);
			}
		};

		return executeInternal(monitor, executor);
	}

	/**
	 * @since 3.0
	 */
	public static <T> T execute(IProgressMonitor monitor, WebRequest<T> request) throws Throwable {
		// check for legacy reasons
		SubMonitor subMonitor = (monitor instanceof SubMonitor) ? (SubMonitor) monitor : SubMonitor.convert(null);

		Future<T> future = CommonsNetPlugin.getExecutorService().submit(request);
		while (true) {
			if (monitor.isCanceled()) {
				request.abort();

				// wait for executor to finish
				future.cancel(false);
				try {
					if (!future.isCancelled()) {
						future.get();
					}
				} catch (CancellationException e) {
					// ignore
				} catch (InterruptedException e) {
					// ignore
				} catch (ExecutionException e) {
					// ignore
				}
				throw new OperationCanceledException();
			}

			try {
				return future.get(POLL_INTERVAL, TimeUnit.MILLISECONDS);
			} catch (ExecutionException e) {
				throw e.getCause();
			} catch (TimeoutException ignored) {
			}

			subMonitor.setWorkRemaining(20);
			subMonitor.worked(1);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T executeInternal(IProgressMonitor monitor, WebRequest<?> request) throws IOException {
		try {
			return (T) execute(monitor, request);
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Error e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static String getBundleVersion(Plugin plugin) {
		if (null == plugin) {
			return ""; //$NON-NLS-1$
		}
		Object bundleVersion = plugin.getBundle().getHeaders().get("Bundle-Version"); //$NON-NLS-1$
		if (null == bundleVersion) {
			return ""; //$NON-NLS-1$
		}
		return stripQualifier((String) bundleVersion);
	}

	/**
	 * @since 3.0
	 */
	public static int getConnectionTimeout() {
		return CONNNECT_TIMEOUT;
	}

	static Credentials getCredentials(final String username, final String password, final InetAddress address) {
		int i = username.indexOf("\\"); //$NON-NLS-1$
		if (i > 0 && i < username.length() - 1 && address != null) {
			return new NTCredentials(username.substring(i + 1), password, address.getHostName(), username.substring(0,
					i));
		} else {
			return new UsernamePasswordCredentials(username, password);
		}
	}

	/**
	 * @since 3.0
	 */
	public static String getHost(String repositoryUrl) {
		String result = repositoryUrl;
		int colonSlashSlash = repositoryUrl.indexOf("://"); //$NON-NLS-1$

		if (colonSlashSlash >= 0) {
			result = repositoryUrl.substring(colonSlashSlash + 3);
		}

		int colonPort = result.indexOf(':');
		int requestPath = result.indexOf('/');

		int substringEnd;

		// minimum positive, or string length
		if (colonPort > 0 && requestPath > 0) {
			substringEnd = Math.min(colonPort, requestPath);
		} else if (colonPort > 0) {
			substringEnd = colonPort;
		} else if (requestPath > 0) {
			substringEnd = requestPath;
		} else {
			substringEnd = result.length();
		}

		return result.substring(0, substringEnd);
	}

	/**
	 * @since 2.2
	 */
	public static Credentials getHttpClientCredentials(AuthenticationCredentials credentials, String host) {
		String username = credentials.getUserName();
		String password = credentials.getPassword();
		int i = username.indexOf("\\"); //$NON-NLS-1$
		if (i > 0 && i < username.length() - 1 && host != null) {
			return new NTCredentials(username.substring(i + 1), password, host, username.substring(0, i));
		} else {
			return new UsernamePasswordCredentials(username, password);
		}
	}

	/**
	 * @since 2.0
	 */
	public static int getPort(String repositoryUrl) {
		int colonSlashSlash = repositoryUrl.indexOf("://"); //$NON-NLS-1$
		int firstSlash = repositoryUrl.indexOf("/", colonSlashSlash + 3); //$NON-NLS-1$
		int colonPort = repositoryUrl.indexOf(':', colonSlashSlash + 1);
		if (firstSlash == -1) {
			firstSlash = repositoryUrl.length();
		}
		if (colonPort < 0 || colonPort > firstSlash) {
			return isRepositoryHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
		}

		int requestPath = repositoryUrl.indexOf('/', colonPort + 1);
		int end = requestPath < 0 ? repositoryUrl.length() : requestPath;
		String port = repositoryUrl.substring(colonPort + 1, end);
		if (port.length() == 0) {
			return isRepositoryHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
		}

		return Integer.parseInt(port);
	}

	/**
	 * @since 2.0
	 */
	public static String getRequestPath(String repositoryUrl) {
		int colonSlashSlash = repositoryUrl.indexOf("://"); //$NON-NLS-1$
		int requestPath = repositoryUrl.indexOf('/', colonSlashSlash + 3);

		if (requestPath < 0) {
			return ""; //$NON-NLS-1$
		} else {
			return repositoryUrl.substring(requestPath);
		}
	}

	public static InputStream getResponseBodyAsStream(HttpMethodBase method, IProgressMonitor monitor)
			throws IOException {
//		return method.getResponseBodyAsStream();
		monitor = Policy.monitorFor(monitor);
		return new PollingInputStream(new TimeoutInputStream(method.getResponseBodyAsStream(), BUFFER_SIZE,
				POLL_INTERVAL, CLOSE_TIMEOUT), POLL_ATTEMPTS, monitor);
	}

	/**
	 * @since 3.0
	 */
	public static int getSocketTimeout() {
		return SOCKET_TIMEOUT;
	}

	/**
	 * Returns the title of a web page.
	 * 
	 * @throws IOException
	 *             if a network occurs
	 * @return the title; null, if the title could not be determined;
	 * @since 3.0
	 */
	public static String getTitleFromUrl(AbstractWebLocation location, IProgressMonitor monitor) throws IOException {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask("Retrieving " + location.getUrl(), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

			HttpClient client = new HttpClient();
			WebUtil.configureHttpClient(client, ""); //$NON-NLS-1$

			GetMethod method = new GetMethod(location.getUrl());
			try {
				HostConfiguration hostConfiguration = WebUtil.createHostConfiguration(client, location, monitor);
				int result = WebUtil.execute(client, hostConfiguration, method, monitor);
				if (result == HttpStatus.SC_OK) {
					InputStream in = WebUtil.getResponseBodyAsStream(method, monitor);
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(in,
								method.getResponseCharSet()));
						HtmlStreamTokenizer tokenizer = new HtmlStreamTokenizer(reader, null);
						try {
							for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
								if (token.getType() == Token.TAG) {
									HtmlTag tag = (HtmlTag) token.getValue();
									if (tag.getTagType() == Tag.TITLE) {
										String text = getText(tokenizer);
										text = text.replaceAll("\n", ""); //$NON-NLS-1$ //$NON-NLS-2$
										text = text.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
										return text.trim();
									}
								}
							}
						} catch (ParseException e) {
							throw new IOException("Error reading url"); //$NON-NLS-1$
						}
					} finally {
						in.close();
					}
				}
			} finally {
				method.releaseConnection();
			}
		} finally {
			monitor.done();
		}
		return null;
	}

	private static String getText(HtmlStreamTokenizer tokenizer) throws IOException, ParseException {
		StringBuilder sb = new StringBuilder();
		for (Token token = tokenizer.nextToken(); token.getType() != Token.EOF; token = tokenizer.nextToken()) {
			if (token.getType() == Token.TEXT) {
				sb.append(token.toString());
			} else if (token.getType() == Token.COMMENT) {
				// ignore
			} else {
				break;
			}
		}
		return StringEscapeUtils.unescapeHtml(sb.toString());
	}

	/**
	 * Returns a user agent string that contains information about the platform and operating system. The
	 * <code>product</code> parameter allows to additional specify custom text that is inserted into the returned
	 * string. The exact return value depends on the environment.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>Headless: <code>Mylyn MyProduct HttpClient/3.1 Java/1.5.0_13 (Sun) Linux/2.6.22-14-generic (i386)</code>
	 * <li>Eclipse:
	 * <code>Mylyn/2.2.0 Eclipse/3.4.0 (org.eclipse.sdk.ide) HttpClient/3.1 Java/1.5.0_13 (Sun) Linux/2.6.22-14-generic (i386; en_CA)</code>
	 * 
	 * @param product
	 *            an identifier that is inserted into the returned user agent string
	 * @return a user agent string
	 * @since 2.3
	 */
	public static String getUserAgent(String product) {
		if (product != null && product.length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(USER_AGENT_PREFIX);
			sb.append(" "); //$NON-NLS-1$
			sb.append(product);
			sb.append(USER_AGENT_POSTFIX);
			return sb.toString();
		} else {
			return USER_AGENT;
		}
	}

	public static void init() {
		// initialization is done in the static initializer		
	}

	/**
	 * Disables logging by default. Set these system properties on launch enables verbose logging of HTTP communication:
	 * 
	 * <pre>
	 * -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog
	 * -Dorg.apache.commons.logging.simplelog.showlogname=true 
	 * -Dorg.apache.commons.logging.simplelog.defaultlog=off
	 * -Dorg.apache.commons.logging.simplelog.log.httpclient.wire=debug
	 * -Dorg.apache.commons.logging.simplelog.log.org.apache.commons.httpclient=off
	 * -Dorg.apache.commons.logging.simplelog.log.org.apache.axis.message=debug
	 * </pre>
	 */
	private static void initCommonsLoggingSettings() {
		defaultSystemProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Only sets system property if they are not already set to a value.
	 */
	private static void defaultSystemProperty(String key, String defaultValue) {
		if (System.getProperty(key) == null) {
			System.setProperty(key, defaultValue);
		}
	}

	private static boolean isRepositoryHttps(String repositoryUrl) {
		return repositoryUrl.matches("https.*"); //$NON-NLS-1$
	}

	private static String stripQualifier(String longVersion) {
		if (longVersion == null) {
			return ""; //$NON-NLS-1$
		}

		String parts[] = longVersion.split("\\."); //$NON-NLS-1$
		StringBuilder version = new StringBuilder();
		if (parts.length > 0) {
			version.append("/"); //$NON-NLS-1$
			version.append(parts[0]);
			if (parts.length > 1) {
				version.append("."); //$NON-NLS-1$
				version.append(parts[1]);
				if (parts.length > 2) {
					version.append("."); //$NON-NLS-1$
					version.append(parts[2]);
				}
			}
		}
		return version.toString();

	}

	/**
	 * For standalone applications that want to provide a global proxy service.
	 * 
	 * @param proxyService
	 *            the proxy service
	 * @since 3.0
	 */
	public static void setProxyService(IProxyService proxyService) {
		CommonsNetPlugin.setProxyService(proxyService);
	}

	/**
	 * @since 3.1
	 */
	public static IProxyService getProxyService() {
		return CommonsNetPlugin.getProxyService();
	}

	/**
	 * @since 3.1
	 */
	public synchronized static void addConnectionManager(HttpConnectionManager connectionManager) {
		if (idleConnectionTimeoutThread == null) {
			idleConnectionTimeoutThread = new IdleConnectionTimeoutThread();
			idleConnectionTimeoutThread.setTimeoutInterval(CONNECTION_TIMEOUT_INTERVAL);
			idleConnectionTimeoutThread.setConnectionTimeout(CONNNECT_TIMEOUT);
			idleConnectionTimeoutThread.start();
		}
		idleConnectionTimeoutThread.addConnectionManager(connectionManager);
	}

	/**
	 * @since 3.1
	 */
	public synchronized static HttpConnectionManager getConnectionManager() {
		if (connectionManager == null) {
			connectionManager = new MultiThreadedHttpConnectionManager();
			addConnectionManager(connectionManager);
		}
		return connectionManager;
	}

	/**
	 * @since 3.1
	 */
	public synchronized static void removeConnectionManager(HttpConnectionManager connectionManager) {
		if (idleConnectionTimeoutThread == null) {
			return;
		}
		idleConnectionTimeoutThread.removeConnectionManager(connectionManager);
	}

	/**
	 * @since 3.1
	 */
	@SuppressWarnings("deprecation")
	public static Proxy getProxy(String host, String proxyType) {
		Assert.isNotNull(host);
		Assert.isNotNull(proxyType);
		IProxyService service = CommonsNetPlugin.getProxyService();
		if (service != null && service.isProxiesEnabled()) {
			// TODO e3.5 move to new proxy API
			IProxyData data = service.getProxyDataForHost(host, proxyType);
			if (data != null && data.getHost() != null) {
				String proxyHost = data.getHost();
				int proxyPort = data.getPort();
				// change the IProxyData default port to the Java default port
				if (proxyPort == -1) {
					proxyPort = 0;
				}

				AuthenticationCredentials credentials = null;
				if (data.isRequiresAuthentication()) {
					credentials = new AuthenticationCredentials(data.getUserId(), data.getPassword());
				}
				return createProxy(proxyHost, proxyPort, credentials);
			}
		}
		return null;
	}

	/**
	 * @since 3.1
	 */
	public static Proxy getProxy(String host, Proxy.Type proxyType) {
		Assert.isNotNull(host);
		Assert.isNotNull(proxyType);
		return getProxy(host, getPlatformProxyType(proxyType));
	}

//	private static Type getJavaProxyType(String type) {
//		return (IProxyData.SOCKS_PROXY_TYPE.equals(type)) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
//	}

	private static String getPlatformProxyType(Type type) {
		return type == Type.SOCKS ? IProxyData.SOCKS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE;
	}

	/**
	 * @since 3.1
	 */
	public static Proxy createProxy(String proxyHost, int proxyPort, AuthenticationCredentials credentials) {
		String proxyUsername = ""; //$NON-NLS-1$
		String proxyPassword = ""; //$NON-NLS-1$
		if (credentials != null) {
			proxyUsername = credentials.getUserName();
			proxyPassword = credentials.getPassword();
		}
		if (proxyHost != null && proxyHost.length() > 0) {
			InetSocketAddress sockAddr = new InetSocketAddress(proxyHost, proxyPort);
			boolean authenticated = (proxyUsername != null && proxyPassword != null && proxyUsername.length() > 0 && proxyPassword.length() > 0);
			if (authenticated) {
				return new AuthenticatedProxy(Type.HTTP, sockAddr, proxyUsername, proxyPassword);
			} else {
				return new Proxy(Type.HTTP, sockAddr);
			}
		}
		return Proxy.NO_PROXY;
	}

}
