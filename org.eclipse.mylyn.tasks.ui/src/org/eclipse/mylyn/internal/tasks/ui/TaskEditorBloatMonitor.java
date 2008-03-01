package org.eclipse.mylyn.internal.tasks.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.mylyn.monitor.ui.AbstractEditorTracker;
import org.eclipse.mylyn.tasks.core.AbstractTask.RepositoryTaskSyncState;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * @author Mik Kersten
 */
public class TaskEditorBloatMonitor extends AbstractEditorTracker {

	private final int MAX_EDITORS = 12;

	@Override
	protected void editorBroughtToTop(IEditorPart part) {
		// ignore
	}

	@Override
	public void editorOpened(IEditorPart editorPartOpened) {
		IWorkbenchPage page = editorPartOpened.getSite().getPage();
		List<IEditorReference> toClose = new ArrayList<IEditorReference>();
		int totalTaskEditors = 0;
		for (IEditorReference editorReference : page.getEditorReferences()) {
			try {
				if (editorReference.getEditorInput() instanceof TaskEditorInput) {
					totalTaskEditors++;
				}
			} catch (PartInitException e) {
				// ignore
			}
		}

		if (totalTaskEditors > MAX_EDITORS) {
			for (IEditorReference editorReference : page.getEditorReferences()) {
				try {
					if (editorReference.getEditorInput() instanceof TaskEditorInput) {
						TaskEditorInput taskEditorInput = (TaskEditorInput) editorReference.getEditorInput();
						TaskEditor taskEditor = (TaskEditor) editorReference.getEditor(false);
						if (taskEditor == null) {
							toClose.add(editorReference);
						} else if (!taskEditor.equals(editorPartOpened)
								&& !taskEditor.isDirty()
								&& RepositoryTaskSyncState.SYNCHRONIZED.equals(taskEditorInput.getTask()
										.getSynchronizationState())) {
							toClose.add(editorReference);
						}
					}
					if ((totalTaskEditors - toClose.size()) < MAX_EDITORS) {
						break;
					}
				} catch (PartInitException e) {
					// ignore
				}
			}
		}

		if (toClose.size() > 0) {
			page.closeEditors(toClose.toArray(new IEditorReference[toClose.size()]), true);
		}
	}

	@Override
	public void editorClosed(IEditorPart editorPart) {
		// ignore
	}
}
