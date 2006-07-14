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

package org.eclipse.mylar.tasklist.tests;

import java.util.Arrays;

import junit.framework.TestCase;

import org.eclipse.mylar.internal.tasks.ui.ui.views.TaskKeyComparator;

/**
 * @author Eugene Kuleshov https://bugs.eclipse.org/bugs/show_bug.cgi?id=129511
 * @author Mik Kersten
 */
public class TaskKeyComparatorTest extends TestCase {

	public void testPatterns() {
		comparisonCheck("", new String[] { null, null, "" });
		comparisonCheck(" ", new String[] { null, null, " " });
		comparisonCheck("aa", new String[] { null, null, "aa" });
		comparisonCheck("11", new String[] { "", "11", "" });
		comparisonCheck("11 aa", new String[] { "", "11", " aa" });
		comparisonCheck(" 11 aa", new String[] { null, null, " 11 aa" });
		comparisonCheck("aa11 bb", new String[] { "aa", "11", " bb" });
		comparisonCheck("aa-11 bb", new String[] { "aa-", "11", " bb" });
		comparisonCheck("aa 11 bb", new String[] { null, null, "aa 11 bb" });
		comparisonCheck("aa bb 11 cc", new String[] { null, null, "aa bb 11 cc" });

		comparisonCheck("aa", "aa", 0);
		comparisonCheck("aa", "bb", -1);
		comparisonCheck("bb", "aa", 1);

		comparisonCheck("aa11", "aa11", 0);
		comparisonCheck("aa11", "aa12", -1);
		comparisonCheck("aa12", "aa11", 1);

		comparisonCheck("aa1", "aa11", -1);
		comparisonCheck("aa1", "aa2", -1);
		comparisonCheck("aa1", "aa21", -1);

		comparisonCheck("aa1 aaa", "aa1 aaa", 0);
		comparisonCheck("aa1 aaa", "aa1 bbb", -1);
		comparisonCheck("aa1 bbb", "aa11 aaa", -1);
	}

	private void comparisonCheck(String s, String[] exptecation) {
		String[] res = new TaskKeyComparator().split(s);
		assertTrue("Invalid " + Arrays.asList(res) + " " + Arrays.asList(exptecation), Arrays.equals(res, exptecation));
	}

	public void comparisonCheck(String s1, String s2, int n) {
		assertEquals(n, new TaskKeyComparator().compare(s1, s2));
	}

}
