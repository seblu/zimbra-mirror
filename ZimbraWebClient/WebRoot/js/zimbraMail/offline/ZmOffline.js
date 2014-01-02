/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
ZmOffline = function(){
    this._convIds = [];
    ZmOffline.CONVERSATION = "conversation";
    ZmOffline.MESSAGE = "message";
    ZmOffline.appCacheDone = false;
    ZmOffline.messageNotShowed = true;
    ZmOffline.cacheMessageLimit = 1000;
    ZmOffline.cacheProgress = [];
    ZmOffline.cacheConversationLimit = 1000;
    ZmOffline.ZmOfflineStore = "zmofflinestore";
    ZmOffline.ATTACHMENT = "Attachment";
    ZmOffline.REQUESTQUEUE = "RequestQueue";
    ZmOffline.syncStarted = false;
    ZmOffline._syncInProgress = false;
    ZmOffline.store = [];
    ZmOffline.folders = [];
    ZmOffline.calendars = {};
    ZmOffline.SUPPORTED_APPS = [ZmApp.MAIL, ZmApp.CONTACTS, ZmApp.CALENDAR];
    ZmOffline.SUPPORTED_MAIL_TREE_VIEWS = [ZmOrganizer.FOLDER, ZmOrganizer.SEARCH, ZmOrganizer.TAG];
    // The number of days we read into the future to get calendar entries
    ZmOffline.CALENDAR_LOOK_BEHIND = 7;
    ZmOffline.CALENDAR_READ_AHEAD  = 21;
};

ZmOffline._checkCacheDone =
function (){
    if (appCtxt.isWebClientOfflineSupported && ZmOffline.appCacheDone && ZmOffline.cacheProgress.length === 0 && ZmOffline.syncStarted && ZmOffline.messageNotShowed){
        appCtxt.setStatusMsg(ZmMsg.offlineCachingDone, ZmStatusView.LEVEL_INFO);
        ZmOffline.messageNotShowed = false;
    }
};

ZmOffline._getOfflineFolders =
function(){
    var folderTree = appCtxt.getFolderTree(appCtxt.accountList.mainAccount);
    var folders = folderTree && folderTree.getByType("FOLDER");
    if (!folders || !folders.length){
        return;
    }
    var offlineFolders = [];
    for (var i=0,length = folders.length; i < length; i++){
        if (folders[i].webOfflineSyncDays && folders[i].webOfflineSyncDays != "0"){
          offlineFolders.push({name:(folders[i].name).toLowerCase(), id:folders[i].nId, webOfflineSyncDays:folders[i].webOfflineSyncDays});
        }

    }
    return offlineFolders;
};

ZmOffline._checkAppCacheDone =
function (){
    ZmOffline.appCacheDone = true;
    ZmOffline._checkCacheDone();
};

ZmOffline.prototype.init =
function(cb){
    if (!appCtxt.isWebClientOffline()){
        this._fixLazyCSSLoadIssues();
        window.applicationCache.addEventListener('cached', function(e) {
            ZmOffline._checkAppCacheDone();
        }, false);

        window.applicationCache.addEventListener('noupdate', function(e) {
            ZmOffline._checkAppCacheDone();
        }, false);
    }

    var callback = (appCtxt.isWebClientOffline()) ? this.setOffline.bind(this, cb) : cb;
    ZmOfflineDB.indexedDB.init(callback);
    this._addListeners();
};

ZmOffline.prototype._fixLazyCSSLoadIssues =
function(){
    var cssUrl = appContextPath + "/css/msgview.css?v=" + cacheKillerVersion;
    var cssFile = localStorage[cssUrl];
    if (!cssFile){
        var result = AjxRpc.invoke(null, cssUrl, null, null, true);
        cssFile = localStorage[cssUrl] = result && result.text;
    }
};

ZmOffline.prototype.setOffline =
function(callback){
    callback();
    window.setTimeout(this._setNetwork.bind(this), 3000);
};

ZmOffline.prototype.setItem =
function(key, value, objStore) {
    if (!objStore){
        return;
    }

    objStore = objStore.toLowerCase();

    try{
        if (key){
            ZmOfflineDB.indexedDB.setItem(key, value, objStore);
        }
    }catch(ex){
        DBG.println(AjxDebug.DBG1, ex);
    }
};

ZmOffline.prototype.getItem =
function(key, callback, params, objStore) {

    if (!objStore){
        return;
    }

    objStore = objStore.toLowerCase();

    if ($.inArray(objStore, ZmOfflineDB.indexedDB.db.objectStoreNames) === -1){
        return;
    }
    var searchRequest = params && params.jsonObj && params.jsonObj.SearchRequest;
    if (searchRequest && searchRequest._jsns === "urn:zimbraMail" ){
        this._syncSearchRequest(callback, objStore, params);
        return;
    }

    if (key){
      ZmOfflineDB.indexedDB.getItem(key, callback, params, objStore);
    }
};
ZmOffline.prototype._addListeners =
function(){
    //$(window).bind("online offline",this._setNetwork.bind(this));
    //$(window).bind("online", this._replayOfflineRequest.bind(this));
    $(window).on("online offline", ZmOffline.checkServerStatus);
    $(document).on("ZWCOffline", this._onZWCOffline.bind(this));
    $(document).on("ZWCOnline", this._onZWCOnline.bind(this));
    setInterval(ZmOffline.checkServerStatus, 10000);
    //ZmZimbraMail.addListener(ZmAppEvent.ACTIVATE, new AjxListener(this, this._onAppActivate));
};


ZmOffline.prototype._setNetwork =
function() {
    var containerEl = document.getElementById(ZmId.SKIN_NETWORK);
	if (!containerEl) {
		return;
	}
    var isOffline = appCtxt.isWebClientOffline();
    if (isOffline){
        this._enableApps(false);
    }

    if (isOffline && !appCtxt.networkBtn){
        var button = new DwtToolBarButton({parent:DwtShell.getShell(window), id: ZmId.OP_GO_OFFLINE});
        button.setImage("Disconnect");
        button.setToolTipContent(ZmMsg.networkChangeWebOffline, true);
        button.reparentHtmlElement(ZmId.SKIN_NETWORK);
        appCtxt.networkBtn = button;
    } else if (!isOffline){
        Dwt.removeChildren(containerEl);
        appCtxt.networkBtn = null;
        //this.sendSyncRequest();
    }
};

ZmOffline.prototype._onZWCOffline =
function() {
    appCtxt.setStatusMsg(ZmMsg.OfflineServerNotReachable);
    this._disableApps();
};

ZmOffline.prototype._onZWCOnline =
function() {
    appCtxt.setStatusMsg(ZmMsg.OfflineServerReachable);
    this._enableApps();
    this._replayOfflineRequest();
    ZmOffline.syncData();
};

ZmOffline.prototype._enableApps =
function() {
    // Configure the tabs
    var appChooser = appCtxt.getAppChooser();
    for (var i in ZmApp.ENABLED_APPS) {
        var appButton = appChooser.getButton(i);
        if (appButton) {
            appButton.setEnabled(true);
        }
    }

    // Enable features for the current app
    var app = appCtxt.getCurrentApp();
    app.enableFeatures();
 };

ZmOffline.prototype._disableApps =
function() {
    // Configure the tabs
    var appChooser = appCtxt.getAppChooser();
    var enabledApps = AjxUtil.keys(ZmApp.ENABLED_APPS);
    var disabledApps = AjxUtil.arraySubstract(enabledApps, ZmOffline.SUPPORTED_APPS);
    for (var j = 0; j < disabledApps.length; j++) {
        var appButton = appChooser.getButton(disabledApps[j]);
        if (appButton) {
            appButton.setEnabled();
        }
    }

    // Disable features for the current app
    var app = appCtxt.getCurrentApp();
    app.enableFeatures();
};

