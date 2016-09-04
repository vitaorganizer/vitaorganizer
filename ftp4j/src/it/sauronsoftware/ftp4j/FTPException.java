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
 * This class helps in represent FTP error codes and messages.
 * 
 * @author Carlo Pelliccia
 */
public class FTPException extends Exception {

	private static final long serialVersionUID = 1L;

	private int code;

	private String message;

	public FTPException(int code) {
		this.code = code;
	}

	public FTPException(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public FTPException(FTPReply reply) {
		StringBuffer message = new StringBuffer();
		String[] lines = reply.getMessages();
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				message.append(System.getProperty("line.separator"));
			}
			message.append(lines[i]);
		}
		this.code = reply.getCode();
		this.message = message.toString();
	}

	/**
	 * Returns the code of the occurred FTP error.
	 * 
	 * @return The code of the occurred FTP error.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the message of the occurred FTP error.
	 * 
	 * @return The message of the occurred FTP error.
	 */
	public String getMessage() {
		return message;
	}

	public String toString() {
		return getClass().getName() + " [code=" + code + ", message= "
				+ message + "]";
	}

}
