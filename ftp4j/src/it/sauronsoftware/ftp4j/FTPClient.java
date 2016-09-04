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

import it.sauronsoftware.ftp4j.connectors.DirectConnector;
import it.sauronsoftware.ftp4j.extrecognizers.DefaultTextualExtensionRecognizer;
import it.sauronsoftware.ftp4j.extrecognizers.ParametricTextualExtensionRecognizer;
import it.sauronsoftware.ftp4j.listparsers.DOSListParser;
import it.sauronsoftware.ftp4j.listparsers.EPLFListParser;
import it.sauronsoftware.ftp4j.listparsers.MLSDListParser;
import it.sauronsoftware.ftp4j.listparsers.NetWareListParser;
import it.sauronsoftware.ftp4j.listparsers.UnixListParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLSocketFactory;

/**
 * This class implements a FTP client.
 * 
 * You can use an instance of this class to connect to a remote FTP site and do
 * FTP operations like directory listing, file upload and download, resume a
 * broken upload/download and so on.
 * 
 * The common flow is: create the object, connect to a remote FTP site with the
 * connect() method, authenticate with login(), do anything you need with the
 * contents of the remote site, quit the site with disconnect().
 * 
 * A FTPClient object can handle a connection per time. Once you have used and
 * disconnected a FTPClient object you can use it again to connect another FTP
 * server.
 * 
 * @author Carlo Pelliccia
 * @version 1.7.1
 */
public class FTPClient {

	/**
	 * The constant for the FTP security level.
	 * 
	 * @since 1.4
	 */
	public static final int SECURITY_FTP = 0;

	/**
	 * The constant for the FTPS (FTP over implicit TLS/SSL) security level.
	 * 
	 * @since 1.4
	 */
	public static final int SECURITY_FTPS = 1;

	/**
	 * The constant for the FTPES (FTP over explicit TLS/SSL) security level.
	 * 
	 * @since 1.4
	 */
	public static final int SECURITY_FTPES = 2;

	/**
	 * The constant for the AUTO file transfer type. It lets the client pick
	 * between textual and binary types, depending on the extension of the file
	 * exchanged through a textual extension recognizer.
	 */
	public static final int TYPE_AUTO = 0;

	/**
	 * The constant for the TEXTUAL file transfer type. It means that the data
	 * sent or received is treated as textual information. This implies charset
	 * conversion during the transfer.
	 */
	public static final int TYPE_TEXTUAL = 1;

	/**
	 * The constant for the BINARY file transfer type. It means that the data
	 * sent or received is treated as a binary stream. The data is taken "as
	 * is", without any charset conversion.
	 */
	public static final int TYPE_BINARY = 2;

	/**
	 * The constant for the MLSD policy that causes the client to use the MLSD
	 * command instead of LIST, but only if the MLSD command is explicitly
	 * supported by the server (the support is tested with the FEAT command).
	 * 
	 * @since 1.5
	 */
	public static final int MLSD_IF_SUPPORTED = 0;

	/**
	 * The constant for the MLSD policy that causes the client to use always the
	 * MLSD command instead of LIST, also if the MLSD command is not explicitly
	 * supported by the server (the support is tested with the FEAT command).
	 * 
	 * @since 1.5
	 */
	public static final int MLSD_ALWAYS = 1;

	/**
	 * The constant for the MLSD policy that causes the client to use always the
	 * LIST command, also if the MLSD command is explicitly supported by the
	 * server (the support is tested with the FEAT command).
	 * 
	 * @since 1.5
	 */
	public static final int MLSD_NEVER = 2;

	/**
	 * The size of the buffer used when sending or receiving data.
	 * 
	 * @since 1.6
	 */
	private static final int SEND_AND_RECEIVE_BUFFER_SIZE = 64 * 1024;

	/**
	 * The DateFormat object used to parse the reply to a MDTM command.
	 */
	private static final DateFormat MDTM_DATE_FORMAT = new SimpleDateFormat(
			"yyyyMMddHHmmss");

	/**
	 * The RegExp Pattern object used to parse the reply to a PASV command.
	 */
	private static final Pattern PASV_PATTERN = Pattern
			.compile("\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3}");

	/**
	 * The RegExp Pattern object used to parse the reply to a PWD command.
	 */
	private static final Pattern PWD_PATTERN = Pattern.compile("\"/.*\"");

	/**
	 * The connector used to connect the remote host.
	 */
	private FTPConnector connector = new DirectConnector();

	/**
	 * The SSL socket factory used to negotiate SSL connections.
	 */
	private SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
			.getDefault();

	/**
	 * The FTPCommunicationListener objects registered on the client.
	 */
	private ArrayList communicationListeners = new ArrayList();

	/**
	 * The FTPListParser objects registered on the client.
	 */
	private ArrayList listParsers = new ArrayList();

	/**
	 * The textual extension recognizer used by the client.
	 */
	private FTPTextualExtensionRecognizer textualExtensionRecognizer = DefaultTextualExtensionRecognizer.getInstance();

	/**
	 * The FTPListParser used successfully during previous connection-scope list
	 * operations.
	 */
	private FTPListParser parser = null;

	/**
	 * If the client is connected, it reports the remote host name or address.
	 */
	private String host = null;

	/**
	 * If the client is connected, it reports the remote port number.
	 */
	private int port = 0;

	/**
	 * The security level. The value should be one of SECURITY_FTP,
	 * SECURITY_FTPS and SECURITY_FTPES constants. Default value is
	 * SECURITY_FTP.
	 */
	private int security = SECURITY_FTP;

	/**
	 * If the client is authenticated, it reports the authentication username.
	 */
	private String username;

	/**
	 * If the client is authenticated, it reports the authentication password.
	 */
	private String password;

	/**
	 * The flag reporting the connection status.
	 */
	private boolean connected = false;

	/**
	 * The flag reporting the authentication status.
	 */
	private boolean authenticated = false;

	/**
	 * The flag for the passive FTP data transfer mode. Default value is true,
	 * cause it's usually the preferred FTP operating mode.
	 */
	private boolean passive = true;

	/**
	 * The type of the data transfer contents (auto, textual, binary). The value
	 * should be one of {@link FTPClient#TYPE_AUTO},
	 * {@link FTPClient#TYPE_TEXTUAL} and {@link FTPClient#TYPE_BINARY}
	 * constants. Default value is TYPE_AUTO.
	 */
	private int type = TYPE_AUTO;

	/**
	 * The MLSD command policy. The value should be one of
	 * {@link FTPClient#MLSD_IF_SUPPORTED}, {@link FTPClient#MLSD_ALWAYS} and
	 * {@link FTPClient#MLSD_NEVER} constants. Default value is
	 * MLSD_IF_SUPPORTED.
	 */
	private int mlsdPolicy = MLSD_IF_SUPPORTED;

	/**
	 * If this value is greater than 0, the auto-noop feature is enabled. If
	 * positive, the field is used as a timeout value (expressed in
	 * milliseconds). If autoNoopDelay milliseconds has passed without any
	 * communication between the client and the server, a NOOP command is
	 * automaticaly sent to the server by the client.
	 */
	private long autoNoopTimeout = 0;

	/**
	 * The auto noop timer thread.
	 */
	private AutoNoopTimer autoNoopTimer;

	/**
	 * The system time (in millis) of the moment when the next auto noop command
	 * should be issued.
	 */
	private long nextAutoNoopTime;

	/**
	 * A flag used to mark whether the connected server supports the resume of
	 * broken transfers.
	 */
	private boolean restSupported = false;

	/**
	 * The name of the charset used to establish textual communications. If not
	 * null the client will use always the given charset. If null the client
	 * tries to auto-detect the server charset. If this attempt fails the client
	 * will use the machine current charset.
	 */
	private String charset = null;

	/**
	 * This flag enables and disables the use of compression (ZLIB) during data
	 * transfers. Compression is enabled when both this flag is true and the
	 * server supports compressed transfers.
	 */
	private boolean compressionEnabled = false;

	/**
	 * A flag used to mark whether the connected server supports UTF-8 pathnames
	 * encoding.
	 */
	private boolean utf8Supported = false;

	/**
	 * A flag used to mark whether the connected server supports the MLSD
	 * command (RFC 3659).
	 */
	private boolean mlsdSupported = false;

	/**
	 * A flag used to mark whether the connected server supports the MODE Z
	 * command.
	 */
	private boolean modezSupported = false;

	/**
	 * A flag used to mark whether MODE Z is enabled.
	 */
	private boolean modezEnabled = false;

	/**
	 * This flag indicates whether the data channel is encrypted.
	 */
	private boolean dataChannelEncrypted = false;

	/**
	 * This flag reports if there's any ongoing abortable data transfer
	 * operation. Its value should be accessed only under the eye of the
	 * abortLock synchronization object.
	 */
	private boolean ongoingDataTransfer = false;

	/**
	 * The InputStream used for data transfer operations.
	 */
	private InputStream dataTransferInputStream = null;

	/**
	 * The OutputStream used for data transfer operations.
	 */
	private OutputStream dataTransferOutputStream = null;

	/**
	 * This flag turns to true when any data transfer stream is closed due to an
	 * abort request.
	 */
	private boolean aborted = false;

	/**
	 * This flags tells if the reply to an ABOR command waits to be consumed.
	 */
	private boolean consumeAborCommandReply = false;

	/**
	 * Lock object used for synchronization.
	 */
	private Object lock = new Object();

	/**
	 * Lock object used for synchronization in abort operations.
	 */
	private Object abortLock = new Object();

	/**
	 * The communication channel established with the server.
	 */
	private FTPCommunicationChannel communication = null;

	/**
	 * Builds and initializes the client.
	 */
	public FTPClient() {
		// The built-in parsers.
		addListParser(new UnixListParser());
		addListParser(new DOSListParser());
		addListParser(new EPLFListParser());
		addListParser(new NetWareListParser());
		addListParser(new MLSDListParser());
	}

	/**
	 * This method returns the connector used to connect the remote host.
	 * 
	 * @return The connector used to connect the remote host.
	 */
	public FTPConnector getConnector() {
		synchronized (lock) {
			return connector;
		}
	}

	/**
	 * This method sets the connector used to connect the remote host.
	 * 
	 * Default one is a
	 * it.sauronsoftware.ftp4j.connectors.direct.DirectConnector instance.
	 * 
	 * @param connector
	 *            The connector used to connect the remote host.
	 * @see DirectConnector
	 */
	public void setConnector(FTPConnector connector) {
		synchronized (lock) {
			this.connector = connector;
		}
	}

