/*
 * Created on Jun 17, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.LiquidLog;
import com.zimbra.soap.LiquidContext;

/**
 * @author schemers
 */
public class RenameCos extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {

        LiquidContext lc = getLiquidContext(context);
	    Provisioning prov = Provisioning.getInstance();

	    String id = request.getAttribute(AdminService.E_ID);
        String newName = request.getAttribute(AdminService.E_NEW_NAME);

	    Cos cos = prov.getCosById(id);
        if (cos == null)
            throw AccountServiceException.NO_SUCH_COS(id);

        String oldName = cos.getName();

        prov.renameCos(id, newName);

        LiquidLog.security.info(LiquidLog.encodeAttrs(
                new String[] {"cmd", "RenameCos","name", oldName, "newName", newName})); 

        // get again with new name...

        cos = prov.getCosById(id);
        if (cos == null)
            throw ServiceException.FAILURE("unabled to get renamed cos: "+id, null);
	    Element response = lc.createElement(AdminService.RENAME_COS_RESPONSE);
	    GetCos.doCos(response, cos);
	    return response;
	}

}