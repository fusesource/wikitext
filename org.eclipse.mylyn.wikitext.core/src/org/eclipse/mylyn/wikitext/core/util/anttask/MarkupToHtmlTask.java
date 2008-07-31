/*******************************************************************************
 * Copyright (c) 2007, 2008 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.core.util.anttask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.mylyn.internal.wikitext.core.parser.builder.DefaultSplittingStrategy;
import org.eclipse.mylyn.internal.wikitext.core.parser.builder.NoSplittingStrategy;
import org.eclipse.mylyn.internal.wikitext.core.parser.builder.SplitOutlineItem;
import org.eclipse.mylyn.internal.wikitext.core.parser.builder.SplittingHtmlDocumentBuilder;
import org.eclipse.mylyn.internal.wikitext.core.parser.builder.SplittingOutlineParser;
import org.eclipse.mylyn.internal.wikitext.core.parser.builder.SplittingStrategy;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;

/**
 * An Ant task for converting lightweight markup to HTML format.
 * 
 * @author David Green
 */
public class MarkupToHtmlTask extends MarkupTask {
	private List<FileSet> filesets = new ArrayList<FileSet>();

	protected String htmlFilenameFormat = "$1.html";

	protected boolean overwrite = true;

	private List<Stylesheet> stylesheets = new ArrayList<Stylesheet>();

	protected File file;

	protected String title;
	
	protected boolean multipleOutputFiles = false;
	
	protected boolean formatOutput = false;
	
	protected boolean navigationImages = false;

	private boolean useInlineCssStyles = true;
	private boolean suppressBuiltInCssStyles = false;
	
	@Override
	public void execute() throws BuildException {
		if (file == null && filesets.isEmpty()) {
			throw new BuildException("Please add one or more source filesets or specify @file");
		}
		if (file != null && !filesets.isEmpty()) {
			throw new BuildException("@file may not be specified if filesets are also specified");
		}
		if (file != null) {
			if (!file.exists()) {
				throw new BuildException(String.format("File cannot be found: %s",file));
			} else if (!file.isFile()) {
				throw new BuildException(String.format("Not a file: %s",file));
			} else if (!file.canRead()) {
				throw new BuildException(String.format("Cannot read file: %s",file));
			}
		}

		MarkupLanguage markupLanguage = createMarkupLanguage();

		for (Stylesheet stylesheet: stylesheets) {
			if (stylesheet.url == null && stylesheet.file == null) {
				throw new BuildException("Must specify one of @file or @url on <stylesheet>");
			}
			if (stylesheet.url != null && stylesheet.file != null) {
				throw new BuildException("May only specify one of @file or @url on <stylesheet>");
			}
			if (stylesheet.file != null) {
				if (!stylesheet.file.exists()) {
					throw new BuildException("Stylesheet file does not exist: "+stylesheet.file);
				}
				if (!stylesheet.file.isFile()) {
					throw new BuildException("Referenced stylesheet is not a file: "+stylesheet.file);
				}
				if (!stylesheet.file.canRead()) {
					throw new BuildException("Cannot read stylesheet: "+stylesheet.file);
				}
			}
		}

		Set<File> outputFolders = new HashSet<File>();
		
		for (FileSet fileset: filesets) {

			File filesetBaseDir = fileset.getDir(getProject());
			DirectoryScanner ds = fileset.getDirectoryScanner(getProject());

			String[] files = ds.getIncludedFiles();
			if (files != null) {
				File baseDir = ds.getBasedir();
				for (String file: files) {
					File inputFile = new File(baseDir,file);
					testForOutputFolderConflict(outputFolders, inputFile);
					try {
						processFile(markupLanguage,filesetBaseDir,inputFile);
					} catch (BuildException e) {
						throw e;
					} catch (Exception e) {
						throw new BuildException(String.format("Cannot process file '%s': %s",inputFile,e.getMessage()),e);
					}
				}
			}
		}
		if (file != null) {
			testForOutputFolderConflict(outputFolders, file);
			try {
				processFile(markupLanguage,file.getParentFile(),file);
			} catch (BuildException e) {
				throw e;
			} catch (Exception e) {
				throw new BuildException(String.format("Cannot process file '%s': %s",file,e.getMessage()),e);
			}
		}
	}

	private void testForOutputFolderConflict(Set<File> outputFolders,
			File inputFile) {
		if (multipleOutputFiles && !outputFolders.add(inputFile.getAbsoluteFile().getParentFile())) {
			log(String.format("multipleOutputFiles have already been created in folder '%s'"),Project.MSG_WARN);
		}
	}

	/**
	 * process the file
	 * 
	 * @param baseDir
	 * @param source
	 * 
	 * @return the lightweight markup, or null if the file was not written
	 * 
	 * @throws BuildException
	 */
	protected String processFile(MarkupLanguage markupLanguage,final File baseDir,final File source) throws BuildException {

		log(String.format("Processing file '%s'",source),Project.MSG_VERBOSE);

		String markupContent = null;

		String name = source.getName();
		if (name.lastIndexOf('.') != -1) {
			name = name.substring(0,name.lastIndexOf('.'));
		}

		File htmlOutputFile = computeHtmlFile(source, name);
		if (!htmlOutputFile.exists() || overwrite || htmlOutputFile.lastModified() < source.lastModified()) {

			if (markupContent == null) {
				markupContent = readFully(source);
			}

			Writer writer;
			try {
				writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(htmlOutputFile)),"utf-8");
			} catch (Exception e) {
				throw new BuildException(String.format("Cannot write to file '%s': %s",htmlOutputFile,e.getMessage()),e);
			}
			try {
				HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer,formatOutput);
				for (Stylesheet stylesheet: stylesheets) {
					if (stylesheet.url != null) {
						builder.addCssStylesheet(stylesheet.url);
					} else {
						builder.addCssStylesheet(stylesheet.file);
					}
				}

