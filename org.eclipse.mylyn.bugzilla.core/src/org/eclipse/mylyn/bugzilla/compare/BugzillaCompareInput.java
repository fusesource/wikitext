/*******************************************************************************
 * Copyright (c) 2003 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylar.bugzilla.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylar.bugzilla.core.BugReport;


/**
 * A two-way or three-way compare for <code>BugReport</code> objects.
 */
public class BugzillaCompareInput extends CompareEditorInput {

	private boolean threeWay= false;
	private Object root;
	private IStructureComparator ancestor = null;
	private IStructureComparator left = null;
	private IStructureComparator right = null;
	
	/**
	 * Constructor.
	 * 
	 * @param configuration
	 *            The compare configuration used in this compare input.
	 * @see CompareConfiguration
	 */
	public BugzillaCompareInput(CompareConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected Object prepareInput(IProgressMonitor monitor) {
		if (left == null || right == null) {
			return null;
		}
		Differencer d= new Differencer();
		root= d.findDifferences(threeWay, monitor, null, ancestor, left, right);
		return root;
	}

	/**
	 * @return The original object that's to be compared (appears on the top of
	 *         the compare view).
	 */
	public IStructureComparator getAncestor() {
		return ancestor;
	}
	
	/**
	 * Sets the original object that's to be compared (appears on the top of the
	 * compare view).
	 * 
	 * @param newAncestor
	 *            The new original object.
	 */
	public void setAncestor(BugReport newAncestor) {
		threeWay = (newAncestor != null);
		BugzillaStructureCreator structureCreator = new BugzillaStructureCreator();
		ancestor = structureCreator.getStructure(newAncestor);
	}
	
	/**
	 * @return The local object that's to be compared (appears on the left side
	 *         of the compare view).
	 */
	public IStructureComparator getLeft() {
		return left;
	}
	
	/**
	 * Sets the local object that's to be compared (appears on the left side of
	 * the compare view).
	 * 
	 * @param newLeft
	 *            The new local object.
	 */
	public void setLeft(BugReport newLeft) {
		BugzillaStructureCreator structureCreator = new BugzillaStructureCreator();
		left = structureCreator.getStructure(newLeft);
	}
	
	/**
	 * @return The online object that's to be compared (appears on the right
	 *         side of the compare view).
	 */
	public IStructureComparator getRight() {
		return right;
	}
	
	/**
	 * Sets the online object that's to be compared (appears on the right side
	 * of the compare view).
	 * 
	 * @param newRight
	 *            The new online object.
	 */
	public void setRight(BugReport newRight) {
		BugzillaStructureCreator structureCreator = new BugzillaStructureCreator();
		right = structureCreator.getStructure(newRight);
	}
	
	/**
	 * @return <code>true</code> if a three-way comparison is to be done.
	 */
	public boolean isThreeWay() {
		return threeWay;
	}
}
