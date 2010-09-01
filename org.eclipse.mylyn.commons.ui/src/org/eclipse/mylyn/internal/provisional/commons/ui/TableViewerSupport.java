/*******************************************************************************
 * Copyright (c) 2010 Frank Becker and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *     Tasktop Technologies - improvements
 *******************************************************************************/

package org.eclipse.mylyn.internal.provisional.commons.ui;

import java.io.File;

import org.eclipse.jface.layout.AbstractColumnLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * @author Frank Becker
 * @author Steffen Pingel
 */
public class TableViewerSupport extends AbstractColumnViewerSupport {
	private final Table table;

	public TableViewerSupport(TableViewer viewer, File stateFile) {
		super(viewer, stateFile);
		this.table = viewer.getTable();
		initializeViewerSupport();
	}

	public TableViewerSupport(TableViewer viewer, File stateFile, boolean[] defaultVisibilities) {
		super(viewer, stateFile, defaultVisibilities);
		this.table = viewer.getTable();
		initializeViewerSupport();
	}

	@Override
	Item[] getColumns() {
		return table.getColumns();
	}

	@Override
	AbstractColumnLayout getColumnLayout() {
		if (table.getLayout() instanceof AbstractColumnLayout) {
			return (AbstractColumnLayout) table.getLayout();
		} else if (table.getParent().getLayout() instanceof AbstractColumnLayout) {
			return (AbstractColumnLayout) table.getParent().getLayout();
		} else {
			return null;
		}
	}

	@Override
	Rectangle getClientArea() {
		return table.getClientArea();
	}

	@Override
	int getHeaderHeight() {
		return table.getHeaderHeight();
	}

	@Override
	int[] getColumnOrder() {
		return table.getColumnOrder();
	}

	@Override
	void setColumnOrder(int[] order) {
		table.setColumnOrder(order);
	}

	@Override
	int getSortDirection() {
		return table.getSortDirection();
	}

	@Override
	void setSortDirection(int direction) {
		table.setSortDirection(direction);
	}

	@Override
	Item getSortColumn() {
		return table.getSortColumn();
	}

	@Override
	void setSortColumn(Item column) {
		if (column instanceof TableColumn) {
			table.setSortColumn(((TableColumn) column));
		}
	}

	@Override
	void addColumnSelectionListener(Item column, SelectionListener selectionListener) {
		if (column instanceof TableColumn) {
			((TableColumn) column).addSelectionListener(selectionListener);
		}
	}

	@Override
	int getColumnWidth(Item column) {
		if (column instanceof TableColumn) {
			return ((TableColumn) column).getWidth();
		}
		return 0;
	}

	@Override
	void setColumnResizable(Item column, boolean resizable) {
		if (column instanceof TableColumn) {
			((TableColumn) column).setResizable(resizable);
		}
	}

	@Override
	void setColumnWidth(Item column, int width) {
		if (column instanceof TableColumn) {
			((TableColumn) column).setWidth(width);
		}
	}

	@Override
	Item getColumn(int index) {
		return table.getColumn(index);
	}

	@Override
	int getColumnIndexOf(Item column) {
		if (column instanceof TableColumn) {
			return table.indexOf(((TableColumn) column));
		}
		return 0;
	}

}
