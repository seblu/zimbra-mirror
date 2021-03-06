/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CacheEntrySelector {

    public static enum CacheEntryBy {

        // case must match protocol
        id, name;

        public static CacheEntryBy fromString(String s) throws ServiceException {
            try {
                return CacheEntryBy.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown key: "+s, e);
            }
        }
    }

    /**
     * @zm-api-field-tag cache-entry-key
     * @zm-api-field-description The key used to identify the cache entry. Meaning determined by <b>{cache-entry-by}</b>
     */
    @XmlValue private final String key;

    /**
     * @zm-api-field-tag cache-entry-by
     * @zm-api-field-description Select the meaning of <b>{cache-entry-key}</b>
     */
    @XmlAttribute(name=AdminConstants.A_BY) private final CacheEntryBy cacheEntryBy;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CacheEntrySelector() {
        this(null, null);
    }

    public CacheEntrySelector(CacheEntryBy by, String key) {
        this.cacheEntryBy = by;
        this.key = key;
    }

    public String getKey() { return key; }

    public CacheEntryBy getBy() { return cacheEntryBy; }
}
