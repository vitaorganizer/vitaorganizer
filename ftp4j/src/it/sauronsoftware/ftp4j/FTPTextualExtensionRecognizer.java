/*
 * ftp4j - A pure Java FTP client library
 * 
 * Copyright (C) 2008-2010 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version
 * 2.1, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License 2.1 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License version 2.1 along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.ftp4j;

/**
 * This interface describes how to implement a textual extension recognizer,
 * which can be plugged into a FTPClient object calling its
 * setTextualExtensionsRecognizer() method.
 * 
 * @author Carlo Pelliccia
 * @see FTPClient#setTextualExtensionRecognizer(FTPTextualExtensionRecognizer)
 */
public interface FTPTextualExtensionRecognizer {

	/**
	 * This method returns true if the given file extension is recognized to be
	 * a textual one.
	 * 
	 * @param ext
	 *            The file extension, always in lower-case.
	 * @return true if the given file extension is recognized to be a textual
	 *         one.
	 */
	public boolean isTextualExt(String ext);

}
