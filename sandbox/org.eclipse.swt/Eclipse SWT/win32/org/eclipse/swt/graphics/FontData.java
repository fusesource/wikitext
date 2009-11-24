/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.graphics;


import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.*;

/**
 * Instances of this class describe operating system fonts.
 * <p>
 * For platform-independent behaviour, use the get and set methods
 * corresponding to the following properties:
 * <dl>
 * <dt>height</dt><dd>the height of the font in points</dd>
 * <dt>name</dt><dd>the face name of the font, which may include the foundry</dd>
 * <dt>style</dt><dd>A bitwise combination of NORMAL, ITALIC and BOLD</dd>
 * </dl>
 * If extra, platform-dependent functionality is required:
 * <ul>
 * <li>On <em>Windows</em>, the data member of the <code>FontData</code>
 * corresponds to a Windows <code>LOGFONT</code> structure whose fields
 * may be retrieved and modified.</li>
 * <li>On <em>X</em>, the fields of the <code>FontData</code> correspond
 * to the entries in the font's XLFD name and may be retrieved and modified.
 * </ul>
 * Application code does <em>not</em> need to explicitly release the
 * resources managed by each instance when those instances are no longer
 * required, and thus no <code>dispose()</code> method is provided.
 *
 * @see Font
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further information</a>
 */

public final class FontData {
	
	/**
	 * A Win32 LOGFONT struct
	 * (Warning: This field is platform dependent)
	 * <p>
	 * <b>IMPORTANT:</b> This field is <em>not</em> part of the SWT
	 * public API. It is marked public only so that it can be shared
	 * within the packages provided by SWT. It is not available on all
	 * platforms and should never be accessed from application code.
	 * </p>
	 */
	public LOGFONT data;
	
	/**
	 * The height of the font data in points
	 * (Warning: This field is platform dependent)
	 * <p>
	 * <b>IMPORTANT:</b> This field is <em>not</em> part of the SWT
	 * public API. It is marked public only so that it can be shared
	 * within the packages provided by SWT. It is not available on all
	 * platforms and should never be accessed from application code.
	 * </p>
	 */
	public float height;
	
