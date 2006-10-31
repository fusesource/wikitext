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

package org.eclipse.mylar.tasks.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.mylar.context.core.MylarStatusHandler;
import org.eclipse.mylar.internal.tasks.core.TaskRepositoriesExternalizer;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class TaskRepositoryManager {

	public static final String OLD_REPOSITORIES_FILE = "repositories.xml";

	public static final String DEFAULT_REPOSITORIES_FILE = "repositories.xml.zip";
	
	public static final String PREF_REPOSITORIES = "org.eclipse.mylar.tasklist.repositories.";

	private Map<String, AbstractRepositoryConnector> repositoryConnectors = new HashMap<String, AbstractRepositoryConnector>();

	private Map<String, Set<TaskRepository>> repositoryMap = new HashMap<String, Set<TaskRepository>>();

	private Set<ITaskRepositoryListener> listeners = new HashSet<ITaskRepositoryListener>();

	private Set<TaskRepository> orphanedRepositories = new HashSet<TaskRepository>();

	public static final String MESSAGE_NO_REPOSITORY = "No repository available, please add one using the Task Repositories view.";

	public static final String PREFIX_LOCAL = "local-";

	private TaskRepositoriesExternalizer externalizer = new TaskRepositoriesExternalizer();

	private TaskList taskList;
	
	public TaskRepositoryManager(TaskList taskList) {
		this.taskList = taskList;
	}

	public Collection<AbstractRepositoryConnector> getRepositoryConnectors() {
		return Collections.unmodifiableCollection(repositoryConnectors.values());
	}

	public AbstractRepositoryConnector getRepositoryConnector(String kind) {
		return repositoryConnectors.get(kind);
	}
	
	public AbstractRepositoryConnector getRepositoryConnector(AbstractRepositoryTask task) {
		return getRepositoryConnector(task.getRepositoryKind());
	}

	public void addRepositoryConnector(AbstractRepositoryConnector repositoryConnector) {
		if (!repositoryConnectors.values().contains(repositoryConnector)) {
			repositoryConnector.init(taskList);
			repositoryConnectors.put(repositoryConnector.getRepositoryType(), repositoryConnector);
		}
	}

	public void addRepository(TaskRepository repository, String repositoryFilePath) {
		Set<TaskRepository> repositories;
		if (!repositoryMap.containsKey(repository.getKind())) {
			repositories = new HashSet<TaskRepository>();
			repositoryMap.put(repository.getKind(), repositories);
		} else {
			repositories = repositoryMap.get(repository.getKind());
		}
		repositories.add(repository);
		saveRepositories(repositoryFilePath);
		for (ITaskRepositoryListener listener : listeners) {
			listener.repositoryAdded(repository);
		}
	}

	public void removeRepository(TaskRepository repository, String repositoryFilePath) {
		Set<TaskRepository> repositories = repositoryMap.get(repository.getKind());
		if (repositories != null) {
			repository.flushAuthenticationCredentials();
			repositories.remove(repository);
		}
		saveRepositories(repositoryFilePath);
		for (ITaskRepositoryListener listener : listeners) {
			listener.repositoryRemoved(repository);
		}
	}

	public void addListener(ITaskRepositoryListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ITaskRepositoryListener listener) {
		listeners.remove(listener);
	}

	public TaskRepository getRepository(String kind, String urlString) {
		if (repositoryMap.containsKey(kind)) {
			for (TaskRepository repository : repositoryMap.get(kind)) {
				if (repository.getUrl().equals(urlString)) {
					return repository;
				}
			}
		}
		return null;
	}
	
	/**
	 * @return first repository that matches the given url
	 */
	public TaskRepository getRepository(String urlString) {
		for (String kind: repositoryMap.keySet()) {
			for (TaskRepository repository : repositoryMap.get(kind)) {
				if (repository.getUrl().equals(urlString)) {
					return repository;
				}
			}
		}		
		return null;
	}

	/**
	 * @return the first connector to accept the URL
	 */
	public AbstractRepositoryConnector getRepositoryForTaskUrl(String url) {
		for (AbstractRepositoryConnector connector : getRepositoryConnectors()) {
			if (connector.getRepositoryUrlFromTaskUrl(url) != null) {
				return connector;
			}
		}
		return null;
	}

	public Set<TaskRepository> getRepositories(String kind) {
		if (repositoryMap.containsKey(kind)) {
			return repositoryMap.get(kind);
		} else {
			return Collections.emptySet();
		}
	}

	public List<TaskRepository> getAllRepositories() {
		List<TaskRepository> repositories = new ArrayList<TaskRepository>();
		for (AbstractRepositoryConnector repositoryConnector : repositoryConnectors.values()) {
			if (repositoryMap.containsKey(repositoryConnector.getRepositoryType())) {
				repositories.addAll(repositoryMap.get(repositoryConnector.getRepositoryType()));
			}
		}
		return repositories;
	}

	public TaskRepository getRepositoryForActiveTask(String repositoryKind, TaskList taskList) {
		List<ITask> activeTasks = taskList.getActiveTasks();
		if (activeTasks.size() == 1) {
			ITask activeTask = activeTasks.get(0);
			if (activeTask instanceof AbstractRepositoryTask) {
				String repositoryUrl = AbstractRepositoryTask.getRepositoryUrl(activeTask.getHandleIdentifier());
				for (TaskRepository repository : getRepositories(repositoryKind)) {
					if (repository.getUrl().equals(repositoryUrl)) {
						return repository;
					}
				}
			}
		}
		return null;
	}

	/**
	 * TODO: implement default support, this just returns first found
	 */
	public TaskRepository getDefaultRepository(String kind) {
		// HACK: returns first repository found
		if (repositoryMap.containsKey(kind)) {
			for (TaskRepository repository : repositoryMap.get(kind)) {
				return repository;
			}
		} else {
			Collection<Set<TaskRepository>> values = repositoryMap.values();
			if (!values.isEmpty()) {
				Set<TaskRepository> repoistorySet = values.iterator().next();
				return (TaskRepository) repoistorySet.iterator().next();
			}
		}
		return null;
	}

	public Map<String, Set<TaskRepository>> readRepositories(String repositoriesFilePath) {

		repositoryMap.clear();
		orphanedRepositories.clear();
		loadRepositories(repositoriesFilePath);

		for (ITaskRepositoryListener listener : listeners) {
			listener.repositoriesRead();
		}
		return repositoryMap;
	}

	private void loadRepositories(String repositoriesFilePath) {
		try {
//			String dataDirectory = TasksUiPlugin.getDefault().getDataDirectory();
			File repositoriesFile = new File(repositoriesFilePath);

			// Will only load repositories for which a connector exists
			for (AbstractRepositoryConnector repositoryConnector : repositoryConnectors.values()) {
				repositoryMap.put(repositoryConnector.getRepositoryType(), new HashSet<TaskRepository>());
			}
			if (repositoriesFile.exists()) {
				Set<TaskRepository> repositories = externalizer.readRepositoriesFromXML(repositoriesFile);
				if (repositories != null && repositories.size() > 0) {
					for (TaskRepository repository : repositories) {
						if (repositoryMap.containsKey(repository.getKind())) {
							repositoryMap.get(repository.getKind()).add(repository);
						} else {
							orphanedRepositories.add(repository);
						}
					}
				}
			}
		} catch (Throwable t) {
			MylarStatusHandler.fail(t, "could not load repositories", false);
		}
	}

	/**
	 * for testing purposes
	 */
	public void setVersion(TaskRepository repository, String version, String repositoriesFilePath) {
		repository.setVersion(version);
		saveRepositories(repositoriesFilePath);
	}

	/**
	 * for testing purposes
	 */
	public void setEncoding(TaskRepository repository, String encoding, String repositoriesFilePath) {
		repository.setCharacterEncoding(encoding);
		saveRepositories(repositoriesFilePath);
	}

	/**
	 * for testing purposes
	 */
	public void setTimeZoneId(TaskRepository repository, String timeZoneId, String repositoriesFilePath) {
		repository.setTimeZoneId(timeZoneId);
		saveRepositories(repositoriesFilePath);
	}

	public void setSyncTime(TaskRepository repository, String syncTime, String repositoriesFilePath) {
		repository.setSyncTimeStamp(syncTime);
		saveRepositories(repositoriesFilePath);

		// String prefIdSyncTime = repository.getUrl() + PROPERTY_DELIM +
		// PROPERTY_SYNCTIMESTAMP;
		// if (repository.getSyncTimeStamp() != null) {
		// MylarTaskListPlugin.getMylarCorePrefs().setValue(prefIdSyncTime,
		// repository.getSyncTimeStamp());
		// }
	}

	public boolean saveRepositories(String destinationPath) {
		if (!Platform.isRunning()) {// || TasksUiPlugin.getDefault() == null) {
			return false;
		}
		Set<TaskRepository> repositoriesToWrite = new HashSet<TaskRepository>(getAllRepositories());
		// if for some reason a repository is added/changed to equal one in the
		// orphaned set the orphan is discarded
		for (TaskRepository repository : orphanedRepositories) {
			if (!repositoriesToWrite.contains(repository)) {
				repositoriesToWrite.add(repository);
			}
		}

		try {
//			String dataDirectory = TasksUiPlugin.getDefault().getDataDirectory();
//			File repositoriesFile = new File(dataDirectory + File.separator + TasksUiPlugin.DEFAULT_REPOSITORIES_FILE);
			File repositoriesFile = new File(destinationPath);
			externalizer.writeRepositoriesToXML(repositoriesToWrite, repositoriesFile);
		} catch (Throwable t) {
			MylarStatusHandler.fail(t, "could not save repositories", false);
			return false;
		}
		return true;
	}

	/**
	 * For testing.
	 */
	public void clearRepositories(String repositoriesFilePath) {
		repositoryMap.clear();
		orphanedRepositories.clear();
		saveRepositories(repositoriesFilePath);
		// for (AbstractRepositoryConnector repositoryConnector :
		// repositoryConnectors.values()) {
		// String prefId = PREF_REPOSITORIES +
		// repositoryConnector.getRepositoryType();
		// MylarTaskListPlugin.getMylarCorePrefs().setValue(prefId, "");
		// }
	}

	public void notifyRepositorySettingsChagned(TaskRepository repository) {
		for (ITaskRepositoryListener listener : listeners) {
			listener.repositorySettingsChanged(repository);
		}
	}
}
