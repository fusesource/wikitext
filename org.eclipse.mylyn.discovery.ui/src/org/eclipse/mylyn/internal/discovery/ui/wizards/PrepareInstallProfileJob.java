/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.internal.discovery.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.equinox.internal.provisional.p2.core.Version;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfileRegistry;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IProvidedCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.repository.IMetadataRepository;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.MatchQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Query;
import org.eclipse.equinox.internal.provisional.p2.ui.actions.InstallAction;
import org.eclipse.equinox.internal.provisional.p2.ui.operations.ProvisioningUtil;
import org.eclipse.equinox.internal.provisional.p2.ui.policy.Policy;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.mylyn.internal.discovery.core.model.ConnectorDescriptor;
import org.eclipse.mylyn.internal.discovery.ui.DiscoveryUi;
import org.eclipse.mylyn.internal.discovery.ui.util.DiscoveryUiUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.SelectionProviderAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 * A job that configures a p2 {@link #getInstallAction() install action} for installing one or more
 * {@link ConnectorDescriptor connectors}. The bulk of the installation work is done by p2; this class just sets up the
 * p2 repository metadata and selects the appropriate features to install. After running the job the
 * {@link #getInstallAction() install action} must be run to perform the installation.
 * 
 * @author David Green
 */
@SuppressWarnings("restriction")
public class PrepareInstallProfileJob implements IRunnableWithProgress {

	private static final String P2_FEATURE_GROUP_SUFFIX = ".feature.group"; //$NON-NLS-1$

	private final List<ConnectorDescriptor> installableConnectors;

	private InstallAction installAction;

	public PrepareInstallProfileJob(List<ConnectorDescriptor> installableConnectors) {
		if (installableConnectors == null || installableConnectors.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.installableConnectors = new ArrayList<ConnectorDescriptor>(installableConnectors);
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			doRun(monitor);
			if (monitor.isCanceled()) {
				throw new InterruptedException();
			}
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
	}

	public void doRun(IProgressMonitor monitor) throws CoreException {
		final int totalWork = installableConnectors.size() * 4;
		monitor.beginTask(Messages.InstallConnectorsJob_task_configuring, totalWork);
		try {
			final String profileId = computeProfileId();
			// verify that we can resolve hostnames
			// this is a pre-emptive check so that we can provide better error handling
			// than that provided by P2.
			{
				Set<String> hostnames = new HashSet<String>();
				for (ConnectorDescriptor descriptor : installableConnectors) {
					URL url = new URL(descriptor.getSiteUrl());
					String host = url.getHost();
					if (host != null && host.length() > 0 && hostnames.add(host)) {
						try {
							InetAddress.getByName(host);
						} catch (UnknownHostException e) {
							throw new CoreException(
									new Status(
											IStatus.ERROR,
											DiscoveryUi.ID_PLUGIN,
											NLS.bind(
													"Error resolving hostname {1} for '{0}': please check your Internet connection and try again.",
													descriptor.getName(), host), e));
						}
					}
				}
			}

			// Tell p2 that it's okay to use these repositories
			Set<URL> repositoryURLs = new HashSet<URL>();
			for (ConnectorDescriptor descriptor : installableConnectors) {
				URL url = new URL(descriptor.getSiteUrl());
				if (repositoryURLs.add(url)) {
					if (monitor.isCanceled()) {
						return;
					}
					ProvisioningUtil.addMetadataRepository(url.toURI(), true);
					ProvisioningUtil.addArtifactRepository(url.toURI(), true);
					ProvisioningUtil.setColocatedRepositoryEnablement(url.toURI(), true);

				}
				monitor.worked(1);
			}
			if (repositoryURLs.isEmpty()) {
				// should never happen
				throw new IllegalStateException();
			}
			// Fetch p2's metadata for these repositories
			List<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
			final Map<IMetadataRepository, URL> repositoryToURL = new HashMap<IMetadataRepository, URL>();
			{
				int unit = installableConnectors.size() / repositoryURLs.size();
				for (URL updateSiteUrl : repositoryURLs) {
					if (monitor.isCanceled()) {
						return;
					}
					IMetadataRepository repository = ProvisioningUtil.loadMetadataRepository(updateSiteUrl.toURI(),
							new SubProgressMonitor(monitor, unit));
					repositories.add(repository);
					repositoryToURL.put(repository, updateSiteUrl);
				}
			}
			// Perform a query to get the installable units.  This causes p2 to determine what features are available
			// in each repository.  We select installable units by matching both the feature id and the repository; it
			// is possible though unlikely that the same feature id is available from more than one of the selected
			// repositories, and we must ensure that the user gets the one that they asked for.
			final List<IInstallableUnit> installableUnits = new ArrayList<IInstallableUnit>();
			{
				int unit = installableConnectors.size() / repositories.size();

				for (final IMetadataRepository repository : repositories) {
					if (monitor.isCanceled()) {
						return;
					}
					URL repositoryUrl = repositoryToURL.get(repository);
					final Set<String> installableUnitIdsThisRepository = new HashSet<String>();
					// determine all installable units for this repository
					for (ConnectorDescriptor descriptor : installableConnectors) {
						try {
							if (repositoryUrl.equals(new URL(descriptor.getSiteUrl()))) {
								installableUnitIdsThisRepository.add(descriptor.getId());
							}
						} catch (MalformedURLException e) {
							// will never happen, ignore
						}
					}
					Collector collector = new Collector();
					Query query = new MatchQuery() {
						@Override
						public boolean isMatch(Object object) {
							if (!(object instanceof IInstallableUnit)) {
								return false;
							}
							IInstallableUnit candidate = (IInstallableUnit) object;

							if ("true".equalsIgnoreCase(candidate.getProperty("org.eclipse.equinox.p2.type.group"))) { //$NON-NLS-1$ //$NON-NLS-2$
								String id = candidate.getId();
								if (isQualifyingFeature(installableUnitIdsThisRepository, id)) {
									IProvidedCapability[] providedCapabilities = candidate.getProvidedCapabilities();
									if (providedCapabilities != null && providedCapabilities.length > 0) {
										for (IProvidedCapability capability : providedCapabilities) {
											if ("org.eclipse.equinox.p2.iu".equals(capability.getNamespace())) { //$NON-NLS-1$
												String name = capability.getName();
												if (isQualifyingFeature(installableUnitIdsThisRepository, name)) {
													return true;
												}
											}
										}
									}
								}
							}
							return false;
						}

						private boolean isQualifyingFeature(final Set<String> installableUnitIdsThisRepository,
								String id) {
							return id.endsWith(P2_FEATURE_GROUP_SUFFIX)
									&& installableUnitIdsThisRepository.contains(id.substring(0,
											id.indexOf(P2_FEATURE_GROUP_SUFFIX)));
						}
					};
					repository.query(query, collector, new SubProgressMonitor(monitor, unit));

					addAll(installableUnits, collector);
				}
			}

			// filter those installable units that have a duplicate in the list with a higher version number.
			// it's possible that some repositories will host multiple versions of a particular feature.  we assume
			// that the user wants the highest version.
			{
				Map<String, Version> symbolicNameToVersion = new HashMap<String, Version>();
				for (IInstallableUnit unit : installableUnits) {
					Version version = symbolicNameToVersion.get(unit.getId());
					if (version == null || version.compareTo(unit.getVersion()) == -1) {
						symbolicNameToVersion.put(unit.getId(), unit.getVersion());
					}
				}
				if (symbolicNameToVersion.size() != installableUnits.size()) {
					for (IInstallableUnit unit : new ArrayList<IInstallableUnit>(installableUnits)) {
						Version version = symbolicNameToVersion.get(unit.getId());
						if (!version.equals(unit.getVersion())) {
							installableUnits.remove(unit);
						}
					}
				}
			}

			// Verify that we found what we were looking for: it's possible that we have connector descriptors
			// that are no longer available on their respective sites.  In that case we must inform the user.
			// (Unfortunately this is the earliest point at which we can know) 
			if (installableUnits.size() < installableConnectors.size()) {
				// at least one selected connector could not be found in a repository
				Set<String> foundIds = new HashSet<String>();
				for (IInstallableUnit unit : installableUnits) {
					String id = unit.getId();
					if (id.endsWith(P2_FEATURE_GROUP_SUFFIX)) {
						id = id.substring(0, id.indexOf(P2_FEATURE_GROUP_SUFFIX));
					}
					foundIds.add(id);
				}

				final String notFound;
				{
					String temp = ""; //$NON-NLS-1$
					for (ConnectorDescriptor descriptor : installableConnectors) {
						if (!foundIds.contains(descriptor.getId())) {
							if (temp.length() > 0) {
								temp += Messages.InstallConnectorsJob_commaSeparator;
							}
							temp += descriptor.getName();
						}
					}
					notFound = temp;
				}
				boolean proceed = false;
				if (!installableUnits.isEmpty()) {
					// instead of aborting here we ask the user if they wish to proceed anyways
					final boolean[] okayToProceed = new boolean[1];
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							okayToProceed[0] = MessageDialog.openQuestion(DiscoveryUiUtil.getShell(),
									Messages.InstallConnectorsJob_questionProceed, NLS.bind(
											Messages.InstallConnectorsJob_questionProceed_long,
											new Object[] { notFound }));
						}
					});
					proceed = okayToProceed[0];
				}
				if (!proceed) {
					throw new CoreException(new Status(IStatus.ERROR, DiscoveryUi.ID_PLUGIN, NLS.bind(
							Messages.InstallConnectorsJob_connectorsNotAvailable, new Object[] { notFound }), null));
				}
			} else if (installableUnits.size() > installableConnectors.size()) {
				// should never ever happen
				throw new IllegalStateException();
			}

			installAction = new InstallAction(Policy.getDefault(), new SelectionProviderAdapter(
					new StructuredSelection(installableUnits)), profileId);

			monitor.done();
		} catch (URISyntaxException e) {
			// should never happen, since we already validated URLs.
			throw new CoreException(new Status(IStatus.ERROR, DiscoveryUi.ID_PLUGIN,
					Messages.InstallConnectorsJob_unexpectedError_url, e));
		} catch (MalformedURLException e) {
			// should never happen, since we already validated URLs.
			throw new CoreException(new Status(IStatus.ERROR, DiscoveryUi.ID_PLUGIN,
					Messages.InstallConnectorsJob_unexpectedError_url, e));
		} finally {
			monitor.done();
		}
	}

	@SuppressWarnings("unchecked")
	private boolean addAll(final List<IInstallableUnit> installableUnits, Collector collector) {
		return installableUnits.addAll(collector.toCollection());
	}

	private String computeProfileId() throws CoreException {
		IProfile profile = ProvisioningUtil.getProfile(IProfileRegistry.SELF);
		if (profile != null) {
			return profile.getProfileId();
		}
		IProfile[] profiles = ProvisioningUtil.getProfiles();
		if (profiles.length > 0) {
			return profiles[0].getProfileId();
		}
		throw new CoreException(new Status(IStatus.ERROR, DiscoveryUi.ID_PLUGIN,
				Messages.InstallConnectorsJob_profileProblem, null));
	}

	public InstallAction getInstallAction() {
		return installAction;
	}
}
