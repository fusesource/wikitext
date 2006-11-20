/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.tasks.ui.wizards;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylar.tasks.core.TaskRepository;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public abstract class AbstractRepositorySettingsPage extends WizardPage {

	protected static final String LABEL_REPOSITORY_LABEL = "Label: ";

	protected static final String LABEL_SERVER = "Server: ";

	protected static final String LABEL_USER = "User ID: ";

	protected static final String LABEL_PASSWORD = "Password: ";

	protected static final String URL_PREFIX_HTTPS = "https://";

	protected static final String URL_PREFIX_HTTP = "http://";

	protected AbstractRepositoryConnector connector;

	protected StringFieldEditor repositoryLabelEditor;

	protected Combo serverUrlCombo;

	private String serverVersion = TaskRepository.NO_VERSION_SPECIFIED;

	protected StringFieldEditor repositoryUserNameEditor;

	protected StringFieldEditor repositoryPasswordEditor;

	protected StringFieldEditor httpAuthUserNameEditor;

	protected StringFieldEditor httpAuthPasswordEditor;

	protected StringFieldEditor proxyHostnameEditor;

	protected StringFieldEditor proxyPortEditor;

	protected StringFieldEditor proxyUserNameEditor;

	protected StringFieldEditor proxyPasswordEditor;

	protected TaskRepository repository;

	private Button validateServerButton;

	private Combo otherEncodingCombo;

	private Button defaultEncoding;

	// private Combo timeZonesCombo;

	protected Button anonymousButton;

	private String oldUsername;

	private String oldPassword;

	private String oldHttpAuthUserId;

	private String oldHttpAuthPassword;

	private boolean needsAnonymousLogin;

	private boolean needsTimeZone;

	private boolean needsEncoding;

	private boolean needsHttpAuth;

	private Composite container;
	
	private Composite httpAuthComp; 
	
	private Composite proxyAuthComp;
	
	private ExpandableComposite httpAuthExpComposite;
	
	private ExpandableComposite proxyExpComposite;

	private Set<String> repositoryUrls;

	private String originalUrl;

	private Button otherEncoding;

	private Button httpAuthButton;

	private boolean needsProxy;

	private Button systemProxyButton;

	private String oldProxyUsername = "";

	private String oldProxyPassword = "";

	// private Button proxyAuthButton;

	private String oldProxyHostname = "";

	private String oldProxyPort = "";

	private Button proxyAuthButton;
	
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	public AbstractRepositorySettingsPage(String title, String description, AbstractRepositoryConnectorUi repositoryUi) {
		super(title);
		super.setTitle(title);
		super.setDescription(description);
		AbstractRepositoryConnector connector = TasksUiPlugin.getRepositoryManager().getRepositoryConnector(
				repositoryUi.getRepositoryType());
		this.connector = connector;

		setNeedsAnonymousLogin(false);
		setNeedsEncoding(true);
		setNeedsTimeZone(true);
	}

	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		FillLayout layout = new FillLayout();
		container.setLayout(layout);

		new Label(container, SWT.NONE).setText(LABEL_SERVER);
		serverUrlCombo = new Combo(container, SWT.DROP_DOWN);
		serverUrlCombo.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				isValidUrl(serverUrlCombo.getText());
				if (getWizard() != null) {
					getWizard().getContainer().updateButtons();
				}
			}
		});

		serverUrlCombo.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore

			}

			public void widgetSelected(SelectionEvent e) {
				isValidUrl(serverUrlCombo.getText());
				if (getWizard() != null) {
					getWizard().getContainer().updateButtons();
				}
			}
		});

		GridDataFactory.fillDefaults().grab(true, false).applyTo(serverUrlCombo);

		repositoryLabelEditor = new StringFieldEditor("", LABEL_REPOSITORY_LABEL, StringFieldEditor.UNLIMITED,
				container) {

			@Override
			protected boolean doCheckState() {
				return true;
				// return isValidUrl(getStringValue());
			}

			@Override
			protected void valueChanged() {
				super.valueChanged();
				if (getWizard() != null) {
					getWizard().getContainer().updateButtons();
				}
			}
		};
		// repositoryLabelEditor.setErrorMessage("error");

		if (needsAnonymousLogin()) {
			anonymousButton = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).applyTo(anonymousButton);

			anonymousButton.setText("Anonymous Access");
			anonymousButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					setAnonymous(anonymousButton.getSelection());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// ignore
				}
			});

			// Label anonymousLabel = new Label(container, SWT.NONE);
			// anonymousLabel.setText("");
		}

		repositoryUserNameEditor = new StringFieldEditor("", LABEL_USER, StringFieldEditor.UNLIMITED, container);
		repositoryPasswordEditor = new RepositoryStringFieldEditor("", LABEL_PASSWORD, StringFieldEditor.UNLIMITED,
				container);
		if (repository != null) {
			originalUrl = repository.getUrl();
			oldUsername = repository.getUserName();
			oldPassword = repository.getPassword();
			if (repository.hasProperty(TaskRepository.AUTH_HTTP_USERNAME)
					&& repository.hasProperty(TaskRepository.AUTH_HTTP_PASSWORD)) {
				oldHttpAuthUserId = repository.getProperty(TaskRepository.AUTH_HTTP_USERNAME);
				oldHttpAuthPassword = repository.getProperty(TaskRepository.AUTH_HTTP_PASSWORD);
			} else {
				oldHttpAuthPassword = "";
				oldHttpAuthUserId = "";
			}

			oldProxyHostname = repository.getProperty(TaskRepository.PROXY_HOSTNAME);
			oldProxyPort = repository.getProperty(TaskRepository.PROXY_PORT);
			if (oldProxyHostname == null)
				oldProxyHostname = "";
			if (oldProxyPort == null)
				oldProxyPort = "";

			oldProxyUsername = repository.getProxyUsername();
			oldProxyPassword = repository.getProxyPassword();
			if (oldProxyUsername == null)
				oldProxyUsername = "";
			if (oldProxyPassword == null)
				oldProxyPassword = "";

			try {
				String repositoryLabel = repository.getRepositoryLabel();
				if (repositoryLabel != null && repositoryLabel.length() > 0) {
					// repositoryLabelCombo.add(repositoryLabel);
					// repositoryLabelCombo.select(0);
					repositoryLabelEditor.setStringValue(repositoryLabel);
				}
				serverUrlCombo.setText(repository.getUrl());
				repositoryUserNameEditor.setStringValue(repository.getUserName());
				repositoryPasswordEditor.setStringValue(repository.getPassword());
			} catch (Throwable t) {
				MylarStatusHandler.fail(t, "could not set field value for: " + repository, false);
			}
		} else {
			oldUsername = "";
			oldPassword = "";
			oldHttpAuthPassword = "";
			oldHttpAuthUserId = "";
		}
		// bug 131656: must set echo char after setting value on Mac
		((RepositoryStringFieldEditor) repositoryPasswordEditor).getTextControl().setEchoChar('*');

		if (needsAnonymousLogin()) {
			// do this after username and password widgets have been intialized
			if (repository != null) {
				setAnonymous(isAnonymousAccess());
			}
		}

		// TODO: put this back if we can't get the info from all connectors
		// if (needsTimeZone()) {
		// Label timeZoneLabel = new Label(container, SWT.NONE);
		// timeZoneLabel.setText("Repository time zone: ");
		// timeZonesCombo = new Combo(container, SWT.READ_ONLY);
		// String[] timeZoneIds = TimeZone.getAvailableIDs();
		// Arrays.sort(timeZoneIds);
		// for (String zone : timeZoneIds) {
		// timeZonesCombo.add(zone);
		// }
		// boolean setZone = false;
		// if (repository != null) {
		// if (timeZonesCombo.indexOf(repository.getTimeZoneId()) > -1) {
		// timeZonesCombo.select(timeZonesCombo.indexOf(repository.getTimeZoneId()));
		// setZone = true;
		// }
		// }
		// if (!setZone) {
		// timeZonesCombo.select(timeZonesCombo.indexOf(TimeZone.getDefault().getID()));
		// }
		// }

		createAdditionalControls(container);

		if (needsEncoding()) {
			Label encodingLabel = new Label(container, SWT.HORIZONTAL);
			encodingLabel.setText("Character Encoding:");
			GridDataFactory.fillDefaults().align(SWT.TOP, SWT.DEFAULT).applyTo(encodingLabel);

			Composite encodingContainer = new Composite(container, SWT.NONE);
			GridLayout gridLayout = new GridLayout(2, false);
			gridLayout.marginWidth = 0;
			gridLayout.marginHeight = 0;
			encodingContainer.setLayout(gridLayout);

			defaultEncoding = new Button(encodingContainer, SWT.RADIO);
			defaultEncoding.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
			defaultEncoding.setText("Default (" + TaskRepository.DEFAULT_CHARACTER_ENCODING + ")");
			defaultEncoding.setSelection(true);

			otherEncoding = new Button(encodingContainer, SWT.RADIO);
			otherEncoding.setText("Other:");
			otherEncodingCombo = new Combo(encodingContainer, SWT.READ_ONLY);
			for (String encoding : Charset.availableCharsets().keySet()) {
				if (!encoding.equals(TaskRepository.DEFAULT_CHARACTER_ENCODING)) {
					otherEncodingCombo.add(encoding);
				}
			}

			setDefaultEncoding();

			otherEncoding.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					if (otherEncoding.getSelection()) {
						defaultEncoding.setSelection(false);
						otherEncodingCombo.setEnabled(true);
					} else {
						defaultEncoding.setSelection(true);
						otherEncodingCombo.setEnabled(false);
					}
				}
			});

			if (repository != null) {
				try {
					String repositoryEncoding = repository.getCharacterEncoding();
					if (repositoryEncoding != null) {// &&
						// !repositoryEncoding.equals(defaultEncoding))
						// {
						if (otherEncodingCombo.getItemCount() > 0
								&& otherEncodingCombo.indexOf(repositoryEncoding) > -1) {
							otherEncodingCombo.setEnabled(true);
							otherEncoding.setSelection(true);
							defaultEncoding.setSelection(false);
							otherEncodingCombo.select(otherEncodingCombo.indexOf(repositoryEncoding));
						} else {
							setDefaultEncoding();
						}
					}
				} catch (Throwable t) {
					MylarStatusHandler.fail(t, "could not set field value for: " + repository, false);
				}
			}
		}

		if (needsHttpAuth()) {
			
			httpAuthExpComposite = toolkit.createExpandableComposite(container, Section.COMPACT
					| Section.TWISTIE | Section.TITLE_BAR);
			httpAuthExpComposite.clientVerticalSpacing = 0;
			GridData gridData_2 = new GridData(SWT.FILL, SWT.FILL, true, false);
			gridData_2.horizontalIndent = -5;
			httpAuthExpComposite.setLayoutData(gridData_2);
			httpAuthExpComposite.setFont(container.getFont());
			httpAuthExpComposite.setBackground(container.getBackground());
			httpAuthExpComposite.setText("Http Authentication");
			httpAuthExpComposite.addExpansionListener(new ExpansionAdapter() {
				public void expansionStateChanged(ExpansionEvent e) {													
					getControl().getShell().pack();					
				}
			});

			GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).applyTo(httpAuthExpComposite);
			
			httpAuthComp = toolkit.createComposite(httpAuthExpComposite, SWT.NONE);
			GridLayout gridLayout2 = new GridLayout();
			gridLayout2.numColumns = 2;
			gridLayout2.verticalSpacing = 0;
			httpAuthComp.setLayout(gridLayout2);
			httpAuthComp.setBackground(container.getBackground());
			httpAuthExpComposite.setClient(httpAuthComp);
			
			
			httpAuthButton = new Button(httpAuthComp, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.TOP, SWT.DEFAULT).span(2, SWT.DEFAULT).applyTo(httpAuthButton);

			httpAuthButton.setText("Enabled");
			httpAuthButton.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					setHttpAuth(httpAuthButton.getSelection());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					// ignore
				}
			});

			httpAuthUserNameEditor = new StringFieldEditor("", "User ID: ", StringFieldEditor.UNLIMITED, httpAuthComp) {

				@Override
				protected boolean doCheckState() {
					return true;
				}

				@Override
				protected void valueChanged() {
					super.valueChanged();
					if (getWizard() != null) {
						getWizard().getContainer().updateButtons();
					}
				}
			};
			httpAuthPasswordEditor = new RepositoryStringFieldEditor("", "Password: ", StringFieldEditor.UNLIMITED,
					httpAuthComp);
			((RepositoryStringFieldEditor) httpAuthPasswordEditor).getTextControl().setEchoChar('*');

			// httpAuthGroup.setEnabled(httpAuthButton.getSelection());
			httpAuthUserNameEditor.setEnabled(httpAuthButton.getSelection(), httpAuthComp);
			httpAuthPasswordEditor.setEnabled(httpAuthButton.getSelection(), httpAuthComp);

			setHttpAuth(oldHttpAuthPassword != null && oldHttpAuthUserId != null && !oldHttpAuthPassword.equals("")
					&& !oldHttpAuthUserId.equals(""));

		}

		if (needsProxy()) {
			addProxySection();
		}

		validateServerButton = new Button(container, SWT.PUSH);
		GridDataFactory.swtDefaults().span(2, SWT.DEFAULT).grab(false, false).applyTo(validateServerButton);
		validateServerButton.setText("Validate Settings");
		validateServerButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				validateSettings();
			}
		});

		setControl(container);
	}

	private void addProxySection() {
		
		proxyExpComposite = toolkit.createExpandableComposite(container, Section.COMPACT
				| Section.TWISTIE | Section.TITLE_BAR);
		proxyExpComposite.clientVerticalSpacing = 0;
		GridData gridData_2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData_2.horizontalIndent = -5;
		proxyExpComposite.setLayoutData(gridData_2);
		proxyExpComposite.setFont(container.getFont());
		proxyExpComposite.setBackground(container.getBackground());
		proxyExpComposite.setText("Proxy Server Configuration");
		proxyExpComposite.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {													
				getControl().getShell().pack();					
			}
		});

		GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).applyTo(proxyExpComposite);
		
		proxyAuthComp = toolkit.createComposite(proxyExpComposite, SWT.NONE);
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.numColumns = 2;
		gridLayout2.verticalSpacing = 0;
		proxyAuthComp.setLayout(gridLayout2);
		proxyAuthComp.setBackground(container.getBackground());
		proxyExpComposite.setClient(proxyAuthComp);
		
		
		systemProxyButton = new Button(proxyAuthComp, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.TOP, SWT.DEFAULT).span(2, SWT.DEFAULT).applyTo(systemProxyButton);

		systemProxyButton.setText("Use Install/Update settings (default)");

		systemProxyButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setUseDefaultProxy(systemProxyButton.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore
			}
		});

		proxyHostnameEditor = new StringFieldEditor("", "Proxy host address: ", StringFieldEditor.UNLIMITED, proxyAuthComp) {

			@Override
			protected boolean doCheckState() {
				return true;
			}

			@Override
			protected void valueChanged() {
				super.valueChanged();
				if (getWizard() != null) {
					getWizard().getContainer().updateButtons();
				}
			}
		};
		proxyHostnameEditor.setStringValue(oldProxyHostname);

		proxyPortEditor = new RepositoryStringFieldEditor("", "Proxy host port: ", StringFieldEditor.UNLIMITED,
				proxyAuthComp);

		proxyPortEditor.setStringValue(oldProxyPort);

		proxyHostnameEditor.setEnabled(systemProxyButton.getSelection(), proxyAuthComp);
		proxyPortEditor.setEnabled(systemProxyButton.getSelection(), proxyAuthComp);

		//************* PROXY AUTHENTICATION **************

		proxyAuthButton = new Button(proxyAuthComp, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.TOP, SWT.DEFAULT).span(2, SWT.DEFAULT).applyTo(proxyAuthButton);

		proxyAuthButton.setText("Enable proxy authentication");
		proxyAuthButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				setProxyAuth(proxyAuthButton.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				// ignore
			}
		});

		proxyUserNameEditor = new StringFieldEditor("", "User ID: ", StringFieldEditor.UNLIMITED, proxyAuthComp) {

			@Override
			protected boolean doCheckState() {
				return true;
			}

			@Override
			protected void valueChanged() {
				super.valueChanged();
				if (getWizard() != null) {
					getWizard().getContainer().updateButtons();
				}
			}
		};
		proxyPasswordEditor = new RepositoryStringFieldEditor("", "Password: ", StringFieldEditor.UNLIMITED, proxyAuthComp);
		((RepositoryStringFieldEditor) proxyPasswordEditor).getTextControl().setEchoChar('*');

		// proxyPasswordEditor.setEnabled(httpAuthButton.getSelection(),
		// advancedComp);
		// ((StringFieldEditor)
		// httpAuthPasswordEditor).setEnabled(httpAuthButton.getSelection(),
		// advancedComp);

		setProxyAuth(oldProxyUsername != null && oldProxyPassword != null && !oldProxyUsername.equals("")
				&& !oldProxyPassword.equals(""));

		setUseDefaultProxy(repository.useDefaultProxy());

	}

	protected void setEncoding(String encoding) {
		if (encoding.equals(TaskRepository.DEFAULT_CHARACTER_ENCODING)) {
			setDefaultEncoding();
		} else {
			if (otherEncodingCombo.indexOf(encoding) != -1) {
				defaultEncoding.setSelection(false);
				otherEncodingCombo.setEnabled(true);
				otherEncoding.setSelection(true);
				otherEncodingCombo.select(otherEncodingCombo.indexOf(encoding));
			} else {
				setDefaultEncoding();
			}
		}
	}

	private void setDefaultEncoding() {
		defaultEncoding.setSelection(true);
		otherEncoding.setSelection(false);
		otherEncodingCombo.setEnabled(false);
		if (otherEncodingCombo.getItemCount() > 0) {
			otherEncodingCombo.select(0);
		}
	}

	public void setAnonymous(boolean selected) {
		if (!needsAnonymousLogin) {
			return;
		}

		anonymousButton.setSelection(selected);

		if (selected) {
			oldUsername = repositoryUserNameEditor.getStringValue();
			oldPassword = ((StringFieldEditor) repositoryPasswordEditor).getStringValue();
			repositoryUserNameEditor.setStringValue(null);
			((StringFieldEditor) repositoryPasswordEditor).setStringValue(null);
		} else {
			repositoryUserNameEditor.setStringValue(oldUsername);
			((StringFieldEditor) repositoryPasswordEditor).setStringValue(oldPassword);
		}

		repositoryUserNameEditor.setEnabled(!selected, container);
		((StringFieldEditor) repositoryPasswordEditor).setEnabled(!selected, container);
	}

	public void setHttpAuth(boolean selected) {
		if (!needsHttpAuth) {
			return;
		}
		httpAuthButton.setSelection(selected);
		if (!selected) {
			oldHttpAuthUserId = httpAuthUserNameEditor.getStringValue();
			oldHttpAuthPassword = ((StringFieldEditor) httpAuthPasswordEditor).getStringValue();
			httpAuthUserNameEditor.setStringValue(null);
			((StringFieldEditor) httpAuthPasswordEditor).setStringValue(null);
		} else {
			httpAuthUserNameEditor.setStringValue(oldHttpAuthUserId);
			((StringFieldEditor) httpAuthPasswordEditor).setStringValue(oldHttpAuthPassword);
		}
		httpAuthUserNameEditor.setEnabled(selected, httpAuthComp);
		((StringFieldEditor) httpAuthPasswordEditor).setEnabled(selected, httpAuthComp);
	}

	public void setUseDefaultProxy(boolean selected) {
		if (!needsProxy) {
			return;
		}

		systemProxyButton.setSelection(selected);

		if (selected) {
			oldProxyHostname = proxyHostnameEditor.getStringValue();
			oldProxyPort = proxyPortEditor.getStringValue();
			// proxyHostnameEditor.setStringValue(null);
			// proxyPortEditor.setStringValue(null);
		} else {
			proxyHostnameEditor.setStringValue(oldProxyHostname);
			proxyPortEditor.setStringValue(oldProxyPort);
		}
		proxyHostnameEditor.setEnabled(!selected, proxyAuthComp);
		proxyPortEditor.setEnabled(!selected, proxyAuthComp);
		proxyAuthButton.setEnabled(!selected);

		setProxyAuth(proxyAuthButton.getSelection());

	}

	public void setProxyAuth(boolean selected) {

		proxyAuthButton.setSelection(selected);
		proxyAuthButton.setEnabled(!systemProxyButton.getSelection());
		if (!selected) {
			oldProxyUsername = proxyUserNameEditor.getStringValue();
			oldProxyPassword = ((StringFieldEditor) proxyPasswordEditor).getStringValue();
			proxyUserNameEditor.setStringValue(null);
			((StringFieldEditor) proxyPasswordEditor).setStringValue(null);
		} else {
			proxyUserNameEditor.setStringValue(oldProxyUsername);
			proxyPasswordEditor.setStringValue(oldProxyPassword);
		}

		proxyUserNameEditor.setEnabled(selected && !systemProxyButton.getSelection(), proxyAuthComp);
		proxyPasswordEditor.setEnabled(selected && !systemProxyButton.getSelection(), proxyAuthComp);
	}

	protected abstract void createAdditionalControls(Composite parent);

	protected abstract void validateSettings();

	protected abstract boolean isValidUrl(String name);

	/* Public for testing. */
	public static String stripSlashes(String url) {
		StringBuilder sb = new StringBuilder(url.trim());
		while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/') {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public String getRepositoryLabel() {
		return repositoryLabelEditor.getStringValue();
	}

	public String getServerUrl() {
		return stripSlashes(serverUrlCombo.getText());
	}

	public String getUserName() {
		return repositoryUserNameEditor.getStringValue();
	}

	public String getPassword() {
		return repositoryPasswordEditor.getStringValue();
	}

	public String getHttpAuthUserId() {
		if (needsHttpAuth()) {
			return httpAuthUserNameEditor.getStringValue();
		} else {
			return "";
		}
	}

	public String getHttpAuthPassword() {
		if (needsHttpAuth()) {
			return httpAuthPasswordEditor.getStringValue();
		} else {
			return "";
		}
	}

	public String getProxyHostname() {
		if (needsProxy()) {
			return proxyHostnameEditor.getStringValue();
		} else {
			return "";
		}
	}

	public String getProxyPort() {
		if (needsProxy()) {
			return proxyPortEditor.getStringValue();
		} else {
			return "";
		}
	}

	public Boolean getUseDefaultProxy() {
		if (needsProxy()) {
			return systemProxyButton.getSelection();
		} else {
			return true;
		}
	}

	public String getProxyUsername() {
		if (needsProxy()) {
			return proxyUserNameEditor.getStringValue();
		} else {
			return "";
		}
	}

	public String getProxyPassword() {
		if (needsProxy()) {
			return proxyPasswordEditor.getStringValue();
		} else {
			return "";
		}
	}

	public void init(IWorkbench workbench) {
		// ignore
	}

	public boolean isAnonymousAccess() {
		return "".equals(getUserName()) && "".equals(getPassword());
	}

	/**
	 * Exposes StringFieldEditor.refreshValidState()
	 * 
	 * TODO: is there a better way?
	 */
	private static class RepositoryStringFieldEditor extends StringFieldEditor {
		public RepositoryStringFieldEditor(String name, String labelText, int style, Composite parent) {
			super(name, labelText, style, parent);
		}

		@Override
		public void refreshValidState() {
			try {
				super.refreshValidState();
			} catch (Exception e) {
				MylarStatusHandler.log(e, "problem refreshing password field");
			}
		}

		@Override
		public Text getTextControl() {
			return super.getTextControl();
		}

	}

	@Override
	public boolean isPageComplete() {
		boolean isComplete = false;

		String url = getServerUrl();
		isComplete = isUniqueUrl(url) && isValidUrl(url);
		if (systemProxyButton != null && proxyHostnameEditor != null) {
			if (isComplete && !getUseDefaultProxy()) {
				isComplete = isValidUrl(getProxyHostname());
			}
		}
		return isComplete;
	}

	protected boolean isUniqueUrl(String urlString) {
		if (!urlString.equals(originalUrl)) {
			if (repositoryUrls == null) {
				List<TaskRepository> repositories = TasksUiPlugin.getRepositoryManager().getAllRepositories();
				repositoryUrls = new HashSet<String>(repositories.size());
				for (TaskRepository repository : repositories) {
					repositoryUrls.add(repository.getUrl());
				}
			}

			if (repositoryUrls.contains(urlString)) {
				setErrorMessage("Repository already exists.");
				return false;
			}
		}
		setErrorMessage(null);
		return true;
	}

	public void setRepository(TaskRepository repository) {
		this.repository = repository;
	}

	public void setVersion(String previousVersion) {
		if (previousVersion == null) {
			serverVersion = TaskRepository.NO_VERSION_SPECIFIED;
		} else {
			serverVersion = previousVersion;
		}
	}

	public String getVersion() {
		return serverVersion;
	}

	public TaskRepository getRepository() {
		return repository;
	}

	public String getCharacterEncoding() {
		if (defaultEncoding == null) {
			return null;
		}

		if (defaultEncoding.getSelection()) {
			return TaskRepository.DEFAULT_CHARACTER_ENCODING;
		} else {
			if (otherEncodingCombo.getSelectionIndex() > -1) {
				return otherEncodingCombo.getItem(otherEncodingCombo.getSelectionIndex());
			} else {
				return TaskRepository.DEFAULT_CHARACTER_ENCODING;
			}
		}
	}

	// public String getTimeZoneId() {
	// return (timeZonesCombo != null) ?
	// timeZonesCombo.getItem(timeZonesCombo.getSelectionIndex()) : null;
	// }

	public TaskRepository createTaskRepository() {
		// TaskRepository repository = new
		// TaskRepository(connector.getRepositoryType(), getServerUrl(),
		// getVersion(),
		// getCharacterEncoding(), getTimeZoneId());

		TaskRepository repository = new TaskRepository(connector.getRepositoryType(), getServerUrl(), getVersion(),
				getCharacterEncoding(), "");
		repository.setRepositoryLabel(getRepositoryLabel());
		repository.setAuthenticationCredentials(getUserName(), getPassword());

		repository.setProperty(TaskRepository.AUTH_HTTP_USERNAME, getHttpAuthUserId());
		repository.setProperty(TaskRepository.AUTH_HTTP_PASSWORD, getHttpAuthPassword());

		repository.setProperty(TaskRepository.PROXY_USEDEFAULT, String.valueOf(getUseDefaultProxy()));
		repository.setProperty(TaskRepository.PROXY_HOSTNAME, getProxyHostname());
		repository.setProperty(TaskRepository.PROXY_PORT, getProxyPort());

		if (getProxyUsername() != null && getProxyPassword() != null && getProxyUsername().length() > 0
				&& getProxyPassword().length() > 0) {
			repository.setProxyAuthenticationCredentials(getProxyUsername(), getProxyPassword());
		}
		// repository.setProperty(TaskRepository.PROXY_USERNAME,
		// getProxyUsername());

		// repository.setProperty(TaskRepository.PROXY_PASSWORD,
		// getProxyPassword());

		// repository.setProperty(TaskRepository.PROXY_USERNAME,
		// getHttpAuthUserId());
		// repository.setProperty(TaskRepository.PROXY_PASSWORD,
		// getHttpAuthPassword());

		updateProperties(repository);
		return repository;
	}

	public AbstractRepositoryConnector getConnector() {
		return connector;
	}

	public boolean needsEncoding() {
		return needsEncoding;
	}

	public boolean needsTimeZone() {
		return needsTimeZone;
	}

	public boolean needsAnonymousLogin() {
		return needsAnonymousLogin;
	}

	public void setNeedsEncoding(boolean needsEncoding) {
		this.needsEncoding = needsEncoding;
	}

	public void setNeedsTimeZone(boolean needsTimeZone) {
		this.needsTimeZone = needsTimeZone;
	}

	public boolean needsHttpAuth() {
		return this.needsHttpAuth;
	}

	public void setNeedsHttpAuth(boolean needsHttpAuth) {
		this.needsHttpAuth = needsHttpAuth;
	}

	public void setNeedsProxy(boolean needsProxy) {
		this.needsProxy = needsProxy;
	}

	public boolean needsProxy() {
		return this.needsProxy;
	}

	public void setNeedsAnonymousLogin(boolean needsAnonymousLogin) {
		this.needsAnonymousLogin = needsAnonymousLogin;
	}

	public void updateProperties(TaskRepository repository) {
		// none
	}

	/** for testing */
	public void setUrl(String url) {
		serverUrlCombo.setText(url);
	}

	/** for testing */
	public void setUserId(String id) {
		repositoryUserNameEditor.setStringValue(id);
	}

	/** for testing */
	public void setPassword(String pass) {
		repositoryPasswordEditor.setStringValue(pass);
	}
}