	/**
	 * The locales of the font
	 */
	String lang, country, variant;
	
/**	 
 * Constructs a new uninitialized font data.
 */
public FontData() {
	data = OS.IsUnicode ? (LOGFONT)new LOGFONTW() : new LOGFONTA();
	// We set the charset field so that
	// wildcard searching will work properly
	// out of the box
	data.lfCharSet = (byte)OS.DEFAULT_CHARSET;
	height = 12;
}

/**
 * Constructs a new font data given the Windows <code>LOGFONT</code>
 * that it should represent.
 * 
 * @param data the <code>LOGFONT</code> for the result
 */
FontData(LOGFONT data, float height) {
	this.data = data;
	this.height = height;
}

/**
 * Constructs a new FontData given a string representation
 * in the form generated by the <code>FontData.toString</code>
 * method.
 * <p>
 * Note that the representation varies between platforms,
 * and a FontData can only be created from a string that was 
 * generated on the same platform.
 * </p>
 *
 * @param string the string representation of a <code>FontData</code> (must not be null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the argument is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the argument does not represent a valid description</li>
 * </ul>
 *
 * @see #toString
 */
public FontData(String string) {
	if (string == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	int start = 0;
	int end = string.indexOf('|');
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	String version1 = string.substring(start, end);
	try {
		if (Integer.parseInt(version1) != 1) SWT.error(SWT.ERROR_INVALID_ARGUMENT); 
	} catch (NumberFormatException e) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	
	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	String name = string.substring(start, end);
	
	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	float height = 0;
	try {
		height = Float.parseFloat(string.substring(start, end));
	} catch (NumberFormatException e) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}
	
	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	int style = 0;
	try {
		style = Integer.parseInt(string.substring(start, end));
	} catch (NumberFormatException e) {
		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	}

	start = end + 1;
	end = string.indexOf('|', start);
	data = OS.IsUnicode ? (LOGFONT)new LOGFONTW() : new LOGFONTA();
	data.lfCharSet = (byte)OS.DEFAULT_CHARSET;
	setName(name);
	setHeight(height);
	setStyle(style);
	if (end == -1) return;
	String platform = string.substring(start, end);

	start = end + 1;
	end = string.indexOf('|', start);
	if (end == -1) return;
	String version2 = string.substring(start, end);

	if (platform.equals("WINDOWS") && version2.equals("1")) {  //$NON-NLS-1$//$NON-NLS-2$
		LOGFONT newData = OS.IsUnicode ? (LOGFONT)new LOGFONTW() : new LOGFONTA();
		try {
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfHeight = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfWidth = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfEscapement = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfOrientation = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfWeight = Integer.parseInt(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfItalic = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfUnderline = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfStrikeOut = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfCharSet = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfOutPrecision = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfClipPrecision = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfQuality = Byte.parseByte(string.substring(start, end));
			start = end + 1;
			end = string.indexOf('|', start);
			if (end == -1) return;
			newData.lfPitchAndFamily = Byte.parseByte(string.substring(start, end));
			start = end + 1;
		} catch (NumberFormatException e) {
			setName(name);
			setHeight(height);
			setStyle(style);
			return;
		}
		TCHAR buffer = new TCHAR(0, string.substring(start), false);
		int length = Math.min(OS.LF_FACESIZE - 1, buffer.length());
		if (OS.IsUnicode) {
			char[] lfFaceName = ((LOGFONTW)newData).lfFaceName;
			System.arraycopy(buffer.chars, 0, lfFaceName, 0, length);
		} else {
			byte[] lfFaceName = ((LOGFONTA)newData).lfFaceName;
			System.arraycopy(buffer.bytes, 0, lfFaceName, 0, length);
		}
		data = newData;
	}
}

/**	 
 * Constructs a new font data given a font name,
 * the height of the desired font in points, 
 * and a font style.
 *
 * @param name the name of the font (must not be null)
 * @param height the font height in points
 * @param style a bit or combination of NORMAL, BOLD, ITALIC
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - when the font name is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the height is negative</li>
 * </ul>
 */
public FontData(String name, int height, int style) {
	if (name == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	data = OS.IsUnicode ? (LOGFONT)new LOGFONTW() : new LOGFONTA();
	setName(name);
	setHeight(height);
	setStyle(style);
	// We set the charset field so that
	// wildcard searching will work properly
	// out of the box
	data.lfCharSet = (byte)OS.DEFAULT_CHARSET;
}

/*public*/ FontData(String name, float height, int style) {
	if (name == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	data = OS.IsUnicode ? (LOGFONT)new LOGFONTW() : new LOGFONTA();
	setName(name);
	setHeight(height);
	setStyle(style);
	// We set the charset field so that
	// wildcard searching will work properly
	// out of the box
	data.lfCharSet = (byte)OS.DEFAULT_CHARSET;
}

/**
 * Compares the argument to the receiver, and returns true
 * if they represent the <em>same</em> object using a class
 * specific comparison.
 *
 * @param object the object to compare with this object
 * @return <code>true</code> if the object is the same as this object and <code>false</code> otherwise
 *
 * @see #hashCode
 */
public boolean equals (Object object) {
	if (object == this) return true;
	if (!(object instanceof FontData)) return false;
	FontData fd = (FontData)object;
	LOGFONT lf = fd.data;
	return data.lfCharSet == lf.lfCharSet &&
		/*
		* This code is intentionally commented.  When creating
		* a FontData, lfHeight is not necessarily set.  Instead
		* we check the height field which is always set.
		*/ 
//		data.lfHeight == lf.lfHeight &&
		height == fd.height &&
		data.lfWidth == lf.lfWidth &&
		data.lfEscapement == lf.lfEscapement &&
		data.lfOrientation == lf.lfOrientation &&
		data.lfWeight == lf.lfWeight &&
		data.lfItalic == lf.lfItalic &&
		data.lfUnderline == lf.lfUnderline &&
		data.lfStrikeOut == lf.lfStrikeOut &&
		data.lfCharSet == lf.lfCharSet &&
		data.lfOutPrecision == lf.lfOutPrecision &&
		data.lfClipPrecision == lf.lfClipPrecision &&
		data.lfQuality == lf.lfQuality &&
		data.lfPitchAndFamily == lf.lfPitchAndFamily &&
		getName().equals(fd.getName());
}

int /*long*/ EnumLocalesProc(int /*long*/ lpLocaleString) {
	
	/* Get the locale ID */
	int length = 8;
	TCHAR buffer = new TCHAR(0, length);
	int byteCount = length * TCHAR.sizeof;
	OS.MoveMemory(buffer, lpLocaleString, byteCount);
	int lcid = Integer.parseInt(buffer.toString(0, buffer.strlen ()), 16);

	/* Check the language */
	int size = OS.GetLocaleInfo(lcid, OS.LOCALE_SISO639LANGNAME, buffer, length);
	if (size <= 0 || !lang.equals(buffer.toString(0, size - 1))) return 1;

	/* Check the country */
	if (country != null) {
		size = OS.GetLocaleInfo(lcid, OS.LOCALE_SISO3166CTRYNAME, buffer, length);
		if (size <= 0 || !country.equals(buffer.toString(0, size - 1))) return 1;
	}

	/* Get the charset */
	size = OS.GetLocaleInfo(lcid, OS.LOCALE_IDEFAULTANSICODEPAGE, buffer, length);
	if (size <= 0) return 1;
	int cp = Integer.parseInt(buffer.toString(0, size - 1));
	int [] lpCs = new int[8];
	OS.TranslateCharsetInfo(cp, lpCs, OS.TCI_SRCCODEPAGE);
	data.lfCharSet = (byte)lpCs[0];

	return 0;
}

/**
 * Returns the height of the receiver in points.
 *
 * @return the height of this FontData
 *
 * @see #setHeight(int)
 */
public int getHeight() {
	return (int)(0.5f + height);
}

/*public*/ float getHeightF() {
	return height;
}

/**
 * Returns the locale of the receiver.
 * <p>
 * The locale determines which platform character set this
 * font is going to use. Widgets and graphics operations that
 * use this font will convert UNICODE strings to the platform
 * character set of the specified locale.
 * </p>
 * <p>
 * On platforms where there are multiple character sets for a
 * given language/country locale, the variant portion of the
 * locale will determine the character set.
 * </p>
 * 
 * @return the <code>String</code> representing a Locale object
 * @since 3.0
 */
public String getLocale () {
	StringBuffer buffer = new StringBuffer ();
	char sep = '_';
	if (lang != null) {
		buffer.append (lang);
		buffer.append (sep);
	}
	if (country != null) {
		buffer.append (country);
		buffer.append (sep);
	}
	if (variant != null) {
		buffer.append (variant);
	}
	
	String result = buffer.toString ();
	int length = result.length ();
	if (length > 0) {
		if (result.charAt (length - 1) == sep) {
			result = result.substring (0, length - 1);
		}
	} 
	return result;
}

/**
 * Returns the name of the receiver.
 * On platforms that support font foundries, the return value will
 * be the foundry followed by a dash ("-") followed by the face name.
 *
 * @return the name of this <code>FontData</code>
 *
 * @see #setName
 */
public String getName() {
	char[] chars;
	if (OS.IsUnicode) {
		chars = ((LOGFONTW)data).lfFaceName;
	} else {
		chars = new char[OS.LF_FACESIZE];
		byte[] bytes = ((LOGFONTA)data).lfFaceName;
		OS.MultiByteToWideChar (OS.CP_ACP, OS.MB_PRECOMPOSED, bytes, bytes.length, chars, chars.length);
	}
	int index = 0;
	while (index < chars.length) {
		if (chars [index] == 0) break;
		index++;
	}
	return new String (chars, 0, index);
}

/**
 * Returns the style of the receiver which is a bitwise OR of 
 * one or more of the <code>SWT</code> constants NORMAL, BOLD
 * and ITALIC.
 *
 * @return the style of this <code>FontData</code>
 * 
 * @see #setStyle
 */
public int getStyle() {
	int style = SWT.NORMAL;
	if (data.lfWeight == 700) style |= SWT.BOLD;
	if (data.lfItalic != 0) style |= SWT.ITALIC;
	return style;
}

/**
 * Returns an integer hash code for the receiver. Any two 
 * objects that return <code>true</code> when passed to 
 * <code>equals</code> must return the same value for this
 * method.
 *
 * @return the receiver's hash
 *
 * @see #equals
 */
public int hashCode () {
	return data.lfCharSet ^ getHeight() ^ data.lfWidth ^ data.lfEscapement ^
		data.lfOrientation ^ data.lfWeight ^ data.lfItalic ^data.lfUnderline ^
		data.lfStrikeOut ^ data.lfCharSet ^ data.lfOutPrecision ^
		data.lfClipPrecision ^ data.lfQuality ^ data.lfPitchAndFamily ^
		getName().hashCode();
}

/**
 * Sets the height of the receiver. The parameter is
 * specified in terms of points, where a point is one
 * seventy-second of an inch.
 *
 * @param height the height of the <code>FontData</code>
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the height is negative</li>
 * </ul>
 * 
 * @see #getHeight
 */
public void setHeight(int height) {
	if (height < 0) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	this.height = height;
	data.lfWidth = 0;
}

/*public*/ void setHeight(float height) {
	if (height < 0) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
	this.height = height;
}

/**
 * Sets the locale of the receiver.
 * <p>
 * The locale determines which platform character set this
 * font is going to use. Widgets and graphics operations that
 * use this font will convert UNICODE strings to the platform
 * character set of the specified locale.
 * </p>
 * <p>
 * On platforms where there are multiple character sets for a
 * given language/country locale, the variant portion of the
 * locale will determine the character set.
 * </p>
 * 
 * @param locale the <code>String</code> representing a Locale object
 * @see java.util.Locale#toString
 */
public void setLocale(String locale) {	
	lang = country = variant = null;
	if (locale != null) {
		char sep = '_';
		int length = locale.length();
		int firstSep, secondSep;
		
		firstSep = locale.indexOf(sep);
		if (firstSep == -1) {
			firstSep = secondSep = length;
		} else {
			secondSep = locale.indexOf(sep, firstSep + 1);
			if (secondSep == -1) secondSep = length;
		}
		if (firstSep > 0) lang = locale.substring(0, firstSep);
		if (secondSep > firstSep + 1) country = locale.substring(firstSep + 1, secondSep);
		if (length > secondSep + 1) variant = locale.substring(secondSep + 1);
	}
	if (lang == null) {
		data.lfCharSet = (byte)OS.DEFAULT_CHARSET;
	} else {
		Callback callback = new Callback (this, "EnumLocalesProc", 1); //$NON-NLS-1$
		int /*long*/ lpEnumLocalesProc = callback.getAddress ();	
		if (lpEnumLocalesProc == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
		OS.EnumSystemLocales(lpEnumLocalesProc, OS.LCID_SUPPORTED);
		callback.dispose ();
	}
}

/**
 * Sets the name of the receiver.
 * <p>
 * Some platforms support font foundries. On these platforms, the name
 * of the font specified in setName() may have one of the following forms:
 * <ol>
 * <li>a face name (for example, "courier")</li>
 * <li>a foundry followed by a dash ("-") followed by a face name (for example, "adobe-courier")</li>
 * </ol>
 * In either case, the name returned from getName() will include the
 * foundry.
 * </p>
 * <p>
 * On platforms that do not support font foundries, only the face name
 * (for example, "courier") is used in <code>setName()</code> and 
 * <code>getName()</code>.
 * </p>
 *
 * @param name the name of the font data (must not be null)
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - when the font name is null</li>
 * </ul>
 *
 * @see #getName
 */
public void setName(String name) {
	if (name == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);

	/* The field lfFaceName must be NULL terminated */
	TCHAR buffer = new TCHAR(0, name, true);
	int length = Math.min(OS.LF_FACESIZE - 1, buffer.length());
	if (OS.IsUnicode) {
		char[] lfFaceName = ((LOGFONTW)data).lfFaceName;
		for (int i = 0; i < lfFaceName.length; i++) lfFaceName[i] = 0;
		System.arraycopy(buffer.chars, 0, lfFaceName, 0, length);
	} else {
		byte[] lfFaceName = ((LOGFONTA)data).lfFaceName;
		for (int i = 0; i < lfFaceName.length; i++) lfFaceName[i] = 0;
		System.arraycopy(buffer.bytes, 0, lfFaceName, 0, length);
	}
}

/**
 * Sets the style of the receiver to the argument which must
 * be a bitwise OR of one or more of the <code>SWT</code> 
 * constants NORMAL, BOLD and ITALIC.  All other style bits are
 * ignored.
 *
 * @param style the new style for this <code>FontData</code>
 *
 * @see #getStyle
 */
public void setStyle(int style) {
	if ((style & SWT.BOLD) == SWT.BOLD) {
		data.lfWeight = 700;
	} else {
		data.lfWeight = 0;
	}
	if ((style & SWT.ITALIC) == SWT.ITALIC) {
		data.lfItalic = 1;
	} else {
		data.lfItalic = 0;
	}
}

/**
 * Returns a string representation of the receiver which is suitable
 * for constructing an equivalent instance using the 
 * <code>FontData(String)</code> constructor.
 *
 * @return a string representation of the FontData
 *
 * @see FontData
 */
public String toString() {
	StringBuffer buffer = new StringBuffer(128);
	buffer.append("1|"); //$NON-NLS-1$
	String name = getName();
	buffer.append(name);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(getHeightF());
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(getStyle());
	buffer.append("|"); //$NON-NLS-1$
	buffer.append("WINDOWS|1|"); //$NON-NLS-1$	
	buffer.append(data.lfHeight);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfWidth);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfEscapement);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfOrientation);  
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfWeight);  
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfItalic);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfUnderline);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfStrikeOut);  
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfCharSet); 
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfOutPrecision);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfClipPrecision);  
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfQuality); 
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(data.lfPitchAndFamily);
	buffer.append("|"); //$NON-NLS-1$
	buffer.append(name);
	return buffer.toString();
}

/**	 
 * Invokes platform specific functionality to allocate a new font data.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>FontData</code>. It is marked public only so that
 * it can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param data the <code>LOGFONT</code> for the font data
 * @param height the height of the font data
 * @return a new font data object containing the specified <code>LOGFONT</code> and height
 */
public static FontData win32_new(LOGFONT data, float height) {
	return new FontData(data, height);
}

}
