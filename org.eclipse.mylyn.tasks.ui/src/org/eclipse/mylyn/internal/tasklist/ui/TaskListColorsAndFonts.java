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

package org.eclipse.mylar.internal.tasklist.ui;

import java.lang.reflect.Field;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

/**
 * @author Mik Kersten
 */
public class TaskListColorsAndFonts {

	public static final Color BACKGROUND_ARCHIVE = new Color(Display.getDefault(), 225, 226, 246);

	public static final Color COLOR_GRAY_LIGHT = new Color(Display.getDefault(), 170, 170, 170);

	public static final Color COLOR_TASK_COMPLETED = new Color(Display.getDefault(), 170, 170, 170);

	public static final Color COLOR_TASK_ACTIVE = new Color(Display.getDefault(), 36, 22, 50);

	public static final Color COLOR_LABEL_CAUTION = new Color(Display.getDefault(), 200, 10, 30);

	public static final Color COLOR_HYPERLINK = new Color(Display.getDefault(), 0, 0, 255);

	public static final Font BOLD = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);

	public static final Font ITALIC = JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);

	public static Font STRIKETHROUGH;

	public static final String THEME_COLOR_TASKLIST_CATEGORY = "org.eclipse.mylar.tasklist.ui.colors.background.category";

	public static final String THEME_COLOR_TASK_OVERDUE = "org.eclipse.mylar.tasklist.ui.colors.foreground.overdue";

	public static final String THEME_COLOR_TASK_THISWEEK_SCHEDULED = "org.eclipse.mylar.tasklist.ui.colors.foreground.thisweek.scheduled";

	public static final String THEME_COLOR_TASK_TODAY_SCHEDULED = "org.eclipse.mylar.tasklist.ui.colors.foreground.today.scheduled";

	public static final String THEME_COLOR_TASK_TODAY_COMPLETED = "org.eclipse.mylar.tasklist.ui.colors.foreground.today.completed";

	static {
		Font defaultFont = JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
		FontData[] defaultData = defaultFont.getFontData();
		if (defaultData != null && defaultData.length == 1) {
			FontData data = new FontData(defaultData[0].getName(), defaultData[0].getHeight(), defaultData[0]
					.getStyle());

			// NOTE: Windowx XP only, for: data.data.lfStrikeOut = 1;
			try {
				Field dataField = data.getClass().getDeclaredField("data");
				Object dataObject = dataField.get(data);
				Class clazz = dataObject.getClass().getSuperclass();
				Field strikeOutFiled = clazz.getDeclaredField("lfStrikeOut");
				strikeOutFiled.set(dataObject, (byte) 1);
				STRIKETHROUGH = new Font(Display.getCurrent(), data);
			} catch (Exception e) {
				// Linux or other platform
				STRIKETHROUGH = defaultFont;
			}
		} else {
			STRIKETHROUGH = defaultFont;
		}
	}

	/**
	 * NOTE: disposal of JFaceResources fonts handled by registry.
	 */
	public static void dispose() {
		if (STRIKETHROUGH != null && !STRIKETHROUGH.isDisposed()) {
			STRIKETHROUGH.dispose();
		}
		BACKGROUND_ARCHIVE.dispose();
		COLOR_LABEL_CAUTION.dispose();
		COLOR_GRAY_LIGHT.dispose();
		COLOR_TASK_COMPLETED.dispose();
		COLOR_TASK_ACTIVE.dispose();
		COLOR_HYPERLINK.dispose();
	}

	
	public static boolean isTaskListTheme(String property) {
		if (property == null) {
			return false;
		} else {
			return property.equals(TaskListColorsAndFonts.THEME_COLOR_TASKLIST_CATEGORY)
					|| property.equals(TaskListColorsAndFonts.THEME_COLOR_TASK_OVERDUE)
					|| property.equals(TaskListColorsAndFonts.THEME_COLOR_TASK_TODAY_COMPLETED)
					|| property.equals(TaskListColorsAndFonts.THEME_COLOR_TASK_TODAY_SCHEDULED)
					|| property.equals(TaskListColorsAndFonts.THEME_COLOR_TASK_THISWEEK_SCHEDULED);
		}
	}


	public static final String TASK_EDITOR_FONT = "org.eclipse.mylar.tasklist.ui.fonts.task.editor.comment";

}
