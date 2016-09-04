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
 * This interface describes the methods requested by an object that can listen
 * data transfer operations. You can supply an object implementing this
 * interface to any upload/download method of the client.
 * 
 * @author Carlo Pelliccia
 */
public interface FTPDataTransferListener {

	/**
	 * Called to notify the listener that the transfer operation has been
	 * initialized.
	 */
	public void started();

	/**
	 * Called to notify the listener that some bytes have been transmitted.
	 * 
	 * @param length
	 *            The number of the bytes transmitted since the last time the
	 *            method was called (or since the begin of the operation, at the
	 *            first call received).
	 */
	public void transferred(int length);

	/**
	 * Called to notify the listener that the transfer operation has been
	 * successfully complete.
	 */
	public void completed();

	/**
	 * Called to notify the listener that the transfer operation has been
	 * aborted.
	 */
	public void aborted();

	/**
	 * Called to notify the listener that the transfer operation has failed due
	 * to an error.
	 */
	public void failed();

}
