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
package it.sauronsoftware.ftp4j.connectors;

import it.sauronsoftware.ftp4j.FTPCommunicationChannel;
import it.sauronsoftware.ftp4j.FTPConnector;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPReply;

import java.io.IOException;
import java.net.Socket;

/**
 * This one connects a remote host via a FTP proxy which supports the SITE or
 * the OPEN proxy method.
 * 
 * The connector's default value for the
 * <em>useSuggestedAddressForDataConnections</em> flag is <em>true</em>.
 * 
 * @author Carlo Pelliccia
 */
public class FTPProxyConnector extends FTPConnector {

	/**
	 * Requires the connection to the remote host through a SITE command after
	 * proxy authentication. Default one.
	 */
	public static int STYLE_SITE_COMMAND = 0;

	/**
	 * Requires the connection to the remote host through a OPEN command without
	 * proxy authentication.
	 */
	public static int STYLE_OPEN_COMMAND = 1;

	/**
	 * The proxy host name.
	 */
	private String proxyHost;

	/**
	 * The proxy port.
	 */
	private int proxyPort;

	/**
	 * The proxyUser for proxy authentication.
	 */
	private String proxyUser;

	/**
	 * The proxyPass for proxy authentication.
	 */
	private String proxyPass;

	/**
	 * The style used by the proxy.
	 */
	public int style = STYLE_SITE_COMMAND;

	/**
	 * Builds the connector.
	 * 
	 * Default value for the style is STYLE_SITE_COMMAND.
	 * 
	 * @param proxyHost
	 *            The proxy host name.
	 * @param proxyPort
	 *            The proxy port.
	 * @param proxyUser
	 *            The username for proxy authentication.
	 * @param proxyPass
	 *            The password for proxy authentication.
	 */
	public FTPProxyConnector(String proxyHost, int proxyPort, String proxyUser,
			String proxyPass) {
		super(true);
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.proxyUser = proxyUser;
		this.proxyPass = proxyPass;
	}

	/**
	 * Builds the connector.
	 * 
	 * Default value for the style is STYLE_SITE_COMMAND.
	 * 
	 * @param proxyHost
	 *            The proxy host name.
	 * @param proxyPort
	 *            The proxy port.
	 */
	public FTPProxyConnector(String proxyHost, int proxyPort) {
		this(proxyHost, proxyPort, "anonymous", "ftp4j");
	}

	/**
	 * Sets the style used by the proxy.
	 * 
	 * {@link FTPProxyConnector#STYLE_SITE_COMMAND} - Requires the connection to
	 * the remote host through a SITE command after proxy authentication.
	 * 
	 * {@link FTPProxyConnector#STYLE_OPEN_COMMAND} - Requires the connection to
	 * the remote host through a OPEN command without proxy authentication.
	 * 
	 * Default value for the style is STYLE_SITE_COMMAND.
	 * 
	 * @param style
	 *            The style.
	 * @see FTPProxyConnector#STYLE_SITE_COMMAND
	 * @see FTPProxyConnector#STYLE_OPEN_COMMAND
	 */
	public void setStyle(int style) {
		if (style != STYLE_OPEN_COMMAND && style != STYLE_SITE_COMMAND) {
			throw new IllegalArgumentException("Invalid style");
		}
		this.style = style;
	}

	public Socket connectForCommunicationChannel(String host, int port)
			throws IOException {
		Socket socket = tcpConnectForCommunicationChannel(proxyHost, proxyPort);
		FTPCommunicationChannel communication = new FTPCommunicationChannel(
				socket, "ASCII");
		// Welcome message.
		FTPReply r;
		try {
			r = communication.readFTPReply();
		} catch (FTPIllegalReplyException e) {
			throw new IOException("Invalid proxy response");
		}
		// Does this reply mean "ok"?
		if (r.getCode() != 220) {
			// Mmmmm... it seems no!
			throw new IOException("Invalid proxy response");
		}
		if (style == STYLE_SITE_COMMAND) {
			// Usefull flags.
			boolean passwordRequired;
			// Send the user and read the reply.
			communication.sendFTPCommand("USER " + proxyUser);
			try {
				r = communication.readFTPReply();
			} catch (FTPIllegalReplyException e) {
				throw new IOException("Invalid proxy response");
			}
			switch (r.getCode()) {
			case 230:
				// Password isn't required.
				passwordRequired = false;
				break;
			case 331:
				// Password is required.
				passwordRequired = true;
				break;
			default:
				// User validation failed.
				throw new IOException("Proxy authentication failed");
			}
			// Password.
			if (passwordRequired) {
				// Send the password.
				communication.sendFTPCommand("PASS " + proxyPass);
				try {
					r = communication.readFTPReply();
				} catch (FTPIllegalReplyException e) {
					throw new IOException("Invalid proxy response");
				}
				if (r.getCode() != 230) {
					// Authentication failed.
					throw new IOException("Proxy authentication failed");
				}
			}
			communication.sendFTPCommand("SITE " + host + ":" + port);
		} else if (style == STYLE_OPEN_COMMAND) {
			communication.sendFTPCommand("OPEN " + host + ":" + port);
		}
		return socket;
	}

	public Socket connectForDataTransferChannel(String host, int port)
			throws IOException {
		return tcpConnectForDataTransferChannel(host, port);
	}

}
