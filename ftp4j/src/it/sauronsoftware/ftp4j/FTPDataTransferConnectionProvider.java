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

import java.net.Socket;

/**
 * A package reserved {@link FTPConnection} provider, used internally by the
 * client to obtain connections for data transfer purposes.
 * 
 * @author cpelliccia
 * 
 */
interface FTPDataTransferConnectionProvider {

	/**
	 * Returns the connection.
	 * 
	 * @return The connection.
	 * @throws FTPException
	 *             If an unexpected error occurs.
	 */
	public Socket openDataTransferConnection() throws FTPDataTransferException;

	/**
	 * Terminates the provider.
	 */
	public void dispose();

}
