/*******************************************************************************
 * Copyright (c) 2004, 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Eugene Kuleshov - improvements
 *******************************************************************************/

package org.eclipse.mylyn.tasks.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.tasks.core.IRepositoryConstants;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.RepositoryPerson;

/**
 * Note that task repositories use Strings for storing time stamps because using Date objects led to the following
 * problems:
 * <ul>
 * <li>Often we are unable to get the time zone of the repository so interpreting the date string correctly doesn't
 * work.</li>
 * <li>Even if we do know the time zone information the local clock may be wrong. This can cause lost incoming when
 * asking the repository for all changes since date X.</li>
 * <li>The solution we have come up with thus far is not to interpret the date as a DATE object but rather simply use
 * the date string given to us by the repository itself.</li>
 * </ul>
 * 
 * @author Mik Kersten
 * @author Rob Elves
 * @author Eugene Kuleshov
 * @author Steffen Pingel
 * @since 2.0
 */
@SuppressWarnings("deprecation")
public final class TaskRepository extends PlatformObject {

	public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8"; //$NON-NLS-1$

	private static final String USERNAME = ".username"; //$NON-NLS-1$

	private static final String PASSWORD = ".password"; //$NON-NLS-1$

	private static final String SAVE_PASSWORD = ".savePassword"; //$NON-NLS-1$

	private static final String ENABLED = ".enabled"; //$NON-NLS-1$

	private static final String AUTH_REPOSITORY = "org.eclipse.mylyn.tasklist.repositories"; //$NON-NLS-1$

