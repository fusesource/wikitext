/*******************************************************************************
 * Copyright 2005-2007, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.mylar.zest.examples.uml;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.mylar.zest.core.widgets.Graph;
import org.eclipse.mylar.zest.core.widgets.GraphConnection;
import org.eclipse.mylar.zest.core.widgets.GraphNode;
import org.eclipse.mylar.zest.core.widgets.ZestStyles;
import org.eclipse.mylar.zest.layouts.LayoutStyles;
import org.eclipse.mylar.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Adds a selection listener to the nodes to tell when a selection event has
 * happened.
 * 
 * @author Ian Bull
 * 
 */
public class UMLExample {
	public static Color classColor = null;

	public static IFigure createClassFigure1() {
		Font classFont = new Font(null, "Arial", 12, SWT.BOLD);
		Label classLabel1 = new Label("Table", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("class_obj.gif")));
		classLabel1.setFont(classFont);

		UMLClassFigure classFigure = new UMLClassFigure(classLabel1);
		Label attribute1 = new Label("columns: Column[]", new Image(Display.getCurrent(), UMLClassFigure.class
				.getResourceAsStream("field_private_obj.gif")));

		Label attribute2 = new Label("rows: Row[]", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("field_private_obj.gif")));

		Label method1 = new Label("getColumns(): Column[]", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("methpub_obj.gif")));
		Label method2 = new Label("getRows(): Row[]", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("methpub_obj.gif")));
		classFigure.getAttributesCompartment().add(attribute1);
		classFigure.getAttributesCompartment().add(attribute2);
		classFigure.getMethodsCompartment().add(method1);
		classFigure.getMethodsCompartment().add(method2);
		classFigure.setSize(-1, -1);

		return classFigure;
	}

	public static IFigure createClassFigure2() {
		Font classFont = new Font(null, "Arial", 12, SWT.BOLD);
		Label classLabel2 = new Label("Column", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("class_obj.gif")));
		classLabel2.setFont(classFont);

		UMLClassFigure classFigure = new UMLClassFigure(classLabel2);
		Label attribute3 = new Label("columnID: int", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("field_private_obj.gif")));
		Label attribute4 = new Label("items: List", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("field_private_obj.gif")));

		Label method3 = new Label("getColumnID(): int", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("methpub_obj.gif")));
		Label method4 = new Label("getItems(): List", new Image(Display.getDefault(), UMLClassFigure.class
				.getResourceAsStream("methpub_obj.gif")));

		classFigure.getAttributesCompartment().add(attribute3);
		classFigure.getAttributesCompartment().add(attribute4);
		classFigure.getMethodsCompartment().add(method3);
		classFigure.getMethodsCompartment().add(method4);
		classFigure.setSize(-1, -1);

		return classFigure;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Display d = new Display();
		Shell shell = new Shell(d);
		shell.setLayout(new FillLayout());
		shell.setSize(400, 400);
		classColor = new Color(null, 255, 255, 206);

		Graph g = new Graph(shell, SWT.NONE);
		g.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
		GraphNode n1 = new GraphNode(g, SWT.NONE);
		GraphNode n2 = new GraphNode(g, SWT.NONE);

		n1.setCustomFigure(createClassFigure1());
		n2.setCustomFigure(createClassFigure2());

		new GraphConnection(g, SWT.NONE, n1, n2);

		g.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);

		shell.open();
		while (!shell.isDisposed()) {
			while (!d.readAndDispatch()) {
				d.sleep();
			}
		}

	}
}
