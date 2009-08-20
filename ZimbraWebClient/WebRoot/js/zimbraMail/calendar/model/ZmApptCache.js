/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Web Client
 * Copyright (C) 2005, 2006, 2007, 2008, 2009 Zimbra, Inc.
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

ZmApptCache = function(calViewController) {
	this._calViewController = calViewController;
	this.clearCache();
};

ZmApptCache.prototype.toString =
function() {
	return "ZmApptCache";
};

ZmApptCache.prototype.clearCache =
function(folderId) {
	if(!folderId) {
		this._cachedApptSummaries = {};
		this._cachedApptVectors = {};
		this._cachedIds = {};
	}else {
		var cacheEntries = this._cachedApptVectors[folderId];
		if(cacheEntries) {
			for(var j in cacheEntries) {
				var cachedVec = cacheEntries[j];
				var len = cachedVec.size();
				for(var i=0; i<len; i++) {
					var appt = cachedVec.get(i);
					if(appt.folderId == folderId) {
						delete this._cachedIds[appt.id];
					}
				}
			}
			
		}
		delete this._cachedApptSummaries[folderId];
		delete this._cachedApptVectors[folderId];
		
	}
	
	this._cachedMergedApptVectors = {};
	var miniCalCache = this._calViewController.getMiniCalCache();
	miniCalCache.clearCache();
};

ZmApptCache._sortFolderId =
function (a,b) {
	return a-b;
};

ZmApptCache.prototype._getCachedMergedKey =
function(params) {
	var sortedFolderIds = [];
	sortedFolderIds = sortedFolderIds.concat(params.folderIds);
	sortedFolderIds.sort();

	// add query to cache key since user searches should not be cached
	var query = params.query && params.query.length > 0
		? (params.query + ":") : "";

	return (params.start + ":" + params.end + ":" + params.fanoutAllDay + ":" + query + sortedFolderIds.join(":"));
};

ZmApptCache.prototype._getCachedMergedVector =
function(cacheKey) {
	return this._cachedMergedApptVectors[cacheKey];
};

ZmApptCache.prototype._cacheMergedVector =
function(vector, cacheKey) {
	this._cachedMergedApptVectors[cacheKey] = vector.clone();
};

ZmApptCache.prototype._getCachedVector =
function(start, end, fanoutAllDay, folderId, query) {
	var folderCache = this._cachedApptVectors[folderId];
	if (folderCache == null)
		folderCache = this._cachedApptVectors[folderId] = {};

	var q = query ? (":" + query) : "";
	var cacheKey = start + ":" + end + ":" + fanoutAllDay + q;

	var vec = folderCache[cacheKey];
	if (vec == null) {
		// try to find it in the appt summaries results
		var apptList = this._getCachedApptSummaries(start, end, folderId, query);
		if (apptList != null) {
			vec = folderCache[cacheKey] = ZmApptList.toVector(apptList, start, end, fanoutAllDay);
		}
	}
	return vec;
};

ZmApptCache.prototype._cacheVector =
function(vector, start, end, fanoutAllDay, folderId, query) {
	var folderCache = this._cachedApptVectors[folderId];
	if (folderCache == null)
		folderCache = this._cachedApptVectors[folderId] = {};

	var q = query ? (":" + query) : "";
	var cacheKey = start + ":" + end + ":" + fanoutAllDay + q;
	folderCache[cacheKey] = vector;
};

ZmApptCache.prototype._cacheApptSummaries =
function(apptList, start, end, folderId, query) {
	var folderCache = this._cachedApptSummaries[folderId];
	if (folderCache == null)
		folderCache = this._cachedApptSummaries[folderId] = {};

	var q = query ? (":" + query) : "";
	var cacheKey = start + ":" + end + q;
	folderCache[cacheKey] = {start:start, end:end, list:apptList};
};

