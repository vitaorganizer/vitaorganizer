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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.StringTokenizer;

/**
 * This is an NVT-ASCII character stream writer.
 * 
 * @author Carlo Pelliccia
 * @version 1.1
 */
class NVTASCIIWriter extends Writer {

	/**
	 * NTV line separator.
	 */
	private static final String LINE_SEPARATOR = "\r\n";

	/**
	 * The wrapped stream.
	 */
	private OutputStream stream;

	/**
	 * The underlying writer.
	 */
	private Writer writer;

	/**
	 * Builds the writer.
	 * 
	 * @param stream
	 *            The underlying stream.
	 * @param charsetName
	 *            The name of a supported charset.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public NVTASCIIWriter(OutputStream stream, String charsetName)
			throws IOException {
		this.stream = stream;
		this.writer = new OutputStreamWriter(stream, charsetName);
	}

	/**
	 * Causes this writer to be closed.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		synchronized (this) {
			writer.close();
		}
	}

	public void flush() throws IOException {
		synchronized (this) {
			writer.flush();
		}
	}

	public void write(char[] cbuf, int off, int len) throws IOException {
		synchronized (this) {
			writer.write(cbuf, off, len);
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
			writer = new OutputStreamWriter(stream, charsetName);
		}
	}

	/**
	 * Writes a line in the stream.
	 * 
	 * @param str
	 *            The line.
	 * @throws IOException
	 *             If an I/O error occurs.
	 */
	public void writeLine(String str) throws IOException {
		StringBuffer buffer = new StringBuffer();
		boolean atLeastOne = false;
		StringTokenizer st = new StringTokenizer(str, LINE_SEPARATOR);
		int count = st.countTokens();
		for (int i = 0; i < count; i++) {
			String line = st.nextToken();
			if (line.length() > 0) {
				if (atLeastOne) {
					buffer.append('\r');
					buffer.append((char) 0);
				}
				buffer.append(line);
				atLeastOne = true;
			}
		}
		if (buffer.length() > 0) {
			String statement = buffer.toString();
			// Sends the statement to the server.
			writer.write(statement);
			writer.write(LINE_SEPARATOR);
			writer.flush();
		}
	}

}
