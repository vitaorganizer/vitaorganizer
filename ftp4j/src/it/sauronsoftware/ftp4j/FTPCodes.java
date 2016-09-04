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
 * This interface is a constants container, each one representing a common FTP
 * response code.
 * 
 * @author Carlo Pelliccia
 */
public interface FTPCodes {

	public int SYNTAX_ERROR = 500;

	public int SYNTAX_ERROR_IN_PARAMETERS = 501;
	
	public int COMMAND_NOT_IMPLEMENTED = 502;

	public int BAD_SEQUENCE_OF_COMMANDS = 503;

	public int COMMAND_PARAMETER_NOT_IMPLEMENTED = 504;

	public int NOT_LOGGED_IN = 530;

	public int FILE_NOT_FOUND = 550;

	public int PAGE_TYPE_UNKNOWN = 551;

	public int EXCEEDED_STORAGE_ALLOCATION = 552;

	public int FILE_NAME_NOT_ALLOWED = 553;

	public int SERVICE_NOT_AVAILABLE = 421;

	public int CANNOT_OPEN_DATA_CONNECTION = 425;

	public int CONNECTION_CLOSED = 426;

	public int FILE_ACTION_NOT_TAKEN = 450;

	public int LOCAL_ERROR_IN_PROCESSING = 451;

	public int FILE_UNAVAILABLE = 452;

	public int USERNAME_OK = 331;

	public int NEED_ACCOUNT = 332;

	public int PENDING_FURTHER_INFORMATION = 350;

	public int COMMAND_OK = 200;

	public int SUPERFLOUS_COMMAND = 202;

	public int STATUS_MESSAGE = 211;

	public int DIRECTORY_STATUS = 212;

	public int FILE_STATUS = 213;

	public int HELP_MESSAGE = 214;

	public int NAME_SYSTEM_TIME = 215;

	public int SERVICE_READY_FOR_NEW_USER = 220;

	public int SERVICE_CLOSING_CONTROL_CONNECTION = 221;

	public int DATA_CONNECTION_OPEN = 225;

	public int DATA_CONNECTION_CLOSING = 226;

	public int ENTER_PASSIVE_MODE = 227;

	public int USER_LOGGED_IN = 230;

	public int FILE_ACTION_COMPLETED = 250;

	public int PATHNAME_CREATED = 257;

	public int RESTART_MARKER = 110;

	public int SERVICE_NOT_READY = 120;

	public int DATA_CONNECTION_ALREADY_OPEN = 125;

	public int FILE_STATUS_OK = 150;

}
