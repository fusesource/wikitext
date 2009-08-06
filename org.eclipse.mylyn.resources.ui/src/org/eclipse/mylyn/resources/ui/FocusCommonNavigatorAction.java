/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.resources.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.context.ui.AbstractAutoFocusViewAction;
import org.eclipse.mylyn.context.ui.InterestFilter;
import org.eclipse.mylyn.internal.context.ui.ContextUiPlugin;
import org.eclipse.mylyn.internal.resources.ui.ResourcesUiBridgePlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.internal.navigator.NavigatorContentService;
import org.eclipse.ui.internal.navigator.actions.LinkEditorAction;
import org.eclipse.ui.internal.navigator.filters.CommonFilterDescriptor;
import org.eclipse.ui.internal.navigator.filters.CommonFilterDescriptorManager;
import org.eclipse.ui.internal.navigator.filters.CoreExpressionFilter;
import org.eclipse.ui.internal.navigator.filters.SelectFiltersAction;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.ILinkHelper;

/**
 * @author Mik Kersten
 * @since 3.0
 */
public abstract class FocusCommonNavigatorAction extends AbstractAutoFocusViewAction {

	private Object linkService;

	private Method linkServiceMethod;

	private boolean resolveFailed;

	private CommonNavigator commonNavigator;

	private CommonFilterDescriptor[] filterDescriptors;

	private Field filterExpressionField1;

	private Field filterExpressionField2;

	public FocusCommonNavigatorAction(InterestFilter interestFilter, boolean manageViewer, boolean manageFilters,
			boolean manageLinking) {
		super(interestFilter, manageViewer, manageFilters, manageLinking);
	}

	@Override
	protected boolean installInterestFilter(StructuredViewer viewer) {
		if (commonNavigator == null) {
			commonNavigator = (CommonNavigator) super.getPartForAction();
		}

		try {
			// XXX: reflection
			Class<?> clazz2 = CoreExpressionFilter.class;
			filterExpressionField1 = clazz2.getDeclaredField("filterExpression"); //$NON-NLS-1$
			filterExpressionField1.setAccessible(true);

			Class<?> clazz1 = CommonFilterDescriptor.class;
			filterExpressionField2 = clazz1.getDeclaredField("filterExpression"); //$NON-NLS-1$
			filterExpressionField2.setAccessible(true);
		} catch (Exception e) {
			StatusHandler.log(new Status(IStatus.ERROR, ResourcesUiBridgePlugin.ID_PLUGIN,
					"Could not determine filter", e)); //$NON-NLS-1$
		}

		filterDescriptors = CommonFilterDescriptorManager.getInstance().findVisibleFilters(
				commonNavigator.getNavigatorContentService());

		return super.installInterestFilter(viewer);
	}

	@Override
	protected ISelection resolveSelection(IEditorPart editor, ITextSelection changedSelection, StructuredViewer viewer)
			throws CoreException {
		if (resolveFailed) {
			return null;
		}
		if (linkServiceMethod == null) {
			// TODO e3.5 replace with call to CommonNavigator.getLinkHelperService()
			try {
				try {
					// e3.5: get helper from common navigator
					Method method = CommonNavigator.class.getDeclaredMethod("getLinkHelperService"); //$NON-NLS-1$
					method.setAccessible(true);
					linkService = method.invoke(commonNavigator);
				} catch (NoSuchMethodException e) {
					// e3.3, e3.4: instantiate helper
					Class<?> clazz = Class.forName("org.eclipse.ui.internal.navigator.extensions.LinkHelperService"); //$NON-NLS-1$
					Constructor<?> constructor = clazz.getConstructor(NavigatorContentService.class);
					linkService = constructor.newInstance((NavigatorContentService) commonNavigator.getCommonViewer()
							.getNavigatorContentService());
				}
				linkServiceMethod = linkService.getClass().getDeclaredMethod("getLinkHelpersFor", IEditorInput.class); //$NON-NLS-1$
			} catch (Throwable e) {
				resolveFailed = true;
				StatusHandler.log(new Status(IStatus.ERROR, ResourcesUiBridgePlugin.ID_PLUGIN,
						"Initialization of LinkHelperService failed", e)); //$NON-NLS-1$
			}
		}

		IEditorInput input = editor.getEditorInput();
		// TODO e3.5 replace with call to linkService.getLinkHelpersFor(editor.getEditorInput());
		ILinkHelper[] helpers;
		try {
			helpers = (ILinkHelper[]) linkServiceMethod.invoke(linkService, editor.getEditorInput());
		} catch (Exception e) {
			return null;
		}

		IStructuredSelection selection = StructuredSelection.EMPTY;
		IStructuredSelection newSelection = StructuredSelection.EMPTY;

		for (ILinkHelper helper : helpers) {
			selection = helper.findSelection(input);
			if (selection != null && !selection.isEmpty()) {
				newSelection = mergeSelection(newSelection, selection);
			}
		}
		if (!newSelection.isEmpty()) {
			return newSelection;
		}
		return null;
	}

