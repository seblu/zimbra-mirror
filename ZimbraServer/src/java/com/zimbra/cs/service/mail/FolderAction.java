/*
 * Created on Aug 30, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author dkarp
 */
public class FolderAction extends ItemAction {

    public static final String OP_RENAME = "rename";
    public static final String OP_EMPTY  = "empty";

	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        Mailbox mbox = getRequestedMailbox(lc);
        OperationContext octxt = lc.getOperationContext();

        Element action = request.getElement(MailService.E_ACTION);
        String operation = action.getAttribute(MailService.A_OPERATION).toLowerCase();

        if (operation.endsWith(OP_UPDATE) || operation.endsWith(OP_TAG) || operation.endsWith(OP_FLAG) || operation.endsWith(OP_SPAM))
            throw ServiceException.INVALID_REQUEST("invalid operation on folder: " + operation, null);
        String successes;
        if (operation.equals(OP_RENAME) || operation.equals(OP_EMPTY))
            successes = handleFolder(octxt, operation, action, mbox);
        else
            successes = handleCommon(octxt, operation, action, mbox, MailItem.TYPE_FOLDER);

        Element response = lc.createElement(MailService.FOLDER_ACTION_RESPONSE);
        Element act = response.addUniqueElement(MailService.E_ACTION);
        act.addAttribute(MailService.A_ID, successes);
        act.addAttribute(MailService.A_OPERATION, operation);
        return response;
	}

    private String handleFolder(OperationContext octxt, String operation, Element action, Mailbox mbox)
    throws ServiceException {
        int id = (int) action.getAttributeLong(MailService.A_ID);

        if (operation.equals(OP_EMPTY))
            mbox.emptyFolder(octxt, id, true);
        else if (operation.equals(OP_RENAME)) {
            String name = action.getAttribute(MailService.A_NAME);
            mbox.renameFolder(octxt, id, name);
        } else
            throw ServiceException.INVALID_REQUEST("unknown operation: " + operation, null);

        return Integer.toString(id);
    }
}
