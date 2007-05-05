/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.service.method;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.LockMgr;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.LockMgr.LockScope;
import com.zimbra.cs.dav.LockMgr.LockType;
import com.zimbra.cs.dav.property.LockDiscovery;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavMethod;

public class Lock extends DavMethod {
	public static final String LOCK  = "LOCK";
	public String getName() {
		return LOCK;
	}
	public void handle(DavContext ctxt) throws DavException, IOException, ServiceException {
		if (!ctxt.hasRequestMessage()) {
			throw new DavException("no request body", HttpServletResponse.SC_BAD_REQUEST, null);
		}
		
		DavContext.Depth depth = ctxt.getDepth();
		if (depth == Depth.one)
			throw new DavException("invalid depth", HttpServletResponse.SC_BAD_REQUEST, null);
		int d = (depth == Depth.zero) ? 0 : 1;
		
		LockMgr.LockScope scope = LockScope.shared;
		LockMgr.LockType type = LockType.write;
		
		Document req = ctxt.getRequestMessage();
		Element top = req.getRootElement();
		if (!top.getName().equals(DavElements.P_LOCKINFO))
			throw new DavException("msg "+top.getName()+" not allowed in LOCK", HttpServletResponse.SC_BAD_REQUEST, null);

		Element e = top.element(DavElements.E_LOCKSCOPE);
		@SuppressWarnings("unchecked")
		List<Element> ls = e.elements();
		for (Element v : ls) {
			if (v.getQName().equals(DavElements.E_EXCLUSIVE))
				scope = LockScope.exclusive;
			else if (v.getQName().equals(DavElements.E_SHARED))
				scope = LockScope.shared;
			else
				throw new DavException("unrecognized scope element "+v.toString(), HttpServletResponse.SC_BAD_REQUEST, null);
		}
		e = top.element(DavElements.E_LOCKTYPE);
		@SuppressWarnings("unchecked")
		List<Element> lt = e.elements();
		for (Element v : lt) {
			if (v.getQName().equals(DavElements.E_WRITE))
				type = LockType.write;
			else
				throw new DavException("unrecognized type element "+v.toString(), HttpServletResponse.SC_BAD_REQUEST, null);
		}
		
		DavResource rs = ctxt.getRequestedResource();
		LockMgr lockmgr = LockMgr.getInstance();
		LockMgr.Lock lock = lockmgr.createLock(ctxt, rs, type, scope, d);
		
		ctxt.getDavResponse().addProperty(ctxt, new LockDiscovery(lock));
		sendResponse(ctxt);
	}
}