	@Override
	protected void select(StructuredViewer viewer, ISelection toSelect) {
		if (commonNavigator == null) {
			commonNavigator = (CommonNavigator) super.getPartForAction();
		}
		if (commonNavigator != null) {
			commonNavigator.selectReveal(toSelect);
		}
	}

	// TODO: should have better way of doing this
	@Override
	protected void setManualFilteringAndLinkingEnabled(boolean on) {
		IViewPart part = super.getPartForAction();
		if (part instanceof CommonNavigator) {
			for (IContributionItem item : ((CommonNavigator) part).getViewSite()
					.getActionBars()
					.getToolBarManager()
					.getItems()) {
				if (item instanceof ActionContributionItem) {
					ActionContributionItem actionItem = (ActionContributionItem) item;
					if (actionItem.getAction() instanceof LinkEditorAction) {
						actionItem.getAction().setEnabled(on);
					}
				}
			}
			for (IContributionItem item : ((CommonNavigator) part).getViewSite()
					.getActionBars()
					.getMenuManager()
					.getItems()) {
				if (item instanceof ActionContributionItem) {
					ActionContributionItem actionItem = (ActionContributionItem) item;
					if (actionItem.getAction() instanceof SelectFiltersAction) {
						actionItem.getAction().setEnabled(on);
					}
				}
			}
		}
	}

	@Override
	protected void setDefaultLinkingEnabled(boolean on) {
		IViewPart part = super.getPartForAction();
		if (part instanceof CommonNavigator) {
			((CommonNavigator) part).setLinkingEnabled(on);
		}
	}

	@Override
	protected boolean isDefaultLinkingEnabled() {
		IViewPart part = super.getPartForAction();
		if (part instanceof CommonNavigator) {
			return ((CommonNavigator) part).isLinkingEnabled();
		}
		return false;
	}

	@Override
	protected boolean isPreservedFilter(ViewerFilter filter) {
		if (filter instanceof CoreExpressionFilter) {
			CoreExpressionFilter expressionFilter = (CoreExpressionFilter) filter;

			Set<String> preservedIds = ContextUiPlugin.getDefault().getPreservedFilterIds(viewPart.getSite().getId());
			if (!preservedIds.isEmpty()) {
				try {
					Expression expression2 = (Expression) filterExpressionField1.get(expressionFilter);

					for (CommonFilterDescriptor commonFilterDescriptor : filterDescriptors) {
						if (preservedIds.contains(commonFilterDescriptor.getId())) {
							Expression expression1 = (Expression) filterExpressionField2.get(commonFilterDescriptor);
							if (expression1 != null && expression1.equals(expression2)) {
								return true;
							}
						}
					}
				} catch (IllegalArgumentException e) {
					StatusHandler.log(new Status(IStatus.ERROR, ResourcesUiBridgePlugin.ID_PLUGIN,
							"Could not determine filter", e)); //$NON-NLS-1$
				} catch (IllegalAccessException e) {
					StatusHandler.log(new Status(IStatus.ERROR, ResourcesUiBridgePlugin.ID_PLUGIN,
							"Could not determine filter", e)); //$NON-NLS-1$
				}
			}
		}
		return false;
	}

	/**
	 * Copied from
	 * 
	 * @{link LinkEditorAction}
	 */
	@SuppressWarnings("unchecked")
	private IStructuredSelection mergeSelection(IStructuredSelection aBase, IStructuredSelection aSelectionToAppend) {
		if (aBase == null || aBase.isEmpty()) {
			return (aSelectionToAppend != null) ? aSelectionToAppend : StructuredSelection.EMPTY;
		} else if (aSelectionToAppend == null || aSelectionToAppend.isEmpty()) {
			return aBase;
		} else {
			List newItems = new ArrayList(aBase.toList());
			newItems.addAll(aSelectionToAppend.toList());
			return new StructuredSelection(newItems);
		}
	}

}