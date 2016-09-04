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
 * Static property keys used by the library.
 * 
 * @author Carlo Pelliccia
 * @since 1.3
 */
interface FTPKeys {

	/**
	 * The key used to retrieve the system property with the port range for
	 * active data transfers. The value has to be in the
	 * <em>startPort-endPort</em> form.
	 */
	public String ACTIVE_DT_PORT_RANGE = "ftp4j.activeDataTransfer.portRange";

	/**
	 * The key used to retrieve the system property with the host IPv4 address
	 * for active data transfers. The value has to be in the <em>x.x.x.x</em>
	 * form.
	 */
	public String ACTIVE_DT_HOST_ADDRESS = "ftp4j.activeDataTransfer.hostAddress";

	/**
	 * The key used to retrieve the system property with the accept timeout for
	 * active data transfars. The value should be ms. Default value is 30000. A
	 * 0 value stands for infinite.
	 */
	public String ACTIVE_DT_ACCEPT_TIMEOUT = "ftp4j.activeDataTransfer.acceptTimeout";

	/**
	 * The key used to retrieve the system property that can force the client to
	 * exchange data by connecting to the IP address suggested by the server
	 * after a PASV command. To avoid frequently reported NAT problems, ftp4j
	 * connects always to the host supplied in the
	 * {@link FTPClient#connect(String)} or
	 * {@link FTPClient#connect(String, int)} methods. The response of a PASV
	 * command is used only to decode the port for the connection. By using the
	 * value &quot;true&quot;, &quot;yes&quot; or &quot;1&quot; on this system
	 * property, ftp4j will change its behaviour and it will connect to the IP
	 * address returned from the server.
	 * 
	 * @since 1.5
	 */
	public String PASSIVE_DT_USE_SUGGESTED_ADDRESS = "ftp4j.passiveDataTransfer.useSuggestedAddress";

}
