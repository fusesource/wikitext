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

package org.eclipse.mylar.internal.bugzilla.core;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylar.core.MylarStatusHandler;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants.BUGZILLA_OPERATION;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants.BUGZILLA_REPORT_STATUS;
import org.eclipse.mylar.internal.bugzilla.core.IBugzillaConstants.BUGZILLA_RESOLUTION;
import org.eclipse.mylar.tasks.core.AbstractAttributeFactory;
import org.eclipse.mylar.tasks.core.IMylarStatusConstants;
import org.eclipse.mylar.tasks.core.ITaskDataHandler;
import org.eclipse.mylar.tasks.core.RepositoryOperation;
import org.eclipse.mylar.tasks.core.RepositoryTaskData;
import org.eclipse.mylar.tasks.core.TaskRepository;

/**
 * @author Mik Kersten
 * @author Rob Elves
 */
public class BugzillaTaskDataHandler implements ITaskDataHandler {

	private static final String OPERATION_INPUT_ASSIGNED_TO = "assigned_to";

	private static final String OPERATION_INPUT_DUP_ID = "dup_id";

	private static final String OPERATION_OPTION_RESOLUTION = "resolution";

	private static final String OPERATION_LABEL_CLOSE = "Mark as CLOSED";

	private static final String OPERATION_LABEL_VERIFY = "Mark as VERIFIED";

	private static final String OPERATION_LABEL_REOPEN = "Reopen bug";

	private static final String OPERATION_LABEL_REASSIGN_DEFAULT = "Reassign to default assignee";

	private static final String OPERATION_LABEL_REASSIGN = "Reassign to";

	private static final String OPERATION_LABEL_DUPLICATE = "Mark as duplicate of #";

	private static final String OPERATION_LABEL_RESOLVE = "Resolve as";

	private static final String OPERATION_LABEL_ACCEPT = "Accept (change status to ASSIGNED)";



	private AbstractAttributeFactory attributeFactory = new BugzillaAttributeFactory();

	private BugzillaRepositoryConnector connector;

	public BugzillaTaskDataHandler(BugzillaRepositoryConnector connector) {
		this.connector = connector;
	}

