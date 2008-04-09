/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.resources.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylyn.context.core.AbstractContextStructureBridge;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionElement;
import org.eclipse.mylyn.context.ui.AbstractContextUiBridge;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.internal.context.ui.ContextUiPlugin;
import org.eclipse.mylyn.internal.context.ui.ContextUiPrefContstants;
import org.eclipse.mylyn.monitor.core.CoreUtil;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.eclipse.mylyn.monitor.ui.AbstractEditorTracker;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * @author Mik Kersten
 */
public class EditorInteractionMonitor extends AbstractEditorTracker {

	public static final String SOURCE_ID = "org.eclipse.mylyn.ide.editor.tracker.interest";

	@Override
	protected void editorBroughtToTop(IEditorPart part) {
		Object object = part.getEditorInput().getAdapter(IResource.class);
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault().getStructureBridge(resource);
			InteractionEvent selectionEvent = new InteractionEvent(InteractionEvent.Kind.SELECTION,
					bridge.getContentType(), bridge.getHandleIdentifier(resource), part.getSite().getId());
			ContextCore.getContextManager().processInteractionEvent(selectionEvent);
		}
	}

	@Override
	public void editorOpened(IEditorPart editorPartOpened) {
		IWorkbenchPage page = editorPartOpened.getSite().getPage();
		List<IEditorReference> toClose = new ArrayList<IEditorReference>();
		for (IEditorReference editorReference : page.getEditorReferences()) {
			try {
				IInteractionElement element = null;
				Object adapter;
				adapter = editorReference.getEditorInput().getAdapter(IResource.class);
				if (adapter instanceof IFile) {
					String handle = ContextCorePlugin.getDefault().getStructureBridge(adapter).getHandleIdentifier(
							adapter);
					element = ContextCore.getContextManager().getElement(handle);
				}
				if (!ContextCore.getContextManager().isContextCapturePaused() && element != null
						&& !element.getInterest().isInteresting() && !isSameEditor(editorPartOpened, editorReference)) {
					toClose.add(editorReference);
				}
			} catch (PartInitException e) {
				// ignore
			}
		}
		if (toClose.size() > 0) {
			page.closeEditors(toClose.toArray(new IEditorReference[toClose.size()]), true);
		}
	}

	private boolean isSameEditor(IEditorPart editorPart1, IEditorReference editorReference2) throws PartInitException {
		if (editorPart1 == null || editorReference2 == null) {
			return false;
		} else {
			return editorPart1.getEditorInput().equals(editorReference2.getEditorInput());
		}
	}

	@Override
	public void editorClosed(IEditorPart editorPart) {
		if (PlatformUI.getWorkbench().isClosing()) {
			return;
		} else if (ContextUiPlugin.getDefault().getPreferenceStore().getBoolean(
				ContextUiPrefContstants.AUTO_MANAGE_EDITOR_CLOSE_ACTION)
//				&& !Workbench.getInstance().getPreferenceStore().getBoolean(IPreferenceConstants.REUSE_EDITORS_BOOLEAN)
				&& !otherEditorsOpenForResource(editorPart)
				&& !(editorPart instanceof CompareEditor)
				&& !(editorPart instanceof IContextAwareEditor)) {

			if (ContextCore.getContextManager().isContextActive()
					&& ContextUiPlugin.getDefault().getPreferenceStore().getBoolean(
							ContextUiPrefContstants.AUTO_MANAGE_EDITOR_CLOSE_WARNING)) {
				try {
					if (!CoreUtil.TEST_MODE) {
						MessageDialog.openInformation(
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
								"Mylyn",
								"Closing a file automatically removes it from the Task Context. "
										+ "This is recommended in order to make the open editors match "
										+ "the focused views. It can be disabled via Preferences -> Mylyn -> Context.\n\n"
										+ "This dialog will not show again.");
					}
				} finally {
					ContextUiPlugin.getDefault().getPreferenceStore().setValue(
							ContextUiPrefContstants.AUTO_MANAGE_EDITOR_CLOSE_WARNING, false);
				}
			}

			IInteractionElement element = null;
			AbstractContextUiBridge uiBridge = ContextUiPlugin.getDefault().getUiBridgeForEditor(editorPart);
			Object object = uiBridge.getObjectForTextSelection(null, editorPart);
			if (object != null) {
				AbstractContextStructureBridge bridge = ContextCorePlugin.getDefault().getStructureBridge(object);
				element = ContextCore.getContextManager().getElement(bridge.getHandleIdentifier(object));
			}
			// TODO: probably should be refactored into delegation
			if (element == null) {
				Object adapter = editorPart.getEditorInput().getAdapter(IResource.class);
				if (adapter instanceof IResource) {
					IResource resource = (IResource) adapter;
					AbstractContextStructureBridge resourceBridge = ContextCorePlugin.getDefault().getStructureBridge(
							resource);
					element = ContextCore.getContextManager().getElement(
							resourceBridge.getHandleIdentifier(resource));
				}
			}
			if (element != null) {
				ContextCore.getContextManager().manipulateInterestForElement(element, false, false, false,
						SOURCE_ID);
			}
		}
	}

	private boolean otherEditorsOpenForResource(IEditorPart editorPart) {
		Object adapter = editorPart.getEditorInput().getAdapter(IResource.class);
		if (adapter instanceof IResource) {
			IResource resource = (IResource) adapter;
			IWorkbenchPage page = editorPart.getSite().getPage();
			for (IEditorReference editorReference : page.getEditorReferences()) {
				try {
					Object otherAdapter;
					otherAdapter = editorReference.getEditorInput().getAdapter(IResource.class);
					if (otherAdapter instanceof IResource && otherAdapter.equals(resource)) {
						return true;
					}
				} catch (PartInitException e) {
					// ignore
				}
			}
		}
		return false;
	}

}
