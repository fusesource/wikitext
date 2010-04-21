/*******************************************************************************
 * Copyright (c) 2010 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.core.service;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.xmlrpc.XmlRpcException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.commons.xmlrpc.CommonXmlRpcClient;

@SuppressWarnings("restriction")
public class BugzillaXmlRpcClient extends CommonXmlRpcClient {

	public static final String XML_BUGZILLA_VERSION = "Bugzilla.version"; //$NON-NLS-1$

	public static final String XML_BUGZILLA_TIME = "Bugzilla.time"; //$NON-NLS-1$ 

	public static final String XML_USER_LOGIN = "User.login"; //$NON-NLS-1$

	public static final String XML_USER_GET = "User.get"; //$NON-NLS-1$

	public static final String XML_BUG_FIELDS = "Bug.fields"; //$NON-NLS-1$

	public static final String XML_PRODUCT_GET_SELECTABLE = "Product.get_selectable_products"; //$NON-NLS-1$

	public static final String XML_PRODUCT_GET_ENTERABLE = "Product.get_enterable_products"; //$NON-NLS-1$

	public static final String XML_PRODUCT_GET_ACCESSIBLE = "Product.get_accessible_products"; //$NON-NLS-1$

	public static final String XML_PRODUCT_GET = "Product.get"; //$NON-NLS-1$

	/*
	 * Parameter Definitions
	 * 
	 */

	public static final String XML_PARAMETER_LOGIN = "login"; //$NON-NLS-1$

	public static final String XML_PARAMETER_PASSWORD = "password"; //$NON-NLS-1$

	public static final String XML_PARAMETER_REMEMBER = "remember"; //$NON-NLS-1$

	public static final String XML_PARAMETER_IDS = "ids"; //$NON-NLS-1$

	public static final String XML_PARAMETER_NAMES = "names"; //$NON-NLS-1$

	public static final String XML_PARAMETER_MATCH = "match"; //$NON-NLS-1$

	/*
	 * Response Parameter Definitions
	 * 
	 */

	public static final String XML_RESPONSE_DB_TIME = "db_time"; //$NON-NLS-1$

	public static final String XML_RESPONSE_WEB_TIME = "web_time"; //$NON-NLS-1$

	public static final String[] XML_BUGZILLA_TIME_RESPONSE_TO_REMOVE = {
			"tz_offset", "tz_short_name", "web_time_utc", "tz_name" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	public static final String XML_RESPONSE_VERSION = "version"; //$NON-NLS-1$

	public static final String XML_RESPONSE_USERS = "users"; //$NON-NLS-1$

	public static final String XML_RESPONSE_ID = "id"; //$NON-NLS-1$

	public static final String XML_RESPONSE_IDS = "ids"; //$NON-NLS-1$

	public static final String XML_RESPONSE_FIELDS = "fields"; //$NON-NLS-1$

	public static final String XML_RESPONSE_PRODUCTS = "products"; //$NON-NLS-1$

	/*
	 * Fields
	 * 
	 */
	private int userID = -1;

	public BugzillaXmlRpcClient(AbstractWebLocation location, HttpClient client) {
		super(location, client);
	}

	public BugzillaXmlRpcClient(AbstractWebLocation location) {
		super(location);
	}

	public String getVersion() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<String>(this) {
			@Override
			public String execute() throws XmlRpcException {
				String result = ""; //$NON-NLS-1$
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_BUGZILLA_VERSION,
						(Object[]) null);
				result = response2String(response, XML_RESPONSE_VERSION);
				return result;
			}
		}).execute();
	}

	public Date getDBTime() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Date>(this) {
			@Override
			public Date execute() throws XmlRpcException {
				Date result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_BUGZILLA_TIME,
						(Object[]) null);
				result = response2Date(response, XML_RESPONSE_DB_TIME);
				return result;
			}
		}).execute();
	}

	public Date getWebTime() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Date>(this) {
			@Override
			public Date execute() throws XmlRpcException {
				Date result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_BUGZILLA_TIME,
						(Object[]) null);
				result = response2Date(response, XML_RESPONSE_WEB_TIME);
				return result;
			}
		}).execute();
	}

	public HashMap<?, ?> getTime() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<HashMap<?, ?>>(this) {
			@Override
			public HashMap<?, ?> execute() throws XmlRpcException {
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_BUGZILLA_TIME,
						(Object[]) null);
				if (response != null) {
					for (String exclude : XML_BUGZILLA_TIME_RESPONSE_TO_REMOVE) {
						response.remove(exclude);
					}
				}
				return response;
			}
		}).execute();
	}

	public int login() throws XmlRpcException {
		userID = -1;
		final AuthenticationCredentials credentials = this.getLocation().getCredentials(AuthenticationType.REPOSITORY);
		if (credentials != null) {
			userID = (new BugzillaXmlRpcOperation<Integer>(this) {
				@SuppressWarnings("serial")
				@Override
				public Integer execute() throws XmlRpcException {
					HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_USER_LOGIN,
							new Object[] { new HashMap<String, Object>() {
								{
									put(XML_PARAMETER_LOGIN, credentials.getUserName());
									put(XML_PARAMETER_PASSWORD, credentials.getPassword());
									put(XML_PARAMETER_REMEMBER, true);
								}
							} });
					if (response != null) {
						Integer result = response2Integer(response, XML_RESPONSE_ID);
						return result;
					}
					return null;
				}
			}).execute();
		}
		return userID;
	}

	private Object[] getUserInfoInternal(final Object[] callParm) throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Object[]>(this) {
			@Override
			public Object[] execute() throws XmlRpcException {
				Object[] result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_USER_GET, callParm);
				result = response2ObjectArray(response, XML_RESPONSE_USERS);
				return result;
			}
		}).execute();
	}

	@SuppressWarnings("serial")
	public Object[] getUserInfoFromIDs(final Integer[] ids) throws XmlRpcException {
		return getUserInfoInternal(new Object[] { new HashMap<String, Object[]>() {
			{
				put(XML_PARAMETER_IDS, ids);
			}
		} });
	}

	@SuppressWarnings("serial")
	public Object[] getUserInfoFromNames(final String[] names) throws XmlRpcException {
		return getUserInfoInternal(new Object[] { new HashMap<String, Object[]>() {
			{
				put(XML_PARAMETER_NAMES, names);
			}
		} });
	}

	public Object[] getUserInfoWithMatch(String[] matchs) throws XmlRpcException {
		HashMap<String, Object[]> parmArray = new HashMap<String, Object[]>();
		Object[] callParm = new Object[] { parmArray };
		parmArray.put(XML_PARAMETER_MATCH, matchs);
		return getUserInfoInternal(callParm);
	}

	private Object[] getFieldsInternal(final Object[] callParm) throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Object[]>(this) {
			@Override
			public Object[] execute() throws XmlRpcException {
				Object[] result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_BUG_FIELDS, callParm);
				result = response2ObjectArray(response, XML_RESPONSE_FIELDS);
				return result;
			}
		}).execute();
	}

	public Object[] getAllFields() throws XmlRpcException {
		return getFieldsInternal(null);
	}

	@SuppressWarnings("serial")
	public Object[] getFieldsWithNames(final String[] names) throws XmlRpcException {
		return getFieldsInternal(new Object[] { new HashMap<String, Object[]>() {
			{
				put(XML_PARAMETER_NAMES, names);
			}
		} });
	}

	@SuppressWarnings("serial")
	public Object[] getFieldsWithIDs(final Integer[] ids) throws XmlRpcException {
		return getFieldsInternal(new Object[] { new HashMap<String, Object[]>() {
			{
				put(XML_PARAMETER_IDS, ids);
			}
		} });
	}

	public Object[] getSelectableProducts() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Object[]>(this) {
			@Override
			public Object[] execute() throws XmlRpcException {
				Object[] result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_PRODUCT_GET_SELECTABLE,
						(Object[]) null);
				result = response2ObjectArray(response, XML_RESPONSE_IDS);
				return result;
			}
		}).execute();
	}

	public Object[] getEnterableProducts() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Object[]>(this) {
			@Override
			public Object[] execute() throws XmlRpcException {
				Object[] result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_PRODUCT_GET_ENTERABLE,
						(Object[]) null);
				result = response2ObjectArray(response, XML_RESPONSE_IDS);
				return result;
			}
		}).execute();
	}

	public Object[] getAccessibleProducts() throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Object[]>(this) {
			@Override
			public Object[] execute() throws XmlRpcException {
				Object[] result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_PRODUCT_GET_ACCESSIBLE,
						(Object[]) null);
				result = response2ObjectArray(response, XML_RESPONSE_IDS);
				return result;
			}
		}).execute();
	}

	public Object[] getProducts(final Integer[] ids) throws XmlRpcException {
		return (new BugzillaXmlRpcOperation<Object[]>(this) {
			@SuppressWarnings("serial")
			@Override
			public Object[] execute() throws XmlRpcException {
				Object[] result = null;
				HashMap<?, ?> response = (HashMap<?, ?>) call(new NullProgressMonitor(), XML_PRODUCT_GET,
						new Object[] { new HashMap<String, Object[]>() {
							{
								put(XML_PARAMETER_IDS, ids);
							}
						} });
				result = response2ObjectArray(response, XML_RESPONSE_PRODUCTS);
				return result;
			}
		}).execute();
	}

	private Object[] response2ObjectArray(HashMap<?, ?> response, String name) throws XmlRpcException {
		Object[] result;
		if (response == null) {
			return null;
		}
		try {
			result = (Object[]) response.get(name);
		} catch (ClassCastException e) {
			result = null;
			throw new XmlRpcClassCastException(e);
		}
		return result;
	}

	private Integer response2Integer(HashMap<?, ?> response, String name) throws XmlRpcException {
		Integer result;
		if (response == null) {
			return null;
		}
		try {
			result = (Integer) response.get(name);
		} catch (ClassCastException e) {
			result = null;
			throw new XmlRpcClassCastException(e);
		}
		return result;
	}

	private String response2String(HashMap<?, ?> response, String name) throws XmlRpcException {
		String result;
		if (response == null) {
			return null;
		}
		try {
			result = (String) response.get(name);
		} catch (ClassCastException e) {
			result = null;
			throw new XmlRpcClassCastException(e);
		}
		return result;
	}

	private Date response2Date(HashMap<?, ?> response, String name) throws XmlRpcException {
		Date result;
		if (response == null) {
			return null;
		}
		try {
			result = (Date) response.get(name);
		} catch (ClassCastException e) {
			result = null;
			throw new XmlRpcClassCastException(e);
		}
		return result;
	}

}