// Mail

ZmOffline.prototype.cacheMailData =
function(){
    if (ZmOffline.folders.length === 0){
        this.initOfflineFolders();
        this.storeFoldersMetaData();
        ZmOfflineDB.indexedDB.addObjectStores(this._cacheOfflineData.bind(this));
        return;
    }
    this._cacheOfflineData();
};

ZmOffline.prototype.initOfflineFolders =
function(){
    ZmOffline.folders = ZmOffline._getOfflineFolders();
    for (var i=0, length = ZmOffline.folders.length;i<length;i++){
        ZmOffline.store.push((ZmOffline.folders[i].name).toLowerCase() + ZmOffline.MESSAGE);
        //ZmOffline.store.push(ZmOffline.folders[i] + ZmOffline.CONVERSATION);
    }

    // Get the possible set of calendars.  Used to insure getFolder allows access to the invite messages
    var calMgr = appCtxt.getCalManager();
    var calViewController = calMgr && calMgr.getCalViewController();
    var calendarIds = calViewController.getOfflineSearchCalendarIds();
    for (var i = 0; i < calendarIds.length; i++) {
        // Store for use in processing mail messages - allow invites
        ZmOffline.calendars[calendarIds[i]] = calendarIds[i];
    }

};

ZmOffline.prototype._cacheOfflineData =
function(){
    appCtxt.setStatusMsg(ZmMsg.offlineCachingSync, ZmStatusView.LEVEL_INFO);
    this._cacheContacts();
    if (!localStorage.getItem("syncToken")){
        for (var i=0, length=ZmOffline.folders.length; i<length;i++){
            this._downloadMessages(ZmOffline.folders[i], 0, ZmOffline.cacheMessageLimit, ZmOffline.MESSAGE, "all", 1, null);
            //this._downloadMessages(ZmOffline.folders[i], 0, ZmOffline.cacheConversationLimit, ZmOffline.CONVERSATION, "u1", 1, this._loadConversations.bind(this, ZmOffline.folders[i] , this._convIds));
        }
        ZmOffline.cacheProgress.push(ZmOffline.CALENDAR_PROGRESS);
        this._downloadCalendar(null, null, null, null, true, null);
    } else{
        this.sendSyncRequest();
    }

};

ZmOffline.prototype._downloadMessages =
function(folder, offset, limit, type, fetch, html, callback){
    if (!folder.webOfflineSyncDays || folder.webOfflineSyncDays == "0"){
        return;
    }

    var date = new Date();
    var offsetDate = AjxDateFormat.getDateInstance(AjxDateFormat.SHORT).format(AjxDateUtil.roll(date, AjxDateUtil.DAY, -(parseInt(folder.webOfflineSyncDays))))
    var jsonObj = {SearchRequest:{_jsns:"urn:zimbraMail"}};
    var request = jsonObj.SearchRequest;
    ZmOffline.cacheProgress.push(folder.name.toLowerCase() + type);
    ZmMailMsg.addRequestHeaders(request);
    request.offset = offset;
    request.limit = limit;
    request.types = type;
    request.fetch = fetch;
    request.html = html;
    request.query = "after:" + offsetDate + " in:\"" + folder.name +"\""
	var respCallback = this._handleResponseLoadMsgs.bind(this, folder, type, callback || null);
	appCtxt.getRequestMgr().sendRequest({jsonObj:jsonObj, asyncMode:true, callback:respCallback});
};


ZmOffline.prototype._handleResponseLoadMsgs =
function(folder, type, callback, result){

    var searchResponse = result._data && result._data.SearchResponse;
    var isConv = (type === ZmOffline.CONVERSATION);
    var messages =  (isConv) ? searchResponse.c : searchResponse.m;
    var folderName = folder.name.toLowerCase();
    DBG.println(AjxDebug.DBG1, "_handleResponseLoadMsgs folder: " + folderName + "  type: " + type);

    if (!messages || (messages.length === 0) ){
        this._updateCacheProgress(folderName + type);
        return;
    }
    if (isConv) {
        for(var i=0, length = messages.length; i < length ; i++){
            this._convIds.push(messages[i].id)
        }
    }
    else {
        this.addItem(messages);
    }
    if (type === ZmOffline.MESSAGE){
        this._updateCacheProgress(folderName + type);
    }
    if (callback){
        callback.run();
    }
};


ZmOffline.CALENDAR_PROGRESS = "Calendar Appointments";
ZmOffline.prototype._downloadCalendar =
function(startTime, endTime, calendarIds, callback, getMessages, previousMessageIds) {
    // Bundle it together in a batch request
    var jsonObj = {BatchRequest:{_jsns:"urn:zimbra", onerror:"continue"}};
    var request = jsonObj.BatchRequest;

    // Get the Cal Manager and CalViewController
    var calMgr = appCtxt.getCalManager();
    var calViewController = calMgr && calMgr.getCalViewController();
    var apptCache = calViewController.getApptCache();

    if (!startTime) {
        var endDate = new Date();
        endDate.setHours(23,59,59,999);
        //grab a week's appt backwards for reminders
        var startDate = new Date(endDate.getTime());
        startDate.setDate(startDate.getDate()-ZmOffline.CALENDAR_LOOK_BEHIND);
        startDate.setHours(0,0,0, 0);
        endDate.setDate(endDate.getDate()+ ZmOffline.CALENDAR_READ_AHEAD);
        startTime = startDate.getTime();
        endTime   = endDate.getTime();
    }

    if (!calendarIds) {
        // Get the calendars ids stored checked calendars for the first (main) account
        calendarIds = [];
        for (var calendarId in  ZmOffline.calendars) {
            calendarIds.push(calendarId);
        }
    }

    // Appt Search Request.  This request will provide data for the calendar view displays, the reminders, and
    // the minical display.  Entries will be stored as ZmAppt data,
    var searchParams = {
        start:            startTime,
        end:              endTime,
        accountFolderIds: calendarIds,
        folderIds:        calendarIds,
        offset:           0
    }
    apptCache.setFolderSearchParams(searchParams.folderIds, searchParams);
    request.SearchRequest = {_jsns:"urn:zimbraMail"};
    apptCache._setSoapParams(request.SearchRequest, searchParams);

    var respCallback = this._handleCalendarResponse.bind(this, startTime, endTime, callback, getMessages, previousMessageIds, null);
    appCtxt.getRequestMgr().sendRequest({jsonObj:jsonObj, asyncMode:true, callback:respCallback});
};

ZmOffline.prototype._downloadByApptId =
function(apptIds, itemQueryClause, startTime, endTime, callback) {
    // Place the search in a batchRequest, so handleCalendarResponse can unpack it in the same way
    var jsonObj = {BatchRequest:{_jsns:"urn:zimbra", onerror:"continue"}};
    jsonObj.BatchRequest.SearchRequest = {_jsns:"urn:zimbraMail"};
    var request = jsonObj.BatchRequest.SearchRequest;
    request.sortBy = "none";
    request.limit  = "500";
    request.offset = 0;
    request.locale = { _content: AjxEnv.DEFAULT_LOCALE };
    request.types  = ZmSearch.TYPE[ZmItem.APPT];
    var query      = itemQueryClause.join(" OR ");
    request.query  = {_content:query};

    // Call with getMessages == false, so callback will continue the downloading, and do the combined getMsgRequest call
    var respCallback = this._handleCalendarResponse.bind(this, startTime, endTime, callback, callback == null, null, apptIds);
    appCtxt.getRequestMgr().sendRequest({jsonObj:jsonObj, asyncMode:true, callback:respCallback});
}

