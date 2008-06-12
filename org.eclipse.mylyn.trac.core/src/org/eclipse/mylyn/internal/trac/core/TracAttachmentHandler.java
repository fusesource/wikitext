/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.trac.core;

import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.internal.tasks.core.deprecated.AbstractAttachmentHandler;
import org.eclipse.mylyn.internal.tasks.core.deprecated.ITaskAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryAttachment;
import org.eclipse.mylyn.internal.tasks.core.deprecated.RepositoryTaskAttribute;
import org.eclipse.mylyn.internal.trac.core.model.TracTicket;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;

/**
 * @author Steffen Pingel
 */
public class TracAttachmentHandler extends AbstractAttachmentHandler {

	private final TracRepositoryConnector connector;

	public TracAttachmentHandler(TracRepositoryConnector connector) {
		this.connector = connector;
	}

	@Override
	public InputStream getAttachmentAsStream(TaskRepository repository, RepositoryAttachment attachment,
			IProgressMonitor monitor) throws CoreException {
		String filename = attachment.getAttributeValue(RepositoryTaskAttribute.ATTACHMENT_FILENAME);
		if (filename == null) {
			throw new CoreException(new RepositoryStatus(repository.getRepositoryUrl(), IStatus.ERROR, TracCorePlugin.ID_PLUGIN,
					RepositoryStatus.ERROR_REPOSITORY, "Attachment download from " + repository.getRepositoryUrl()
							+ " failed, missing attachment filename."));
		}

		try {
			ITracClient client = connector.getClientManager().getTracClient(repository);
			int id = Integer.parseInt(attachment.getTaskId());
			return client.getAttachmentData(id, filename, monitor);
		} catch (Exception e) {
			throw new CoreException(TracCorePlugin.toStatus(e, repository));
		}
	}

	@Override
	public void uploadAttachment(TaskRepository repository, ITask task, ITaskAttachment attachment,
			String comment, IProgressMonitor monitor) throws CoreException {
		if (!TracRepositoryConnector.hasAttachmentSupport(repository, task)) {
			throw new CoreException(new RepositoryStatus(repository.getRepositoryUrl(), IStatus.INFO, TracCorePlugin.ID_PLUGIN,
					RepositoryStatus.ERROR_REPOSITORY, "Attachments are not supported by this repository access type"));
		}

		monitor.beginTask("Uploading attachment", IProgressMonitor.UNKNOWN);
		try {
			try {
				ITracClient client = connector.getClientManager().getTracClient(repository);
				int id = Integer.parseInt(task.getTaskId());
				client.putAttachmentData(id, attachment.getFilename(), attachment.getDescription(),
						attachment.createInputStream(), monitor);
				if (comment != null && comment.length() > 0) {
					TracTicket ticket = new TracTicket(id);
					client.updateTicket(ticket, comment, monitor);
				}
			} catch (Exception e) {
				throw new CoreException(TracCorePlugin.toStatus(e, repository));
			}
		} finally {
			monitor.done();
		}
	}

	@Override
	public boolean canDownloadAttachment(TaskRepository repository, ITask task) {
		if (repository == null) {
			return false;
		}
		return TracRepositoryConnector.hasAttachmentSupport(repository, task);
	}

	@Override
	public boolean canUploadAttachment(TaskRepository repository, ITask task) {
		if (repository == null) {
			return false;
		}
		return TracRepositoryConnector.hasAttachmentSupport(repository, task);
	}

	@Override
	public boolean canDeprecate(TaskRepository repository, RepositoryAttachment attachment) {
		return false;
	}

	@Override
	public void updateAttachment(TaskRepository repository, RepositoryAttachment attachment) throws CoreException {
		throw new UnsupportedOperationException();
	}

}
