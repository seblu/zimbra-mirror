/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class TagAction extends ItemAction  {

	public static final String OP_RENAME = "rename";
	public static final String OP_COLOR  = "color";
	
	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_TAG) || operation.endsWith(OP_FLAG) || operation.endsWith(OP_MOVE) ||
                operation.endsWith(OP_UPDATE) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on tag: " + operation, null);
        String successes;
        if (operation.equals(OP_RENAME) || operation.equals(OP_COLOR))
            successes = handleTag(octxt, operation, action, mbox);
        else
            successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_TAG);

        Element response = lc.createElement(MailService.TAG_ACTION_RESPONSE);
    	Element result = response.addUniqueElement(MailService.E_ACTION);
    	result.addAttribute(MailService.A_ID, successes);
    	result.addAttribute(MailService.A_OPERATION, operation);
        return response;
	}

    private String handleTag(OperationContext octxt, String operation, Element action, Mailbox mbox)
    throws ServiceException {
        int id = (int) action.getAttributeLong(MailService.A_ID);

        if (operation.equals(OP_RENAME)) {
            String name = action.getAttribute(MailService.A_NAME);
            mbox.renameTag(octxt, id, name);
        } else if (operation.equals(OP_COLOR)) {
            byte color = (byte) action.getAttributeLong(MailService.A_COLOR);
            mbox.colorTag(octxt, id, color);
        } else
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);

        return Integer.toString(id);
    }
}
