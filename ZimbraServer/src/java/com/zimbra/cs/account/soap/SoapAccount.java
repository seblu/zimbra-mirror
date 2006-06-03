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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.soap;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.service.ServiceException;

public class SoapAccount extends SoapNamedEntry implements Account {

    public SoapAccount(String name, String id, Map<String, Object> attrs) {
        super(name, id, attrs);
    }

    @Override
    public void modifyAttrs(Map<String, ? extends Object> attrs)
            throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void modifyAttrs(Map<String, ? extends Object> attrs,
            boolean checkImmutable) throws ServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reload() throws ServiceException {
        // TODO Auto-generated method stub

    }

    public String getAccountStatus() {
        return getAttr(Provisioning.A_zimbraAccountStatus);
    }

    public String[] getAliases() throws ServiceException {
        return getMultiAttr(Provisioning.A_zimbraMailAlias);        
    }

    public Map<String, Object> getAttrs(boolean prefsOnly, boolean applyCos)
            throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Cos getCOS() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public CalendarUserType getCalendarUserType() throws ServiceException {
        String cutype = getAttr(Provisioning.A_zimbraAccountCalendarUserType,
                CalendarUserType.USER.toString());
        return CalendarUserType.valueOf(cutype);
    }

    public Set<String> getDistributionLists() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<DistributionList> getDistributionLists(boolean directOnly,
            Map<String, String> via) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public Domain getDomain() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getDomainName() {
        int index = mName.indexOf('@');
        if (index != -1) return mName.substring(index+1);
        else return null;
    }

    public Server getServer() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public ICalTimeZone getTimeZone() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUid() {
        return super.getAttr(Provisioning.A_uid);        
    }

    public boolean inDistributionList(String zimbraId) throws ServiceException {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean saveToSent() throws ServiceException {
        return getBooleanAttr(Provisioning.A_zimbraPrefSaveToSent, false);
    }

}