ZmOffline.prototype._handleCalendarResponse =
function(startTime, endTime, callback, getMessages, previousMessageIds, apptIds, response) {

    var batchResp   = response && response._data && response._data.BatchResponse;
    var searchResp  = batchResp && batchResp.SearchResponse && batchResp.SearchResponse[0];

    try{
        var rawAppt;
        var appt;
        var apptList = new ZmApptList();
        // Convert the raw appts into ZmAppt objects.  Each rawAppt may represent several actual appointments (if the
        // appt is a recurring one), with differing start and end dates.  So break a raw appt into its component appts
        // and store them individually, with start and end time index info.
        apptList.loadFromSummaryJs(searchResp.appt);
        var numAppt = apptList.size();
        var apptContainers = [];
        var apptContainer;
        var apptStartTime;
        var apptEndTime;
        for (var i = 0; i < numAppt; i++) {
            appt       = apptList.get(i);
            this._cleanApptForStorage(appt);
            apptStartTime  = appt.startDate.getTime();
            apptEndTime    = appt.endDate.getTime();
            // If this was called via _downloadByApptId (Sync items), prune if a synced item is outside
            // of the display window
            if (((apptStartTime >= startTime) && (apptStartTime <= endTime)) ||
                ((apptEndTime   >= startTime) && (apptEndTime   <= endTime)) ) {
                // The appts do not contain a unique id for each instance.  Generate one (used as the primary key),
                // for each appt/instance and store the appt with a container that has the index fields
                apptContainer = {appt: appt,
                                 instanceId: this._createApptPrimaryKey(appt),
                                 id:         appt.id,
                                 invId:      appt.invId,
                                 startDate:  apptStartTime,
                                 endDate:    apptEndTime
                                };
                apptContainers.push(apptContainer);
            }
            if (apptIds) {
                delete apptIds[appt.id];
            }
        }
        // Store the new/modified entries
        ZmOfflineDB.setItem(apptContainers, ZmApp.CALENDAR, null, this.calendarDownloadErrorCallback.bind(this));

        // Sync informed us that the remainining apptIds were modified, but we did not find them when searching.
        // This implies they were moved to trash.  We don't track the Trash folder offline, so delete them.
        var search;
        var offlineDeleteTrashedAppts = this._offlineDeleteAppts.bind(this);
        for (var apptId in apptIds) {
            search = [apptId];
            // If its a recurring appt, several ZmAppts may share the same id.  Find them and delete them all
            ZmOfflineDB.doIndexSearch(search, ZmApp.CALENDAR, null, offlineDeleteTrashedAppts, null, "id");
        }

        // Now make a server read to get the detailed appt invites, for edit view and tooltips
        var msgIds = previousMessageIds ? previousMessageIds : [];
        if (getMessages) {
            if (searchResp.appt) {
                for (var j = 0; j < searchResp.appt.length; j++) {
                    // If present, accumulate both the series id and invId.  The user may ask for a series or
                    // individual appt.  ZmCalItem.getDetails does:
                    //   var id = seriesMode ? (this.seriesInvId || this.invId || this.id) : this.invId;
                    rawAppt = searchResp.appt[j];
                    var seriesId = rawAppt.seriesInvId || rawAppt.invId || rawAppt.id;
                    msgIds.push(seriesId);
                    if (seriesId !== rawAppt.invId) {
                        msgIds.push(rawAppt.invId);
                    }
                }
                var updateProgress = this._updateCacheProgress.bind(this, ZmOffline.CALENDAR_PROGRESS);
                this._loadMessages(msgIds, updateProgress);
            } else {
                this._updateCacheProgress(ZmOffline.CALENDAR_PROGRESS);
            }
        }

        var calMgr = appCtxt.getCalManager();
        var calViewController = calMgr && calMgr.getCalViewController();
        var apptCache = calViewController.getApptCache();
        apptCache.clearCache();
    }catch(ex){
        DBG.println(AjxDebug.DBG1, ex);
        if (!callback) {
            this._updateCacheProgress(ZmOffline.CALENDAR_PROGRESS);
        }
    }

    if (callback) {
        callback.run(msgIds);
    }

};
ZmOffline.prototype.calendarDownloadErrorCallback =
function(e) {
    DBG.println(AjxDebug.DBG1, "Error while adding appts to indexedDB.  Error = " + e);
}

ZmOffline.prototype._createApptPrimaryKey =
function(appt) {
    return appt.id + ":" + appt.startDate.getTime();
}

ZmOffline.prototype._cleanApptForStorage =
function(appt) {
    delete appt.list;
    delete appt._list;
    delete appt._evt;
    delete appt._evtMgr;
}

ZmOffline.prototype._updateCacheProgress =
function(folderName){

    var index = $.inArray(folderName, ZmOffline.cacheProgress);
    if(index != -1){
        ZmOffline.cacheProgress.splice(index, 1);
    }
    if (ZmOffline.cacheProgress.length === 0){
        this.sendSyncRequest();
        ZmOffline._checkCacheDone();
    }
    DBG.println(AjxDebug.DBG1, "_updateCacheProgress folder: " + folderName + " ZmOffline.cacheProgress " + ZmOffline.cacheProgress.join());


};

ZmOffline.prototype._loadConversations =
function(store, convIds){
    if (!convIds || convIds.length === 0){
        return;
    }
    var soapDoc = AjxSoapDoc.create("BatchRequest", "urn:zimbra");
    soapDoc.setMethodAttribute("onerror", "continue");

    for (var i=0, length = convIds.length;i< length; i++) {
        var requestNode = soapDoc.set("GetConvRequest",null,null,"urn:zimbraMail");
        var conv = soapDoc.set("c", null, requestNode);
        conv.setAttribute("id", convIds[i]);
        conv.setAttribute("read", 0);
        conv.setAttribute("html", 1);
        conv.setAttribute("needExp", 1);
        conv.setAttribute("max", 250000);
        conv.setAttribute("fetch", "all");
        conv.setAttribute("header", ["{n:List-ID}","{n:X-Zimbra-DL}","{n:IN-REPLY-TO}"]);
     }

    var respCallback = this._cacheConversations.bind(this, store);
    appCtxt.getRequestMgr().sendRequest({
        soapDoc: soapDoc,
        asyncMode: true,
        callback: respCallback
    });

};

ZmOffline.prototype._cacheConversations  =
function(folder, result){
    var convs = result.getResponse().BatchResponse.GetConvResponse;
    if (!convs  || convs.length === 0){
        return;
    }
    var objStore = folder.toLowercase() + ZmOffline.CONVERSATION;
    for (var i=0, length = convs.length || 0; i<length; i++){
        this.addItem(convs[i].c[0], ZmOffline.CONVERSATION, objStore);
    }
    var index = $.inArray(objStore, ZmOffline.cacheProgress);
    if(index != -1){
        ZmOffline.cacheProgress.splice(index, 1);
    }
    ZmOffline._checkCacheDone();
};

ZmOffline.prototype._getValue =
function(message, isConv){
    var result = {
   "Header":{
      "context":{
         "session":{
            "id":"offline_id",
            "_content":"offline_content"
         },
         "change":{
            "token":"offline_token"
         },
         "_jsns":"urn:zimbra"
      }
   },
   "Body":{},
    "_jsns":"urn:zimbraSoap"
    };
    if (isConv){
        result.Body.SearchConvResponse = {
         "_jsns": "urn:zimbraMail",
         "offset":"0",
         "sortBy":"dateDesc",
         "more":false,
         "_jsns":"urn:zimbraMail"
        }
        result.Body.SearchConvResponse.m = message.m;
    }else{
       result.Body.GetMsgResponse = {"m" : [message]};
    }
    return result;
};


