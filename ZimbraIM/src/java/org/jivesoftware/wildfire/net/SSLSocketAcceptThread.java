package org.jivesoftware.wildfire.net;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ConnectionManager;
import org.jivesoftware.wildfire.ServerPort;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Implements a network front end with a dedicated thread reading
 * each incoming socket. The old SSL method always uses a blocking model.
 *
 * @author Gaston Dombiak
 */
public class SSLSocketAcceptThread extends Thread {

    /**
     * The default Jabber socket
     */
    public static final int DEFAULT_PORT = 5223;

    /**
     * Holds information about the port on which the server will listen for connections.
     */
    private ServerPort serverPort;

    /**
     * True while this thread should continue running.
     */
    private boolean notTerminated = true;

    /**
     * The accept socket we're running
     */
    private ServerSocket serverSocket;

    /**
     * Connection manager handling connections created by this thread. *
     */
    private ConnectionManager connManager;
    /**
     * The number of SSL related exceptions occuring rapidly that should signal a need
     * to shutdown the SSL port.
     */
    private static final int MAX_SSL_EXCEPTIONS = 10;

    /**
     * Creates an instance using the default port, TLS transport security, and
     * JVM defaults for all security settings.
     *
     * @param connManager the connection manager that will manage connections
     *      generated by this thread
     * @throws IOException if there was trouble initializing the SSL configuration.
     */
    public SSLSocketAcceptThread(ConnectionManager connManager, ServerPort serverPort)
            throws IOException {
        super("Secure Socket Listener");
        // Listen on a specific network interface if it has been set.
        String interfaceName = JiveGlobals.getXMLProperty("network.interface");
        InetAddress bindInterface = null;
        if (interfaceName != null) {
            try {
                if (interfaceName.trim().length() > 0) {
                    bindInterface = InetAddress.getByName(interfaceName);
                    // Create the new server port based on the new bind address
                    serverPort = new ServerPort(serverPort.getPort(),
                            serverPort.getDomainNames().get(0), interfaceName, serverPort.isSecure(),
                            serverPort.getSecurityType(), serverPort.getType());
                }
            }
            catch (UnknownHostException e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
        }
        this.connManager = connManager;
        this.serverPort = serverPort;
        int port = serverPort.getPort();
        serverSocket = SSLConfig.createServerSocket(port, bindInterface);
    }

    /**
     * Retrieve the port this server socket is bound to.
     *
     * @return the port the socket is bound to.
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Returns information about the port on which the server is listening for connections.
     *
     * @return information about the port on which the server is listening for connections.
     */
    public ServerPort getServerPort() {
        return serverPort;
    }

    /**
     * Unblock the thread and force it to terminate.
     */
    public void shutdown() {
        notTerminated = false;
        try {
            ServerSocket sSock = serverSocket;
            serverSocket = null;
            if (sSock != null) {
                sSock.close();
            }
        }
        catch (IOException e) {
            // we don't care, no matter what, the socket should be dead
        }
    }

    /**
     * About as simple as it gets.  The thread spins around an accept
     * call getting sockets and handing them to the SocketManager.
     * We need to detect run away failures since an SSL configuration
     * problem can cause the loop to spin, constantly rethrowing SSLExceptions
     * (e.g. if a certificate is in the keystore that can't be verified).
     */
    public void run() {
        long lastExceptionTime = 0;
        int exceptionCounter = 0;
        while (notTerminated) {
            try {
                Socket sock = serverSocket.accept();
                Log.debug("SSL Connect " + sock.toString());
                SocketReader reader = connManager.createSocketReader(sock, true, serverPort);
                // Create a new reading thread for each new connected client
                Thread thread = new Thread(reader, reader.getName());
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY);
                thread.start();
            }
            catch (SSLException se) {
                long exceptionTime = System.currentTimeMillis();
                if (exceptionTime - lastExceptionTime > 1000) {
                    // if the time between SSL exceptions is too long
                    // reset the counter
                    exceptionCounter = 1;
                }
                else {
                    // If this exception occured within a second of the last one
                    // we need to count it
                    exceptionCounter++;
                }
                lastExceptionTime = exceptionTime;
                Log.error(LocaleUtils.getLocalizedString("admin.error.ssl"), se);
                // and if the number of consecutive exceptions exceeds the limit
                // we should assume there's an SSL problem or DOS attack and shutdown
                if (exceptionCounter > MAX_SSL_EXCEPTIONS) {
                    String msg = "Shutting down SSL port - " +
                            "suspected configuration problem";
                    Log.error(msg);
                    Log.info(msg);
                    shutdown();
                }
            }
            catch (Throwable e) {
                if (notTerminated) {
                    Log.error(LocaleUtils.getLocalizedString("admin.error.ssl"), e);
                }
            }
        }
        try {
            ServerSocket sSock = serverSocket;
            serverSocket = null;
            if (sSock != null) {
                sSock.close();
            }
        }
        catch (IOException e) {
            // we don't care, no matter what, the socket should be dead
        }
    }
}
