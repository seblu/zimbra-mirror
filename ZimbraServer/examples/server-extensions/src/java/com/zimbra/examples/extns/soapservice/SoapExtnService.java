/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.examples.extns.soapservice;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;

/**
 * Registers <code>HelloWorld</code> handler with SOAP document dispatcher.
 *
 * @author vmahajan
 */
public class SoapExtnService implements DocumentService {

    /**
     * Registers <code>DocumentHandler<code>.
     *
     * @param dispatcher document dispatcher
     */
    public void registerHandlers(DocumentDispatcher dispatcher) {
        dispatcher.registerHandler(HelloWorld.REQUEST_QNAME, new HelloWorld());
    }
}
