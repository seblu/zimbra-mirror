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

package com.zimbra.cs.lmtpserver.utils;

import java.util.Map;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import com.zimbra.cs.lmtpserver.LmtpAddress;

public class LmtpAddressTester {
	
	public static void main(String[] args) throws IOException {
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		String line;
		int i = 0;
		while ((line = in.readLine()) != null) {
			i++;
			
			if (line.startsWith("#") || line.length() == 0) {
				continue;
			}
			
			int colon = line.indexOf(':');
			if (colon < 0) {
				System.err.println("missing colon:line " + i + ":" + line);
				continue;
			}
			String result = line.substring(0, colon);
			String input = line.substring(colon + 1);
			
			boolean validity;
			if ("+".equals(result)) {
				validity = true;
			} else if ("-".equals(result)) {
				validity = false;
			} else {
				System.err.println("missing result:line " + i + ":" + line);
				continue;
			}
			
			if (test(input, args) != validity) {
				System.err.println("incorrect result:line " + i + ":" + line);
			}
		}
	}
	
	private static boolean test(String line, String[] allowedParams) {
		System.out.println("==>" + line + "<==");
		LmtpAddress addr = new LmtpAddress(line, allowedParams);
		System.out.println("  valid=" + addr.isValid());
		if (addr.isValid()) {
			System.out.println("  local-part=/" + addr.getLocalPart() + "/");
			System.out.println("  domain-part=/" + addr.getDomainPart() + "/");
			Map params = addr.getParameters();
			int i = 0;
			for (Iterator it = params.entrySet().iterator(); it.hasNext();) {
				Map.Entry e = (Map.Entry) it.next();
				String key = (String)e.getKey();
				String val = (String)e.getValue();
				System.out.println("  [" + i + "] key=/" + key + "/ val=/" + val + "/");
				i++;
			}
		}
		return addr.isValid();
	}
}