ZmApptCache.prototype._getCachedApptSummaries =
function(start, end, folderId, query) {
	var found = false;

	var folderCache = this._cachedApptSummaries[folderId];
	if (folderCache == null)
		folderCache = this._cachedApptSummaries[folderId] = {};

	var q = query ? (":" + query) : "";
	var cacheKey = start + ":" + end + q;

	// see if this particular range is cached
	var entry = this._cachedApptSummaries[cacheKey];
	if (entry != null) { return entry.list; }

	// look through all cache results. typically if we are asking for a week/day,
	// the month range will already be in the cache
	for (var key in folderCache) {
		entry = folderCache[key];
		if (start >= entry.start && end <= entry.end) {
			found = true;
			break;
		}
	}
	if (!found) { return null; }

	// hum. should this ever happen?
	if (entry.start == start && entry.end == end) {
		return entry.list;
	}

	// get subset, and cache it for future use (mainly if someone pages back and forth)
	var apptList = entry.list.getSubset(start, end);
	folderCache[cacheKey] = {start:start, end:end, list:apptList};
	return apptList;
};

ZmApptCache.prototype._updateCachedIds =
function(apptList) {
	var list = apptList.getVector();
	var size = list.size();
	for (var i=0; i < size; i++) {
		var ao = list.get(i);
		this._cachedIds[ao.id] = 1;
	}
};

/**
* Returns a vector of appt summaries for the specified time range across the
* specified folders.
* @param start 				[long]				start time in MS
* @param end				[long]				end time in MS
* @param fanoutAllDay		[Boolean]*
* @param folderIds			[Array]*			list of calendar folder Id's (null means use checked calendars in overview)
* @param callback			[AjxCallback]*		callback to call once search results are returned
* @param noBusyOverlay		[Boolean]*			don't show veil during search
*/
ZmApptCache.prototype.getApptSummaries =
function(params) {

	var apptVec = this.setSearchParams(params);

	if (apptVec != null && (apptVec instanceof AjxVector)) {
		return apptVec;
	}

	// this array will hold a list of appts as we collect them from the server
	this._rawAppts = [];

	if (params.callback) {
		this._search(params);
	} else {
		return this._search(params);
	}
};

ZmApptCache.prototype.setSearchParams =
function(params) {
	if (!(params.folderIds instanceof Array)) {
		params.folderIds = [params.folderIds];
	} else if (params.folderIds.length == 0) {
		var newVec = new AjxVector();
		if (params.callback) {
			params.callback.run(newVec);
		}
		return newVec;
	}

	params.mergeKey = this._getCachedMergedKey(params);
	var list = this._getCachedMergedVector(params.mergeKey);
	if (list != null) {
		if (params.callback) {
			params.callback.run(list.clone());
		}
		return list.clone();
	}

	params.needToFetch = [];
	params.resultList = [];

	for (var i=0; i < params.folderIds.length; i++) {
		var fid = params.folderIds[i];
		// check vector cache first
		list = this._getCachedVector(params.start, params.end, params.fanoutAllDay, fid);
		if (list != null) {
			params.resultList.push(list);
		} else {
			params.needToFetch.push(fid); // need to make soap call
		}
	}

	// if already cached, return from cache
	if (params.needToFetch.length == 0) {
		var newList = ZmApptList.mergeVectors(params.resultList);
		this._cacheMergedVector(newList, params.mergeKey);
		if (params.callback) {
			params.callback.run(newList);
		}
		return newList;
	}

	var folderIdMapper = {};
	var query = "";
	for (var i = 0; i < params.needToFetch.length; i++) {
		var fid = params.needToFetch[i];

		// map remote folder ids into local ones while processing search since
		// server wont do it for us (see bug 7083)
		var folder = appCtxt.getById(fid);
		var rid = folder ? folder.getRemoteId() : fid;
		folderIdMapper[rid] = fid;

		if (query.length) {
			query += " OR ";
		}
		query += "inid:" + (AjxUtil.isNumeric(fid) ? fid : ['"', fid, '"'].join(""));
		
	}
	params.queryHint = query;
	params.folderIdMapper = folderIdMapper;
	params.offset = 0;	
};