ZmOffline.syncData =
function(){
    var groupMailBy = appCtxt.get(ZmSetting.GROUP_MAIL_BY);
    var mlc = (groupMailBy == ZmSetting.GROUP_BY_CONV) ? AjxDispatcher.run("GetConvListController") :  AjxDispatcher.run("GetTradController");
    mlc && mlc.runRefresh(); // Mails
    var clc = AjxDispatcher.run("GetContactListController");
    clc && clc.runRefresh(); // Contacts
    var cm = appCtxt.getCalManager();
    var cvc = cm && cm.getCalViewController();
    cvc && cvc.runRefresh(); // Appointments
};

ZmOffline.prototype.getSyncRequest =
function(){
    var syncToken = localStorage.getItem("syncToken");
    var soapDoc = AjxSoapDoc.create("SyncRequest", "urn:zimbraMail");
    if (syncToken) {
        soapDoc.set("token", syncToken);
    }
    return  soapDoc;

}

ZmOffline.prototype.isSyncInProgress =
function(){
  return ZmOffline._syncInProgress;
};

ZmOffline.prototype._setSyncInProgress =
function(value){
 ZmOffline._syncInProgress = value;
};

ZmOffline.prototype.sendSyncRequest =
function(){
    if (this.isSyncInProgress()){
        return;
    }
    this._setSyncInProgress(true);
	var syncReq = this.getSyncRequest();
	appCtxt.getAppController().sendRequest({
		soapDoc:syncReq,
		asyncMode:true,
		noBusyOverlay:true,
		callback:this.syncHandler.bind(this)
	});

};

ZmOffline.prototype.syncHandler =
function(result){
    var response = result && result.getResponse();
    var syncResponse = response && response.SyncResponse;
    var syncToken =  syncResponse.token;
    if (syncToken){
        localStorage.setItem("syncToken", syncToken);
    }
    ZmOffline.syncStarted = true;
    ZmOffline._checkCacheDone();
    this._processSyncData(syncResponse);  // To be called when user switchs from offline to online or first access
};

ZmOffline.prototype._processSyncData =
function(syncResponse){
    this._setSyncInProgress(false);
    if (!syncResponse){
        return;
    }

    // Handle delete
    this._handledeleteItems(syncResponse.deleted);

    this._handleUpdateItems(syncResponse.m,    "message");
    this._handleUpdateAppts(syncResponse.appt, "appt");

    //this._handleUpdateItems(syncResponse.c, "conversation");  Dependency on Bug#81962 (Need sync support for conversations)


};

ZmOffline.prototype._handledeleteItems =
function(deletedItems){

    if (!deletedItems || deletedItems.length == 0){
        return;
    }

    var ids = deletedItems[0].ids;

    if (ids){
        this._deleteItemByIds(ids.split(','));
    }

};

ZmOffline.prototype._handleUpdateItems =
function(items, type){

// Handle create and modify

    if (!items || items.length === 0 ){
        return;
    }

    var updated = [], created = [], deleted = [], item=null, folder = null;

    for (var i=0, length = items.length; i < length; i++){
        item = items[i];
        if (item.l === "2" || item.l === "6" || item.l === "5"){
            if (item.cid){
                updated.push(item);
            }else{
                created.push(item.id);
            }

        } else {
            deleted.push(item.id);
        }

    }

// Create

    if (created.length){
        this._loadMessages(created);
    }


// Modify

    if (updated.length){
        var store = null;
        for (var i=0, length = updated.length;i<length;i++){
            this.modifyItem(updated[i].id, updated[i], type);
        }
    }

    if (deleted.length){
        this._deleteItemByIds(deleted); // If messages are moved
    }

};


// Different enough from mail to warrent its own function
ZmOffline.prototype._handleUpdateAppts =
function(items, type){

    if (!items) items = [];

    var item = null;
    var folders = {};
    var startOfDayDate = new Date();
    var currentTime    = startOfDayDate.getTime();
    var endOfDayDate   = new Date(currentTime);

    // We only care about changes within our display window.
    startOfDayDate.setHours(0,0,0,0);
    var newStartTime = startOfDayDate.getTime() - (AjxDateUtil.MSEC_PER_DAY * ZmOffline.CALENDAR_LOOK_BEHIND);
    endOfDayDate.setHours(23,59,59,999)
    var newEndTime   = endOfDayDate.getTime()   + (AjxDateUtil.MSEC_PER_DAY * ZmOffline.CALENDAR_READ_AHEAD);

    var previousEndTime = newEndTime;
    var lastSyncTime = localStorage.getItem("calendarSyncTime");
    if (lastSyncTime) {
        previousEndTime = parseInt(lastSyncTime) + (AjxDateUtil.MSEC_PER_DAY * ZmOffline.CALENDAR_READ_AHEAD);
    }
    localStorage.setItem("calendarSyncTime", endOfDayDate.getTime());

    // Accumulate the ids of the altered appts
    var itemQueryClause = [];
    var apptIds  = {};
    for (var i = 0, length = items.length; i < length; i++){
        item = items[i];
        apptIds[item.id] = true;
        itemQueryClause.push("item:\"" + item.id.toString() + "\"");
    }

    // If this is not the first download, we need to extend the visible range beyond the last update which read
    // calendar items from current-1week to current+3weeks.  So if 2 days has passed since the previous
    // read, read from the previousEnd to previousEnd+2days;  We also update previousEnd (by setting
    // calendarSyncTime in localstore).
    var doEndRangeDownloadCallback = (newEndTime > previousEndTime) ?
        this._downloadCalendar.bind(this, previousEndTime, newEndTime, null, null, true) : null;

    if (itemQueryClause.length > 0) {
        this._downloadByApptId(apptIds, itemQueryClause, newStartTime, newEndTime, doEndRangeDownloadCallback);
    } else if (doEndRangeDownloadCallback) {
        doEndRangeDownloadCallback.run();
    }

    // Delete items < newStartTime, in order to prune old entries and prevent monotonic accumulation
    // Search for items whose endTime is from 0 to newStartTime
    var search = [0, newStartTime];
    var errorCallback = this._expiredErrorCallback.bind(this);
    var offlineSearchExpiredAppts = this._offlineDeleteAppts.bind(this);
    ZmOfflineDB.doIndexSearch(search, ZmApp.CALENDAR, null, offlineSearchExpiredAppts, errorCallback, "endDate");
};

ZmOffline.prototype._offlineDeleteAppts =
function(apptContainers) {
    var appt;
    for (var i = 0; i < apptContainers.length; i++) {
        appt = apptContainers[i].appt;
        ZmOfflineDB.indexedDB.deleteItem(this._createApptPrimaryKey(appt), ZmApp.CALENDAR, null);
    }
}

ZmOffline.prototype._expiredErrorCallback =
function(e) {
    DBG.println(AjxDebug.DBG1, "Error while reading expired appts in indexedDB.  Error = " + e);
}







