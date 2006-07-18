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

package com.zimbra.cs.zclient;

import java.util.List;


public interface ZConversationHit extends ZSearchHit {

    /**
     * @return conversation's id
     */
    public String getId();
    
    /**
     * @return comma-separated list of tag ids
     */
    public String getTagIds();
    
    public long getDate();
    
    public String getFlags();
    
    public boolean hasFlags();
    
    public boolean hasTags();
    
    public boolean isUnread();

    public boolean isFlagged();

    public boolean isSentByMe();

    public boolean hasAttachment();

    public String getSubject();
    
    public String getFragment();
    
    public int getMessageCount();
    
    public List<String> getMatchedMessageIds();
    
    public List<ZEmailAddress> getRecipients();    

}