ZmApptCache.prototype._search =
function(params) {
	var jsonObj = {SearchRequest:{_jsns:"urn:zimbraMail"}};
	var request = jsonObj.SearchRequest;

	this._setSoapParams(request, params);

	if (params.callback) {
		var respCallback = new AjxCallback(this, this._getApptSummariesResponse, [params]);
		var errorCallback = new AjxCallback(this, this._getApptSummariesError);
		appCtxt.getAppController().sendRequest({jsonObj:jsonObj, asyncMode:true, callback:respCallback, errorCallback:errorCallback, noBusyOverlay:params.noBusyOverlay});
	} else {
		var response = appCtxt.getAppController().sendRequest({jsonObj: jsonObj});
		var result = new ZmCsfeResult(response, false);
		return this._getApptSummariesResponse(params, result);
	}
};

ZmApptCache.prototype.batchRequest =
function(searchParams, miniCalParams, reminderSearchParams) {
	var jsonObj = {BatchRequest:{_jsns:"urn:zimbra", onerror:"continue"}};
	var request = jsonObj.BatchRequest;

	if (searchParams) {
		if (!searchParams.folderIds) {
			searchParams.folderIds = this._calViewController.getCheckedCalendarFolderIds();
		}
		searchParams.query = this._calViewController._userQuery;
		var apptVec = this.setSearchParams(searchParams);

		//search data in cache
		if (apptVec != null && (apptVec instanceof AjxVector)) {
			this._cachedVec = apptVec;
		} else {
			var searchRequest = request.SearchRequest = {_jsns:"urn:zimbraMail"};
			this._setSoapParams(searchRequest, searchParams);
		}
	}

	if (reminderSearchParams) {
		if (!reminderSearchParams.folderIds) {
			reminderSearchParams.folderIds = this._calViewController.getCheckedCalendarFolderIds();
		}

		//reminder search params is only for grouping reminder related srch
		var apptVec = this.setSearchParams(reminderSearchParams);

		if (!apptVec) {
			var searchRequest ={_jsns:"urn:zimbraMail"};
			request.SearchRequest = request.SearchRequest ? [request.SearchRequest, searchRequest] : searchRequest;
			this._setSoapParams(searchRequest, reminderSearchParams);
		}
	}

	var miniCalCache = this._calViewController.getMiniCalCache();
	cacheData = miniCalCache.getCacheData(miniCalParams);

	// mini cal data in cache
	if (cacheData && cacheData.length > 0) {
		miniCalCache.highlightMiniCal(cacheData);
		if (miniCalParams.callback) {
			miniCalParams.callback.run(cacheData);
		}
	} else {
		var miniCalRequest = request.GetMiniCalRequest = {_jsns:"urn:zimbraMail"};
		miniCalCache._setSoapParams(miniCalRequest, miniCalParams);
	}

	// both mini cal and search data is in cache : no need to send request
	if (searchParams && !request.SearchRequest && !request.GetMiniCalRequest) {
		// setSoapParams would have invoked callback when this condition occurs
		return;
	}

	if ((searchParams && searchParams.callback) || miniCalParams.callback) {
		var params = {
			jsonObj: jsonObj,
			asyncMode: true,
			callback: (new AjxCallback(this, this.handleBatchResponse, [searchParams, miniCalParams, reminderSearchParams])),
			errorCallback: (new AjxCallback(this, this.handleBatchResponseError, [searchParams, miniCalParams, reminderSearchParams])),
			noBusyOverlay: true,
			accountName: appCtxt.isOffline && appCtxt.getMainAccount().name
		};
		appCtxt.getAppController().sendRequest(params);
	} else {
		var response = appCtxt.getAppController().sendRequest({jsonObj:jsonObj});
		var batchResp = (response && response.BatchResponse) ? response.BatchResponse : null;
		return this.processBatchResponse(batchResp, searchParams, miniCalParams);
	}
};

