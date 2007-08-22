/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.2
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimlets
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Zimlet;

class LdapZimlet extends Zimlet implements LdapEntry {

    private String mDn;
    
	public LdapZimlet(String dn, Attributes attrs) throws NamingException {
        super(LdapUtil.getAttrString(attrs, Provisioning.A_cn),
                LdapUtil.getAttrString(attrs, Provisioning.A_cn),                 
                LdapUtil.getAttrs(attrs));
        mDn = dn;
	}
	
    public String getDN() {
        return mDn;
    }
}
