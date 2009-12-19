/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2006, 2007, 2008, 2009 Zimbra, Inc.
 *
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

ZmBriefcaseBaseView = function(params) {

	if (arguments.length == 0) { return; }
	
	params.posStyle = params.posStyle || DwtControl.ABSOLUTE_STYLE;
	params.type = ZmItem.BRIEFCASE_ITEM;
	params.pageless = (params.pageless !== false);
	ZmListView.call(this, params);
};

ZmBriefcaseBaseView.prototype = new ZmListView;
ZmBriefcaseBaseView.prototype.constructor = ZmBriefcaseBaseView;

ZmBriefcaseBaseView.prototype.getTitle =
function() {
	//TODO: title is the name of the current folder
	return [ZmMsg.zimbraTitle, this._controller.getApp().getDisplayName()].join(": ");
};

ZmBriefcaseBaseView.prototype._getToolTip =
function(params) {

	var item = params.item;
	if (item.isFolder) { return null; }

	var prop = [{name:ZmMsg.briefcasePropName, value:item.name}];
	if (item.size) {
		prop.push({name:ZmMsg.briefcasePropSize, value:AjxUtil.formatSize(item.size)});
	}
	if (item.modifyDate) {
		var dateFormatter = AjxDateFormat.getDateTimeInstance(AjxDateFormat.FULL, AjxDateFormat.MEDIUM);
		var dateStr = dateFormatter.format(item.modifyDate);
		prop.push({name:ZmMsg.briefcasePropModified, value:dateStr});
	}

	var subs = {
		fileProperties:	prop,
		tagTooltip:		this._getTagToolTip(item)
	};
	return AjxTemplate.expand("briefcase.Briefcase#Tooltip", subs);
};

ZmBriefcaseBaseView.prototype._getItemCountType =
function() {
	return null;
};

// Support DnD file uploading (see dnd zimlet)
ZmBriefcaseBaseView.prototype.uploadFiles =
function() {
    var attachDialog = appCtxt.getUploadDialog();
    var app = this._controller.getApp();
    attachDialog._uploadCallback = new AjxCallback(app, app._handleUploadNewItem);
    var files = this.processUploadFiles();
    attachDialog.uploadFiles(files, document.getElementById("zdnd_form"), {id:this._controller._currentFolder});
};

ZmBriefcaseBaseView.prototype.processUploadFiles =
function() {
	var files = [];
	var ulEle = document.getElementById('zdnd_ul');
    if (ulEle) {
        for (var i = 0; i < ulEle.childNodes.length; i++) {
            var liEle = ulEle.childNodes[i];
            var inputEl = liEle.childNodes[0];
            if (inputEl.name != "_attFile_") continue;
            if (!inputEl.value) continue;
            var file = {
                fullname: inputEl.value,
                name: inputEl.value.replace(/^.*[\\\/:]/, "")
            };
            files.push(file);
         }
   }
   return files;
}
