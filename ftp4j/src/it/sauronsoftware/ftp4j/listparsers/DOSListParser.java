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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPListParser;

/**
 * This parser can handle the MSDOS-style LIST responses.
 * 
 * @author Carlo Pelliccia
 */
public class DOSListParser implements FTPListParser {

	private static final Pattern PATTERN = Pattern
			.compile("^(\\d{2})-(\\d{2})-(\\d{2})\\s+(\\d{2}):(\\d{2})(AM|PM)\\s+"
					+ "(<DIR>|\\d+)\\s+([^\\\\/*?\"<>|]+)$");

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"MM/dd/yy hh:mm a");

	public FTPFile[] parse(String[] lines) throws FTPListParseException {
		int size = lines.length;
		FTPFile[] ret = new FTPFile[size];
		for (int i = 0; i < size; i++) {
			Matcher m = PATTERN.matcher(lines[i]);
			if (m.matches()) {
				String month = m.group(1);
				String day = m.group(2);
				String year = m.group(3);
				String hour = m.group(4);
				String minute = m.group(5);
				String ampm = m.group(6);
				String dirOrSize = m.group(7);
				String name = m.group(8);
				ret[i] = new FTPFile();
				ret[i].setName(name);
				if (dirOrSize.equalsIgnoreCase("<DIR>")) {
					ret[i].setType(FTPFile.TYPE_DIRECTORY);
					ret[i].setSize(0);
				} else {
					long fileSize;
					try {
						fileSize = Long.parseLong(dirOrSize);
					} catch (Throwable t) {
						throw new FTPListParseException();
					}
					ret[i].setType(FTPFile.TYPE_FILE);
					ret[i].setSize(fileSize);
				}
				String mdString = month + "/" + day + "/" + year + " " + hour
						+ ":" + minute + " " + ampm;
				Date md;
				try {
					synchronized (DATE_FORMAT) {
						md = DATE_FORMAT.parse(mdString);
					}
				} catch (ParseException e) {
					throw new FTPListParseException();
				}
				ret[i].setModifiedDate(md);
			} else {
				throw new FTPListParseException();
			}
		}
		return ret;
	}

}