ZmApptCache.prototype.processBatchResponse =
function(batchResp, searchParams, miniCalParams, reminderSearchParams) {

	var miniCalCache = this._calViewController.getMiniCalCache();
	var miniCalResp = batchResp && batchResp.GetMiniCalResponse;
	var searchResp = batchResp && batchResp.SearchResponse;

	if (batchResp && batchResp.Fault) {
		if (this._processErrorCode(batchResp)) {
			return;
		}
	}

	if (miniCalResp) {
		var data = [];
		miniCalCache.processBatchResponse(miniCalResp, data);
		miniCalCache.highlightMiniCal(data);
		miniCalCache.updateCache(miniCalParams, data);

		if (miniCalParams.callback) {
			miniCalParams.callback.run(data);
		}
	}

	if (!searchResp || !searchParams) { return; }

	// currently we send only one search request in batch
	if (!(searchResp instanceof Array)) {
		searchResp = [searchResp];
	};

	if (searchResp.length > 1) {
		//process reminder list
		this.processSearchResponse(searchResp[1], reminderSearchParams);
	}

	var newList = this.processSearchResponse(searchResp[0], searchParams);
	return this.handleSearchCallback(searchParams, newList);
};

ZmApptCache.prototype.handleSearchCallback =
function(searchParams, newList) {
	if (searchParams.callback) {
		searchParams.callback.run(newList, null, searchParams.query);
	} else {
		return newList;
	}
};

ZmApptCache.prototype.handleBatchResponseError =
function(searchParams, miniCalParams, reminderSearchParams, response) {
	var resp = response && response._data && response._data.BatchResponse;
	this._calViewController.setSearchInProgress(false);
	this._processErrorCode(resp);
};

ZmApptCache.prototype._processErrorCode =
function(resp) {
	if (resp && resp.Fault && (resp.Fault.length > 0)) {
		var errors = [];
		var id;
		for (var i = 0; i < resp.Fault.length; i++) {
			var fault = resp.Fault[i];
			var error = (fault && fault.Detail) ? fault.Detail.Error : null;
			var code = error ? error.Code : null;
			var attrs = error ? error.a : null;
			if (code == ZmCsfeException.ACCT_NO_SUCH_ACCOUNT || code == ZmCsfeException.MAIL_NO_SUCH_MOUNTPOINT) {
				for(var j in attrs) {
					var attr = attrs[j];
					if(attr && (attr.t == "IID") && (attr.n == "itemId")) {
						id = attr._content;
					}
				}
				
			} else {
				DBG.println("Unknown error occurred: "+code);
				errors[fault.requestId] = fault;
			}
		}

		if (id && appCtxt.getById(id)) {
			var folder = appCtxt.getById(id);
            folder.noSuchFolder = true;
			this.handleDeleteMountpoint(folder);
			return true;
		}
		return false;
	}

	return false;
};

ZmApptCache.prototype.handleDeleteMountpoint =
function(organizer) {
	// Change its appearance in the tree.
	var tc = appCtxt.getOverviewController().getTreeController(ZmOrganizer.CALENDAR);
	var treeView = tc.getTreeView(appCtxt.getCurrentApp().getOverviewId());
	var node = treeView && treeView.getTreeItemById(organizer.id);
	if (organizer && node) {
		node.setText(organizer.getName(true));
	}
	this.runErrorRecovery();
};

ZmApptCache.prototype.runErrorRecovery =
function() {
	if (this._calViewController) {
		this._calViewController._updateCheckedCalendars();
		if (this._calViewController.onErrorRecovery) {
			this._calViewController.onErrorRecovery.run();
		}
	}
};

ZmApptCache.prototype.handleBatchResponse =
function(searchParams, miniCalParams, reminderSearchParams, response) {
	var batchResp = response && response._data && response._data.BatchResponse;
	return this.processBatchResponse(batchResp, searchParams, miniCalParams, reminderSearchParams);
};

ZmApptCache.prototype._setSoapParams =
function(request, params) {
	request.sortBy = "none";
	request.limit = "500";
	//AjxEnv.DEFAULT_LOCALE is set to the browser's locale setting in the case when
	//the user's (or their COS) locale is not set.
	request.locale = { _content: AjxEnv.DEFAULT_LOCALE };
	request.calExpandInstStart = params.start;
	request.calExpandInstEnd = params.end;
	request.types = ZmSearch.TYPE[ZmItem.APPT];
	request.offset = params.offset;

	var query = params.query;
	if (params.queryHint) {
		query = (query != null)
			? (query + " (" + params.queryHint + ")")
			: params.queryHint;
	}
	request.query = {_content:query};
};

