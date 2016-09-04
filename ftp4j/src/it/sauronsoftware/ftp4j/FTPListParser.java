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
 * Implement this interface to build a new LIST parser. List parsers are called
 * to parse the result of a FTP LIST command send to the server in the list()
 * method. You can add a custom parser to your instance of FTPClient calling on
 * it the method addListParser.
 * 
 * @author Carlo Pelliccia
 * @see FTPClient#addListParser(FTPListParser)
 */
public interface FTPListParser {

	/**
	 * Parses a LIST command response and builds an array of FTPFile objects.
	 * 
	 * @param lines
	 *            The response to parse, splitted by line.
	 * @return An array of FTPFile objects representing the result of the
	 *         operation.
	 * @throws FTPListParseException
	 *             If this parser cannot parse the given response.
	 */
	public FTPFile[] parse(String[] lines) throws FTPListParseException;

}
