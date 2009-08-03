/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.lmtpserver;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mina.MinaHandler;
import com.zimbra.cs.mina.MinaIoSessionOutputStream;
import com.zimbra.cs.mina.MinaOutputStream;
import com.zimbra.cs.mina.LineBuffer;
import com.zimbra.cs.store.BlobBuilder;
import com.zimbra.cs.store.StoreManager;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class MinaLmtpHandler extends LmtpHandler implements MinaHandler {
    private final IoSession mSession;
    private BlobBuilder data;
    private MinaOutputStream mOutputStream;

    private static final long WRITE_TIMEOUT = 5000;
    
    public MinaLmtpHandler(MinaLmtpServer server, IoSession session) {
        super(server);
        mSession = session;
        mEnvelope = new LmtpEnvelope();
        mOutputStream = new MinaIoSessionOutputStream(mSession);
        mWriter = new LmtpWriter(mOutputStream);
        mSession.setIdleTime(IdleStatus.BOTH_IDLE, mConfig.getMaxIdleSeconds());
    }
    
    public void messageReceived(Object msg) throws IOException {
        if (data != null) {
            receiveData((LineBuffer) msg);
            if (data.isFinished()) {
                processMessageData(data.getBlob());
                data.dispose();
                data = null;
            }
        } else {
            processCommand((String) msg);
        }
    }

    private void receiveData(LineBuffer lb) throws IOException {
        if (lb.matches(".\r\n")) {
            try {
                data.finish();
            } catch (ServiceException e) {
                throw (IOException)
                    new IOException("Unable to write blob").initCause(e);
            }
        } else {
            ByteBuffer bb = lb.buf();
            if (lb.startsWith("..")) {
                bb.position(bb.position() + 1); // Skip leading '.'
            }
            data.append(bb);
        }
    }

    public void connectionOpened() {
        setupConnection(((InetSocketAddress) mSession.getRemoteAddress()).getAddress());
    }

    public void connectionClosed() {
        cleanup();
    }

    public void connectionIdle() {
        notifyIdleConnection();
        dropConnection();
    }

    @Override
    protected void continueDATA() throws IOException {
        try {
            data = StoreManager.getInstance().getBlobBuilder();
        } catch (ServiceException e) {
            throw (IOException)
                new IOException("Unable to create blob builder").initCause(e);
        }
        data.append(getAdditionalHeaders().getBytes());
    }
    
    @Override
    protected void dropConnection() {
        dropConnection(WRITE_TIMEOUT);
    }

    public void dropConnection(long timeout) {
        if (!mSession.isConnected()) return;
        try {
            mOutputStream.close();
        } catch (IOException e) {
            // Should never happen...
        }
        if (timeout >= 0) {
            // Wait for all remaining bytes to be written
            if (!mOutputStream.join(timeout)) {
                ZimbraLog.lmtp.warn("Force closing session because write timed out: " + mSession);
            }
        }
        cleanup();
    }

    private void cleanup() {
        mSession.close();
        if (data != null) {
            data.dispose();
            data = null;
        }
    }
    
    @Override
    protected boolean setupConnection(Socket connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean processCommand() {
        throw new UnsupportedOperationException();
    }
}
