/*******************************************************************************
 * Copyright 2005, CHISEL Group, University of Victoria, Victoria, BC, Canada.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The Chisel Group, University of Victoria
 *******************************************************************************/
package org.eclipse.mylar.zest.core.viewers;

import org.eclipse.jface.viewers.IStructuredContentProvider;
/**
 * 
 * @author irbull
 *
 */
public interface IGraphEntityContentProvider extends IStructuredContentProvider {
	
	public Object[] getElements(Object inputElement);
	
	/**
	 * Gets the elements this object is connected to
	 * @param entity
	 * @return
	 */
	public Object[] getConnectedTo( Object entity );
	
	public double getWeight( Object entity1, Object entity2 );
}
