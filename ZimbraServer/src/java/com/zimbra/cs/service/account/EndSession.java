/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013 Zimbra Software, LLC.
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
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * End the current session immediately cleaning up all resources used by the session
 * including the notification buffer and logging the session out from IM if applicable
 */
public class EndSession extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (zsc.hasSession()) {
            Session s = getSession(zsc);
            endSession(s);
        }
        boolean clearCookies = request.getAttributeBool(AccountConstants.A_LOG_OFF, false);
        if (clearCookies || getAuthenticatedAccount(zsc).isForceClearCookies()) {
            context.put(SoapServlet.INVALIDATE_COOKIES, true);
            try {
				zsc.getAuthToken().deRegister();
			} catch (AuthTokenException e) {
				throw ServiceException.FAILURE("Failed to de-register an auth token", e);
			}
        }
        getAuthenticatedAccount(zsc).cleanExpiredTokens();
        Element response = zsc.createElement(AccountConstants.END_SESSION_RESPONSE);
        return response;
    }
}
