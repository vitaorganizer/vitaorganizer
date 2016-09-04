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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * This is an NVT-ASCII character stream reader.
 * 
 * @author Carlo Pelliccia
 * @version 1.1
 */
class NVTASCIIReader extends Reader {

	/**
	 * This system line separator chars sequence.
	 */
	private static final String SYSTEM_LINE_SEPARATOR = System
			.getProperty("line.separator");

	/**
	 * The wrapped stream.
	 */
	private InputStream stream;

	/**
	 * The underlying reader.
	 */
	private Reader reader;

	/**
	 * Builds the reader.
	 * 
	 * @param stream
	 *            The underlying stream.
	 * @param charsetName
	 *            The name of a supported charset.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public NVTASCIIReader(InputStream stream, String charsetName)
			throws IOException {
		this.stream = stream;
		reader = new InputStreamReader(stream, charsetName);
	}

	public void close() throws IOException {
		synchronized (this) {
			reader.close();
		}
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
		synchronized (this) {
			return reader.read(cbuf, off, len);
		}
	}

	/**
	 * Changes the current charset.
	 * 
	 * @param charsetName
	 *            The new charset.
	 * @throws IOException
	 *             If I/O error occurs.
	 * @since 1.1
	 */
	public void changeCharset(String charsetName) throws IOException {
		synchronized (this) {
			reader = new InputStreamReader(stream, charsetName);
		}
	}

	/**
	 * Reads a line from the stream.
	 * 
	 * @return The line read, or null if the end of the stream is reached.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public String readLine() throws IOException {
		StringBuffer buffer = new StringBuffer();
		int previous = -1;
		int current = -1;
		do {
			int i = reader.read();
			if (i == -1) {
				if (buffer.length() == 0) {
					return null;
				} else {
					return buffer.toString();
				}
			}
			previous = current;
			current = i;
			if (/* previous == '\r' && */current == '\n') {
				// End of line.
				return buffer.toString();
			} else if (previous == '\r' && current == 0) {
				// Literal new line.
				buffer.append(SYSTEM_LINE_SEPARATOR);
			} else if (current != 0 && current != '\r') {
				buffer.append((char) current);
			}
		} while (true);
	}

}
