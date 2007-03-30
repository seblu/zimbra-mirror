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
 * The Original Code is: Zimbra Collaboration Suite Web Client
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */

function ZmTradView(parent, className, posStyle, controller, dropTgt) {

	className = className || "ZmTradView";
	ZmDoublePaneView.call(this, parent, className, posStyle, ZmController.TRAD_VIEW, controller, dropTgt);
}

ZmTradView.prototype = new ZmDoublePaneView;
ZmTradView.prototype.constructor = ZmTradView;

ZmTradView.prototype.toString = 
function() {
	return "ZmTradView";
}

ZmTradView.prototype.setItem =
function(msgs) {
	ZmDoublePaneView.prototype.setItem.call(this, msgs);

	this._mailListView.set(msgs, ZmItem.F_DATE);

	// XXX: for now always show the first msg 
	//      (whether user clicked inbox or paginated)
	var list = this._mailListView.getList();
	var selectedItem = list ? list.get(0) : null
	if (selectedItem) {
		this._mailListView.setSelection(selectedItem, false, true);
	}
}

ZmTradView.prototype._resetSize = 
function(newWidth, newHeight) {
	if (newHeight <= 0)
		return;
	
	if (this._isMsgViewVisible()) {
		var sashHeight = this._msgSash.getSize().y;
		if (!this._sashMoved) {
			var listViewHeight = (newHeight / 2) - DwtListView.HEADERITEM_HEIGHT;
			this._mailListView.resetHeight(listViewHeight);
			this._msgView.setBounds(Dwt.DEFAULT, listViewHeight + sashHeight, Dwt.DEFAULT,
									newHeight - (listViewHeight + sashHeight));
			this._msgSash.setLocation(Dwt.DEFAULT, listViewHeight);
		} else {
			var mvHeight = newHeight - this._msgView.getLocation().y;
			var minHeight = this._msgView.getMinHeight();
			if (mvHeight < minHeight) {
				this._mailListView.resetHeight(newHeight - minHeight);
				this._msgView.setBounds(Dwt.DEFAULT, (newHeight - minHeight) + sashHeight,
										Dwt.DEFAULT, minHeight - sashHeight);
			} else {
				this._msgView.setSize(Dwt.DEFAULT, mvHeight);
			}
			this._msgSash.setLocation(Dwt.DEFAULT, this._msgView.getLocation().y - sashHeight);
		}
	} else {
		this._mailListView.resetHeight(newHeight);
	}
	this._mailListView._resetColWidth();
}

ZmTradView.prototype._sashCallback =
function(delta) {

	if (!this._sashMoved)
		this._sashMoved = true;

	if (delta > 0) {
		var newMsgViewHeight = this._msgView.getSize().y - delta;
		var minMsgViewHeight = this._msgView.getMinHeight();
		if (newMsgViewHeight > minMsgViewHeight) {
			// moving sash down
			this._mailListView.resetHeight(this._mailListView.getSize().y + delta);
			this._msgView.setSize(Dwt.DEFAULT, newMsgViewHeight);
			this._msgView.setLocation(Dwt.DEFAULT, this._msgView.getLocation().y + delta);
		} else {
			delta = 0;
		}
	} else {
		var absDelta = Math.abs(delta);
		
		if (!this._minMLVHeight) {
			var list = this._mailListView.getList();
			if (list && list.size()) {
				var item = list.get(0);
				var div = document.getElementById(this._mailListView._getItemId(item));
				this._minMLVHeight = DwtListView.HEADERITEM_HEIGHT + (Dwt.getSize(div).y * 2);
			} else {
				this._minMLVHeight = DwtListView.HEADERITEM_HEIGHT;
			}
		}
		
		if (this._msgSash.getLocation().y - absDelta > this._minMLVHeight) {
			// moving sash up
			this._mailListView.resetHeight(this._mailListView.getSize().y - absDelta);
			this._msgView.setSize(Dwt.DEFAULT, this._msgView.getSize().y + absDelta);
			this._msgView.setLocation(Dwt.DEFAULT, this._msgView.getLocation().y - absDelta);
		} else {
			delta = 0;
		}
	}

	if (delta)
		this._mailListView._resetColWidth();

	return delta;
}
