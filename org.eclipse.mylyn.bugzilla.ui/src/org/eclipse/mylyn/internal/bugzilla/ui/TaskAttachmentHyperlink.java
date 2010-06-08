/*******************************************************************************
 * Copyright (c) 2009, 2010 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.ui;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.mylyn.internal.bugzilla.core.IBugzillaConstants;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;

/**
 * @since 3.2
 */
public final class TaskAttachmentHyperlink implements IHyperlink {

	private final IRegion region;

	private final TaskRepository repository;

	private final String attachmentId;

	public TaskAttachmentHyperlink(IRegion region, TaskRepository repository, String attachmentId) {
		Assert.isNotNull(repository);
		this.region = region;
		this.repository = repository;
		this.attachmentId = attachmentId;
	}

	public IRegion getHyperlinkRegion() {
		return region;
	}

	public String getHyperlinkText() {
		return MessageFormat.format(Messages.TaskAttachmentHyperlink_Open_Attachment_X_in_X, attachmentId,
				repository.getRepositoryLabel());
	}

	public String getTypeLabel() {
		return null;
	}

	public void open() {
		String url = repository.getUrl() + IBugzillaConstants.URL_GET_ATTACHMENT_SUFFIX + attachmentId;
		TasksUiUtil.openUrl(url);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attachmentId == null) ? 0 : attachmentId.hashCode());
		result = prime * result + ((region == null) ? 0 : region.hashCode());
		result = prime * result + ((repository == null) ? 0 : repository.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TaskAttachmentHyperlink other = (TaskAttachmentHyperlink) obj;
		if (attachmentId == null) {
			if (other.attachmentId != null) {
				return false;
			}
		} else if (!attachmentId.equals(other.attachmentId)) {
			return false;
		}
		if (region == null) {
			if (other.region != null) {
				return false;
			}
		} else if (!region.equals(other.region)) {
			return false;
		}
		if (repository == null) {
			if (other.repository != null) {
				return false;
			}
		} else if (!repository.equals(other.repository)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "TaskAttachmentHyperlink [attachmentId=" + attachmentId + ", region=" + region + ", repository=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ repository + "]"; //$NON-NLS-1$
	}

}
