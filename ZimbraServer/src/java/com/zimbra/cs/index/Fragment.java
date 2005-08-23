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
 * Created on Apr 7, 2005
 */
package com.zimbra.cs.index;

import com.zimbra.cs.util.StringUtil;

/**
 * @author dkarp
 */
public class Fragment {

    private static final int MAX_FRAGMENT_LENGTH = 100;

    private static final String CALENDAR_SEPARATOR = "*~*~*~*~*~*~*~*~*~*";

    private static final String STOPWORD_REPLY_1   = "--Original Message--";
    private static final String STOPWORD_REPLY_2   = "-- Original Message --";
    private static final String STOPWORD_REPLY_3   = "________________________________";
    private static final String STOPWORD_FORWARD_1 = "--Forwarded Message--";
    private static final String STOPWORD_FORWARD_2 = "-- Forwarded Message --";

    private static boolean isHeader(String text, boolean allowSpaces) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ':')
                return (i != 0);
            else if (!allowSpaces && Character.isWhitespace(c))
                return false;
            else if (c == '\r' || c == '\n')
                return false;
        }
        return false;
    }

    private static String skipFragmentHeader(String fragment, boolean calendar) {
        String backup = fragment;

        // skip all the "From:", "When:", "Organizer:", etc. lines 
        int returnIndex = -1;
        while (fragment.length() > 0) {
            returnIndex = fragment.indexOf('\n');
            if (returnIndex == -1)
                return backup;
            if (!isHeader(fragment, calendar)) {
                if (calendar) {
                    // if the next line is the separator, the fragment is the *rest* of the content
                    String line = fragment.substring(0, returnIndex).trim();
                    if (line.equals(CALENDAR_SEPARATOR))
                        fragment = fragment.substring(returnIndex + 1).trim();
                }
                break;
            }
            fragment = fragment.substring(returnIndex + 1).trim();
        }

        return (fragment.length() > 0 ? fragment : backup);
    }

    private static String skipQuotedText(String fragment, boolean twoLineHeader) {
        String backup = fragment;
        
        // skip the quote header ("On foosday, Herbie wrote:\n")
        int returnIndex = -1;
        int headerLines = (twoLineHeader ? 2 : 1);
        for (int i = 0; i < headerLines; i++) {
            if (fragment.startsWith(">") || fragment.startsWith("|"))
                break;
            returnIndex = fragment.indexOf('\n', returnIndex + 1);
            if (returnIndex == -1)
                break;
            String firstLine = fragment.substring(0, returnIndex).trim();
            if (firstLine.endsWith(":")) {
                fragment = fragment.substring(returnIndex + 1).trim();
                // also handle the "[snipped]" non-quotes here
                if (fragment.startsWith("[")) {
                    returnIndex = fragment.indexOf('\n');
                    if (returnIndex != -1 && fragment.substring(0, returnIndex).trim().endsWith("]"))
                        fragment = fragment.substring(returnIndex + 1).trim();
                }
                break;
            }
        }
        
        // skip quoted text
        if (fragment.startsWith(">")) {
            do {
                returnIndex = fragment.indexOf('\n');
                fragment = (returnIndex == -1 ? "" : fragment.substring(returnIndex + 1).trim());
            } while (fragment.startsWith(">"));
        } else if (fragment.startsWith("|")) {
            do {
                returnIndex = fragment.indexOf('\n');
                fragment = (returnIndex == -1 ? "" : fragment.substring(returnIndex + 1).trim());
            } while (fragment.startsWith("|"));
        } else if (fragment != backup)
            fragment = backup;
        
        return fragment;
    }

    public static String getFragment(String content, boolean hasCalendar) {
        String remainder = content.trim();
        String backup    = remainder;
        StringBuffer fragment = new StringBuffer();

        // skip the "Where:", "When:", "Organizer:", etc. headers for Outlook calendar invites
        if (hasCalendar)
            remainder = skipFragmentHeader(remainder, true);

        // skip "On foosday, Herbie wrote:\n> blah..."
        remainder = skipQuotedText(remainder, true);
        boolean elided = false, mainMessage = true;
        while (fragment.length() <= Fragment.MAX_FRAGMENT_LENGTH) {
            // detect quoted stuff and skip it (or unquote it if we're fragmenting a message that's nothing but quotes)
            if (remainder.startsWith(">") || remainder.startsWith("|")) {
                if (mainMessage) {
                    remainder = skipQuotedText(remainder, false);
                    elided = true;
                } else
                    remainder = remainder.substring(1).trim();
            }
            // if we're done, we're done
            if (remainder.equals(""))
                break;
            // DASH-DASH-SPACE-RETURN is the standard signature delimiter
            int returnIndex = remainder.indexOf('\n');
            if ((returnIndex == 3 && remainder.startsWith("-- ")) || (returnIndex == 4 && remainder.startsWith("-- \r")))
                break;
            // check for another "On foosday, Herbie wrote:\n> blah..." and skip (unless we're fragmenting a message that's nothing but quotes)
            String line = (returnIndex == -1 ? remainder : remainder.substring(0, returnIndex)).trim();
            if (mainMessage && line.endsWith(":") && returnIndex != -1) {
                String trimmed = skipQuotedText(remainder, false);
                if (trimmed != remainder) {
                    remainder = trimmed;
                    elided = true;
                    continue;
                }
            }
            // sanitize for XML and collapse whitespace and some chars that people make lines ot of
            line = StringUtil.stripControlCharacters(line).replaceAll("\\s+", " ").replaceAll("---+", "--").replaceAll("===+", "==");
            // check for stopwords that mean that what follows is a quoted message
            if (line.equalsIgnoreCase(STOPWORD_REPLY_1) || line.equalsIgnoreCase(STOPWORD_REPLY_2) ||
                    line.equalsIgnoreCase(STOPWORD_REPLY_3) || line.equalsIgnoreCase(STOPWORD_FORWARD_1) ||
                    line.equalsIgnoreCase(STOPWORD_FORWARD_2)) {
                if (fragment.length() != 0 || returnIndex == -1)
                    break;
                // if we're here, the message was nothing but quoted text, so leave in the "-- Original Message --" and add a few more lines
                fragment.append(line);
                remainder = skipFragmentHeader(remainder.substring(returnIndex).trim(), false);
                elided = mainMessage = false;
                continue;
            }
            // and now we can finally add the line to the fragment we're building up
            if (fragment.length() != 0) {
                if (elided)
                    fragment.append(" ...");
                fragment.append(' ');
            }
            fragment.append(line);
            remainder = (returnIndex == -1 ? "" : remainder.substring(returnIndex).trim());
            elided = false;
        }

        // almost done!  just make sure we haven't accidentally trimmed off everything (in which case we'll just use the original content)
        String result = fragment.toString();
        if (result.equals(""))
            result = StringUtil.stripControlCharacters(backup).trim().replaceAll("\\s+", " ").replaceAll("---+", "--").replaceAll("===+", "==");
        boolean isTruncated = (result.length() > Fragment.MAX_FRAGMENT_LENGTH);
        result = result.substring(0, isTruncated ? Fragment.MAX_FRAGMENT_LENGTH : result.length());
        
        // try to break at word boundaries and add an ellipsis if we've truncated the fragment
        // can't use the actual ellipsis character ('?') because of broken fonts on Linux
        if (isTruncated) {
            int wspIndex = Math.max(result.lastIndexOf('\n'), result.lastIndexOf(' ')); 
            wspIndex = Math.max(wspIndex, result.lastIndexOf('\t'));
            if (wspIndex != -1)
                result = result.substring(0, wspIndex).trim();
            if (!result.endsWith(" ..."))
                result = result.concat(" ...");
        }

        return result;
    }
}