	// transient
	private IStatus errorStatus = null;

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} to access
	 *             credentials
	 */
	@Deprecated
	public static final String AUTH_PASSWORD = AUTH_REPOSITORY + PASSWORD;

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} to access
	 *             credentials
	 */
	@Deprecated
	public static final String AUTH_USERNAME = AUTH_REPOSITORY + USERNAME;

	@Deprecated
	public static final String ANONYMOUS_LOGIN = "org.eclipse.mylyn.tasklist.repositories.anonymous"; //$NON-NLS-1$

	private static final String AUTH_HTTP = "org.eclipse.mylyn.tasklist.repositories.httpauth"; //$NON-NLS-1$

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} to access
	 *             credentials
	 */
	@Deprecated
	public static final String AUTH_HTTP_PASSWORD = AUTH_HTTP + PASSWORD;

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} to access
	 *             credentials
	 */
	@Deprecated
	public static final String AUTH_HTTP_USERNAME = AUTH_HTTP + USERNAME;

	public static final String NO_VERSION_SPECIFIED = "unknown"; //$NON-NLS-1$

	private static final String AUTH_SCHEME = "Basic"; //$NON-NLS-1$

	private static final String AUTH_REALM = ""; //$NON-NLS-1$

	private static final URL DEFAULT_URL;

	private static final String PROPERTY_CONFIG_TIMESTAMP = "org.eclipse.mylyn.tasklist.repositories.configuration.timestamp"; //$NON-NLS-1$

	public static final String PROXY_USEDEFAULT = "org.eclipse.mylyn.tasklist.repositories.proxy.usedefault"; //$NON-NLS-1$

	public static final String PROXY_HOSTNAME = "org.eclipse.mylyn.tasklist.repositories.proxy.hostname"; //$NON-NLS-1$

	public static final String PROXY_PORT = "org.eclipse.mylyn.tasklist.repositories.proxy.port"; //$NON-NLS-1$

	private static final String AUTH_PROXY = "org.eclipse.mylyn.tasklist.repositories.proxy"; //$NON-NLS-1$

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} to access
	 *             credentials
	 */
	@Deprecated
	public static final String PROXY_USERNAME = AUTH_PROXY + USERNAME;

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} to access
	 *             credentials
	 */
	@Deprecated
	public static final String PROXY_PASSWORD = AUTH_PROXY + PASSWORD;

	public static final String OFFLINE = "org.eclipse.mylyn.tasklist.repositories.offline"; //$NON-NLS-1$

	// HACK: Lock used to work around race condition in
	// Platform.add/get/flushAuthorizationInfo()
	private static final Object LOCK = new Object();

	private final Set<PropertyChangeListener> propertyChangeListeners = new HashSet<PropertyChangeListener>();

	// HACK: private credentials for headless operation
	private static Map<String, Map<String, String>> credentials = new HashMap<String, Map<String, String>>();

	static {
		URL url = null;
		try {
			url = new URL("http://eclipse.org/mylyn"); //$NON-NLS-1$
		} catch (Exception ex) {
			// TODO ?
		}
		DEFAULT_URL = url;
	}

	private static String getKeyPrefix(AuthenticationType type) {
		switch (type) {
		case HTTP:
			return AUTH_HTTP;
		case PROXY:
			return AUTH_PROXY;
		case REPOSITORY:
			return AUTH_REPOSITORY;
		}
		throw new IllegalArgumentException("Unknown authentication type: " + type); //$NON-NLS-1$
	}

	private boolean isCachedUserName;

	private String cachedUserName;

	private final Map<String, String> properties = new LinkedHashMap<String, String>();

	/**
	 * Stores properties that are not persisted. Note that this map is currently cleared when flushCredentials() is
	 * invoked.
	 */
	private final Map<String, String> transientProperties = new HashMap<String, String>();

	/*
	 * TODO: should be externalized and added to extension point, see bug 183606
	 */
	private boolean isBugRepository = false;

	private transient volatile boolean updating;

	public TaskRepository(String connectorKind, String repositoryUrl) {
		this(connectorKind, repositoryUrl, NO_VERSION_SPECIFIED);
	}

	/**
	 * @deprecated use {@link #setProperty(String, String)} instead of passing a map
	 */
	@Deprecated
	public TaskRepository(String kind, String serverUrl, Map<String, String> properties) {
		setProperty(IRepositoryConstants.PROPERTY_CONNECTOR_KIND, kind);
		setProperty(IRepositoryConstants.PROPERTY_URL, serverUrl);
		this.properties.putAll(properties);
		// use platform proxy by default (headless will need to set this to false)
		this.setProperty(TaskRepository.PROXY_USEDEFAULT, new Boolean(true).toString());
	}

	/**
	 * for testing purposes sets repository time zone to local default time zone sets character encoding to
	 * DEFAULT_CHARACTER_ENCODING
	 */
	@Deprecated
	public TaskRepository(String kind, String serverUrl, String version) {
		this(kind, serverUrl, version, DEFAULT_CHARACTER_ENCODING, TimeZone.getDefault().getID());
	}

	@Deprecated
	public TaskRepository(String connectorKind, String repositoryUrl, String version, String encoding, String timeZoneId) {
		Assert.isNotNull(connectorKind);
		Assert.isNotNull(repositoryUrl);
		setProperty(IRepositoryConstants.PROPERTY_CONNECTOR_KIND, connectorKind);
		setProperty(IRepositoryConstants.PROPERTY_URL, repositoryUrl);
		setProperty(IRepositoryConstants.PROPERTY_VERSION, version);
		setProperty(IRepositoryConstants.PROPERTY_ENCODING, encoding);
		setProperty(IRepositoryConstants.PROPERTY_TIMEZONE, timeZoneId);
		// use platform proxy by default (headless will need to set this to false)
		this.setProperty(TaskRepository.PROXY_USEDEFAULT, new Boolean(true).toString());

		// for backwards compatibility to versions prior to 2.2
		this.setProperty(AUTH_REPOSITORY + SAVE_PASSWORD, String.valueOf(true));
		this.setProperty(AUTH_HTTP + SAVE_PASSWORD, String.valueOf(true));
		this.setProperty(AUTH_PROXY + SAVE_PASSWORD, String.valueOf(true));
	}

	private ISecurePreferences getSecurePreferences() {
		ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault()
				.node(ITasksCoreConstants.ID_PLUGIN);
		securePreferences = securePreferences.node(EncodingUtils.encodeSlashes(getRepositoryUrl()));
		return securePreferences;
	}

	private void addAuthInfo(String username, String password, String userProperty, String passwordProperty) {
		if (Platform.isRunning()) {
			if (useSecureStorage()) {
				try {
					ISecurePreferences securePreferences = getSecurePreferences();
					if (userProperty.equals(getKeyPrefix(AuthenticationType.REPOSITORY) + USERNAME)) {
						this.setProperty(userProperty, username);
					} else {
						securePreferences.put(userProperty, username, false);
					}
					securePreferences.put(passwordProperty, password, true);
				} catch (StorageException e) {
					StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
							"Could not store authorization credentials", e)); //$NON-NLS-1$
				}
			} else {
				synchronized (LOCK) {
					Map<String, String> map = getAuthInfo();
					if (map == null) {
						map = new HashMap<String, String>();
					}
					try {
						try {
							map.put(userProperty, username);
							map.put(passwordProperty, password);
							Platform.addAuthorizationInfo(new URL(getRepositoryUrl()), AUTH_REALM, AUTH_SCHEME, map);
						} catch (MalformedURLException ex) {
							Platform.addAuthorizationInfo(DEFAULT_URL, getRepositoryUrl(), AUTH_SCHEME, map);
						}
					} catch (CoreException e) {
						StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
								"Could not set authorization credentials", e)); //$NON-NLS-1$
					}
				}
			}
		} else {
			synchronized (LOCK) {
				Map<String, String> headlessCreds = credentials.get(getRepositoryUrl());
				if (headlessCreds == null) {
					headlessCreds = new HashMap<String, String>();
					credentials.put(getRepositoryUrl(), headlessCreds);
				}
				headlessCreds.put(userProperty, username);
				headlessCreds.put(passwordProperty, password);
			}
		}
	}

	@SuppressWarnings( { "unchecked" })
	private Map<String, String> getAuthInfo() {
		synchronized (LOCK) {
			if (Platform.isRunning()) {
				try {
					return Platform.getAuthorizationInfo(new URL(getRepositoryUrl()), AUTH_REALM, AUTH_SCHEME);
				} catch (MalformedURLException ex) {
					return Platform.getAuthorizationInfo(DEFAULT_URL, getRepositoryUrl(), AUTH_SCHEME);
				} catch (Exception e) {
					StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
							"Could not retrieve authorization credentials", e)); //$NON-NLS-1$
				}
			} else {
				Map<String, String> headlessCreds = credentials.get(getRepositoryUrl());
				if (headlessCreds == null) {
					headlessCreds = new HashMap<String, String>();
					credentials.put(getRepositoryUrl(), headlessCreds);
				}
				return headlessCreds;
			}
			return null;
		}
	}

	/**
	 * @deprecated use {@code flushAuthenticationCredentials()}
	 */
	@Deprecated
	public void clearCredentials() {
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object instanceof TaskRepository) {
			TaskRepository repository = (TaskRepository) object;
			return getConnectorKind().equals(repository.getConnectorKind())
					&& getRepositoryUrl().equals(repository.getRepositoryUrl());
		}
		return false;
	}

	public void flushAuthenticationCredentials() {
		// legacy support for versions prior to 2.2 that did not set the enable flag
		setProperty(getKeyPrefix(AuthenticationType.HTTP) + ENABLED, null);
		setProperty(getKeyPrefix(AuthenticationType.PROXY) + ENABLED, null);
		setProperty(getKeyPrefix(AuthenticationType.REPOSITORY) + ENABLED, null);

		synchronized (this) {
			transientProperties.clear();
			isCachedUserName = false;
		}

		synchronized (LOCK) {
			if (Platform.isRunning()) {
				if (useSecureStorage()) {
					if (Platform.isRunning()) {
						ISecurePreferences securePreferences = getSecurePreferences();
						securePreferences.removeNode();
						this.setProperty(AuthenticationType.REPOSITORY + USERNAME, ""); //$NON-NLS-1$
					} else {
						Map<String, String> headlessCreds = credentials.get(getRepositoryUrl());
						if (headlessCreds != null) {
							headlessCreds.clear();
						}
					}
				} else {
					try {
						try {
							Platform.flushAuthorizationInfo(new URL(getRepositoryUrl()), AUTH_REALM, AUTH_SCHEME);
						} catch (MalformedURLException ex) {
							Platform.flushAuthorizationInfo(DEFAULT_URL, getRepositoryUrl(), AUTH_SCHEME);
						}
					} catch (CoreException e) {
						// FIXME propagate exception?
						StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
								"Could not flush authorization credentials", e)); //$NON-NLS-1$
					}
				}
			} else {
				Map<String, String> headlessCreds = credentials.get(getRepositoryUrl());
				if (headlessCreds != null) {
					headlessCreds.clear();
				}
			}
		}
	}

	private boolean useSecureStorage() {
		String useSecure = getProperty(ITasksCoreConstants.PROPERTY_USE_SECURE_STORAGE);
		if (useSecure != null) {
			return "true".equals(useSecure); //$NON-NLS-1$
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private String getAuthInfo(String property) {
		if (Platform.isRunning()) {
			if (useSecureStorage()) {
				String propertyValue = null;
				if (property.equals(getKeyPrefix(AuthenticationType.REPOSITORY) + USERNAME)) {
					propertyValue = this.getProperty(property);
				} else {
					try {
						ISecurePreferences securePreferences = getSecurePreferences();
						propertyValue = securePreferences.get(property, null);
					} catch (StorageException e) {
						StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
								"Could not retrieve authorization credentials", e)); //$NON-NLS-1$
					}
				}
				return propertyValue;
			} else {
				synchronized (LOCK) {
					try {
						Map<String, String> map = Platform.getAuthorizationInfo(new URL(getRepositoryUrl()),
								AUTH_REALM, AUTH_SCHEME);
						if (map != null) {
							String propertyValue = map.get(property);
							return propertyValue;
						}
					} catch (MalformedURLException ex) {
						Map<String, String> map = Platform.getAuthorizationInfo(DEFAULT_URL, getRepositoryUrl(),
								AUTH_SCHEME);
						if (map != null) {
							String propertyValue = map.get(property);
							return propertyValue;
						}
					} catch (Exception e) {
						StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
								"Could not retrieve authorization credentials", e)); //$NON-NLS-1$
					}
					return null;
				}
			}
		} else {
			synchronized (LOCK) {
				Map<String, String> headlessCreds = credentials.get(getRepositoryUrl());
				if (headlessCreds == null) {
					headlessCreds = new HashMap<String, String>();
					credentials.put(getRepositoryUrl(), headlessCreds);
				}
				return headlessCreds.get(property);
			}
		}
	}

	public String getCharacterEncoding() {
		final String encoding = properties.get(IRepositoryConstants.PROPERTY_ENCODING);
		return encoding == null || "".equals(encoding) ? DEFAULT_CHARACTER_ENCODING : encoding; //$NON-NLS-1$
	}

	/**
	 * Get the last refresh date as initialized {@link Date} object, null if not set<br />
	 * 
	 * @return {@link Date} configuration date, null if not set
	 */
	public Date getConfigurationDate() {
		Date configDate = null;
		String value = this.getProperty(PROPERTY_CONFIG_TIMESTAMP);
		try {
			configDate = new Date(Long.valueOf(value).longValue());

		} catch (Exception e) {

		}
		return configDate;
	}

	/**
	 * @return "<unknown>" if kind is unknown
	 */
	public String getConnectorKind() {
		String kind = properties.get(IRepositoryConstants.PROPERTY_CONNECTOR_KIND);
		if (kind != null) {
			return kind;
		} else {
			return IRepositoryConstants.KIND_UNKNOWN;
		}
	}

	/**
	 * Returns the credentials for an authentication type.
	 * 
	 * @param authType
	 *            the type of authentication
	 * @return null, if no credentials are set for <code>authType</code>
	 * @since 3.0
	 */
	public synchronized AuthenticationCredentials getCredentials(AuthenticationType authType) {
		String key = getKeyPrefix(authType);

		String enabled = getProperty(key + ENABLED);
		if (enabled == null || "true".equals(enabled)) { //$NON-NLS-1$
			String userName = getAuthInfo(key + USERNAME);
			String password;

			String savePassword = getProperty(key + SAVE_PASSWORD);
			if (savePassword != null && "true".equals(savePassword)) { //$NON-NLS-1$
				password = getAuthInfo(key + PASSWORD);
			} else {
				password = transientProperties.get(key + PASSWORD);
			}

			if (userName == null) {
				userName = ""; //$NON-NLS-1$
			}
			if (password == null) {
				password = ""; //$NON-NLS-1$
			}

			if (enabled == null && userName.length() == 0) {
				// API30: legacy support for versions prior to 2.2 that did not set the enable flag, remove for 3.0
				return null;
			}

			return new AuthenticationCredentials(userName, password);
		} else {
			return null;
		}
	}

	/**
	 * @deprecated use {@link #getCredentials(AuthenticationType)} instead
	 */
	@Deprecated
	public String getHttpPassword() {
		return getPassword(AuthenticationType.HTTP);
	}

	/**
	 * @deprecated use {@link #getCredentials(AuthenticationType)} instead
	 */
	@Deprecated
	public String getHttpUser() {
		return getUserName(AuthenticationType.HTTP);
	}

	/**
	 * @deprecated use {@link #getCredentials(AuthenticationType)} instead
	 */
	@Deprecated
	public String getPassword() {
		return getPassword(AuthenticationType.REPOSITORY);
	}

	/**
	 * Legacy support for < 2.2. Remove in 2.3.
	 */
	private String getPassword(AuthenticationType authType) {
		AuthenticationCredentials credentials = getCredentials(authType);
		return (credentials != null) ? credentials.getPassword() : null;
	}

	public Map<String, String> getProperties() {
		return new LinkedHashMap<String, String>(this.properties);
	}

	public String getProperty(String name) {
		return this.properties.get(name);
	}

	/**
	 * @deprecated use {@link #getCredentials(AuthenticationType)} instead
	 */
	@Deprecated
	public String getProxyPassword() {
		return getPassword(AuthenticationType.PROXY);
	}

	/**
	 * @deprecated use {@link #getCredentials(AuthenticationType)} instead
	 */
	@Deprecated
	public String getProxyUsername() {
		return getUserName(AuthenticationType.PROXY);
	}

	/**
	 * @return the URL if the label property is not set
	 */
	public String getRepositoryLabel() {
		String label = properties.get(IRepositoryConstants.PROPERTY_LABEL);
		if (label != null && label.length() > 0) {
			return label;
		} else {
			return getRepositoryUrl();
		}
	}

	/**
	 * @since 3.0
	 */
	public boolean getSavePassword(AuthenticationType authType) {
		String value = getProperty(getKeyPrefix(authType) + SAVE_PASSWORD);
		return value != null && "true".equals(value); //$NON-NLS-1$
	}

	public String getSynchronizationTimeStamp() {
		return this.properties.get(IRepositoryConstants.PROPERTY_SYNCTIMESTAMP);
	}

	public String getTimeZoneId() {
		final String timeZoneId = properties.get(IRepositoryConstants.PROPERTY_TIMEZONE);
		return timeZoneId == null || "".equals(timeZoneId) ? TimeZone.getDefault().getID() : timeZoneId; //$NON-NLS-1$
	}

	public String getUrl() {
		return getRepositoryUrl();
	}

	/**
	 * @since 3.0
	 */
	public String getRepositoryUrl() {
		return properties.get(IRepositoryConstants.PROPERTY_URL);
	}

	/**
	 * The username is cached since it needs to be retrieved frequently (e.g. for Task List decoration).
	 */
	public String getUserName() {
		// NOTE: if anonymous, user name is "" string so we won't go to keyring
		if (!isCachedUserName) {
			if (useSecureStorage()) {
				// do not open secure store for username to avoid prompting user for password during initialization 
				cachedUserName = getProperty(getKeyPrefix(AuthenticationType.REPOSITORY) + USERNAME);
			} else {
				cachedUserName = getUserName(AuthenticationType.REPOSITORY);
			}
			isCachedUserName = true;
		}
		return cachedUserName;
	}

	/**
	 * Legacy support for < 2.2. Remove in 2.3.
	 */
	private String getUserName(AuthenticationType authType) {
		AuthenticationCredentials credentials = getCredentials(authType);
		return (credentials != null) ? credentials.getUserName() : null;
	}

	public String getVersion() {
		final String version = properties.get(IRepositoryConstants.PROPERTY_VERSION);
		return version == null || "".equals(version) ? NO_VERSION_SPECIFIED : version; //$NON-NLS-1$
	}

	/**
	 * @deprecated use #getCredentials(AuthenticationType) instead
	 */
	@Deprecated
	public boolean hasCredentials() {
		String username = getUserName();
		String password = getPassword();
		return username != null && username.length() > 0 && password != null && password.length() > 0;
	}

	@Override
	public int hashCode() {
		return getConnectorKind().hashCode();
	}

	public boolean hasProperty(String name) {
		String value = getProperty(name);
		return value != null && value.trim().length() > 0;
	}

	/**
	 * @deprecated #getCredentials(AuthenticationType) == null instead
	 */
	@Deprecated
	public boolean isAnonymous() {
		return getProperty(ANONYMOUS_LOGIN) == null || "true".equals(getProperty(ANONYMOUS_LOGIN)); //$NON-NLS-1$
	}

	public boolean isBugRepository() {
		return isBugRepository;
	}

	/**
	 * Use platform proxy settings
	 */
	public boolean isDefaultProxyEnabled() {
		return "true".equals(getProperty(PROXY_USEDEFAULT)); //$NON-NLS-1$
	}

	public boolean isOffline() {
		return getProperty(OFFLINE) != null && "true".equals(getProperty(OFFLINE)); //$NON-NLS-1$
	}

	public void removeProperty(String key) {
		this.properties.remove(key);
	}

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} instead
	 */
	@Deprecated
	public void setAuthenticationCredentials(String username, String password) {
		setCredentials(AuthenticationType.REPOSITORY, username, password);
	}

	public void setBugRepository(boolean isBugRepository) {
		this.isBugRepository = isBugRepository;
	}

	public void setCharacterEncoding(String characterEncoding) {
		properties.put(IRepositoryConstants.PROPERTY_ENCODING, characterEncoding == null ? DEFAULT_CHARACTER_ENCODING
				: characterEncoding);
	}

	/**
	 * Set the Configuration date to the {@link Date} indicated.
	 * 
	 * @param configuration
	 *            date {@link {@link Date}
	 */
	final public void setConfigurationDate(final Date date) {
		this.setProperty(PROPERTY_CONFIG_TIMESTAMP, String.valueOf(date.getTime()));
		//  should persist here, but that can only be done by the TaskRepositoryManager
		// However this is also included when persisting ordinary sync time
	}

	/**
	 * Sets the credentials for <code>authType</code>.
	 * 
	 * @param authType
	 *            the type of authentication
	 * @param credentials
	 *            the credentials, if null, the credentials for <code>authType</code> will be flushed
	 * @param savePassword
	 *            if true, the password will be persisted in the platform key ring; otherwise it will be stored in
	 *            memory only
	 * @since 3.0
	 */
	public synchronized void setCredentials(AuthenticationType authType, AuthenticationCredentials credentials,
			boolean savePassword) {
		String key = getKeyPrefix(authType);

		setProperty(key + SAVE_PASSWORD, String.valueOf(savePassword));

		if (credentials == null) {
			setProperty(key + ENABLED, String.valueOf(false));
			transientProperties.remove(key + PASSWORD);
			addAuthInfo("", "", key + USERNAME, key + PASSWORD); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			setProperty(key + ENABLED, String.valueOf(true));
			if (savePassword) {
				addAuthInfo(credentials.getUserName(), credentials.getPassword(), key + USERNAME, key + PASSWORD);
				transientProperties.remove(key + PASSWORD);
			} else {
				addAuthInfo(credentials.getUserName(), "", key + USERNAME, key + PASSWORD); //$NON-NLS-1$
				transientProperties.put(key + PASSWORD, credentials.getPassword());
			}
		}

		if (authType == AuthenticationType.REPOSITORY) {
			if (credentials == null) {
				this.cachedUserName = null;
				this.isCachedUserName = false;
			} else {
				this.cachedUserName = credentials.getUserName();
				this.isCachedUserName = true;
			}
		}
	}

	/**
	 * Legacy support for < 2.2. Remove in 2.3.
	 */
	private void setCredentials(AuthenticationType type, String username, String password) {
		if (username == null) {
			setCredentials(type, null, true);
		} else {
			setCredentials(type, new AuthenticationCredentials(username, password), true);
		}

	}

	/**
	 * @deprecated use esetCredentials(AuthenticationType, AuthenticationCredentials, boolean)
	 */
	@Deprecated
	public void setHttpAuthenticationCredentials(String username, String password) {
		setCredentials(AuthenticationType.HTTP, username, password);
	}

	public void setOffline(boolean offline) {
		properties.put(OFFLINE, String.valueOf(offline));
	}

	/**
	 * @deprecated use {@link #setCredentials(AuthenticationType, AuthenticationCredentials, boolean)} instead
	 */
	@Deprecated
	public void setProxyAuthenticationCredentials(String username, String password) {
		setCredentials(AuthenticationType.PROXY, username, password);
	}

	public void setRepositoryLabel(String repositoryLabel) {
		setProperty(IRepositoryConstants.PROPERTY_LABEL, repositoryLabel);
	}

	/**
	 * ONLY for use by IRepositoryConstants. To set the sync time call IRepositoryConstants.setSyncTime(repository,
	 * date);
	 */
	public void setSynchronizationTimeStamp(String syncTime) {
		setProperty(IRepositoryConstants.PROPERTY_SYNCTIMESTAMP, syncTime);
	}

	public void setProperty(String key, String newValue) {
		Assert.isLegal(!key.matches(".*\\s.*")); //$NON-NLS-1$
		String oldValue = this.properties.get(key);
		if ((oldValue != null && !oldValue.equals(newValue)) || (oldValue == null && newValue != null)) {
			this.properties.put(key.intern(), (newValue != null) ? newValue.intern() : null);
			notifyChangeListeners(key, oldValue, newValue);
		}
	}

	private void notifyChangeListeners(String key, String old, String value) {
		PropertyChangeEvent event = new PropertyChangeEvent(this, key, old, value);
		for (PropertyChangeListener listener : propertyChangeListeners) {
			listener.propertyChange(event);
		}
	}

	public void setTimeZoneId(String timeZoneId) {
		setProperty(IRepositoryConstants.PROPERTY_TIMEZONE, timeZoneId == null ? TimeZone.getDefault().getID()
				: timeZoneId);
	}

	/**
	 * @deprecated Use {@link #setRepositoryUrl(String)} instead
	 */
	@Deprecated
	public void setUrl(String newUrl) {
		setRepositoryUrl(newUrl);
	}

	/**
	 * @since 3.0
	 */
	public void setRepositoryUrl(String repositoryUrl) {
		Assert.isNotNull(repositoryUrl);
		properties.put(IRepositoryConstants.PROPERTY_URL, repositoryUrl.intern());
	}

	public void setVersion(String ver) {
		properties.put(IRepositoryConstants.PROPERTY_VERSION, ver == null ? NO_VERSION_SPECIFIED : ver);
	}

	@Override
	public String toString() {
		return getRepositoryUrl();
	}

	/**
	 * @since 3.0
	 */
	public boolean isUpdating() {
		return updating;
	}

	/**
	 * @since 3.0
	 */
	public void setUpdating(boolean updating) {
		this.updating = updating;
	}

	/**
	 * @since 3.0
	 */
	public IRepositoryPerson createPerson(String personId) {
		return new RepositoryPerson(this, personId);
	}

	/**
	 * @since 3.0
	 */
	public IStatus getStatus() {
		return errorStatus;
	}

	/**
	 * @since 3.0
	 */
	public void setStatus(IStatus errorStatus) {
		this.errorStatus = errorStatus;
	}

	/**
	 * @since 3.0
	 */
	public void addChangeListener(PropertyChangeListener listener) {
		propertyChangeListeners.add(listener);
	}

	/**
	 * @since 3.0
	 */
	public void removeChangeListener(PropertyChangeListener listener) {
		propertyChangeListeners.remove(listener);
	}

	/**
	 * @since 3.1
	 */
	public void setDefaultProxyEnabled(boolean useDefaultProxy) {
		setProperty(TaskRepository.PROXY_USEDEFAULT, String.valueOf(useDefaultProxy));
	}

}
