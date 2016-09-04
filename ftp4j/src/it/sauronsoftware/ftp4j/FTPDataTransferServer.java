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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * This class implements a local server to make data transfer with the remote
 * FTP server.
 * 
 * @author Carlo Pelliccia
 */
class FTPDataTransferServer implements FTPDataTransferConnectionProvider,
		Runnable {

	/**
	 * The ServerSocket object waiting for the incoming connection.
	 */
	private ServerSocket serverSocket = null;

	/**
	 * The socket to be established with the remote host.
	 */
	private Socket socket;

	/**
	 * The exception thrown during the wait for a connection (if any!).
	 */
	private IOException exception;

	/**
	 * The thread executing the listening for incoming connection routine.
	 */
	private Thread thread;

	/**
	 * Build the object.
	 * 
	 * @throws FTPDataTransferException
	 *             If a I/O error occurs.
	 */
	public FTPDataTransferServer() throws FTPDataTransferException {
		boolean useRange = false;
		String aux = System.getProperty(FTPKeys.ACTIVE_DT_PORT_RANGE);
		int start = 0;
		int stop = 0;
		if (aux != null) {
			boolean valid = false;
			StringTokenizer st = new StringTokenizer(aux, "-");
			if (st.countTokens() == 2) {
				String s1 = st.nextToken();
				String s2 = st.nextToken();
				int v1;
				try {
					v1 = Integer.parseInt(s1);
				} catch (NumberFormatException e) {
					v1 = 0;
				}
				int v2;
				try {
					v2 = Integer.parseInt(s2);
				} catch (NumberFormatException e) {
					v2 = 0;
				}
				if (v1 > 0 && v2 > 0 && v2 >= v1) {
					start = v1;
					stop = v2;
					valid = true;
					useRange = true;
				}
			}
			if (!valid) {
				// warning to the developer
				System.err.println("WARNING: invalid value \"" + aux
						+ "\" for the " + FTPKeys.ACTIVE_DT_PORT_RANGE
						+ " system property. The value should "
						+ "be in the start-stop form, with "
						+ "start > 0, stop > 0 and start <= stop.");
			}
		}
		if (useRange) {
			ArrayList availables = new ArrayList();
			for (int i = start; i <= stop; i++) {
				availables.add(new Integer(i));
			}
			int size;
			boolean done = false;
			while (!done && (size = availables.size()) > 0) {
				int rand = (int) Math.floor(Math.random() * size);
				int port = ((Integer) availables.remove(rand)).intValue();
				// Tries with the obtained value;
				try {
					serverSocket = new ServerSocket();
					serverSocket.setReceiveBufferSize(512 * 1024);
					serverSocket.bind(new InetSocketAddress(port));
					done = true;
				} catch (IOException e) {
					// Port not available.
				}
			}
			if (!done) {
				throw new FTPDataTransferException(
						"Cannot open the ServerSocket. "
								+ "No available port found in range " + aux);
			}
		} else {
			// Don't use a port range.
			try {
				serverSocket = new ServerSocket();
				serverSocket.setReceiveBufferSize(512 * 1024);
				serverSocket.bind(new InetSocketAddress(0));
			} catch (IOException e) {
				throw new FTPDataTransferException(
						"Cannot open the ServerSocket", e);
			}
		}
		// Starts the server thread.
		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Returns the local port the server socket is bounded.
	 * 
	 * @return The local port.
	 */
	public int getPort() {
		return serverSocket.getLocalPort();
	}

	public void run() {
		int timeout = 30000;
		String aux = System.getProperty(FTPKeys.ACTIVE_DT_ACCEPT_TIMEOUT);
		if (aux != null) {
			boolean valid = false;
			int value;
			try {
				value = Integer.parseInt(aux);
			} catch (NumberFormatException e) {
				value = -1;
			}
			if (value >= 0) {
				timeout = value;
				valid = true;
			}
			if (!valid) {
				// warning to the developer
				System.err.println("WARNING: invalid value \"" + aux
						+ "\" for the " + FTPKeys.ACTIVE_DT_ACCEPT_TIMEOUT
						+ " system property. The value should "
						+ "be an integer greater or equal to 0.");
			}
		}
		try {
			// Set the socket timeout.
			serverSocket.setSoTimeout(timeout);
			// Wait for the incoming connection.
			socket = serverSocket.accept();
			socket.setSendBufferSize(512 * 1024);
		} catch (IOException e) {
			exception = e;
		} finally {
			// Close the server socket.
			try {
				serverSocket.close();
			} catch (IOException e) {
				;
			}
		}
	}

	/**
	 * Disposes the server and interrupts every operating stream.
	 */
	public void dispose() {
		// Close the server socket (if open).
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				;
			}
		}
	}

	public Socket openDataTransferConnection() throws FTPDataTransferException {
		if (socket == null && exception == null) {
			try {
				thread.join();
			} catch (Exception e) {
				;
			}
		}
		if (exception != null) {
			throw new FTPDataTransferException(
					"Cannot receive the incoming connection", exception);
		}
		if (socket == null) {
			throw new FTPDataTransferException("No socket available");
		}
		return socket;
	}

}
