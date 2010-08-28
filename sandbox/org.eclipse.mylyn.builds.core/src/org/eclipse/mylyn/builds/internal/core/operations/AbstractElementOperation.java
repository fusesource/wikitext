/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.builds.internal.core.operations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.builds.core.IBuildElement;
import org.eclipse.mylyn.builds.internal.core.BuildsCorePlugin;
import org.eclipse.mylyn.commons.core.IOperationMonitor;

/**
 * @author Steffen Pingel
 */
public abstract class AbstractElementOperation<T extends IBuildElement> extends AbstractOperation {

	public AbstractElementOperation(IOperationService service) {
		super(service);
	}

	protected abstract BuildJob doCreateJob(T element);

	protected List<T> doInitInput() {
		final AtomicReference<List<T>> input = new AtomicReference<List<T>>();
		getService().getRealm().syncExec(new Runnable() {
			public void run() {
				List<T> servers = doSyncInitInput();
				register(servers);
				input.set(servers);
			}

		});
		return input.get();
	}

	protected abstract List<T> doSyncInitInput();

	public void execute() {
		getService().getScheduler().schedule(init());
	}

	public List<BuildJob> init() {
		List<T> input = doInitInput();

		List<BuildJob> jobs = new ArrayList<BuildJob>(input.size());
		for (final T element : input) {
			BuildJob job = doCreateJob(element);
			connect(job, element);
			jobs.add(job);
		}
		return jobs;
	}

	public IStatus doExecute(IOperationMonitor progress) {
		List<BuildJob> jobs = init();
		MultiStatus result = new MultiStatus(BuildsCorePlugin.ID_PLUGIN, 0, "Operation failed", null);
		progress.beginTask("", jobs.size()); //$NON-NLS-1$
		try {
			for (BuildJob job : jobs) {
				IStatus status = job.run(progress.newChild(1));
				handleResult(job);
				final IBuildElement element = (IBuildElement) job.getAdapter(IBuildElement.class);
				if (element != null) {
					getService().getRealm().asyncExec(new Runnable() {
						public void run() {
							unregister(element);
						}
					});
				}
				if (status.getSeverity() == IStatus.CANCEL) {
					return Status.CANCEL_STATUS;
				} else if (!status.isOK()) {
					result.add(status);
				}
			}
		} finally {
			getService().getRealm().asyncExec(new Runnable() {
				public void run() {
					unregisterAll();
				}
			});
		}
		return result;
	}

}