	/**
	 * Sets the SSL socket factory used to negotiate SSL connections.
	 * 
	 * @param sslSocketFactory
	 *            The SSL socket factory used to negotiate SSL connections.
	 * 
	 * @since 1.4
	 */
	public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
		synchronized (lock) {
			this.sslSocketFactory = sslSocketFactory;
		}
	}

	/**
	 * Returns the SSL socket factory used to negotiate SSL connections.
	 * 
	 * @return The SSL socket factory used to negotiate SSL connections.
	 * 
	 * @since 1.4
	 */
	public SSLSocketFactory getSSLSocketFactory() {
		synchronized (lock) {
			return sslSocketFactory;
		}
	}

	/**
	 * Sets the security level for the connection. This method should be called
	 * before starting a connection with a server. The security level must be
	 * expressed using one of the SECURITY_FTP, SECURITY_FTPS and SECURITY_FTPES
	 * costants.
	 * 
	 * SECURITY_FTP, which is the default value, applies the basic FTP security
	 * level.
	 * 
	 * SECURITY_FTPS applies the FTPS security level, which is FTP over implicit
	 * TLS/SSL.
	 * 
	 * SECURITY_FTPES applies the FTPES security level, which is FTP over
	 * explicit TLS/SSL.
	 * 
	 * @param security
	 *            The security level.
	 * @throws IllegalStateException
	 *             If the client is already connected to a server.
	 * @throws IllegalArgumentException
	 *             If the supplied security level is not valid.
	 * @since 1.4
	 */
	public void setSecurity(int security) throws IllegalStateException,
			IllegalArgumentException {
		if (security != SECURITY_FTP && security != SECURITY_FTPS && security != SECURITY_FTPES) {
			throw new IllegalArgumentException("Invalid security");
		}
		synchronized (lock) {
			if (connected) {
				throw new IllegalStateException(
						"The security level of the connection can't be "
								+ "changed while the client is connected");
			}
			this.security = security;
		}
	}

	/**
	 * Returns the security level used by the client in the connection.
	 * 
	 * @return The security level, which could be one of the SECURITY_FTP,
	 *         SECURITY_FTPS and SECURITY_FTPES costants.
	 * 
	 * @since 1.4
	 */
	public int getSecurity() {
		return security;
	}

	/**
	 * Applies SSL encryption to an already open socket.
	 * 
	 * @param socket
	 *            The already established socket.
	 * @param host
	 *            The logical destination host.
	 * @param port
	 *            The logical destination port.
	 * @return The SSL socket.
	 * @throws IOException
	 *             If the SSL negotiation fails.
	 */
	private Socket ssl(Socket socket, String host, int port) throws IOException {
		return sslSocketFactory.createSocket(socket, host, port, true);
	}

	/**
	 * This method enables/disables the use of the passive mode.
	 * 
	 * @param passive
	 *            If true the passive mode is enabled.
	 */
	public void setPassive(boolean passive) {
		synchronized (lock) {
			this.passive = passive;
		}
	}

	/**
	 * This methods sets how to treat the contents during a file transfer.
	 * 
	 * The type supplied should be one of TYPE_AUTO, TYPE_TEXTUAL or TYPE_BINARY
	 * constants. Default value is TYPE_AUTO.
	 * 
	 * {@link FTPClient#TYPE_TEXTUAL} means that the data sent or received is
	 * treated as textual information. This implies charset conversion during
	 * the transfer.
	 * 
	 * {@link FTPClient#TYPE_BINARY} means that the data sent or received is
	 * treated as a binary stream. The data is taken "as is", without any
	 * charset conversion.
	 * 
	 * {@link FTPClient#TYPE_AUTO} lets the client pick between textual and
	 * binary types, depending on the extension of the file exchanged, using a
	 * FTPTextualExtensionRecognizer instance, which could be set through the
	 * setTextualExtensionRecognizer method. The default recognizer is an
	 * instance of {@link DefaultTextualExtensionRecognizer}.
	 * 
	 * @param type
	 *            The type.
	 * @throws IllegalArgumentException
	 *             If the supplied type is not valid.
	 * @see FTPClient#setTextualExtensionRecognizer(FTPTextualExtensionRecognizer)
	 * @see DefaultTextualExtensionRecognizer
	 */
	public void setType(int type) throws IllegalArgumentException {
		if (type != TYPE_AUTO && type != TYPE_BINARY && type != TYPE_TEXTUAL) {
			throw new IllegalArgumentException("Invalid type");
		}
		synchronized (lock) {
			this.type = type;
		}
	}

	/**
	 * This method returns the value suggesting how the client encode and decode
	 * the contents during a data transfer.
	 * 
	 * @return The type as a numeric value. The value could be compared to the
	 *         constants {@link FTPClient#TYPE_AUTO},
	 *         {@link FTPClient#TYPE_BINARY} and {@link FTPClient#TYPE_TEXTUAL}.
	 */
	public int getType() {
		synchronized (lock) {
			return type;
		}
	}

	/**
	 * This method lets the user control how the client chooses whether to use
	 * or not the MLSD command (RFC 3659) instead of the base LIST command.
	 * 
	 * The type supplied should be one of MLSD_IF_SUPPORTED, MLSD_ALWAYS or
	 * MLSD_NEVER constants. Default value is MLSD_IF_SUPPORTED.
	 * 
	 * {@link FTPClient#MLSD_IF_SUPPORTED} means that the client should use the
	 * MLSD command only if it is explicitly supported by the server.
	 * 
	 * {@link FTPClient#MLSD_ALWAYS} means that the client should use always the
	 * MLSD command, also if the MLSD command is not explicitly supported by the
	 * server
	 * 
	 * {@link FTPClient#MLSD_NEVER} means that the client should use always only
	 * the LIST command, also if the MLSD command is explicitly supported by the
	 * server.
	 * 
	 * The support for the MLSD command is tested by the client after the
	 * connection to the remote server, with the FEAT command.
	 * 
	 * @param mlsdPolicy
	 *            The MLSD policy.
	 * @throws IllegalArgumentException
	 *             If the supplied MLSD policy value is not valid.
	 * @since 1.5
	 */
	public void setMLSDPolicy(int mlsdPolicy) throws IllegalArgumentException {
		if (type != MLSD_IF_SUPPORTED && type != MLSD_ALWAYS && type != MLSD_NEVER) {
			throw new IllegalArgumentException("Invalid MLSD policy");
		}
		synchronized (lock) {
			this.mlsdPolicy = mlsdPolicy;
		}
	}

	/**
	 * This method returns the value suggesting how the client chooses whether
	 * to use or not the MLSD command (RFC 3659) instead of the base LIST
	 * command.
	 * 
	 * @return The MLSD policy as a numeric value. The value could be compared
	 *         to the constants {@link FTPClient#MLSD_IF_SUPPORTED},
	 *         {@link FTPClient#MLSD_ALWAYS} and {@link FTPClient#MLSD_NEVER}.
	 * @since 1.5
	 */
	public int getMLSDPolicy() {
		synchronized (lock) {
			return mlsdPolicy;
		}
	}

	/**
	 * Returns the name of the charset used to establish textual communications.
	 * If not null the client will use always the given charset. If null the
	 * client tries to auto-detect the server charset. If this attempt fails the
	 * client will use the machine current charset.
	 * 
	 * @return The name of the charset used to establish textual communications.
	 * @since 1.1
	 */
	public String getCharset() {
		synchronized (lock) {
			return charset;
		}
	}

	/**
	 * Sets the name of the charset used to establish textual communications. If
	 * not null the client will use always the given charset. If null the client
	 * tries to auto-detect the server charset. If this attempt fails the client
	 * will use the machine current charset.
	 * 
	 * @param charset
	 *            The name of the charset used to establish textual
	 *            communications.
	 * @since 1.1
	 */
	public void setCharset(String charset) {
		synchronized (lock) {
			this.charset = charset;
			if (connected) {
				try {
					communication.changeCharset(pickCharset());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Checks whether the connected server explicitly supports resuming of
	 * broken data transfers.
	 * 
	 * @return true if the server supports resuming, false otherwise.
	 * @since 1.5.1
	 */
	public boolean isResumeSupported() {
		synchronized (lock) {
			return restSupported;
		}
	}

	/**
	 * Checks whether the connected remote FTP server supports compressed data
	 * transfers (uploads, downloads, list operations etc.). If so, the
	 * compression of any subsequent data transfer (upload, download, list etc.)
	 * can be compressed, saving bandwidth. To enable compression call
	 * {@link FTPClient#setCompressionEnabled(boolean)} .
	 * 
	 * The returned value is not significant if the client is not connected and
	 * authenticated.
	 * 
	 * @return <em>true</em> if compression of data transfers is supported on
	 *         the server-side, <em>false</em> otherwise.
	 * @see FTPClient#isCompressionEnabled()
	 * @since 1.5
	 */
	public boolean isCompressionSupported() {
		return modezSupported;
	}

	/**
	 * Enables or disables the use of compression during any subsequent data
	 * transfer. Compression is enabled when both the supplied value and the
	 * {@link FTPClient#isCompressionSupported()}) returned value are
	 * <em>true</em>.
	 * 
	 * The default value is <em>false</em>.
	 * 
	 * @param compressionEnabled
	 *            <em>true</em> to enable the use of compression during any
	 *            subsequent file transfer, <em>false</em> to disable the
	 *            feature.
	 * @see FTPClient#isCompressionSupported()
	 * @since 1.5
	 */
	public void setCompressionEnabled(boolean compressionEnabled) {
		this.compressionEnabled = compressionEnabled;
	}

	/**
	 * Checks whether the use of compression is enabled on the client-side.
	 * 
	 * Please note that compressed transfers are actually enabled only if both
	 * this method and {@link FTPClient#isCompressionSupported()} return
	 * <em>true</em>.
	 * 
	 * @return <em>true</em> if compression is enabled, <em>false</em>
	 *         otherwise.
	 * @see FTPClient#isCompressionSupported()
	 * @since 1.5
	 */
	public boolean isCompressionEnabled() {
		return compressionEnabled;
	}

	/**
	 * This method returns the textual extension recognizer used by the client.
	 * 
	 * Default one is {@link DefaultTextualExtensionRecognizer}.
	 * 
	 * @return The textual extension recognizer used by the client.
	 * @see DefaultTextualExtensionRecognizer
	 */
	public FTPTextualExtensionRecognizer getTextualExtensionRecognizer() {
		synchronized (lock) {
			return textualExtensionRecognizer;
		}
	}

	/**
	 * This method sets the textual extension recognizer used by the client.
	 * 
	 * The default one is {@link DefaultTextualExtensionRecognizer}.
	 * 
	 * You can plug your own by implementing the
	 * {@link FTPTextualExtensionRecognizer} interface. For your convenience the
	 * ftp4j gives you another FTPTextualExtensionRecognizer implementation,
	 * which is {@link ParametricTextualExtensionRecognizer}.
	 * 
	 * @param textualExtensionRecognizer
	 *            The textual extension recognizer used by the client.
	 * @see DefaultTextualExtensionRecognizer
	 * @see ParametricTextualExtensionRecognizer
	 */
	public void setTextualExtensionRecognizer(FTPTextualExtensionRecognizer textualExtensionRecognizer) {
		synchronized (lock) {
			this.textualExtensionRecognizer = textualExtensionRecognizer;
		}
	}

	/**
	 * This method tests if this client is authenticated.
	 * 
	 * @return true if this client is authenticated, false otherwise.
	 */
	public boolean isAuthenticated() {
		synchronized (lock) {
			return authenticated;
		}
	}

	/**
	 * This method tests if this client is connected to a remote FTP server.
	 * 
	 * @return true if this client is connected to a remote FTP server, false
	 *         otherwise.
	 */
	public boolean isConnected() {
		synchronized (lock) {
			return connected;
		}
	}

	/**
	 * This method tests if this client works in passive FTP mode.
	 * 
	 * @return true if this client is configured to work in passive FTP mode.
	 */
	public boolean isPassive() {
		synchronized (lock) {
			return passive;
		}
	}

	/**
	 * If the client is connected, it reports the remote host name or address.
	 * 
	 * @return The remote host name or address.
	 */
	public String getHost() {
		synchronized (lock) {
			return host;
		}
	}

	/**
	 * If the client is connected, it reports the remote port number.
	 * 
	 * @return The remote port number.
	 */
	public int getPort() {
		synchronized (lock) {
			return port;
		}
	}

	/**
	 * If the client is authenticated, it reports the authentication password.
	 * 
	 * @return The authentication password.
	 */
	public String getPassword() {
		synchronized (lock) {
			return password;
		}
	}

	/**
	 * If the client is authenticated, it reports the authentication username.
	 * 
	 * @return The authentication username.
	 */
	public String getUsername() {
		synchronized (lock) {
			return username;
		}
	}

	/**
	 * Enable and disable the auto-noop feature.
	 * 
	 * If the supplied value is greater than 0, the auto-noop feature is
	 * enabled, otherwise it is disabled. If positive, the field is used as a
	 * timeout value (expressed in milliseconds). If autoNoopDelay milliseconds
	 * has passed without any communication between the client and the server, a
	 * NOOP command is automaticaly sent to the server by the client.
	 * 
	 * The default value for the auto noop delay is 0 (disabled).
	 * 
	 * @param autoNoopTimeout
	 *            The duration of the auto-noop timeout, in milliseconds. If 0
	 *            or less, the auto-noop feature is disabled.
	 * 
	 * @since 1.5
	 */
	public void setAutoNoopTimeout(long autoNoopTimeout) {
		synchronized (lock) {
			if (connected && authenticated) {
				stopAutoNoopTimer();
			}
			long oldValue = this.autoNoopTimeout;
			long newValue = autoNoopTimeout;
			this.autoNoopTimeout = autoNoopTimeout;
			if (oldValue != 0 && newValue != 0 && nextAutoNoopTime > 0) {
				nextAutoNoopTime = nextAutoNoopTime - (oldValue - newValue);
			}
			if (connected && authenticated) {
				startAutoNoopTimer();
			}
		}
	}

	/**
	 * Returns the duration of the auto-noop timeout, in milliseconds. If 0 or
	 * less, the auto-noop feature is disabled.
	 * 
	 * @return The duration of the auto-noop timeout, in milliseconds. If 0 or
	 *         less, the auto-noop feature is disabled.
	 * 
	 * @since 1.5
	 */
	public long getAutoNoopTimeout() {
		synchronized (lock) {
			return autoNoopTimeout;
		}
	}

	/**
	 * This method adds a FTPCommunicationListener to the object.
	 * 
	 * @param listener
	 *            The listener.
	 */
	public void addCommunicationListener(FTPCommunicationListener listener) {
		synchronized (lock) {
			communicationListeners.add(listener);
			if (communication != null) {
				communication.addCommunicationListener(listener);
			}
		}
	}

	/**
	 * This method removes a FTPCommunicationListener previously added to the
	 * object.
	 * 
	 * @param listener
	 *            The listener to be removed.
	 */
	public void removeCommunicationListener(FTPCommunicationListener listener) {
		synchronized (lock) {
			communicationListeners.remove(listener);
			if (communication != null) {
				communication.removeCommunicationListener(listener);
			}
		}
	}

	/**
	 * This method returns a list with all the {@link FTPCommunicationListener}
	 * used by the client.
	 * 
	 * @return A list with all the FTPCommunicationListener used by the client.
	 */
	public FTPCommunicationListener[] getCommunicationListeners() {
		synchronized (lock) {
			int size = communicationListeners.size();
			FTPCommunicationListener[] ret = new FTPCommunicationListener[size];
			for (int i = 0; i < size; i++) {
				ret[i] = (FTPCommunicationListener) communicationListeners.get(i);
			}
			return ret;
		}
	}

	/**
	 * This method adds a {@link FTPListParser} to the object.
	 * 
	 * @param listParser
	 *            The list parser.
	 */
	public void addListParser(FTPListParser listParser) {
		synchronized (lock) {
			listParsers.add(listParser);
		}
	}

	/**
	 * This method removes a {@link FTPListParser} previously added to the
	 * object.
	 * 
	 * @param listParser
	 *            The list parser to be removed.
	 */
	public void removeListParser(FTPListParser listParser) {
		synchronized (lock) {
			listParsers.remove(listParser);
		}
	}

	/**
	 * This method returns a list with all the {@link FTPListParser} used by the
	 * client.
	 * 
	 * @return A list with all the FTPListParsers used by the client.
	 */
	public FTPListParser[] getListParsers() {
		synchronized (lock) {
			int size = listParsers.size();
			FTPListParser[] ret = new FTPListParser[size];
			for (int i = 0; i < size; i++) {
				ret[i] = (FTPListParser) listParsers.get(i);
			}
			return ret;
		}
	}

	/**
	 * This method connects the client to the remote FTP host, using the default
	 * port value 21 (990 if security level is set to FTPS, see
	 * {@link FTPClient#setSecurity(int)}).
	 * 
	 * @param host
	 *            The hostname of the remote server.
	 * @return The server welcome message, one line per array element.
	 * @throws IllegalStateException
	 *             If the client is already connected to a remote host.
	 * @throws IOException
	 *             If an I/O occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the server refuses the connection.
	 */
	public String[] connect(String host) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		int def;
		if (security == SECURITY_FTPS) {
			def = 990;
		} else {
			def = 21;
		}
		return connect(host, def);
	}

	/**
	 * This method connects the client to the remote FTP host.
	 * 
	 * @param host
	 *            The host name or address of the remote server.
	 * @param port
	 *            The port listened by the remote server.
	 * @return The server welcome message, one line per array element.
	 * @throws IllegalStateException
	 *             If the client is already connected to a remote host.
	 * @throws IOException
	 *             If an I/O occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the server refuses the connection.
	 */
	public String[] connect(String host, int port)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client already connected to any host?
			if (connected) {
				throw new IllegalStateException("Client already connected to "
						+ host + " on port " + port);
			}
			// Ok, it's connection time. Let's try!
			Socket connection = null;
			try {
				// Open the connection.
				connection = connector.connectForCommunicationChannel(host, port);
				if (security == SECURITY_FTPS) {
					connection = ssl(connection, host, port);
				}
				// Open the communication channel.
				communication = new FTPCommunicationChannel(connection, pickCharset());
				for (Iterator i = communicationListeners.iterator(); i.hasNext();) {
					communication.addCommunicationListener((FTPCommunicationListener) i.next());
				}
				// Welcome message.
				FTPReply wm = communication.readFTPReply();
				// Does this reply mean "ok"?
				if (!wm.isSuccessCode()) {
					// Mmmmm... it seems no!
					throw new FTPException(wm);
				}
				// Flag this object as connected to the remote host.
				this.connected = true;
				this.authenticated = false;
				this.parser = null;
				this.host = host;
				this.port = port;
				this.username = null;
				this.password = null;
				this.utf8Supported = false;
				this.restSupported = false;
				this.mlsdSupported = false;
				this.modezSupported = false;
				this.dataChannelEncrypted = false;
				// Returns the welcome message.
				return wm.getMessages();
			} catch (IOException e) {
				// D'oh!
				throw e;
			} finally {
				// If connection has failed...
				if (!connected) {
					if (connection != null) {
						// Close the connection, 'cause it should be open.
						try {
							connection.close();
						} catch (Throwable t) {
							;
						}
					}
				}
			}
		}
	}

	/**
	 * Aborts the current connection attempt. It can be called by a secondary
	 * thread while the client is blocked in a <em>connect()</em> call. The
	 * connect() method will exit with an {@link IOException}.
	 * 
	 * @since 1.7
	 */
	public void abortCurrentConnectionAttempt() {
		connector.abortConnectForCommunicationChannel();
	}

	/**
	 * This method disconnects from the remote server, optionally performing the
	 * QUIT procedure.
	 * 
	 * @param sendQuitCommand
	 *            If true the QUIT procedure with the server will be performed,
	 *            otherwise the connection is abruptly closed by the client
	 *            without sending any advice to the server.
	 * @throws IllegalStateException
	 *             If the client is not connected to a remote host.
	 * @throws IOException
	 *             If an I/O occurs (can be thrown only if sendQuitCommand is
	 *             true).
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way (can be thrown only
	 *             if sendQuitCommand is true).
	 * @throws FTPException
	 *             If the server refuses the QUIT command (can be thrown only if
	 *             sendQuitCommand is true).
	 */
	public void disconnect(boolean sendQuitCommand)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Stops the auto noop timer (if started).
			if (authenticated) {
				stopAutoNoopTimer();
			}
			// Send QUIT?
			if (sendQuitCommand) {
				// Call the QUIT command.
				communication.sendFTPCommand("QUIT");
				FTPReply r = communication.readFTPReply();
				if (!r.isSuccessCode()) {
					throw new FTPException(r);
				}
			}
			// Close the communication.
			communication.close();
			communication = null;
			// Reset the connection flag.
			connected = false;
		}
	}

	/**
	 * This method causes the communication channel to be abruptly closed. Use
	 * it carefully, since this one is not thread-safe. It is given as an
	 * "emergency brake" to close the control connection when it is blocked. A
	 * thread-safe solution for the same purpose is a call to disconnect(false).
	 * 
	 * @see FTPClient#disconnect(boolean)
	 */
	public void abruptlyCloseCommunication() {
		// Close the communication.
		if (communication != null) {
			communication.close();
			communication = null;
		}
		// Reset the connection flag.
		connected = false;
		// Stops the auto noop timer.
		stopAutoNoopTimer();
	}

	/**
	 * This method authenticates the user against the server.
	 * 
	 * @param username
	 *            The username.
	 * @param password
	 *            The password (if none set it to null).
	 * @throws IllegalStateException
	 *             If the client is not connected. Call the connect() method
	 *             before!
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If login fails.
	 */
	public void login(String username, String password)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		login(username, password, null);
	}

	/**
	 * This method authenticates the user against the server.
	 * 
	 * @param username
	 *            The username.
	 * @param password
	 *            The password (if none set it to null).
	 * @param account
	 *            The account (if none set it to null). Be careful: some servers
	 *            don't implement this feature.
	 * @throws IllegalStateException
	 *             If the client is not connected. Call the connect() method
	 *             before!
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If login fails.
	 */
	public void login(String username, String password, String account)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// AUTH TLS command if security is FTPES
			if (security == SECURITY_FTPES) {
				communication.sendFTPCommand("AUTH TLS");
				FTPReply r = communication.readFTPReply();
				if (r.isSuccessCode()) {
					communication.ssl(sslSocketFactory);
				} else {
					communication.sendFTPCommand("AUTH SSL");
					r = communication.readFTPReply();
					if (r.isSuccessCode()) {
						communication.ssl(sslSocketFactory);
					} else {
						throw new FTPException(r.getCode(), "SECURITY_FTPES cannot be applied: " +
								"the server refused both AUTH TLS and AUTH SSL commands");
					}
				}
			}
			// Reset the authentication flag.
			authenticated = false;
			// Usefull flags.
			boolean passwordRequired;
			boolean accountRequired;
			// Send the user and read the reply.
			communication.sendFTPCommand("USER " + username);
			FTPReply r = communication.readFTPReply();
			switch (r.getCode()) {
			case 230:
				// Password and account aren't required.
				passwordRequired = false;
				accountRequired = false;
				break;
			case 331:
				// Password is required.
				passwordRequired = true;
				// Account... maybe! More information later...
				accountRequired = false;
				break;
			case 332:
				// Password is not required, but account is required.
				passwordRequired = false;
				accountRequired = true;
			default:
				// User validation failed.
				throw new FTPException(r);
			}
			// Password.
			if (passwordRequired) {
				if (password == null) {
					throw new FTPException(331);
				}
				// Send the password.
				communication.sendFTPCommand("PASS " + password);
				r = communication.readFTPReply();
				switch (r.getCode()) {
				case 230:
					// Account is not required.
					accountRequired = false;
					break;
				case 332:
					// Account is required.
					accountRequired = true;
					break;
				default:
					// Authentication failed.
					throw new FTPException(r);
				}
			}
			// Account.
			if (accountRequired) {
				if (account == null) {
					throw new FTPException(332);
				}
				// Send the account.
				communication.sendFTPCommand("ACCT " + account);
				r = communication.readFTPReply();
				switch (r.getCode()) {
				case 230:
					// Well done!
					break;
				default:
					// Something goes wrong.
					throw new FTPException(r);
				}
			}
			// Well, if this point is reached the client could consider itself
			// as authenticated.
			this.authenticated = true;
			this.username = username;
			this.password = password;
		}
		// Post-login operations.
		postLoginOperations();
		// Starts the auto noop timer.
		startAutoNoopTimer();
	}

	/**
	 * Performs some post-login operations, such trying to detect server support
	 * for utf8.
	 * 
	 * @throws IllegalStateException
	 *             If the client is not connected. Call the connect() method
	 *             before!
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If login fails.
	 */
	private void postLoginOperations() throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			utf8Supported = false;
			restSupported = false;
			mlsdSupported = false;
			modezSupported = false;
			dataChannelEncrypted = false;
			communication.sendFTPCommand("FEAT");
			FTPReply r = communication.readFTPReply();
			if (r.getCode() == 211) {
				String[] lines = r.getMessages();
				for (int i = 1; i < lines.length - 1; i++) {
					String feat = lines[i].trim().toUpperCase();
					// REST STREAM supported?
					if ("REST STREAM".equalsIgnoreCase(feat)) {
						restSupported = true;
						continue;
					}
					// UTF8 supported?
					if ("UTF8".equalsIgnoreCase(feat)) {
						utf8Supported = true;
						communication.changeCharset("UTF-8");
						continue;
					}
					// MLSD supported?
					if ("MLSD".equalsIgnoreCase(feat)) {
						mlsdSupported = true;
						continue;
					}
					// MODE Z supported?
					if ("MODE Z".equalsIgnoreCase(feat) || feat.startsWith("MODE Z ")) {
						modezSupported = true;
						continue;
					}
				}
			}
			// Turn UTF 8 on (if supported).
			if (utf8Supported) {
				communication.sendFTPCommand("OPTS UTF8 ON");
				communication.readFTPReply();
			}
			// Data channel security.
			if (security == SECURITY_FTPS || security == SECURITY_FTPES) {
				communication.sendFTPCommand("PBSZ 0");
				communication.readFTPReply();
				communication.sendFTPCommand("PROT P");
				FTPReply reply = communication.readFTPReply();
				if (reply.isSuccessCode()) {
					dataChannelEncrypted = true;
				}
			}
		}
	}

	/**
	 * This method performs a logout operation for the current user, leaving the
	 * connection open, thus it can be used to start a new user session. Be
	 * careful with this: some FTP servers don't implement this feature, even
	 * though it is a standard FTP one.
	 * 
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void logout() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Send the REIN command.
			communication.sendFTPCommand("REIN");
			FTPReply r = communication.readFTPReply();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			} else {
				// Stops the auto noop timer.
				stopAutoNoopTimer();
				// Ok. Not authenticated, now.
				authenticated = false;
				username = null;
				password = null;
			}
		}
	}

	/**
	 * This method performs a "noop" operation with the server.
	 * 
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If login fails.
	 */
	public void noop() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Safe code
			try {
				// Send the noop.
				communication.sendFTPCommand("NOOP");
				FTPReply r = communication.readFTPReply();
				if (!r.isSuccessCode()) {
					throw new FTPException(r);
				}
			} finally {
				// Resets auto noop timer.
				touchAutoNoopTimer();
			}
		}
	}

	/**
	 * This method sends a custom command to the server. Don't use this method
	 * to send standard commands already supported by the client: this should
	 * cause unexpected results.
	 * 
	 * @param command
	 *            The command line.
	 * @return The reply supplied by the server, parsed and served in an object
	 *         way mode.
	 * @throws IllegalStateException
	 *             If this client is not connected.
	 * @throws IOException
	 *             If a I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 */
	public FTPReply sendCustomCommand(String command)
			throws IllegalStateException, IOException, FTPIllegalReplyException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Sends the command.
			communication.sendFTPCommand(command);
			// Resets auto noop timer.
			touchAutoNoopTimer();
			// Returns the reply.
			return communication.readFTPReply();
		}
	}

	/**
	 * This method sends a SITE specific command to the server.
	 * 
	 * @param command
	 *            The site command.
	 * @return The reply supplied by the server, parsed and served in an object
	 *         way mode.
	 * @throws IllegalStateException
	 *             If this client is not connected.
	 * @throws IOException
	 *             If a I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 */
	public FTPReply sendSiteCommand(String command)
			throws IllegalStateException, IOException, FTPIllegalReplyException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Sends the command.
			communication.sendFTPCommand("SITE " + command);
			// Resets auto noop timer.
			touchAutoNoopTimer();
			// Returns the reply.
			return communication.readFTPReply();
		}
	}

	/**
	 * Call this method to switch the user current account. Be careful with
	 * this: some FTP servers don't implement this feature, even though it is a
	 * standard FTP one.
	 * 
	 * @param account
	 *            The account.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If login fails.
	 */
	public void changeAccount(String account) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Send the ACCT command.
			communication.sendFTPCommand("ACCT " + account);
			// Gets the reply.
			FTPReply r = communication.readFTPReply();
			// Resets auto noop timer.
			touchAutoNoopTimer();
			// Evaluates the response.
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method asks and returns the current working directory.
	 * 
	 * @return path The path to the current working directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public String currentDirectory() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Send the PWD command.
			communication.sendFTPCommand("PWD");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Parse the response.
			String[] messages = r.getMessages();
			if (messages.length != 1) {
				throw new FTPIllegalReplyException();
			}
			Matcher m = PWD_PATTERN.matcher(messages[0]);
			if (m.find()) {
				return messages[0].substring(m.start() + 1, m.end() - 1);
			} else {
				throw new FTPIllegalReplyException();
			}
		}
	}

	/**
	 * This method changes the current working directory.
	 * 
	 * @param path
	 *            The path to the new working directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void changeDirectory(String path) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Send the CWD command.
			communication.sendFTPCommand("CWD " + path);
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method changes the current working directory to the parent one.
	 * 
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void changeDirectoryUp() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the CWD command.
			communication.sendFTPCommand("CDUP");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method asks and returns the last modification date of a file or
	 * directory.
	 * 
	 * @param path
	 *            The path to the file or the directory.
	 * @return The file/directory last modification date.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public Date modifiedDate(String path) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the MDTM command.
			communication.sendFTPCommand("MDTM " + path);
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			String[] messages = r.getMessages();
			if (messages.length != 1) {
				throw new FTPIllegalReplyException();
			} else {
				try {
					return MDTM_DATE_FORMAT.parse(messages[0]);
				} catch (ParseException e) {
					throw new FTPIllegalReplyException();
				}
			}
		}
	}

	/**
	 * This method asks and returns a file size in bytes.
	 * 
	 * @param path
	 *            The path to the file.
	 * @return The file size in bytes.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public long fileSize(String path) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the TYPE I command.
			communication.sendFTPCommand("TYPE I");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Sends the SIZE command.
			communication.sendFTPCommand("SIZE " + path);
			r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			String[] messages = r.getMessages();
			if (messages.length != 1) {
				throw new FTPIllegalReplyException();
			} else {
				try {
					return Long.parseLong(messages[0]);
				} catch (Throwable t) {
					throw new FTPIllegalReplyException();
				}
			}
		}
	}

	/**
	 * This method renames a remote file or directory. It can also be used to
	 * move a file or a directory.
	 * 
	 * In example:
	 * 
	 * <pre>
	 * client.rename(&quot;oldname&quot;, &quot;newname&quot;); // This one renames
	 * </pre>
	 * 
	 * <pre>
	 * client.rename(&quot;the/old/path/oldname&quot;, &quot;/a/new/path/newname&quot;); // This one moves
	 * </pre>
	 * 
	 * @param oldPath
	 *            The current path of the file (or directory).
	 * @param newPath
	 *            The new path for the file (or directory).
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void rename(String oldPath, String newPath)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the RNFR command.
			communication.sendFTPCommand("RNFR " + oldPath);
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (r.getCode() != 350) {
				throw new FTPException(r);
			}
			// Sends the RNFR command.
			communication.sendFTPCommand("RNTO " + newPath);
			r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method deletes a remote file.
	 * 
	 * @param path
	 *            The path to the file.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void deleteFile(String path) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the DELE command.
			communication.sendFTPCommand("DELE " + path);
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method deletes a remote directory.
	 * 
	 * @param path
	 *            The path to the directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void deleteDirectory(String path) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the RMD command.
			communication.sendFTPCommand("RMD " + path);
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method creates a new remote directory in the current working one.
	 * 
	 * @param directoryName
	 *            The name of the new directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public void createDirectory(String directoryName)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the MKD command.
			communication.sendFTPCommand("MKD " + directoryName);
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
		}
	}

	/**
	 * This method calls the HELP command on the remote server, returning a list
	 * of lines with the help contents.
	 * 
	 * @return The help contents, splitted by line.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public String[] help() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the HELP command.
			communication.sendFTPCommand("HELP");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			return r.getMessages();
		}
	}

	/**
	 * This method returns the remote server status, as the result of a FTP STAT
	 * command.
	 * 
	 * @return The remote server status, splitted by line.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 */
	public String[] serverStatus() throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Sends the STAT command.
			communication.sendFTPCommand("STAT");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			return r.getMessages();
		}
	}

	/**
	 * This method lists the entries of the current working directory parsing
	 * the reply to a FTP LIST command.
	 * 
	 * The response to the LIST command is parsed through the FTPListParser
	 * objects registered on the client. The distribution of ftp4j contains some
	 * standard parsers already registered on every FTPClient object created. If
	 * they don't work in your case (a FTPListParseException is thrown), you can
	 * build your own parser implementing the FTPListParser interface and add it
	 * to the client by calling its addListParser() method.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The list() method will break with a
	 * FTPAbortedException.
	 * 
	 * @param fileSpec
	 *            A file filter string. Depending on the server implementation,
	 *            wildcard characters could be accepted.
	 * @return The list of the files (and directories) in the current working
	 *         directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @throws FTPListParseException
	 *             If none of the registered parsers can handle the response
	 *             sent by the server.
	 * @see FTPListParser
	 * @see FTPClient#addListParser(FTPListParser)
	 * @see FTPClient#getListParsers()
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 * @see FTPClient#listNames()
	 * @since 1.2
	 */
	public FTPFile[] list(String fileSpec) throws IllegalStateException,
			IOException, FTPIllegalReplyException, FTPException,
			FTPDataTransferException, FTPAbortedException,
			FTPListParseException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// ASCII, please!
			communication.sendFTPCommand("TYPE A");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Prepares the connection for the data transfer.
			FTPDataTransferConnectionProvider provider = openDataTransferChannel();
			// MLSD or LIST command?
			boolean mlsdCommand;
			if (mlsdPolicy == MLSD_IF_SUPPORTED) {
				mlsdCommand = mlsdSupported;
			} else if (mlsdPolicy == MLSD_ALWAYS) {
				mlsdCommand = true;
			} else {
				mlsdCommand = false;
			}
			String command = mlsdCommand ? "MLSD" : "LIST";
			// Adds the file/directory selector.
			if (fileSpec != null && fileSpec.length() > 0) {
				command += " " + fileSpec;
			}
			// Prepares the lines array.
			ArrayList lines = new ArrayList();
			// Local abort state.
			boolean wasAborted = false;
			// Sends the command.
			communication.sendFTPCommand(command);
			try {
				Socket dtConnection;
				try {
					dtConnection = provider.openDataTransferConnection();
				} finally {
					provider.dispose();
				}
				// Change the operation status.
				synchronized (abortLock) {
					ongoingDataTransfer = true;
					aborted = false;
					consumeAborCommandReply = false;
				}
				// Fetch the list from the data transfer connection.
				NVTASCIIReader dataReader = null;
				try {
					// Opens the data transfer connection.
					dataTransferInputStream = dtConnection.getInputStream();
					// MODE Z enabled?
					if (modezEnabled) {
						dataTransferInputStream = new InflaterInputStream(dataTransferInputStream);
					}
					// Let's do it!
					dataReader = new NVTASCIIReader(dataTransferInputStream, mlsdCommand ? "UTF-8" : pickCharset());
					String line;
					while ((line = dataReader.readLine()) != null) {
						if (line.length() > 0) {
							lines.add(line);
						}
					}
				} catch (IOException e) {
					synchronized (abortLock) {
						if (aborted) {
							throw new FTPAbortedException();
						} else {
							throw new FTPDataTransferException(
									"I/O error in data transfer", e);
						}
					}
				} finally {
					if (dataReader != null) {
						try {
							dataReader.close();
						} catch (Throwable t) {
							;
						}
					}
					try {
						dtConnection.close();
					} catch (Throwable t) {
						;
					}
					// Set to null the instance-level input stream.
					dataTransferInputStream = null;
					// Change the operation status.
					synchronized (abortLock) {
						wasAborted = aborted;
						ongoingDataTransfer = false;
						aborted = false;
					}
				}
			} finally {
				r = communication.readFTPReply();
				touchAutoNoopTimer();
				if (r.getCode() != 150 && r.getCode() != 125) {
					throw new FTPException(r);
				}
				// Consumes the result reply of the transfer.
				r = communication.readFTPReply();
				if (!wasAborted && r.getCode() != 226) {
					throw new FTPException(r);
				}
				// ABOR command response (if needed).
				if (consumeAborCommandReply) {
					communication.readFTPReply();
					consumeAborCommandReply = false;
				}
			}
			// Build an array of lines.
			int size = lines.size();
			String[] list = new String[size];
			for (int i = 0; i < size; i++) {
				list[i] = (String) lines.get(i);
			}
			// Parse the list.
			FTPFile[] ret = null;
			if (mlsdCommand) {
				// Forces the MLSDListParser.
				MLSDListParser parser = new MLSDListParser();
				ret = parser.parse(list);
			} else {
				// Is there any already successful parser?
				if (parser != null) {
					// Yes, let's try with it.
					try {
						ret = parser.parse(list);
					} catch (FTPListParseException e) {
						// That parser doesn't work anymore.
						parser = null;
					}
				}
				// Is there an available result?
				if (ret == null) {
					// Try to parse the list with every available parser.
					for (Iterator i = listParsers.iterator(); i.hasNext();) {
						FTPListParser aux = (FTPListParser) i.next();
						try {
							// Let's try!
							ret = aux.parse(list);
							// This parser smells good!
							parser = aux;
							// Leave the loop.
							break;
						} catch (FTPListParseException e) {
							// Let's try the next one.
							continue;
						}
					}
				}
			}
			if (ret == null) {
				// None of the parsers can handle the list response.
				throw new FTPListParseException();
			} else {
				// Return the parsed list.
				return ret;
			}
		}
	}

	/**
	 * This method lists the entries of the current working directory parsing
	 * the reply to a FTP LIST command.
	 * 
	 * The response to the LIST command is parsed through the FTPListParser
	 * objects registered on the client. The distribution of ftp4j contains some
	 * standard parsers already registered on every FTPClient object created. If
	 * they don't work in your case (a FTPListParseException is thrown), you can
	 * build your own parser implementing the FTPListParser interface and add it
	 * to the client by calling its addListParser() method.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The list() method will break with a
	 * FTPAbortedException.
	 * 
	 * @return The list of the files (and directories) in the current working
	 *         directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @throws FTPListParseException
	 *             If none of the registered parsers can handle the response
	 *             sent by the server.
	 * @see FTPListParser
	 * @see FTPClient#addListParser(FTPListParser)
	 * @see FTPClient#getListParsers()
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 * @see FTPClient#listNames()
	 */
	public FTPFile[] list() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException,
			FTPDataTransferException, FTPAbortedException, FTPListParseException {
		return list(null);
	}

	/**
	 * This method lists the entries of the current working directory with a FTP
	 * NLST command.
	 * 
	 * The response consists in an array of string, each one reporting the name
	 * of a file or a directory placed in the current working directory. For a
	 * more detailed directory listing procedure look at the list() method.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The listNames() method will break with a
	 * FTPAbortedException.
	 * 
	 * @return The list of the files (and directories) in the current working
	 *         directory.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @throws FTPListParseException
	 *             If none of the registered parsers can handle the response
	 *             sent by the server.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 * @see FTPClient#list()
	 */
	public String[] listNames() throws IllegalStateException, IOException, FTPIllegalReplyException, FTPException,
			FTPDataTransferException, FTPAbortedException, FTPListParseException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// ASCII, please!
			communication.sendFTPCommand("TYPE A");
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Prepares the lines array.
			ArrayList lines = new ArrayList();
			// Local abort state.
			boolean wasAborted = false;
			// Prepares the connection for the data transfer.
			FTPDataTransferConnectionProvider provider = openDataTransferChannel();
			// Send the NLST command.
			communication.sendFTPCommand("NLST");
			try {
				Socket dtConnection;
				try {
					dtConnection = provider.openDataTransferConnection();
				} finally {
					provider.dispose();
				}
				// Change the operation status.
				synchronized (abortLock) {
					ongoingDataTransfer = true;
					aborted = false;
					consumeAborCommandReply = false;
				}
				// Fetch the list from the data transfer connection.
				NVTASCIIReader dataReader = null;
				try {
					// Opens the data transfer connection.
					dataTransferInputStream = dtConnection.getInputStream();
					// MODE Z enabled?
					if (modezEnabled) {
						dataTransferInputStream = new InflaterInputStream(dataTransferInputStream);
					}
					// Let's do it!
					dataReader = new NVTASCIIReader(dataTransferInputStream, pickCharset());
					String line;
					while ((line = dataReader.readLine()) != null) {
						if (line.length() > 0) {
							lines.add(line);
						}
					}
				} catch (IOException e) {
					synchronized (abortLock) {
						if (aborted) {
							throw new FTPAbortedException();
						} else {
							throw new FTPDataTransferException(
									"I/O error in data transfer", e);
						}
					}
				} finally {
					if (dataReader != null) {
						try {
							dataReader.close();
						} catch (Throwable t) {
							;
						}
					}
					try {
						dtConnection.close();
					} catch (Throwable t) {
						;
					}
					// Set to null the instance-level input stream.
					dataTransferInputStream = null;
					// Change the operation status.
					synchronized (abortLock) {
						wasAborted = aborted;
						ongoingDataTransfer = false;
						aborted = false;
					}
				}
			} finally {
				r = communication.readFTPReply();
				if (r.getCode() != 150 && r.getCode() != 125) {
					throw new FTPException(r);
				}
				// Consumes the result reply of the transfer.
				r = communication.readFTPReply();
				if (!wasAborted && r.getCode() != 226) {
					throw new FTPException(r);
				}
				// ABOR command response (if needed).
				if (consumeAborCommandReply) {
					communication.readFTPReply();
					consumeAborCommandReply = false;
				}
			}
			// Build an array.
			int size = lines.size();
			String[] list = new String[size];
			for (int i = 0; i < size; i++) {
				list[i] = (String) lines.get(i);
			}
			return list;
		}
	}

	/**
	 * This method uploads a file to the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param file
	 *            The file to upload.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void upload(File file) throws IllegalStateException,
			FileNotFoundException, IOException, FTPIllegalReplyException,
			FTPException, FTPDataTransferException, FTPAbortedException {
		upload(file, 0, null);
	}

	/**
	 * This method uploads a file to the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param file
	 *            The file to upload.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void upload(File file, FTPDataTransferListener listener)
			throws IllegalStateException, FileNotFoundException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		upload(file, 0, listener);
	}

	/**
	 * This method uploads a file to the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param file
	 *            The file to upload.
	 * @param restartAt
	 *            The restart point (number of bytes already uploaded). Use
	 *            {@link FTPClient#isResumeSupported()} to check if the server
	 *            supports resuming of broken data transfers.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void upload(File file, long restartAt) throws IllegalStateException,
			FileNotFoundException, IOException, FTPIllegalReplyException,
			FTPException, FTPDataTransferException, FTPAbortedException {
		upload(file, restartAt, null);
	}

	/**
	 * This method uploads a file to the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param file
	 *            The file to upload.
	 * @param restartAt
	 *            The restart point (number of bytes already uploaded). Use
	 *            {@link FTPClient#isResumeSupported()} to check if the server
	 *            supports resuming of broken data transfers.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void upload(File file, long restartAt,
			FTPDataTransferListener listener) throws IllegalStateException,
			FileNotFoundException, IOException, FTPIllegalReplyException,
			FTPException, FTPDataTransferException, FTPAbortedException {
		if (!file.exists()) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (IOException e) {
			throw new FTPDataTransferException(e);
		}
		try {
			upload(file.getName(), inputStream, restartAt, restartAt, listener);
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (FTPIllegalReplyException e) {
			throw e;
		} catch (FTPException e) {
			throw e;
		} catch (FTPDataTransferException e) {
			throw e;
		} catch (FTPAbortedException e) {
			throw e;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Throwable t) {
					;
				}
			}
		}
	}

	/**
	 * This method uploads a content to the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param fileName
	 *            The name of the remote file.
	 * @param inputStream
	 *            The source of data.
	 * @param restartAt
	 *            The restart point (number of bytes already uploaded). Use
	 *            {@link FTPClient#isResumeSupported()} to check if the server
	 *            supports resuming of broken data transfers.
	 * @param streamOffset
	 *            The offset to skip in the stream.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void upload(String fileName, InputStream inputStream,
			long restartAt, long streamOffset, FTPDataTransferListener listener)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Select the type of contents.
			int tp = type;
			if (tp == TYPE_AUTO) {
				tp = detectType(fileName);
			}
			if (tp == TYPE_TEXTUAL) {
				communication.sendFTPCommand("TYPE A");
			} else if (tp == TYPE_BINARY) {
				communication.sendFTPCommand("TYPE I");
			}
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Prepares the connection for the data transfer.
			FTPDataTransferConnectionProvider provider = openDataTransferChannel();
			// REST command (if supported and/or requested).
			if (restSupported || restartAt > 0) {
				boolean done = false;
				try {
					communication.sendFTPCommand("REST " + restartAt);
					r = communication.readFTPReply();
					touchAutoNoopTimer();
					if (r.getCode() != 350 && ((r.getCode() != 501 && r.getCode() != 502) || restartAt > 0)) {
						throw new FTPException(r);
					}
					done = true;
				} finally {
					if (!done) {
						provider.dispose();
					}
				}
			}
			// Local abort state.
			boolean wasAborted = false;
			// Send the STOR command.
			communication.sendFTPCommand("STOR " + fileName);
			try {
				Socket dtConnection;
				try {
					dtConnection = provider.openDataTransferConnection();
				} finally {
					provider.dispose();
				}
				// Change the operation status.
				synchronized (abortLock) {
					ongoingDataTransfer = true;
					aborted = false;
					consumeAborCommandReply = false;
				}
				// Upload the stream.
				try {
					// Skips.
					inputStream.skip(streamOffset);
					// Opens the data transfer connection.
					dataTransferOutputStream = dtConnection.getOutputStream();
					// MODE Z enabled?
					if (modezEnabled) {
						dataTransferOutputStream = new DeflaterOutputStream(dataTransferOutputStream);
					}
					// Listeners.
					if (listener != null) {
						listener.started();
					}
					// Let's do it!
					if (tp == TYPE_TEXTUAL) {
						Reader reader = new InputStreamReader(inputStream);
						Writer writer = new OutputStreamWriter(
								dataTransferOutputStream, pickCharset());
						char[] buffer = new char[SEND_AND_RECEIVE_BUFFER_SIZE];
						int l;
						while ((l = reader.read(buffer)) != -1) {
							writer.write(buffer, 0, l);
							writer.flush();
							if (listener != null) {
								listener.transferred(l);
							}
						}
					} else if (tp == TYPE_BINARY) {
						byte[] buffer = new byte[SEND_AND_RECEIVE_BUFFER_SIZE];
						int l;
						while ((l = inputStream.read(buffer)) != -1) {
							dataTransferOutputStream.write(buffer, 0, l);
							dataTransferOutputStream.flush();
							if (listener != null) {
								listener.transferred(l);
							}
						}
					}
				} catch (IOException e) {
					synchronized (abortLock) {
						if (aborted) {
							if (listener != null) {
								listener.aborted();
							}
							throw new FTPAbortedException();
						} else {
							if (listener != null) {
								listener.failed();
							}
							throw new FTPDataTransferException(
									"I/O error in data transfer", e);
						}
					}
				} finally {
					// Closing stream and data connection.
					if (dataTransferOutputStream != null) {
						try {
							dataTransferOutputStream.close();
						} catch (Throwable t) {
							;
						}
					}
					try {
						dtConnection.close();
					} catch (Throwable t) {
						;
					}
					// Set to null the instance-level input stream.
					dataTransferOutputStream = null;
					// Change the operation status.
					synchronized (abortLock) {
						wasAborted = aborted;
						ongoingDataTransfer = false;
						aborted = false;
					}
				}
			} finally {
				// Data transfer command reply.
				r = communication.readFTPReply();
				touchAutoNoopTimer();
				if (r.getCode() != 150 && r.getCode() != 125) {
					throw new FTPException(r);
				}
				// Consumes the result reply of the transfer.
				r = communication.readFTPReply();
				if (!wasAborted && r.getCode() != 226) {
					throw new FTPException(r);
				}
				// ABOR command response (if needed).
				if (consumeAborCommandReply) {
					communication.readFTPReply();
					consumeAborCommandReply = false;
				}
			}
			// Listener notification.
			if (listener != null) {
				listener.completed();
			}
		}
	}

	/**
	 * This method appends the contents of a local file to an existing file on
	 * the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param file
	 *            The local file whose contents will be appended to the remote
	 *            file.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 * @since 1.6
	 */
	public void append(File file) throws IllegalStateException,
			FileNotFoundException, IOException, FTPIllegalReplyException,
			FTPException, FTPDataTransferException, FTPAbortedException {
		append(file, null);
	}

	/**
	 * This method uploads a file to the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param file
	 *            The local file whose contents will be appended to the remote
	 *            file.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 * @since 1.6
	 */
	public void append(File file, FTPDataTransferListener listener)
			throws IllegalStateException, FileNotFoundException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		if (!file.exists()) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
		} catch (IOException e) {
			throw new FTPDataTransferException(e);
		}
		try {
			append(file.getName(), inputStream, 0, listener);
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (FTPIllegalReplyException e) {
			throw e;
		} catch (FTPException e) {
			throw e;
		} catch (FTPDataTransferException e) {
			throw e;
		} catch (FTPAbortedException e) {
			throw e;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Throwable t) {
					;
				}
			}
		}
	}

	/**
	 * This method appends data to an existing file on the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param fileName
	 *            The name of the remote file.
	 * @param inputStream
	 *            The source of data.
	 * @param streamOffset
	 *            The offset to skip in the stream.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 * @since 1.6
	 */
	public void append(String fileName, InputStream inputStream,
			long streamOffset, FTPDataTransferListener listener)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Select the type of contents.
			int tp = type;
			if (tp == TYPE_AUTO) {
				tp = detectType(fileName);
			}
			if (tp == TYPE_TEXTUAL) {
				communication.sendFTPCommand("TYPE A");
			} else if (tp == TYPE_BINARY) {
				communication.sendFTPCommand("TYPE I");
			}
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Local abort state.
			boolean wasAborted = false;
			// Prepares the connection for the data transfer.
			FTPDataTransferConnectionProvider provider = openDataTransferChannel();
			// Send the STOR command.
			communication.sendFTPCommand("APPE " + fileName);
			try {
				Socket dtConnection;
				try {
					dtConnection = provider.openDataTransferConnection();
				} finally {
					provider.dispose();
				}
				// Change the operation status.
				synchronized (abortLock) {
					ongoingDataTransfer = true;
					aborted = false;
					consumeAborCommandReply = false;
				}
				// Upload the stream.
				try {
					// Skips.
					inputStream.skip(streamOffset);
					// Opens the data transfer connection.
					dataTransferOutputStream = dtConnection.getOutputStream();
					// MODE Z enabled?
					if (modezEnabled) {
						dataTransferOutputStream = new DeflaterOutputStream(dataTransferOutputStream);
					}
					// Listeners.
					if (listener != null) {
						listener.started();
					}
					// Let's do it!
					if (tp == TYPE_TEXTUAL) {
						Reader reader = new InputStreamReader(inputStream);
						Writer writer = new OutputStreamWriter(
								dataTransferOutputStream, pickCharset());
						char[] buffer = new char[SEND_AND_RECEIVE_BUFFER_SIZE];
						int l;
						while ((l = reader.read(buffer)) != -1) {
							writer.write(buffer, 0, l);
							writer.flush();
							if (listener != null) {
								listener.transferred(l);
							}
						}
					} else if (tp == TYPE_BINARY) {
						byte[] buffer = new byte[SEND_AND_RECEIVE_BUFFER_SIZE];
						int l;
						while ((l = inputStream.read(buffer)) != -1) {
							dataTransferOutputStream.write(buffer, 0, l);
							dataTransferOutputStream.flush();
							if (listener != null) {
								listener.transferred(l);
							}
						}
					}
				} catch (IOException e) {
					synchronized (abortLock) {
						if (aborted) {
							if (listener != null) {
								listener.aborted();
							}
							throw new FTPAbortedException();
						} else {
							if (listener != null) {
								listener.failed();
							}
							throw new FTPDataTransferException(
									"I/O error in data transfer", e);
						}
					}
				} finally {
					// Closing stream and data connection.
					if (dataTransferOutputStream != null) {
						try {
							dataTransferOutputStream.close();
						} catch (Throwable t) {
							;
						}
					}
					try {
						dtConnection.close();
					} catch (Throwable t) {
						;
					}
					// Set to null the instance-level input stream.
					dataTransferOutputStream = null;
					// Change the operation status.
					synchronized (abortLock) {
						wasAborted = aborted;
						ongoingDataTransfer = false;
						aborted = false;
					}
				}
			} finally {
				r = communication.readFTPReply();
				touchAutoNoopTimer();
				if (r.getCode() != 150 && r.getCode() != 125) {
					throw new FTPException(r);
				}
				// Consumes the result reply of the transfer.
				r = communication.readFTPReply();
				if (!wasAborted && r.getCode() != 226) {
					throw new FTPException(r);
				}
				// ABOR command response (if needed).
				if (consumeAborCommandReply) {
					communication.readFTPReply();
					consumeAborCommandReply = false;
				}
			}
			// Notifies the listener.
			if (listener != null) {
				listener.completed();
			}
		}
	}

	/**
	 * This method downloads a remote file from the server to a local file.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param remoteFileName
	 *            The name of the file to download.
	 * @param localFile
	 *            The local file.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void download(String remoteFileName, File localFile)
			throws IllegalStateException, FileNotFoundException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		download(remoteFileName, localFile, 0, null);
	}

	/**
	 * This method downloads a remote file from the server to a local file.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param remoteFileName
	 *            The name of the file to download.
	 * @param localFile
	 *            The local file.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void download(String remoteFileName, File localFile,
			FTPDataTransferListener listener) throws IllegalStateException,
			FileNotFoundException, IOException, FTPIllegalReplyException,
			FTPException, FTPDataTransferException, FTPAbortedException {
		download(remoteFileName, localFile, 0, listener);
	}

	/**
	 * This method resumes a download operation from the remote server to a
	 * local file.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param remoteFileName
	 *            The name of the file to download.
	 * @param localFile
	 *            The local file.
	 * @param restartAt
	 *            The restart point (number of bytes already downloaded). Use
	 *            {@link FTPClient#isResumeSupported()} to check if the server
	 *            supports resuming of broken data transfers.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void download(String remoteFileName, File localFile, long restartAt)
			throws IllegalStateException, FileNotFoundException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		download(remoteFileName, localFile, restartAt, null);
	}

	/**
	 * This method resumes a download operation from the remote server to a
	 * local file.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param remoteFileName
	 *            The name of the file to download.
	 * @param localFile
	 *            The local file.
	 * @param restartAt
	 *            The restart point (number of bytes already downloaded). Use
	 *            {@link FTPClient#isResumeSupported()} to check if the server
	 *            supports resuming of broken data transfers.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws FileNotFoundException
	 *             If the supplied file cannot be found.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void download(String remoteFileName, File localFile, long restartAt,
			FTPDataTransferListener listener) throws IllegalStateException,
			FileNotFoundException, IOException, FTPIllegalReplyException,
			FTPException, FTPDataTransferException, FTPAbortedException {
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(localFile, restartAt > 0);
		} catch (IOException e) {
			throw new FTPDataTransferException(e);
		}
		try {
			download(remoteFileName, outputStream, restartAt, listener);
		} catch (IllegalStateException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (FTPIllegalReplyException e) {
			throw e;
		} catch (FTPException e) {
			throw e;
		} catch (FTPDataTransferException e) {
			throw e;
		} catch (FTPAbortedException e) {
			throw e;
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (Throwable t) {
					;
				}
			}
		}
	}

	/**
	 * This method resumes a download operation from the remote server.
	 * 
	 * Calling this method blocks the current thread until the operation is
	 * completed. The operation could be interrupted by another thread calling
	 * abortCurrentDataTransfer(). The method will break with a
	 * FTPAbortedException.
	 * 
	 * @param fileName
	 *            The name of the remote file.
	 * @param outputStream
	 *            The destination stream of data read during the download.
	 * @param restartAt
	 *            The restart point (number of bytes already downloaded). Use
	 *            {@link FTPClient#isResumeSupported()} to check if the server
	 *            supports resuming of broken data transfers.
	 * @param listener
	 *            The listener for the operation. Could be null.
	 * @throws IllegalStateException
	 *             If the client is not connected or not authenticated.
	 * @throws IOException
	 *             If an I/O error occurs.
	 * @throws FTPIllegalReplyException
	 *             If the server replies in an illegal way.
	 * @throws FTPException
	 *             If the operation fails.
	 * @throws FTPDataTransferException
	 *             If a I/O occurs in the data transfer connection. If you
	 *             receive this exception the transfer failed, but the main
	 *             connection with the remote FTP server is in theory still
	 *             working.
	 * @throws FTPAbortedException
	 *             If operation is aborted by another thread.
	 * @see FTPClient#abortCurrentDataTransfer(boolean)
	 */
	public void download(String fileName, OutputStream outputStream,
			long restartAt, FTPDataTransferListener listener)
			throws IllegalStateException, IOException,
			FTPIllegalReplyException, FTPException, FTPDataTransferException,
			FTPAbortedException {
		synchronized (lock) {
			// Is this client connected?
			if (!connected) {
				throw new IllegalStateException("Client not connected");
			}
			// Is this client authenticated?
			if (!authenticated) {
				throw new IllegalStateException("Client not authenticated");
			}
			// Select the type of contents.
			int tp = type;
			if (tp == TYPE_AUTO) {
				tp = detectType(fileName);
			}
			if (tp == TYPE_TEXTUAL) {
				communication.sendFTPCommand("TYPE A");
			} else if (tp == TYPE_BINARY) {
				communication.sendFTPCommand("TYPE I");
			}
			FTPReply r = communication.readFTPReply();
			touchAutoNoopTimer();
			if (!r.isSuccessCode()) {
				throw new FTPException(r);
			}
			// Prepares the connection for the data transfer.
			FTPDataTransferConnectionProvider provider = openDataTransferChannel();
			// REST command (if supported and/or requested).
			if (restSupported || restartAt > 0) {
				boolean done = false;
				try {
					communication.sendFTPCommand("REST " + restartAt);
					r = communication.readFTPReply();
					touchAutoNoopTimer();
					if (r.getCode() != 350 && ((r.getCode() != 501 && r.getCode() != 502) || restartAt > 0)) {
						throw new FTPException(r);
					}
					done = true;
				} finally {
					if (!done) {
						provider.dispose();
					}
				}
			}
			// Local abort state.
			boolean wasAborted = false;
			// Send the RETR command.
			communication.sendFTPCommand("RETR " + fileName);
			try {
				Socket dtConnection;
				try {
					dtConnection = provider.openDataTransferConnection();
				} finally {
					provider.dispose();
				}
				// Change the operation status.
				synchronized (abortLock) {
					ongoingDataTransfer = true;
					aborted = false;
					consumeAborCommandReply = false;
				}
				// Download the stream.
				try {
					// Opens the data transfer connection.
					dataTransferInputStream = dtConnection.getInputStream();
					// MODE Z enabled?
					if (modezEnabled) {
						dataTransferInputStream = new InflaterInputStream(dataTransferInputStream);
					}
					// Listeners.
					if (listener != null) {
						listener.started();
					}
					// Let's do it!
					if (tp == TYPE_TEXTUAL) {
						Reader reader = new InputStreamReader(
								dataTransferInputStream, pickCharset());
						Writer writer = new OutputStreamWriter(outputStream);
						char[] buffer = new char[SEND_AND_RECEIVE_BUFFER_SIZE];
						int l;
						while ((l = reader.read(buffer, 0, buffer.length)) != -1) {
							writer.write(buffer, 0, l);
							writer.flush();
							if (listener != null) {
								listener.transferred(l);
							}
						}
					} else if (tp == TYPE_BINARY) {
						byte[] buffer = new byte[SEND_AND_RECEIVE_BUFFER_SIZE];
						int l;
						while ((l = dataTransferInputStream.read(buffer, 0,
								buffer.length)) != -1) {
							outputStream.write(buffer, 0, l);
							if (listener != null) {
								listener.transferred(l);
							}
						}
					}
				} catch (IOException e) {
					synchronized (abortLock) {
						if (aborted) {
							if (listener != null) {
								listener.aborted();
							}
							throw new FTPAbortedException();
						} else {
							if (listener != null) {
								listener.failed();
							}
							throw new FTPDataTransferException(
									"I/O error in data transfer", e);
						}
					}
				} finally {
					// Closing stream and data connection.
					if (dataTransferInputStream != null) {
						try {
							dataTransferInputStream.close();
						} catch (Throwable t) {
							;
						}
					}
					try {
						dtConnection.close();
					} catch (Throwable t) {
						;
					}
					// Set to null the instance-level input stream.
					dataTransferInputStream = null;
					// Change the operation status.
					synchronized (abortLock) {
						wasAborted = aborted;
						ongoingDataTransfer = false;
						aborted = false;
					}
				}
			} finally {
				r = communication.readFTPReply();
				touchAutoNoopTimer();
				if (r.getCode() != 150 && r.getCode() != 125) {
					throw new FTPException(r);
				}
				// Consumes the result reply of the transfer.
				r = communication.readFTPReply();
				if (!wasAborted && r.getCode() != 226) {
					throw new FTPException(r);
				}
				// ABOR command response (if needed).
				if (consumeAborCommandReply) {
					communication.readFTPReply();
					consumeAborCommandReply = false;
				}
			}
			// Notifies the listener.
			if (listener != null) {
				listener.completed();
			}
		}
	}

	/**
	 * This method detects the type for a file transfer.
	 */
	private int detectType(String fileName) throws IOException,
			FTPIllegalReplyException, FTPException {
		int start = fileName.lastIndexOf('.') + 1;
		int stop = fileName.length();
		if (start > 0 && start < stop - 1) {
			String ext = fileName.substring(start, stop);
			ext = ext.toLowerCase();
			if (textualExtensionRecognizer.isTextualExt(ext)) {
				return TYPE_TEXTUAL;
			} else {
				return TYPE_BINARY;
			}
		} else {
			return TYPE_BINARY;
		}
	}

	/**
	 * This method opens a data transfer channel.
	 */
	private FTPDataTransferConnectionProvider openDataTransferChannel()
			throws IOException, FTPIllegalReplyException, FTPException,
			FTPDataTransferException {
		// MODE Z?
		if (modezSupported && compressionEnabled) {
			if (!modezEnabled) {
				// Sends the MODE Z command.
				communication.sendFTPCommand("MODE Z");
				FTPReply r = communication.readFTPReply();
				touchAutoNoopTimer();
				if (r.isSuccessCode()) {
					modezEnabled = true;
				}
			}
		} else {
			if (modezEnabled) {
				// Sends the MODE S command.
				communication.sendFTPCommand("MODE S");
				FTPReply r = communication.readFTPReply();
				touchAutoNoopTimer();
				if (r.isSuccessCode()) {
					modezEnabled = false;
				}
			}
		}
		// Active or passive?
		if (passive) {
			return openPassiveDataTransferChannel();
		} else {
			return openActiveDataTransferChannel();
		}
	}

	/**
	 * This method opens a data transfer channel in active mode.
	 */
	private FTPDataTransferConnectionProvider openActiveDataTransferChannel()
			throws IOException, FTPIllegalReplyException, FTPException,
			FTPDataTransferException {
		// Create a FTPDataTransferServer object.
		FTPDataTransferServer server = new FTPDataTransferServer() {
			public Socket openDataTransferConnection()
					throws FTPDataTransferException {
				Socket socket = super.openDataTransferConnection();
				if (dataChannelEncrypted) {
					try {
						socket = ssl(socket, socket.getInetAddress().getHostName(), socket.getPort());
					} catch (IOException e) {
						try {
							socket.close();
						} catch (Throwable t) {
						}
						throw new FTPDataTransferException(e);
					}
				}
				return socket;
			}
		};
		int port = server.getPort();
		int p1 = port >>> 8;
		int p2 = port & 0xff;
		int[] addr = pickLocalAddress();
		// Send the port command.
		communication.sendFTPCommand("PORT " + addr[0] + "," + addr[1] + "," + addr[2] + "," +
				addr[3] + "," + p1 + "," + p2);
		FTPReply r = communication.readFTPReply();
		touchAutoNoopTimer();
		if (!r.isSuccessCode()) {
			// Disposes.
			server.dispose();
			// Closes the already open connection (if any).
			try {
				Socket aux = server.openDataTransferConnection();
				aux.close();
			} catch (Throwable t) {
				;
			}
			// Throws the exception.
			throw new FTPException(r);
		}
		return server;
	}

	/**
	 * This method opens a data transfer channel in passive mode.
	 */
	private FTPDataTransferConnectionProvider openPassiveDataTransferChannel()
			throws IOException, FTPIllegalReplyException, FTPException,
			FTPDataTransferException {
		// Send the PASV command.
		communication.sendFTPCommand("PASV");
		// Read the reply.
		FTPReply r = communication.readFTPReply();
		touchAutoNoopTimer();
		if (!r.isSuccessCode()) {
			throw new FTPException(r);
		}
		// Use a regexp to extract the remote address and port.
		String addressAndPort = null;
		String[] messages = r.getMessages();
		for (int i = 0; i < messages.length; i++) {
			Matcher m = PASV_PATTERN.matcher(messages[i]);
			if (m.find()) {
				int start = m.start();
				int end = m.end();
				addressAndPort = messages[i].substring(start, end);
				break;
			}
		}
		if (addressAndPort == null) {
			// The remote server has not sent the coordinates for the
			// data transfer connection.
			throw new FTPIllegalReplyException();
		}
		// Parse the string extracted from the reply.
		StringTokenizer st = new StringTokenizer(addressAndPort, ",");
		int b1 = Integer.parseInt(st.nextToken());
		int b2 = Integer.parseInt(st.nextToken());
		int b3 = Integer.parseInt(st.nextToken());
		int b4 = Integer.parseInt(st.nextToken());
		int p1 = Integer.parseInt(st.nextToken());
		int p2 = Integer.parseInt(st.nextToken());
		final String pasvHost = b1 + "." + b2 + "." + b3 + "." + b4;
		final int pasvPort = (p1 << 8) | p2;
		FTPDataTransferConnectionProvider provider = new FTPDataTransferConnectionProvider() {

			public Socket openDataTransferConnection() throws FTPDataTransferException {
				// Establish the connection.
				Socket dtConnection;
				try {
					String selectedHost = connector.getUseSuggestedAddressForDataConnections() ? pasvHost : host;
					dtConnection = connector.connectForDataTransferChannel(selectedHost, pasvPort);
					if (dataChannelEncrypted) {
						dtConnection = ssl(dtConnection, selectedHost, pasvPort);
					}
				} catch (IOException e) {
					throw new FTPDataTransferException("Cannot connect to the remote server", e);
				}
				return dtConnection;
			}

			public void dispose() {
				// nothing to do
			}

		};
		return provider;
	}

	/**
	 * If there's any ongoing data transfer operation, this method aborts it.
	 * 
	 * @param sendAborCommand
	 *            If true the client will negotiate the abort procedure with the
	 *            server, through the standard FTP ABOR command. Otherwise the
	 *            open data transfer connection will be closed without any
	 *            advise has sent to the server.
	 * @throws IOException
	 *             If the ABOR command cannot be sent due to any I/O error. This
	 *             could happen only if force is false.
	 * @throws FTPIllegalReplyException
	 *             If the server reply to the ABOR command is illegal. This
	 *             could happen only if force is false.
	 */
	public void abortCurrentDataTransfer(boolean sendAborCommand)
			throws IOException, FTPIllegalReplyException {
		synchronized (abortLock) {
			if (ongoingDataTransfer && !aborted) {
				if (sendAborCommand) {
					communication.sendFTPCommand("ABOR");
					touchAutoNoopTimer();
					consumeAborCommandReply = true;
				}
				if (dataTransferInputStream != null) {
					try {
						dataTransferInputStream.close();
					} catch (Throwable t) {
						;
					}
				}
				if (dataTransferOutputStream != null) {
					try {
						dataTransferOutputStream.close();
					} catch (Throwable t) {
						;
					}
				}
				aborted = true;
			}
		}
	}

	/**
	 * Returns the name of the charset that should be used in textual
	 * transmissions.
	 * 
	 * @return The name of the charset that should be used in textual
	 *         transmissions.
	 */
	private String pickCharset() {
		if (charset != null) {
			return charset;
		} else if (utf8Supported) {
			return "UTF-8";
		} else {
			return System.getProperty("file.encoding");
		}
	}

	/**
	 * Picks the local address for an active data transfer operation.
	 * 
	 * @return The local address as a 4 integer values array.
	 * @throws IOException
	 *             If an unexpected I/O error occurs while trying to resolve the
	 *             local address.
	 */
	private int[] pickLocalAddress() throws IOException {
		// Forced address?
		int[] ret = pickForcedLocalAddress();
		// Auto-detect?
		if (ret == null) {
			ret = pickAutoDetectedLocalAddress();
		}
		// Returns.
		return ret;
	}

	/**
	 * If a local address for active data transfers has been supplied through
	 * the {@link FTPKeys#ACTIVE_DT_HOST_ADDRESS}, it returns it as a 4 elements
	 * integer array; otherwise it returns null.
	 * 
	 * @return The forced local address, or null.
	 */
	private int[] pickForcedLocalAddress() {
		int[] ret = null;
		String aux = System.getProperty(FTPKeys.ACTIVE_DT_HOST_ADDRESS);
		if (aux != null) {
			boolean valid = false;
			StringTokenizer st = new StringTokenizer(aux, ".");
			if (st.countTokens() == 4) {
				valid = true;
				int[] arr = new int[4];
				for (int i = 0; i < 4; i++) {
					String tk = st.nextToken();
					try {
						arr[i] = Integer.parseInt(tk);
					} catch (NumberFormatException e) {
						arr[i] = -1;
					}
					if (arr[i] < 0 || arr[i] > 255) {
						valid = false;
						break;
					}
				}
				if (valid) {
					ret = arr;
				}
			}
			if (!valid) {
				// warning to the developer
				System.err.println("WARNING: invalid value \"" + aux
						+ "\" for the " + FTPKeys.ACTIVE_DT_HOST_ADDRESS
						+ " system property. The value should "
						+ "be in the x.x.x.x form.");
			}
		}
		return ret;
	}

	/**
	 * Auto-detects the local network address, and returns it in the form of a 4
	 * elements integer array.
	 * 
	 * @return The detected local address.
	 * @throws IOException
	 *             If an unexpected I/O error occurs while trying to resolve the
	 *             local address.
	 */
	private int[] pickAutoDetectedLocalAddress() throws IOException {
		InetAddress addressObj = InetAddress.getLocalHost();
		byte[] addr = addressObj.getAddress();
		int b1 = addr[0] & 0xff;
		int b2 = addr[1] & 0xff;
		int b3 = addr[2] & 0xff;
		int b4 = addr[3] & 0xff;
		int[] ret = { b1, b2, b3, b4 };
		return ret;
	}

	public String toString() {
		synchronized (lock) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(getClass().getName());
			buffer.append(" [connected=");
			buffer.append(connected);
			if (connected) {
				buffer.append(", host=");
				buffer.append(host);
				buffer.append(", port=");
				buffer.append(port);
			}
			buffer.append(", connector=");
			buffer.append(connector);
			buffer.append(", security=");
			switch (security) {
			case SECURITY_FTP:
				buffer.append("SECURITY_FTP");
				break;
			case SECURITY_FTPS:
				buffer.append("SECURITY_FTPS");
				break;
			case SECURITY_FTPES:
				buffer.append("SECURITY_FTPES");
				break;
			}
			buffer.append(", authenticated=");
			buffer.append(authenticated);
			if (authenticated) {
				buffer.append(", username=");
				buffer.append(username);
				buffer.append(", password=");
				StringBuffer buffer2 = new StringBuffer();
				for (int i = 0; i < password.length(); i++) {
					buffer2.append('*');
				}
				buffer.append(buffer2);
				buffer.append(", restSupported=");
				buffer.append(restSupported);
				buffer.append(", utf8supported=");
				buffer.append(utf8Supported);
				buffer.append(", mlsdSupported=");
				buffer.append(mlsdSupported);
				buffer.append(", mode=modezSupported");
				buffer.append(modezSupported);
				buffer.append(", mode=modezEnabled");
				buffer.append(modezEnabled);
			}
			buffer.append(", transfer mode=");
			buffer.append(passive ? "passive" : "active");
			buffer.append(", transfer type=");
			switch (type) {
			case TYPE_AUTO:
				buffer.append("TYPE_AUTO");
				break;
			case TYPE_BINARY:
				buffer.append("TYPE_BINARY");
				break;
			case TYPE_TEXTUAL:
				buffer.append("TYPE_TEXTUAL");
				break;
			}
			buffer.append(", textualExtensionRecognizer=");
			buffer.append(textualExtensionRecognizer);
			FTPListParser[] listParsers = getListParsers();
			if (listParsers.length > 0) {
				buffer.append(", listParsers=");
				for (int i = 0; i < listParsers.length; i++) {
					if (i > 0) {
						buffer.append(", ");
					}
					buffer.append(listParsers[i]);
				}
			}
			FTPCommunicationListener[] communicationListeners = getCommunicationListeners();
			if (communicationListeners.length > 0) {
				buffer.append(", communicationListeners=");
				for (int i = 0; i < communicationListeners.length; i++) {
					if (i > 0) {
						buffer.append(", ");
					}
					buffer.append(communicationListeners[i]);
				}
			}
			buffer.append(", autoNoopTimeout=");
			buffer.append(autoNoopTimeout);
			buffer.append("]");
			return buffer.toString();
		}
	}

	/**
	 * Starts the auto-noop timer thread.
	 */
	private void startAutoNoopTimer() {
		if (autoNoopTimeout > 0) {
			autoNoopTimer = new AutoNoopTimer();
			autoNoopTimer.start();
		}
	}

	/**
	 * Stops the auto-noop timer thread.
	 * 
	 * @since 1.5
	 */
	private void stopAutoNoopTimer() {
		if (autoNoopTimer != null) {
			autoNoopTimer.interrupt();
			autoNoopTimer = null;
		}
	}

	/**
	 * Resets the auto noop timer.
	 */
	private void touchAutoNoopTimer() {
		if (autoNoopTimer != null) {
			nextAutoNoopTime = System.currentTimeMillis() + autoNoopTimeout;
		}
	}

	/**
	 * The auto noop timer thread.
	 */
	private class AutoNoopTimer extends Thread {

		public void run() {
			synchronized (lock) {
				if (nextAutoNoopTime <= 0 && autoNoopTimeout > 0) {
					nextAutoNoopTime = System.currentTimeMillis() + autoNoopTimeout;
				}
				while (!Thread.interrupted() && autoNoopTimeout > 0) {
					// Sleep till the next NOOP.
					long delay = nextAutoNoopTime - System.currentTimeMillis();
					if (delay > 0) {
						try {
							lock.wait(delay);
						} catch (InterruptedException e) {
							break;
						}
					}
					// Is it really time to NOOP?
					if (System.currentTimeMillis() >= nextAutoNoopTime) {
						// Yes!
						try {
							noop();
						} catch (Throwable t) {
							; // ignore...
						}
					}
				}
			}
		}

	}

}
