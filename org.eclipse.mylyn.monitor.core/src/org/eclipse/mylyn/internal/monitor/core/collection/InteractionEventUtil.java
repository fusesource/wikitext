/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.monitor.core.collection;

import org.eclipse.mylyn.monitor.core.InteractionEvent;

/**
 * @author Mik Kersten
 */
public class InteractionEventUtil {

	public static String getCleanOriginId(InteractionEvent event) {
		String cleanOriginId = "";
		String originId = event.getOriginId();

		if (event.getKind().equals(InteractionEvent.Kind.COMMAND)) {
			for (int i = 0; i < originId.length(); i++) {
				char curChar = originId.charAt(i);
				if (!(curChar == '&')) {
					if (Character.getType(curChar) == Character.CONTROL) {
						cleanOriginId = cleanOriginId.concat(" ");
					} else {
						cleanOriginId = cleanOriginId.concat(String.valueOf(curChar));
					}
				}
			}
			return cleanOriginId;
		} else {
			return originId;
		}
	}

}