ZmOffline.prototype._updateItem =
function(type, modifiedItem, result){
    var store = null, callback = null;
    var folder =  this._getFolder(modifiedItem.l);
    if (!folder){
        return;
    }
    folder = folder.toLowerCase();
    var offlineItem = result && result.response && result.response.Body && result.response.Body.GetMsgResponse.m[0];

    DBG.println(AjxDebug.DBG1, "ZmOffline.prototype.modifyItem : offlineItem " + JSON.stringify(offlineItem));

    if (offlineItem){
        var prevFolder = offlineItem && this._getFolder(offlineItem.l);
        prevFolder = prevFolder && prevFolder.toLowerCase();
        var prevKey = offlineItem && offlineItem.id;
        $.extend(offlineItem, modifiedItem);
        folder = this._getFolder(offlineItem.l);
        folder = folder && folder.toLowerCase();
        if (folder){
            callback = this.addItem.bind(this, offlineItem, type, (folder + type));
        }
        ZmOfflineDB.indexedDB.deleteItem(prevKey, (prevFolder + type), callback);
    } else {
        if (folder){
            this.addItem(modifiedItem, type, (folder + type));
        }
    }
};

ZmOffline.prototype._loadMessages =
function(msgIds, callback){
    if (!msgIds || msgIds.length === 0){
        return;
    }
    var soapDoc = AjxSoapDoc.create("BatchRequest", "urn:zimbra");
    soapDoc.setMethodAttribute("onerror", "continue");
    for (var i=0, length = msgIds.length;i<length;i++){
        var requestNode = soapDoc.set("GetMsgRequest",null,null,"urn:zimbraMail");
        var msg = soapDoc.set("m", null, requestNode);
        msg.setAttribute("id", msgIds[i]);
        msg.setAttribute("read", 0);
        msg.setAttribute("html", 1);
        msg.setAttribute("needExp", 1);
        msg.setAttribute("max", 250000);
     }
    var respCallback = this._cacheMessages.bind(this, callback);
    appCtxt.getRequestMgr().sendRequest({
        soapDoc: soapDoc,
        asyncMode: true,
        callback: respCallback
    });

};

ZmOffline.prototype._cacheMessages =
function(callback, result){
    var response = result.getResponse();
    var msgs = response && response.BatchResponse.GetMsgResponse;
    this._putItems(msgs);
    if (callback) {
        callback.run();
    }
};

ZmOffline.prototype._putItems =
function(items){
    if (!items || items.length === 0 ){
        return;
    }
    var key = null, value = null, msg = null;
    for (var i=0, length = items.length; i<length; i++){
        msg = items[i].m[0];
        folder = this._getFolder(msg.l);
        if (folder){  // Only inbox for now
            this.addItem(msg, ZmOffline.MESSAGE,  + ZmOffline.MESSAGE );
        } else { // Message is moved
            ZmOfflineDB.indexedDB.deleteItemById(msg.id);
        }
    }
};

ZmOffline.prototype._syncSearchRequest =
function(callback, store, params){
    ZmOfflineDB.indexedDB.getAll(store, this._generateMsgSearchResponse.bind(this, callback, params, store), ZmOffline.cacheMessageLimit);
};

ZmOffline.prototype.syncFoldersMetaData =
function(){
    for (var i=0, length = ZmOffline.folders.length;i<length;i++){
        this.setFolderMetaData(ZmOffline.folders[i].id, localStorage.getItem(ZmOffline.folders[i].id));
    }

};

ZmOffline.prototype.setFolderMetaData =
function(id, data){
    var folderData = JSON.parse(data);
    var folder = appCtxt.getById(id);
    if (folder && folderData){
        folder.numUnread = folderData.numUnread;
        folder.numTotal = folderData.numTotal;
        folder.sizeTotal = folderData.sizeTotal;
    }

};


ZmOffline.prototype.storeFolderMetaData =
function(id, data){
    var folderData = {};
    folderData.numUnread = data.numUnread;
    folderData.numTotal = data.numTotal;
    folderData.sizeTotal = data.sizeTotal;
    localStorage.setItem(id, JSON.stringify(folderData));
};

ZmOffline.prototype.storeFoldersMetaData =
function(){
    for (var i=0, length = ZmOffline.folders.length;i<length;i++){
        this.storeFolderMetaData(ZmOffline.folders[i].id, appCtxt.getById(ZmOffline.folders[i].id));
    }
};

ZmOffline.prototype._generateMsgSearchResponse =
function(callback, params, store, messages){
    var searchResponse = [];
    var response = {
   "Header":{
      "context":{
         "session":{
            "id":"offline_id",
            "_content":"offline_content"
         },
         "change":{
            "token":"offline_token"
         },
         "_jsns":"urn:zimbra"
      }
   },
   "Body":{
       "SearchResponse":{
           "sortBy": "dateDesc",
           "offset": 0,
           "more": false
       },
       "_jsns":"urn:zimbraSoap"
   }
   };
    for (i = (messages.length -1); i > -1 ; i--){
        var msg = this._getHeaders(messages[i], store);
        searchResponse.push(msg);
    }
    searchResponse = searchResponse.sort(function(item1, item2){
        if (item1['d'] === item2['d']){
            return ((Math.abs(parseInt(item1['id'])) < Math.abs(parseInt(item2['id'])) ) ? 1 : -1);
        }
        return ((item1['d'] < item2['d'] ) ? 1 : -1);
    });
    appCtxt._msgSearchResponse = searchResponse;
    if (store.match(/message$/)){
        response.Body.SearchResponse.m = searchResponse;
    } else {
        response.Body.SearchResponse.c = searchResponse;
    }

    if (!params._skipResponse){
        params.response = response;
    } else {
        params = null;
    }

    callback(params);
};

ZmOffline.prototype._getHeaders =
function(item, store){
    var headers = null;
    mailItem = item.value.Body;
    var headers = store.match(/message$/) ? mailItem.GetMsgResponse.m[0] : mailItem.SearchConvResponse.c;
    if (headers && headers.mp){
       delete (headers.mp)
    }
    return headers;
};

ZmOffline.prototype._replayOfflineRequest =
function() {
    var callback = this._sendOfflineRequest.bind(this);
    ZmOfflineDB.indexedDB.getItemInRequestQueue(false, callback)
};

ZmOffline.prototype._sendOfflineRequest =
function(result) {

    if (!result || result.length === 0) {
        return;
    }

    var obj = result.shift();

    if (obj) {
        var methodName = obj.methodName;
        if (methodName) {
            if (methodName === "SendMsgRequest" || methodName === "SaveDraftRequest") {
                var msg = obj[methodName].m;
                var flags = msg.f;
                var attach = msg.attach;
                if (attach) {
                    for (var j in attach) {
                        if (attach[j] && attach[j].isOfflineUploaded) {
                            return this._uploadOfflineAttachments(obj, msg, result);
                        }
                    }
                }
                if (flags && flags.indexOf(ZmItem.FLAG_OFFLINE_CREATED) !== -1) {
                    msg.f = flags.replace(ZmItem.FLAG_OFFLINE_CREATED, "");//Removing the offline created flag
                    delete msg.id;//Removing the temporary id
                    delete msg.did;//Removing the temporary draft id
                }
            }
            var params = {
                noBusyOverlay : true,
                asyncMode : true,
                callback : this._handleResponseSendOfflineRequest.bind(this, obj, result),
                errorCallback : this._handleErrorResponseSendOfflineRequest.bind(this, obj, result),
                jsonObj : {}
            };
            params.jsonObj[methodName] = obj[methodName];
            return appCtxt.getRequestMgr().sendRequest(params);
        }
    }

    this._sendOfflineRequest(result);
};

ZmOffline.prototype._handleResponseSendOfflineRequest =
function(obj, result) {
    this._sendOfflineRequest(result);
    var notify = {
        deleted : {
            id : obj.id.toString()
        },
        modified : {
            folder : [{
                id : ZmFolder.ID_OUTBOX,
                n : appCtxt.getById(ZmFolder.ID_OUTBOX).numTotal - 1
            }]
        }
    };
    appCtxt.getRequestMgr()._notifyHandler(notify);
    ZmOfflineDB.indexedDB.deleteItemInRequestQueue(obj.oid);
};

