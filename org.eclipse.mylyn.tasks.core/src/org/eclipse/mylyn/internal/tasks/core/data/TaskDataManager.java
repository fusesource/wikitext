/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.core.data;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.internal.tasks.core.ITaskListRunnable;
import org.eclipse.mylyn.internal.tasks.core.ITasksCoreConstants;
import org.eclipse.mylyn.internal.tasks.core.TaskDataStorageManager;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.monitor.core.StatusHandler;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.AbstractTask;
import org.eclipse.mylyn.tasks.core.ITaskRepositoryManager;
import org.eclipse.mylyn.tasks.core.RepositoryTaskAttribute;
import org.eclipse.mylyn.tasks.core.RepositoryTaskData;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.mylyn.tasks.core.data.ITaskDataManager;
import org.eclipse.mylyn.tasks.core.data.ITaskDataWorkingCopy;
import org.eclipse.mylyn.tasks.core.data.TaskData;

/**
 * Encapsulates synchronization policy.
 * 
 * @author Mik Kersten
 * @author Rob Elves
 * @author Steffen Pingel
 */
public final class TaskDataManager implements ITaskDataManager {

	private static final String ENCODING_UTF_8 = "UTF-8";

	private static final String EXTENSION = ".zip";

	private static final String FOLDER_DATA = "tasks";

	private static final String FOLDER_DATA_1_0 = "offline";

	private String dataPath;

	private final ITaskRepositoryManager repositoryManager;

	@Deprecated
	private final TaskDataStorageManager taskDataStorageManager;

	private final TaskDataStore taskDataStore;

	private final TaskList taskList;

	public TaskDataManager(TaskDataStorageManager taskDataManager, TaskDataStore taskDataStore,
			ITaskRepositoryManager repositoryManager, TaskList taskList) {
		this.taskDataStorageManager = taskDataManager;
		this.taskDataStore = taskDataStore;
		this.repositoryManager = repositoryManager;
		this.taskList = taskList;
	}

	/** public for testing purposes */
	@Deprecated
	public boolean checkHasIncoming(AbstractTask repositoryTask, RepositoryTaskData newData) {
		if (repositoryTask.getSynchronizationState() == RepositoryTaskSyncState.INCOMING) {
			return true;
		}

		String lastModified = repositoryTask.getLastReadTimeStamp();
		RepositoryTaskAttribute modifiedDateAttribute = newData.getAttribute(RepositoryTaskAttribute.DATE_MODIFIED);
		if (lastModified != null && modifiedDateAttribute != null && modifiedDateAttribute.getValue() != null) {
			if (lastModified.trim().compareTo(modifiedDateAttribute.getValue().trim()) == 0) {
				// Only set to synchronized state if not in incoming state.
				// Case of incoming->sync handled by markRead upon opening
				// or a forced synchronization on the task only.
				return false;
			}

			Date modifiedDate = newData.getAttributeFactory().getDateForAttributeType(
					RepositoryTaskAttribute.DATE_MODIFIED, modifiedDateAttribute.getValue());
			Date lastModifiedDate = newData.getAttributeFactory().getDateForAttributeType(
					RepositoryTaskAttribute.DATE_MODIFIED, lastModified);
			if (modifiedDate != null && lastModifiedDate != null && modifiedDate.equals(lastModifiedDate)) {
				return false;
			}
		}

		return true;
	}

