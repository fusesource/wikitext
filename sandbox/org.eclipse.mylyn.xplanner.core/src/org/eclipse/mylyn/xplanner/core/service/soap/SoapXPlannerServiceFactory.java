/*******************************************************************************
 * Copyright (c) 2007 - 2007 CodeGear and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.mylyn.xplanner.core.service.soap;

import org.eclipse.mylyn.xplanner.core.service.soap.SoapXPlannerService;
import org.eclipse.mylyn.xplanner.core.service.*;

/**
 * @author Ravi Kumar 
 * @author Helen Bershadskaya 
 */
public class SoapXPlannerServiceFactory implements XPlannerServiceFactory {

	public XPlannerService createService(XPlannerClient client) {
		// TODO Auto-generated method stub
		return new SoapXPlannerService(client);
	}

}