ZmOffline.prototype._handleErrorResponseSendOfflineRequest =
function(obj, result) {
    this._sendOfflineRequest(result);
    return true;
};

/**
 * Adds conversation or message into the offline database.
 * @param {item}	message or conversation that needs to be added to offline database.
 * @param {type}    type of the mail item ("message" or "conversation").
 */

ZmOffline.prototype.addItem =
function(item, type, store){
    var isConv = (type === ZmOffline.CONVERSATION);
    var value = this._getValue(item, isConv);
    if (value && value.Body && value.Body.GetMsgResponse) {
        var callback = this._fetchMsgAttachments.bind(this, item);
        ZmOfflineDB.setItem(item, ZmApp.MAIL, callback);
        return;
    }
    store = store || ((item.l) ? this._getFolder(item.l) + type : ZmOffline.ZmOfflineStore);
    this.setItem(item.id, value, store);
};

ZmOffline.prototype._getFolder =
function(index){
    for (var i=0, length = ZmOffline.folders.length; i< length; i++){
        if (ZmOffline.folders[i].id == index){
            return ZmOffline.folders[i].name;
        }
    }
    if (ZmOffline.calendars[index]) {
        return ZmOffline.calendars[index];
    } else {
        return null;
    }
};

/**
 * Deletes conversations or messages from the offline database.
 * @param {deletedIds}	array of message/convesation id's to be deleted from offline database.
 * @param {type}    type of the mail item ("message" or "conversation").
 */

ZmOffline.prototype.deleteItem =
function(deletedIds, type, folder){
    if (!deletedIds || deletedIds.length === 0){
        return;
    }
    var store = folder + type;
    for (var i=0, length = deletedIds.length;i < length; i++){
        ZmOfflineDB.indexedDB.deleteItem(deletedIds[i], store);
    }

};


/**
 * Modifies  message or conversation in the offline database.
 * @param {id}	    message/convesation id that should be modified.
 * @param {newItem}    modified item or an object with modified fields as {{'f','fu'}, ['tn':'aaa'] ... }.
 * @param {type}    message or conversation
 */

ZmOffline.prototype.modifyItem =
function(id, newItem, type){
    DBG.println(AjxDebug.DBG1, "ZmOffline.prototype.modifyItem : id " + id);

    var updateItem = this._updateItem.bind(this, type, newItem);
    var cb = this.getItem.bind(this, id, updateItem, {});
    ZmOfflineDB.indexedDB.getItemById(id, cb);
};


ZmOffline.prototype._deleteItemByIds =
function(ids){
    if (!ids || ids.length === 0){
        return;
    }
    for(var i=0, length = ids.length; i < length; i++){
        ZmOfflineDB.indexedDB.deleteItemById(ids[i]);
    }
};

/**
 * Modifies  message or conversation in the offline database.
 * @param {id}	    message/convesation id that should be modified.
 * @param {stores}   Array of store [folder + type] values
 */


ZmOffline.prototype._deleteItemById =
function(id, stores){
      for (store in stores){
          ZmOfflineDB.indexedDB.deleteItem(id, store);
      }
};

ZmOffline.deleteAllOfflineData =
function(){
    DBG.println(AjxDebug.DBG1, "Delete all offline data");

    delete localStorage['syncToken'];
    indexedDB.deleteDatabase('ZmOfflineDB');
    indexedDB.deleteDatabase("OfflineLog");
};
/*
ZmOffline.prototype._enableMailFeatures =
function(online) {
    var mailApp = appCtxt.getApp(ZmApp.MAIL);
    var mlc = mailApp.getMailListController();
    var view = appCtxt.isWebClientOffline() ? ZmId.VIEW_TRAD : localStorage.getItem("MAILVIEW");
    mlc.switchView(view, true);
    var toolbar = mlc._toolbar[mlc._currentViewId];
    toolbar && mlc._resetOperations(mlc._toolbar[mlc._currentViewId], 1);

    var overview = mailApp.getOverview();
    var children = (overview && overview.getChildren()) || [];
    var selector = null;

    if (online){
        for (var i=0,length = children.length; i<length; i++){
            children[i].setVisible(true);
        }
        $("[id$='_addshare_link']").show();
        var folderTree = appCtxt.getFolderTree(appCtxt.accountList.mainAccount);
        var folders = folderTree && folderTree.getByType("FOLDER");
        var treeItem = null;
        for (var i=1, length = folders.length; i<length;i++){
                treeItem = overview.getTreeItemById(folders[i].id);
                if (treeItem){
                    treeItem.setVisible(true);
                }
        }
    } else {
        for (var i=0,length = children.length; i<length; i++){
            selector = "#" + children[i].getHTMLElId() + " .DwtTreeItemLevel1ChildDiv > div";
            if (children[i].type !== "FOLDER"){
               children[i].setVisible(false);
            }
        }
        $("[id$='_addshare_link']").hide()
        var folderTree = appCtxt.getFolderTree(appCtxt.accountList.mainAccount);
        var folders = folderTree && folderTree.getByType("FOLDER");
        var treeItem = null;
        for (var i=1, length = folders.length; i<length;i++){
            if (folders[i].name == "Outbox"){
                continue;
            }
            if (folders[i].webOfflineSyncDays == "0"){
                treeItem = overview.getTreeItemById(folders[i].id);
                if (treeItem){
                    treeItem.setVisible(false);
                }
            }
        }
    }
};
*/

ZmOffline.closeDB =
function(){
    if (!localStorage.getItem("syncToken")){
        DBG.println(AjxDebug.DBG1, "Incomplete initial sync, deleting message data");
        ZmOffline.deleteAllOfflineData();
        return;
    }
    DBG.println(AjxDebug.DBG1, "Closing offline databases");

    ZmOfflineDB.indexedDB.close();
};