	public ITaskDataWorkingCopy createWorkingCopy(final AbstractTask task, String kind) throws CoreException {
		final TaskDataState state;
		final File file = getFile(task, kind);
		if (!file.exists()) {
			File oldFile = getFile10(task, kind);
			state = taskDataStore.getTaskDataState(oldFile);
			// save migrated task data right away
			taskDataStore.putTaskData(file, state);
		} else {
			state = taskDataStore.getTaskDataState(file);
		}
		if (state == null) {
			throw new CoreException(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN, "Task data at \"" + file
					+ "\" not found"));
		}
		state.init(this, task);
		state.revert();
		taskList.run(new ITaskListRunnable() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				task.setMarkReadPending(false);
				taskDataStore.putLastRead(file, state.getRepositoryData());
				if (task.getSynchronizationState() == RepositoryTaskSyncState.OUTGOING) {
					task.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);
				} else if (task.getSynchronizationState() == RepositoryTaskSyncState.CONFLICT) {
					task.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
				}
			}
		});
		taskList.notifyTaskChanged(task, false);
		return state;
	}

	public void discardEdits(final AbstractTask task, final String kind) throws CoreException {
		taskList.run(new ITaskListRunnable() {
			public void execute(IProgressMonitor monitor) throws CoreException {
				taskDataStore.discardEdits(getFile(task, kind));
				if (task.getSynchronizationState() == RepositoryTaskSyncState.OUTGOING) {
					task.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);
				} else if (task.getSynchronizationState() == RepositoryTaskSyncState.CONFLICT) {
					task.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
				}
			}
		});
		taskList.notifyTaskChanged(task, true);
	}

	@Deprecated
	public void discardOutgoing(AbstractTask repositoryTask) {
		taskDataStorageManager.discardEdits(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());
		repositoryTask.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);
		taskList.notifyTaskChanged(repositoryTask, true);
	}

	private File findFile(AbstractTask task, String kind) {
		File file = getFile(task, kind);
		if (file.exists()) {
			return file;
		}
		return getFile10(task, kind);
	}

	public String getDataPath() {
		return dataPath;
	}

	private File getFile(AbstractTask task, String kind) {
		try {
			String pathName = task.getConnectorKind() + "-"
					+ URLEncoder.encode(task.getRepositoryUrl(), ENCODING_UTF_8);
			String fileName = kind + "-" + URLEncoder.encode(task.getTaskId(), ENCODING_UTF_8) + EXTENSION;
			File path = new File(dataPath + File.separator + FOLDER_DATA, pathName);
			return new File(path, fileName);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private File getFile10(AbstractTask task, String kind) {
		try {
			String pathName = URLEncoder.encode(task.getRepositoryUrl(), ENCODING_UTF_8);
			String fileName = task.getTaskId() + EXTENSION;
			File path = new File(dataPath + File.separator + FOLDER_DATA_1_0, pathName);
			return new File(path, fileName);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}

	private File getFileAndCreatePath(AbstractTask task, String kind) {
		File file = getFile(task, kind);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		return file;
	}

	public TaskData getTaskData(AbstractTask task, String kind) throws CoreException {
		TaskDataState state = taskDataStore.getTaskDataState(findFile(task, kind));
		if (state == null) {
			return null;
		}
		return state.getRepositoryData();
	}

	public boolean hasTaskData(AbstractTask task, String kind) {
		return getFile(task, kind).exists();
	}

	public void putTaskData(final AbstractTask task, final TaskData taskData, boolean user) throws CoreException {
		final AbstractRepositoryConnector connector = repositoryManager.getRepositoryConnector(task.getConnectorKind());
		final TaskRepository repository = repositoryManager.getRepository(task.getConnectorKind(),
				task.getRepositoryUrl());

		final boolean changed = connector.hasChanged(task, taskData);
		if (changed || user) {
			taskList.run(new ITaskListRunnable() {
				public void execute(IProgressMonitor monitor) throws CoreException {
					if (!taskData.isPartial()) {
						// TODO migrate old task data?
						File file = getFileAndCreatePath(task, task.getConnectorKind());
						taskDataStore.putTaskData(file, taskData, task.isMarkReadPending());
						task.setMarkReadPending(false);
					}

					connector.updateTaskFromTaskData(repository, task, taskData);

					if (changed) {
						RepositoryTaskSyncState state = task.getSynchronizationState();
						switch (state) {
						case OUTGOING:
							state = RepositoryTaskSyncState.CONFLICT;
							break;
						case SYNCHRONIZED:
							state = RepositoryTaskSyncState.INCOMING;
							break;
						}
						task.setSynchronizationState(state);
					}
					task.setStale(false);
					task.setSynchronizing(false);
				}
			});
			taskList.notifyTaskChanged(task, false);
		}
	}

	/**
	 * Saves incoming data and updates task sync state appropriately
	 * 
	 * @return true if call results in change of sync state
	 */
	@Deprecated
	public synchronized boolean saveIncoming(final AbstractTask repositoryTask, final RepositoryTaskData newTaskData,
			boolean forceSync) {
		Assert.isNotNull(newTaskData);
		final RepositoryTaskSyncState startState = repositoryTask.getSynchronizationState();
		RepositoryTaskSyncState status = repositoryTask.getSynchronizationState();

		RepositoryTaskData previousTaskData = taskDataStorageManager.getNewTaskData(repositoryTask.getRepositoryUrl(),
				repositoryTask.getTaskId());

		if (repositoryTask.isSubmitting()) {
			status = RepositoryTaskSyncState.SYNCHRONIZED;
			repositoryTask.setSubmitting(false);
			TaskDataStorageManager dataManager = taskDataStorageManager;
			dataManager.discardEdits(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId());

			taskDataStorageManager.setNewTaskData(newTaskData);
			/**
			 * If we set both so we don't see our own changes
			 * 
			 * @see RepositorySynchronizationManager.setTaskRead(AbstractTask, boolean)
			 */
			// taskDataManager.setOldTaskData(repositoryTask.getHandleIdentifier(),
			// newTaskData);
		} else {

			switch (status) {
			case OUTGOING:
				if (checkHasIncoming(repositoryTask, newTaskData)) {
					status = RepositoryTaskSyncState.CONFLICT;
				}
				taskDataStorageManager.setNewTaskData(newTaskData);
				break;

			case CONFLICT:
				// fall through to INCOMING (conflict implies incoming)
			case INCOMING:
				// only most recent incoming will be displayed if two
				// sequential incoming's /conflicts happen

				taskDataStorageManager.setNewTaskData(newTaskData);
				break;
			case SYNCHRONIZED:
				boolean hasIncoming = checkHasIncoming(repositoryTask, newTaskData);
				if (hasIncoming) {
					status = RepositoryTaskSyncState.INCOMING;
					repositoryTask.setNotified(false);
				}
				if (hasIncoming || previousTaskData == null || forceSync) {
					taskDataStorageManager.setNewTaskData(newTaskData);
				}
				break;
			}
		}
		repositoryTask.setSynchronizationState(status);
		return startState != repositoryTask.getSynchronizationState();
	}

	@Deprecated
	public void saveOffline(AbstractTask task, RepositoryTaskData taskData) {
		taskDataStorageManager.setNewTaskData(taskData);
	}

	/**
	 * @param repositoryTask
	 *            task that changed
	 * @param modifiedAttributes
	 *            attributes that have changed during edit session
	 */
	@Deprecated
	public synchronized void saveOutgoing(AbstractTask repositoryTask, Set<RepositoryTaskAttribute> modifiedAttributes) {
		repositoryTask.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
		taskDataStorageManager.saveEdits(repositoryTask.getRepositoryUrl(), repositoryTask.getTaskId(),
				Collections.unmodifiableSet(modifiedAttributes));
		taskList.notifyTaskChanged(repositoryTask, false);
	}

	public void setDataPath(String dataPath) {
		this.dataPath = dataPath;
	}

	/**
	 * @param task
	 *            repository task to mark as read or unread
	 * @param read
	 *            true to mark as read, false to mark as unread
	 */
	public void setTaskRead(final AbstractTask task, final boolean read) {
		if (!getFile(task, task.getConnectorKind()).exists()) {
			setTaskReadDeprecated(task, read);
			return;
		}

		try {
			taskList.run(new ITaskListRunnable() {
				public void execute(IProgressMonitor monitor) throws CoreException {
					if (read) {
						if (task.getSynchronizationState() == RepositoryTaskSyncState.INCOMING) {
							task.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);
							task.setMarkReadPending(true);
						} else if (task.getSynchronizationState() == RepositoryTaskSyncState.CONFLICT) {
							task.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
							task.setMarkReadPending(true);
						}
					} else {
						if (task.getSynchronizationState() == RepositoryTaskSyncState.SYNCHRONIZED) {
							task.setSynchronizationState(RepositoryTaskSyncState.INCOMING);
							task.setMarkReadPending(false);
						}
					}
				}
			});
		} catch (CoreException e) {
			StatusHandler.log(new Status(IStatus.ERROR, ITasksCoreConstants.ID_PLUGIN,
					"Unexpected error while marking task read", e));
		}
		taskList.notifyTaskChanged(task, false);
	}

	@Deprecated
	private void setTaskReadDeprecated(AbstractTask repositoryTask, boolean read) {
		RepositoryTaskData taskData = taskDataStorageManager.getNewTaskData(repositoryTask.getRepositoryUrl(),
				repositoryTask.getTaskId());

		if (read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.INCOMING)) {
			if (taskData != null && taskData.getLastModified() != null) {
				repositoryTask.setLastReadTimeStamp(taskData.getLastModified());
				taskDataStorageManager.setOldTaskData(taskData);
			}
			repositoryTask.setSynchronizationState(RepositoryTaskSyncState.SYNCHRONIZED);
			taskList.notifyTaskChanged(repositoryTask, false);
		} else if (read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.CONFLICT)) {
			if (taskData != null && taskData.getLastModified() != null) {
				repositoryTask.setLastReadTimeStamp(taskData.getLastModified());
			}
			repositoryTask.setSynchronizationState(RepositoryTaskSyncState.OUTGOING);
			taskList.notifyTaskChanged(repositoryTask, false);
		} else if (read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.SYNCHRONIZED)) {
			if (taskData != null && taskData.getLastModified() != null) {
				repositoryTask.setLastReadTimeStamp(taskData.getLastModified());
				// By setting old every time (and not setting upon submission)
				// we see our changes
				// If condition is enabled and we save old in OUTGOING handler
				// our own changes
				// will not be displayed after submission.
				// if
				// (dataManager.getOldTaskData(repositoryTask.getHandleIdentifier())
				// == null) {
				taskDataStorageManager.setOldTaskData(taskData);
				// }
			}
//			else if (repositoryTask.getLastReadTimeStamp() == null && repositoryTask.isLocal()) {
//				// fall back for cases where the stamp is missing, set bogus date
//				repositoryTask.setLastReadTimeStamp(LocalTask.SYNC_DATE_NOW);
//			}

		} else if (!read && repositoryTask.getSynchronizationState().equals(RepositoryTaskSyncState.SYNCHRONIZED)) {
			repositoryTask.setSynchronizationState(RepositoryTaskSyncState.INCOMING);
			taskList.notifyTaskChanged(repositoryTask, false);
		}

		// for connectors that don't support task data set read date to now (bug#204741)
		if (read && taskData == null && repositoryTask.isLocal()) {
			repositoryTask.setLastReadTimeStamp((new Date()).toString());
		}
	}

	public void putEdits(AbstractTask task, String kind, TaskData editsData) throws CoreException {
		taskDataStore.putEdits(getFile(task, kind), editsData);
	}

	@Deprecated
	public RepositoryTaskData getNewTaskData(String repositoryUrl, String taskId) {
		return taskDataStorageManager.getNewTaskData(repositoryUrl, taskId);
	}

	@Deprecated
	public void setNewTaskData(RepositoryTaskData taskData) {
		taskDataStorageManager.setNewTaskData(taskData);
	}

}
