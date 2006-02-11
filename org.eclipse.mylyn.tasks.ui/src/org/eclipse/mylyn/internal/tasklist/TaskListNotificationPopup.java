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

package org.eclipse.mylar.internal.tasklist;

import java.util.List;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author Rob Elves
 */
public class TaskListNotificationPopup extends PopupDialog {

	static boolean takeFocusOnOpen = false;

	static boolean persistBounds = false;

	static boolean showDialogMenu = false;

	static boolean showPersistAction = false;

	static String titleText = null;

	static String infoText = null;

	private FormToolkit toolkit;

	private Form form;

	private Rectangle bounds;

	List<ITaskListNotification> notifications;

	public TaskListNotificationPopup(Shell parent) {
		super(parent, PopupDialog.INFOPOPUP_SHELLSTYLE, takeFocusOnOpen, persistBounds, showDialogMenu,
				showPersistAction, titleText, infoText);

	}

	protected void adjustBounds() {
		initializeBounds();
	}

	public void setContents(List<ITaskListNotification> notifications) {
		this.notifications = notifications;
	}

	// try it with eclipse forms
	protected final Control createDialogArea(final Composite parent) {

		// parent.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		GridLayout parentLayout = new GridLayout();
		parentLayout.marginHeight = 0;
		parentLayout.marginWidth = 0;

		parent.setLayout(parentLayout);
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);

		form.getBody().setBackground(parent.getBackground());

		GridLayout formLayout = new GridLayout();
		parentLayout.marginHeight = 0;
		parentLayout.marginWidth = 0;
		form.getBody().setLayout(formLayout);

		Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR);

		// section.setText("Notifications:");
		section.setLayout(new GridLayout());

		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());
		for (final ITaskListNotification notification : notifications) {
			ImageHyperlink link = toolkit.createImageHyperlink(sectionClient, SWT.WRAP | SWT.TOP);
			link.setBackground(parent.getBackground());
			link.setFont(parent.getFont());
			link.setText(notification.getDescription());
			link.setImage(notification.getNotificationIcon());
			link.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					notification.setNotified(true);
					notification.openResource();
				}
			});
		}

		section.setClient(sectionClient);

		Composite buttonsComposite = toolkit.createComposite(sectionClient);
		buttonsComposite.setLayout(new GridLayout(2, false));

		Button buttonOpenAll = toolkit.createButton(buttonsComposite, "Open All", SWT.NONE);
		buttonOpenAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (ITaskListNotification notification : notifications) {
					notification.setNotified(true);
					notification.openResource();
				}
				close();
			}
		});

		Button buttonDismiss = toolkit.createButton(buttonsComposite, "Dismiss", SWT.NONE);
		buttonDismiss.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (ITaskListNotification notification : notifications) {
					notification.setNotified(true);
				}
				close();
			}
		});
		// toolkit.paintBordersFor(parent);
		form.pack();
		return parent;
	}

	/**
	 * Initialize the shell's bounds.
	 */
	public void initializeBounds() {
		// if we don't remember the dialog bounds then reset
		// to be the defaults (behaves like inplace outline view)
		Rectangle oldBounds = restoreBounds();
		if (oldBounds != null) {
			getShell().setBounds(oldBounds);
			return;
		}
		Point size = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Point location = getDefaultLocation(size);
		getShell().setBounds(new Rectangle(location.x, location.y, size.x, size.y));
	}

	private Point getDefaultLocation(Point initialSize) {
		Monitor monitor = getShell().getDisplay().getPrimaryMonitor();
		if (getShell() != null) {
			monitor = getShell().getMonitor();
		}

		Rectangle monitorBounds = monitor.getClientArea();
		Point centerPoint;
		if (getShell() != null) {
			centerPoint = Geometry.centerPoint(getShell().getBounds());
		} else {
			centerPoint = Geometry.centerPoint(monitorBounds);
		}

		return new Point(centerPoint.x - (initialSize.x / 2), Math.max(monitorBounds.y, Math.min(centerPoint.y
				- (initialSize.y * 2 / 3), monitorBounds.y + monitorBounds.height - initialSize.y)));
	}

	private Rectangle restoreBounds() {
		// bounds = new Rectangle(0,0,150,100);
		bounds = form.getBounds();// new Rectangle(0, 0, 250, 200);

		Rectangle maxBounds = null;
		if (getShell() != null && !getShell().isDisposed())
			maxBounds = getShell().getDisplay().getClientArea();
		else {
			// fallback
			Display display = Display.getCurrent();
			if (display == null)
				display = Display.getDefault();
			if (display != null && !display.isDisposed())
				maxBounds = display.getBounds();
		}

		if (bounds.width > -1 && bounds.height > -1) {
			if (maxBounds != null) {
				bounds.width = Math.min(bounds.width, maxBounds.width);
				bounds.height = Math.min(bounds.height, maxBounds.height);
			}
			// Enforce an absolute minimal size
			bounds.width = Math.max(bounds.width, 30);
			bounds.height = Math.max(bounds.height, 30);
		}

		if (bounds.x > -1 && bounds.y > -1 && maxBounds != null) {
			bounds.x = Math.max(bounds.x, maxBounds.x);
			bounds.y = Math.max(bounds.y, maxBounds.y);

			if (bounds.width > -1 && bounds.height > -1) {
				bounds.x = maxBounds.width - bounds.width;
				bounds.y = maxBounds.height - bounds.height;
			}
		}
		return bounds;
	}

}