				builder.setTitle(title==null?name:title);
				builder.setEmitDtd(true);
				builder.setUseInlineStyles(useInlineCssStyles);
				builder.setSuppressBuiltInStyles(suppressBuiltInCssStyles);
				
				SplittingStrategy splittingStrategy = multipleOutputFiles?new DefaultSplittingStrategy():new NoSplittingStrategy();
				SplittingOutlineParser outlineParser = new SplittingOutlineParser();
				outlineParser.setMarkupLanguage(markupLanguage.clone());
				outlineParser.setSplittingStrategy(splittingStrategy);
				SplitOutlineItem item = outlineParser.parse(markupContent);
				item.setSplitTarget(htmlOutputFile.getName());
				SplittingHtmlDocumentBuilder splittingBuilder = new SplittingHtmlDocumentBuilder();
				splittingBuilder.setRootBuilder(builder);
				splittingBuilder.setOutline(item);
				splittingBuilder.setRootFile(htmlOutputFile);
				splittingBuilder.setNavigationImages(navigationImages);
				
				MarkupParser parser = new MarkupParser();
				parser.setMarkupLanaguage(markupLanguage);
				parser.setBuilder(splittingBuilder);

				parser.parse(markupContent);
				
				processed(markupContent,item,baseDir,source);
			} finally {
				try {
					writer.close();
				} catch (Exception e) {
					throw new BuildException(String.format("Cannot write to file '%s': %s",htmlOutputFile,e.getMessage()),e);
				}
			}
		}
		return markupContent;
	}

	void processed(String markupContent, SplitOutlineItem item,final File baseDir,final File source) {
	}

	protected File computeHtmlFile(final File source, String name) {
		return new File(source.getParentFile(),htmlFilenameFormat.replace("$1", name));
	}

	protected String readFully(File inputFile) {
		StringWriter w = new StringWriter();
		try {
			Reader r = new InputStreamReader(new BufferedInputStream(new FileInputStream(inputFile)));
			try {
				int i;
				while ((i = r.read()) != -1) {
					w.write((char)i);
				}
			} finally {
				r.close();
			}
		} catch (IOException e) {
			throw new BuildException(String.format("Cannot read file '%s': %s",inputFile,e.getMessage()),e);
		}
		return w.toString();
	}

	/**
	 * @see #setHtmlFilenameFormat(String)
	 */
	public String getHtmlFilenameFormat() {
		return htmlFilenameFormat;
	}

	/**
	 * The format of the HTML output file.  Consists of a pattern where the
	 * '$1' is replaced with the filename of the input file.  Default value is
	 * <code>$1.html</code>
	 * 
	 * @param htmlFilenameFormat
	 */
	public void setHtmlFilenameFormat(String htmlFilenameFormat) {
		this.htmlFilenameFormat = htmlFilenameFormat;
	}

	/**
	 * The document title, as it appears in the head
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * The document title, as it appears in the head
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * the file to process
	 */
	public File getFile() {
		return file;
	}

	/**
	 * the file to process
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * Adds a set of files to process.
	 */
	public void addFileset(FileSet set) {
		filesets.add(set);
	}

	public void addStylesheet(Stylesheet stylesheet) {
		if (stylesheet == null) {
			throw new IllegalArgumentException();
		}
		stylesheets.add(stylesheet);
	}

	/**
	 * indicate if output should be generated to multiple output files.
	 */
	public boolean isMultipleOutputFiles() {
		return multipleOutputFiles;
	}

	/**
	 * indicate if output should be generated to multiple output files.
	 */
	public void setMultipleOutputFiles(boolean multipleOutputFiles) {
		this.multipleOutputFiles = multipleOutputFiles;
	}

	/**
	 * indicate if the output should be formatted
	 */
	public boolean isFormatOutput() {
		return formatOutput;
	}

	/**
	 * indicate if the output should be formatted
	 */
	public void setFormatOutput(boolean formatOutput) {
		this.formatOutput = formatOutput;
	}

	/**
	 * indicate if navigation links should be images
	 */
	public boolean isNavigationImages() {
		return navigationImages;
	}


	/**
	 * indicate if navigation links should be images
	 */
	public void setNavigationImages(boolean navigationImages) {
		this.navigationImages = navigationImages;
	}


	
	/**
	 * @see HtmlDocumentBuilder#isUseInlineStyles()
	 */
	public boolean isUseInlineCssStyles() {
		return useInlineCssStyles;
	}

	/**
	 * @see HtmlDocumentBuilder#isUseInlineStyles()
	 */
	public void setUseInlineCssStyles(boolean useInlineCssStyles) {
		this.useInlineCssStyles = useInlineCssStyles;
	}

	/**
	 * @see HtmlDocumentBuilder#isSuppressBuiltInStyles()
	 */
	public boolean isSuppressBuiltInCssStyles() {
		return suppressBuiltInCssStyles;
	}

	/**
	 * @see HtmlDocumentBuilder#isSuppressBuiltInStyles()
	 */
	public void setSuppressBuiltInCssStyles(boolean suppressBuiltInCssStyles) {
		this.suppressBuiltInCssStyles = suppressBuiltInCssStyles;
	}



	public static class Stylesheet {
		private File file;
		private String url;

		public File getFile() {
			return file;
		}
		public void setFile(File file) {
			this.file = file;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
	}
	
}
