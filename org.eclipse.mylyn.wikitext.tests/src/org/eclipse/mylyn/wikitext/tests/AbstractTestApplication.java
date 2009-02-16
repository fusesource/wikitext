/*******************************************************************************
 * Copyright (c) 2007, 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.tests;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.parser.util.MarkupToDocbook;
import org.eclipse.mylyn.wikitext.ui.viewer.MarkupViewer;
import org.eclipse.mylyn.wikitext.ui.viewer.MarkupViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author David Green
 */
public abstract class AbstractTestApplication {

	private boolean dirty;

	private Browser browser;

	private MarkupViewer viewer;

	private String markup;

	private TextViewer htmlSourceViewer;

	private TextViewer docbookSourceViewer;

	protected void doMain() throws IOException {
		markup = "";

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());

		CTabFolder folder = new CTabFolder(shell, SWT.BORDER);

		createWikiTextViewer(folder);
		createBrowser(folder);
		createMarkupSourceViewer(folder);
		createHtmlSourceViewer(folder);
		createDocbookSourceViewer(folder);

		updateDependentViewers();

		folder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent selectionevent) {
				widgetSelected(selectionevent);
			}

			public void widgetSelected(SelectionEvent selectionevent) {
				if (dirty) {
					updateViewers();
				}
			}

		});

		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private void createHtmlSourceViewer(CTabFolder folder) {
		CTabItem viewerItem = new CTabItem(folder, SWT.NONE);
		viewerItem.setText("HTML Source");

		htmlSourceViewer = new TextViewer(folder, SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		Document document = new Document("");
		htmlSourceViewer.setDocument(document);

		viewerItem.setControl(htmlSourceViewer.getControl());
	}

	private void createDocbookSourceViewer(CTabFolder folder) {
		CTabItem viewerItem = new CTabItem(folder, SWT.NONE);
		viewerItem.setText("DocBook Source");

		docbookSourceViewer = new TextViewer(folder, SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.READ_ONLY);
		Document document = new Document("");
		docbookSourceViewer.setDocument(document);

		viewerItem.setControl(docbookSourceViewer.getControl());
	}

	private void createMarkupSourceViewer(CTabFolder folder) {
		CTabItem viewerItem = new CTabItem(folder, SWT.NONE);
		viewerItem.setText(createMarkupLanguage().getName() + " Source");

		TextViewer viewer = new TextViewer(folder, SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
		Document document = new Document(markup);

		document.addDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
			}

			public void documentChanged(DocumentEvent event) {
				dirty = true;
				markup = event.getDocument().get();
			}

		});
		viewer.setDocument(document);

		viewerItem.setControl(viewer.getControl());
	}

	private void createBrowser(CTabFolder folder) {
		CTabItem viewerItem = new CTabItem(folder, SWT.NONE);
		viewerItem.setText("Browser");

		browser = new Browser(folder, SWT.NONE);
		viewerItem.setControl(browser);
	}

	private void updateDependentViewers() {
		{
			MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(createMarkupLanguage());
			StringWriter html = new StringWriter();
			try {
				HtmlDocumentBuilder builder = new HtmlDocumentBuilder(html);
				parser.setBuilder(builder);
				parser.parse(markup);
			} catch (FactoryConfigurationError e1) {
				throw new IllegalStateException(e1);
			}

			htmlSourceViewer.getDocument().set(html.toString());
		}
		{
			MarkupToDocbook markupToDocbook = new MarkupToDocbook();
			markupToDocbook.setMarkupLanguage(createMarkupLanguage());
			markupToDocbook.setBookTitle("Book");
			String docbook;
			try {
				docbook = markupToDocbook.parse(markup);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			docbookSourceViewer.getDocument().set(docbook);
		}
		{
			MarkupParser parser = new MarkupParser();
			parser.setMarkupLanguage(createMarkupLanguage());
			String html = parser.parseToHtml(markup);
			browser.setText(html);
		}
	}

	protected abstract MarkupLanguage createMarkupLanguage();

	private void createWikiTextViewer(CTabFolder folder) {
		CTabItem viewerItem = new CTabItem(folder, SWT.NONE);
		viewerItem.setText("WikiText Viewer");

		viewer = new MarkupViewer(folder, null, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		viewer.setMarkupLanguage(createMarkupLanguage());

		MarkupViewerConfiguration configuration = new MarkupViewerConfiguration(viewer);
		viewer.configure(configuration);
		viewer.getTextWidget().setEditable(false);
		viewer.setMarkup(markup);
		viewerItem.setControl(viewer.getControl());

		// output to the console info about annotations near the caret
		viewer.getTextWidget().addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent e) {
				int offset = e.x;
				List<String> annotations = new ArrayList<String>();
				Iterator<Annotation> it = viewer.getAnnotationModel().getAnnotationIterator();
				while (it.hasNext()) {
					Annotation annotation = it.next();
					Position position = viewer.getAnnotationModel().getPosition(annotation);
					if (Math.abs(position.getOffset() - offset) < 5) {
						annotations.add(String.format("[%s,%s] %s %s", position.getOffset(), position.getLength(),
								position.getOffset() - offset, annotation.getType()));
					}
				}
				System.out.println("offset: " + offset + " annotations: " + annotations);
			}
		});
	}

	protected void updateViewers() {
		viewer.setMarkup(markup);
		updateDependentViewers();
		dirty = false;
	}
}