	public RepositoryTaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
		try {

			BugzillaClient client = connector.getClientManager().getClient(repository);
			int bugId = Integer.parseInt(taskId);
			RepositoryTaskData taskData;
			try {
				taskData = client.getTaskData(bugId);
			} catch (CoreException e) {
				// TODO: Move retry handling into client
				if (e.getStatus().getCode() == IMylarStatusConstants.REPOSITORY_LOGIN_ERROR) {
					taskData = client.getTaskData(bugId);
				} else {
					throw e;
				}
			}
			if (taskData != null) {
				try {
					configureTaskData(repository, taskData);
				} catch (CoreException ce) {
					// retry since data retrieved may be corrupt
					taskData = client.getTaskData(bugId);
					if (taskData != null) {
						configureTaskData(repository, taskData);
					}
				}
				return taskData;
			}
			return null;

		} catch (IOException e) {
			throw new CoreException(new BugzillaStatus(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID,
					IMylarStatusConstants.IO_ERROR, repository.getUrl(), e));
		}
	}

	public String postTaskData(TaskRepository repository, RepositoryTaskData taskData, IProgressMonitor monitor) throws CoreException {
		try {
			BugzillaClient client = connector.getClientManager().getClient(repository);
			try {
				return client.postTaskData(taskData);
			} catch (CoreException e) {
				// TODO: Move retry handling into client
				if (e.getStatus().getCode() == IMylarStatusConstants.REPOSITORY_LOGIN_ERROR) {
					return client.postTaskData(taskData);
				} else {
					throw e;
				}

			}

		} catch (IOException e) {
			throw new CoreException(new BugzillaStatus(IStatus.ERROR, BugzillaCorePlugin.PLUGIN_ID,
					IMylarStatusConstants.IO_ERROR, repository.getUrl(), e));
		}
	}

	public AbstractAttributeFactory getAttributeFactory(String repositoryUrl, String repositoryKind, String taskKind) {
		// we don't care about the repository information right now
		return attributeFactory;
	}
	
	public AbstractAttributeFactory getAttributeFactory(RepositoryTaskData taskData) {
		return getAttributeFactory(taskData.getRepositoryUrl(), taskData.getRepositoryKind(), taskData.getTaskKind());
	}

	private void configureTaskData(TaskRepository repository, RepositoryTaskData taskData) throws CoreException {
		connector.updateAttributeOptions(repository, taskData);
		addValidOperations(taskData, repository.getUserName());
	}
	
	private void addValidOperations(RepositoryTaskData bugReport, String userName) throws CoreException {
		BUGZILLA_REPORT_STATUS status;
		try {
			status = BUGZILLA_REPORT_STATUS.valueOf(bugReport.getStatus());
		} catch (RuntimeException e) {
			MylarStatusHandler.log(e, "Unrecognized status: " + bugReport.getStatus());
			status = BUGZILLA_REPORT_STATUS.NEW;
		}
		switch (status) {
		case UNCONFIRMED:
		case REOPENED:
		case NEW:
			addOperation(bugReport, BUGZILLA_OPERATION.none, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.accept, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.resolve, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.duplicate, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reassign, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reassignbycomponent, userName);
			break;
		case ASSIGNED:
			addOperation(bugReport, BUGZILLA_OPERATION.none, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.resolve, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.duplicate, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reassign, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reassignbycomponent, userName);
			break;
		case RESOLVED:
			addOperation(bugReport, BUGZILLA_OPERATION.none, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reopen, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.verify, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.close, userName);
			break;
		case CLOSED:
			addOperation(bugReport, BUGZILLA_OPERATION.none, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reopen, userName);
			break;
		case VERIFIED:
			addOperation(bugReport, BUGZILLA_OPERATION.none, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.reopen, userName);
			addOperation(bugReport, BUGZILLA_OPERATION.close, userName);
		}
	}

	private void addOperation(RepositoryTaskData bugReport, BUGZILLA_OPERATION opcode, String userName) {
		RepositoryOperation newOperation = null;
		switch (opcode) {
		case none:
			newOperation = new RepositoryOperation(opcode.toString(), "Leave as " + bugReport.getStatus() + " "
					+ bugReport.getResolution());
			newOperation.setChecked(true);
			break;
		case accept:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_ACCEPT);
			break;
		case resolve:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_RESOLVE);
			newOperation.setUpOptions(OPERATION_OPTION_RESOLUTION);
			for (BUGZILLA_RESOLUTION resolution : BUGZILLA_RESOLUTION.values()) {
				newOperation.addOption(resolution.toString(), resolution.toString());
			}
			break;
		case duplicate:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_DUPLICATE);
			newOperation.setInputName(OPERATION_INPUT_DUP_ID);
			newOperation.setInputValue("");
			break;
		case reassign:
			String localUser = userName;
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_REASSIGN);
			newOperation.setInputName(OPERATION_INPUT_ASSIGNED_TO);
			newOperation.setInputValue(localUser);
			break;
		case reassignbycomponent:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_REASSIGN_DEFAULT);
			break;
		case reopen:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_REOPEN);
			break;
		case verify:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_VERIFY);
			break;
		case close:
			newOperation = new RepositoryOperation(opcode.toString(), OPERATION_LABEL_CLOSE);
			break;
		default:
			break;
		// MylarStatusHandler.log("Unknown bugzilla operation code recieved",
		// BugzillaRepositoryUtil.class);
		}
		if (newOperation != null) {
			bugReport.addOperation(newOperation);
		}
	}

	public boolean initializeTaskData(TaskRepository repository, RepositoryTaskData data, IProgressMonitor monitor)
			throws CoreException {
		// Bugzilla needs a product to create task data
		return false;
	}


}