ZmOffline.generateMsgResponse =
function(result) {
    var resp = [],
        obj,
        msgNode,
        generatedMsg,
        messagePart,
        i,
        length;

    result = [].concat(result);
    for (i = 0, length = result.length; i < length; i++) {
        obj = result[i];
        if (obj) {
            msgNode = obj[obj.methodName] && obj[obj.methodName]["m"];
            if (msgNode) {
                generatedMsg = {
                    id : msgNode.id,
                    f : msgNode.f || "",
                    mid : msgNode.mid,
                    cid : msgNode.cid,
                    idnt : msgNode.idnt,
                    e : msgNode.e,
                    l : "",
                    fr : "",
                    su : msgNode.su._content,
                    mp : [],
                    d : msgNode.d
                };
                //Flags
                if (obj.methodName === "SendMsgRequest") {
                    generatedMsg.f = generatedMsg.f.replace(ZmItem.FLAG_ISSENT, "").concat(ZmItem.FLAG_ISSENT);
                }
                else if (obj.methodName === "SaveDraftRequest") {
                    generatedMsg.f = generatedMsg.f.replace(ZmItem.FLAG_ISDRAFT, "").concat(ZmItem.FLAG_ISDRAFT);
                }
                if (msgNode.attach) {//attachment is there
                    generatedMsg.f = generatedMsg.f.replace(ZmItem.FLAG_ATTACH, "").concat(ZmItem.FLAG_ATTACH);
                }
                //Folder id
                if (obj.methodName === "SendMsgRequest") {
                    generatedMsg.l = ZmFolder.ID_OUTBOX.toString();
                }
                else if (obj.methodName === "SaveDraftRequest") {
                    generatedMsg.l = ZmFolder.ID_DRAFTS.toString();
                }
                //Message part
                messagePart = msgNode.mp[0];
                if (messagePart) {
                    var attach = msgNode.attach;
                    if (attach && attach.aid) { //attachment is there

                        generatedMsg.mp.push({
                            ct : ZmMimeTable.MULTI_MIXED,
                            part: ZmMimeTable.TEXT,
                            mp : []
                        });

                        if (messagePart.ct === ZmMimeTable.TEXT_PLAIN) {

                            generatedMsg.mp[0].mp.push({
                                body : true,
                                part : "1",
                                ct : ZmMimeTable.TEXT_PLAIN,
                                content : messagePart.content._content
                            });
                            generatedMsg.fr = generatedMsg.mp[0].mp[0].content;

                        } else if (messagePart.ct === ZmMimeTable.MULTI_ALT) {

                            generatedMsg.mp[0].mp.push({
                                ct : ZmMimeTable.MULTI_ALT,
                                part : "1",
                                mp : [{
                                        ct : ZmMimeTable.TEXT_PLAIN,
                                        part : "1.1",
                                        content : (messagePart.mp[0].content) ? messagePart.mp[0].content._content : ""
                                       },
                                       {
                                        ct : ZmMimeTable.TEXT_HTML,
                                        part : "1.2",
                                        body : true,
                                        content : (messagePart.mp[1].content) ? messagePart.mp[1].content._content : ""
                                       }
                                ]
                            });
                            generatedMsg.fr = generatedMsg.mp[0].mp[0].mp[0].content;

                        }

                        var attachIds = attach.aid.split(",");
                        for (var j = 0; j < attachIds.length; j++) {
                            var attachment = attach[attachIds[j]];
                            if (attachment) {
                                generatedMsg.mp[0].mp.push({
                                    cd : "attachment",
                                    ct : attachment.ct,
                                    filename : attachment.filename,
                                    aid : attachment.aid,
                                    s : attachment.s,
                                    data : attachment.data,
                                    isOfflineUploaded : attachment.isOfflineUploaded,
                                    part : (j + 2).toString()
                                });
                            }
                        }

                    } else {

                        if (messagePart.ct === ZmMimeTable.TEXT_PLAIN) {

                            generatedMsg.mp.push({
                                ct : ZmMimeTable.TEXT_PLAIN,
                                body : true,
                                part : "1",
                                content : messagePart.content._content
                            });
                            generatedMsg.fr = generatedMsg.mp[0].content;

                        } else if (messagePart.ct === ZmMimeTable.MULTI_ALT) {

                            generatedMsg.mp.push({
                                ct : ZmMimeTable.MULTI_ALT,
                                part: ZmMimeTable.TEXT,
                                mp : [{
                                        ct : ZmMimeTable.TEXT_PLAIN,
                                        part : "1",
                                        content : (messagePart.mp[0].content) ? messagePart.mp[0].content._content : ""
                                    },
                                    {
                                        ct : ZmMimeTable.TEXT_HTML,
                                        part : "2",
                                        body : true,
                                        content : (messagePart.mp[1].content) ? messagePart.mp[1].content._content : ""
                                    }
                                ]
                            });
                            generatedMsg.fr = generatedMsg.mp[0].mp[0].content;

                        }
                    }
                }
            }
            resp.push(generatedMsg);
        }
    }
    return resp;
};

/**
 * For ZWC offline, adds outbox folder
 */
ZmOffline.addOutboxFolder =
function() {
    if (!appCtxt.isWebClientOfflineSupported) {
        return;
    }
    var folderTree = appCtxt.getFolderTree(),
        root = folderTree.root,
        folderObj = {
            id: ZmFolder.ID_OUTBOX,
            absFolderPath: "/Outbox",
            activesyncdisabled: false,
            name: "Outbox"
        };
    var folder = ZmFolderTree.createFolder(ZmOrganizer.FOLDER, root, folderObj, folderTree, null, "folder");
    root.children.add(folder);
    ZmOffline.updateOutboxFolderCount();
};

ZmOffline.updateOutboxFolderCount =
function() {
    var key = {methodName : "SendMsgRequest"};
    ZmOfflineDB.indexedDB.getItemCountInRequestQueue(key, ZmOffline.updateOutboxFolderCountCallback);
};

ZmOffline.updateOutboxFolderCountCallback =
function(count) {
    var outboxFolder = appCtxt.getById(ZmFolder.ID_OUTBOX);
    if (outboxFolder) {
        outboxFolder.notifyModify({n : count});
    }
};

ZmOffline.prototype._modifyWebOfflineSyncDays =
function(folderId){
    var folderInfo = appCtxt.getById(folderId);
    var storeName = (folderInfo.name).toLowerCase() + ZmOffline.MESSAGE;
    var callback = this._downloadMessages.bind(this, folderInfo, 0, ZmOffline.cacheMessageLimit, ZmOffline.MESSAGE, "all", 1, null);
    if ($.inArray(storeName, ZmOfflineDB.indexedDB.db.objectStoreNames) !== -1) {
        ZmOfflineDB.indexedDB.clearObjStore(storeName, callback);
    } else {
        ZmOffline.store.push(storeName);
        ZmOfflineDB.indexedDB.addObjectStores(callback);
    }

};

ZmOffline.prototype._uploadOfflineAttachments =
function (obj, msg, result) {
    var attach = msg.attach,
        aidArray = attach.aid.split(",");

    for (var i = 0; i < aidArray.length; i++) {
        var aid = aidArray[i],
            attachment = attach[aid];
        if (attachment && attachment.isOfflineUploaded) {
            var blob = AjxUtil.dataURItoBlob(attachment.data);
            if (blob) {
                blob.name = attachment.filename;
                var callback = this._uploadOfflineAttachmentsCallback.bind(this, attachment.aid, obj, msg, result);
                var errorCallback = this._uploadOfflineAttachmentsErrorCallback.bind(this, attachment.aid, obj, msg, result);
                ZmComposeController.prototype._uploadImage(blob, callback, errorCallback);
            }
        }
    }
};

ZmOffline.prototype._uploadOfflineAttachmentsCallback =
function(attachmentId, obj, msg, result, uploadResponse) {
    var attach = msg.attach,
        isOfflineUploaded = false;

    delete attach[attachmentId];
    attach.aid = attach.aid.replace(attachmentId, uploadResponse[0].aid);
    for (var j in attach) {
        if (attach[j] && attach[j].isOfflineUploaded) {
            isOfflineUploaded = true;
        }
    }
    if (isOfflineUploaded === false) {
        this._sendOfflineRequest(result.concat(obj));
    }
};

ZmOffline.prototype._uploadOfflineAttachmentsErrorCallback =
function(attachmentId, obj, msg, result) {
    var attach = msg.attach,
        isOfflineUploaded = false;

    delete attach[attachmentId];
    AjxUtil.arrayRemove(attach.aid, attachmentId);
    for (var j in attach) {
        if (attach[j] && attach[j].isOfflineUploaded) {
            isOfflineUploaded = true;
        }
    }
    if (isOfflineUploaded === false) {
        this._sendOfflineRequest(result.concat(obj));
    }
};

ZmOffline.prototype._uploadOfflineInlineAttachments =
function (obj, msg) {
    var template = document.createElement("template");
    template.innerHTML = msg.mp[0].mp[1].content._content;
    var dataURIImageNodeList = template.content.querySelectorAll("img[src^='data:']");
    for (var i = 0; i < dataURIImageNodeList.length; i++) {
        var blob = AjxUtil.dataURItoBlob(dataURIImageNodeList[i].src);
        if (blob) {
            var callback = this._uploadOfflineInlineAttachmentsCallback.bind(this, dataURIImageNodeList[i], obj, msg, template);
            ZmComposeController.prototype._uploadImage(blob, callback);
        }
    }

};