ZmApptCache.prototype._getApptSummariesResponse =
function(params, result) {
	// TODO: mark both as needing refresh?
	if (!result) { return; }

	var callback = params.callback;
	var resp;
	try {
		resp = result.getResponse();
	} catch (ex) {
		if (callback) {
			callback.run(result);
		}
		return;
	}

	var searchResp = resp.SearchResponse;
	var newList = this.processSearchResponse(searchResp, params);
	if(newList == null) { return; }

	if (callback) {
		callback.run(newList, params.query, result);
	} else {
		return newList;
	}
};

ZmApptCache.prototype._getApptSummariesError =
function(ex) {
	if (ex.code == ZmCsfeException.MAIL_QUERY_PARSE_ERROR) {
		var d = appCtxt.getMsgDialog();
		d.setMessage(ZmMsg.errorCalendarParse);
		d.popup();
		return true;
	}
};

ZmApptCache.prototype.processSearchResponse = 
function(searchResp, params) {

	if (!searchResp) {
		if (this._cachedVec) {
			var resultList = this._cachedVec.clone();
			this._cachedVec = null;
			return resultList;
		}
		return null;
	}

	if (searchResp && searchResp.appt && searchResp.appt.length) {
		this._rawAppts = this._rawAppts != null 
			? this._rawAppts.concat(searchResp.appt)
			: searchResp.appt;

		// if "more" flag set, keep requesting more appts
		if (searchResp.more) {
			var lastAppt = searchResp.appt[searchResp.appt.length-1];
			if (lastAppt) {
				params.offset += 500;
				this._search(params);
				return;
			}
		}
	}

	if (this._rawAppts && this._rawAppts.length) {
		var fanoutAllDay = params.fanoutAllDay;
		var folderIds = params.needToFetch;
		var start = params.start;
		var end = params.end;
		var query = params.query;

		// create a list of appts for each folder returned
		var folder2List = {};
		for (var j = 0; j < this._rawAppts.length; j++) {
			var fid = params.folderIdMapper && params.folderIdMapper[this._rawAppts[j].l];
			if (!folder2List[fid]) {
				folder2List[fid] = [];
			}
			folder2List[fid].push(this._rawAppts[j]);
		}

		if (folderIds && folderIds.length) {
			for (var i = 0; i < folderIds.length; i++) {
				var folderId = folderIds[i];
				var apptList = new ZmApptList();
				apptList.loadFromSummaryJs(folder2List[folderId]);

				// cache it
				this._updateCachedIds(apptList);
				this._cacheApptSummaries(apptList, start, end, folderId, query);

				// convert to sorted vector
				var list = ZmApptList.toVector(apptList, start, end, fanoutAllDay, params.includeReminders);
				this._cacheVector(list, start, end, fanoutAllDay, folderId, query); // id in response tied back to folder id

				params.resultList.push(list);
			}
		}
	}

	// merge all the data and return
	var newList = ZmApptList.mergeVectors(params.resultList);
	this._cacheMergedVector(newList, params.mergeKey);

	this._rawAppts = null;
	return newList;
};

// return true if the cache contains the specified id(s)
// id can be an array or a single id.
ZmApptCache.prototype.containsAnyId =
function(ids) {
	if (!ids) { return false; }
	if (ids instanceof Array) {
		for (var i=0; i < ids.length; i++) {
			if (this._cachedIds[ids[i]])
				return true;
		}
	} else {
		if (this._cachedIds[ids])
			return true;
	}
	return false;
};

// similar to  containsAnyId, though deals with an object
// (or array of objects) that have the id property
ZmApptCache.prototype.containsAnyItem =
function(items) {
	if (!items) { return false; }
	if (items instanceof Array) {
		for (var i=0; i < items.length; i++) {
			if (items[i].id && this._cachedIds[items[i].id])
				return true;
		}
	} else {
		if (items.id && this._cachedIds[items.id])
			return true;
	}
	return false;
};
