/*******************************************************************************
 * Copyright (c) 2006 - 2006 Mylar eclipse.org project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylar project committers - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

/**
 * @author Mik Kersten
 */
public class CompositeTaskImageDescriptor extends CompositeImageDescriptor {

	private ImageData base;

	private ImageData kind;
	
	protected Point size;

	public static final int WIDTH_DECORATION = 6;
	
	private static final int WIDTH_ICON = 16;
		
	static int WIDTH;
	
	static {
		WIDTH = WIDTH_DECORATION + WIDTH_ICON;
	}
	
	public CompositeTaskImageDescriptor(ImageDescriptor icon, ImageDescriptor overlayKind) {
		this.base = getImageData(icon);
		if (overlayKind != null) {
			this.kind = getImageData(overlayKind);
		}
		this.size = new Point(WIDTH, base.height);
	}
	
	@Override
	protected void drawCompositeImage(int width, int height) {
		drawImage(base, WIDTH_DECORATION, 1);
		if (kind != null) {
			drawImage(kind, WIDTH_DECORATION+5, 6);
		}
	}

	private ImageData getImageData(ImageDescriptor descriptor) {
		ImageData data = descriptor.getImageData(); 
		// see bug 51965: getImageData can return null
		if (data == null) {
			data = DEFAULT_IMAGE_DATA;
		}
		return data;
	}

	@Override
	protected Point getSize() {
		return new Point(size.x, size.y);
	}
}