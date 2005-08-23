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

package com.zimbra.cs.client;

public class LmcNote {
    private String id;
    private String tags;
    private String date;
    private String folder;
    private String position;
    private String color;
    private String content;    

    public void setID(String i) { id = i; }
    public void setTags(String t) { tags = t; }
    public void setDate(String d) { date = d; }
    public void setFolder(String f) { folder = f; }
    public void setPosition(String p) { position = p; }
    public void setColor(String c) { color = c; }
    public void setContent(String c) { content = c; }

    public String getID() { return id; }
    public String getTags() { return tags; }
    public String getDate() { return date; }
    public String getFolder() { return folder; }
    public String getPosition() { return position; }
    public String getColor() { return color; }
    public String getContent() { return content; }
    
    public String toString() {
    	return "Note ID=" + id + " date=" + date + " tags=" + tags +
            " folder=" + folder + " position=" + position + 
            " color=" + color + " content=" + content;
    }
}
