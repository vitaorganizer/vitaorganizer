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
package it.sauronsoftware.ftp4j.extrecognizers;

import java.util.ArrayList;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPTextualExtensionRecognizer;

/**
 * A textual extension recognizer with parametric extensions, which can be added
 * or removed at runtime.
 * 
 * @author Carlo Pelliccia
 * @see FTPClient#setTextualExtensionRecognizer(FTPTextualExtensionRecognizer)
 */
public class ParametricTextualExtensionRecognizer implements
		FTPTextualExtensionRecognizer {

	/**
	 * Extension list.
	 */
	private ArrayList exts = new ArrayList();

	/**
	 * It builds the recognizer with an empty extension list.
	 */
	public ParametricTextualExtensionRecognizer() {
		;
	}

	/**
	 * It builds the recognizer with an initial extension list.
	 * 
	 * @param exts
	 *            The initial extension list.
	 */
	public ParametricTextualExtensionRecognizer(String[] exts) {
		for (int i = 0; i < exts.length; i++) {
			addExtension(exts[i]);
		}
	}

	/**
	 * It builds the recognizer with an initial extension list.
	 * 
	 * @param exts
	 *            The initial extension list.
	 */
	public ParametricTextualExtensionRecognizer(ArrayList exts) {
		int size = exts.size();
		for (int i = 0; i < size; i++) {
			Object aux = exts.get(i);
			if (aux instanceof String) {
				String ext = (String) aux;
				addExtension(ext);
			}
		}
	}

	/**
	 * This method adds an extension to the recognizer.
	 * 
	 * @param ext
	 *            The extension.
	 */
	public void addExtension(String ext) {
		synchronized (exts) {
			ext = ext.toLowerCase();
			exts.add(ext);
		}
	}

	/**
	 * This method removes an extension to the recognizer.
	 * 
	 * @param ext
	 *            The extension to be removed.
	 */
	public void removeExtension(String ext) {
		synchronized (exts) {
			ext = ext.toLowerCase();
			exts.remove(ext);
		}
	}

	/**
	 * This method returns the recognized extension list.
	 * 
	 * @return The list with all the extensions recognized to be for textual
	 *         files.
	 */
	public String[] getExtensions() {
		synchronized (exts) {
			int size = exts.size();
			String[] ret = new String[size];
			for (int i = 0; i < size; i++) {
				ret[i] = (String) exts.get(i);
			}
			return ret;
		}
	}

	public boolean isTextualExt(String ext) {
		synchronized (exts) {
			return exts.contains(ext);
		}
	}

}