ZmOffline.prototype._uploadOfflineInlineAttachmentsCallback =
function(img, obj, msg, template, uploadResponse) {
    //img.src =
    var dataURIImageNodeList = template.content.querySelectorAll("img[src^='data:']");
    if (dataURIImageNodeList.length === 0) {
        delete msg.isInlineAttachment;
        this._sendOfflineRequest([].concat(obj));
    }
};

ZmOffline.checkServerStatus =
function(doStop) {
    $.ajax({
        type: "HEAD",
        url: "/public/blank.html",
        statusCode: {
            0: function() {
                if (ZmOffline.isServerReachable === true) {
                    if (doStop) {
                        // Reset here - state must be correct for functions triggered by ZWCOffline
                        ZmOffline.isServerReachable = false;
                        $.event.trigger({
                            type: "ZWCOffline"
                        });
                    }
                    else {
                        return ZmOffline.checkServerStatus(true);
                    }
                }
                ZmOffline.isServerReachable = false;
            },
            200: function() {
                if (ZmOffline.isServerReachable === false) {
                    if (doStop) {
                        // Reset here - state must be correct for functions triggered by ZWCOnline
                        ZmOffline.isServerReachable = true;
                        $.event.trigger({
                            type: "ZWCOnline"
                        });
                    }
                    else {
                        return ZmOffline.checkServerStatus(true);
                    }
                }
                ZmOffline.isServerReachable = true;
            }
        }
    });
};

ZmOffline.isOnlineMode =
function() {
    return appCtxt.isWebClientOfflineSupported && !appCtxt.isWebClientOffline();
};

ZmOffline.prototype._fetchMsgAttachments =
function(messages) {
    var attachments = [];
    messages = ZmOffline.recreateMsg(messages);
    messages.forEach(function(msg) {
        var mailMsg = ZmMailMsg.createFromDom(msg, {'list':[]});
        var attachInfo = mailMsg.getAttachmentInfo();
        attachInfo.forEach(function(attachment) {
            attachments.push(attachment);
        });
    });
    this._saveAttachments(attachments);
};

ZmOffline.prototype._saveAttachments =
function(attachments) {
    if (!attachments || attachments.length === 0) {
        return;
    }
    var attachment = attachments.shift();
    var callback = this._isAttachmentSavedinIndexedDBCallback.bind(this, attachment, attachments);
    ZmOffline.isAttachmentSavedinIndexedDB(attachment, callback);
};

ZmOffline.isAttachmentSavedinIndexedDB =
function(attachment, callback, errorCallback) {
    var key = "id=" + attachment.mid + "&part=" + attachment.part;
    ZmOfflineDB.getItemCount(key, ZmOffline.ATTACHMENT, callback, errorCallback);
};

ZmOffline.prototype._isAttachmentSavedinIndexedDBCallback =
function(attachment, attachments, count) {
    var callback = this._saveAttachments.bind(this, attachments);
    if (count === 0) {
        var request = $.ajax({
            url: attachment.url,
            headers: {'X-Zimbra-Encoding':'x-base64'}
        });

        request.done(function(response) {
            var key = "id=" + attachment.mid + "&part=" + attachment.part;
            var item = {
                id : key,
                mid : attachment.mid,
                url : attachment.url,
                type : attachment.ct,
                name : attachment.label,
                size : attachment.sizeInBytes,
                content : response
            };
            ZmOfflineDB.setItem(item, ZmOffline.ATTACHMENT, callback, callback);
        }.bind(this));

        request.fail(callback);
    }
    else {
        callback();
    }
};

ZmOffline.modifyMsg =
function(msg) {

    var result = [].concat(msg).map(function(item) {
        if (item.f) {
            item.f = item.f.split("");
        }
        if (item.su) {
            item.su = item.su.split(" ");
        }
        if (item.fr) {
            item.fr = item.fr.split(" ");
        }
        if (item.tn) {
            item.tn = item.tn.split(",");
        }
        if (item.l) {
            item.l = item.l.toString();
        }
        if (item.e) {
            var to = [];
            var cc = [];
            item.e.forEach(function(element){
                if (element.a) {
                    if (element.t === "f") {
                        item.e.from = element.a.toLowerCase();
                    }
                    else if (element.t === "t") {
                        to.push(element.a.toLowerCase());
                    }
                    else if (element.t === "c") {
                        cc.push(element.a.toLowerCase());
                    }
                }
            });
            if (to.length) {
                item.e.to = to;
            }
            if (cc.length) {
                item.e.cc = cc;
            }
        }
        return item;
    });

    return result;
};

ZmOffline.recreateMsg =
function(msg) {

    var result = [].concat(msg).map(function(item) {
        if (Array.isArray(item.f)) {
            item.f = item.f.join();
        }
        if (Array.isArray(item.su)) {
            item.su = item.su.join(" ");
        }
        if (Array.isArray(item.fr)) {
            item.fr = item.fr.join(" ");
        }
        if (Array.isArray(item.tn)) {
            item.tn = item.tn.join();
        }
        if (item.e) {
            delete item.e.from;
            delete item.e.to;
            delete item.e.cc;
        }
        return item;
    });

    return result;
};

ZmOffline.modifyContact =
function(contact) {

    var result = [].concat(contact).map(function(item) {
        if (item.tn) {
            item.tn = item.tn.split(",");
        }
        if (item._attrs) {
            if (item._attrs.jobTitle) {
                item._attrs.jobTitle = item._attrs.jobTitle.split(" ");
            }
        }
        return item;
    });

    return result;
};

ZmOffline.recreateContact =
function(contact) {

    var result = [].concat(contact).map(function(item) {
        if (Array.isArray(item.tn)) {
            item.tn = item.tn.join();
        }
        if (item._attrs) {
            if (Array.isArray(item._attrs.jobTitle)) {
                item._attrs.jobTitle = item._attrs.jobTitle.join(" ");
            }
        }
        return item;
    });

    return result;
};

ZmOffline.prototype._cacheContacts =
function() {
    var jsonObj = {SearchRequest:{_jsns:"urn:zimbraMail"}};
    var request = jsonObj.SearchRequest;
    ZmMailMsg.addRequestHeaders(request);
    request.offset = 0;
    request.limit = 200;
    request.query = "in:contacts or in:\"Emailed Contacts\"";
    request.types = "contact";
    request.sortBy = "priorityDesc";
    var respCallback = this._handleResponseCacheContacts.bind(this);
    var params = {
        jsonObj : jsonObj,
        asyncMode : true,
        callback : respCallback
    };
    appCtxt.getRequestMgr().sendRequest(params);
};

ZmOffline.prototype._handleResponseCacheContacts =
function(result, callback) {
    var response = result && result.getResponse();
    if (!response) {
        return;
    }
    var contacts = response.SearchResponse && response.SearchResponse.cn;
    if (!contacts) {
        return;
    }
    console.log("contacts length "+contacts.length);
    ZmOfflineDB.setItem(contacts, ZmApp.CONTACTS, callback);
};

/*
ZmOffline.prototype._onAppActivate =
function(ev) {
    var item = ev && ev.item;
    var appName = item && item.getName();
    if (appName === ZmApp.MAIL) {
        if (appCtxt.isWebClientOffline()) {
            this._disableMailFeatures();
        }
    }
};
*/