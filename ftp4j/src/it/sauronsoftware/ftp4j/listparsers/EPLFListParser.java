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
package it.sauronsoftware.ftp4j.listparsers;

import java.util.Date;
import java.util.StringTokenizer;

import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;

/**
 * This parser can handle the EPLF format.
 * 
 * @author Carlo Pelliccia
 */
public class EPLFListParser implements FTPListParser {

	public FTPFile[] parse(String[] lines) throws FTPListParseException {
		int size = lines.length;
		FTPFile[] ret = null;
		for (int i = 0; i < size; i++) {
			String l = lines[i];
			// Validate the plus sign.
			if (l.charAt(0) != '+') {
				throw new FTPListParseException();
			}
			// Split the facts from the filename.
			int a = l.indexOf('\t');
			if (a == -1) {
				throw new FTPListParseException();
			}
			String facts = l.substring(1, a);
			String name = l.substring(a + 1, l.length());
			// Parse the facts.
			Date md = null;
			boolean dir = false;
			long fileSize = 0;
			StringTokenizer st = new StringTokenizer(facts, ",");
			while (st.hasMoreTokens()) {
				String f = st.nextToken();
				int s = f.length();
				if (s > 0) {
					if (s == 1) {
						if (f.equals("/")) {
							// This is a directory.
							dir = true;
						}
					} else {
						char c = f.charAt(0);
						String value = f.substring(1, s);
						if (c == 's') {
							// Size parameter.
							try {
								fileSize = Long.parseLong(value);
							} catch (Throwable t) {
								;
							}
						} else if (c == 'm') {
							// Modified date.
							try {
								long m = Long.parseLong(value);
								md = new Date(m * 1000);
							} catch (Throwable t) {
								;
							}
						}
					}
				}
			}
			// Create the related FTPFile object.
			if (ret == null) {
				ret = new FTPFile[size];
			}
			ret[i] = new FTPFile();
			ret[i].setName(name);
			ret[i].setModifiedDate(md);
			ret[i].setSize(fileSize);
			ret[i].setType(dir ? FTPFile.TYPE_DIRECTORY : FTPFile.TYPE_FILE);
		}
		return ret;
	}

	public static void main(String[] args) throws Throwable {
		String[] test = { "+i8388621.29609,m824255902,/,\tdev",
				"+i8388621.44468,m839956783,r,s10376,\tRFCEPLF" };
		EPLFListParser parser = new EPLFListParser();
		FTPFile[] f = parser.parse(test);
		for (int i = 0; i < f.length; i++) {
			System.out.println(f[i]);
		}
	}
}
