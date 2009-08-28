window.addEventListener("load", ZMTB_PrefInit, false);
function ZMTB_PrefInit()
{
	var pref = window._zmtbPrefs = new ZMTB_Prefs();
}
var ZMTB_Prefs = function()
{
	var This=this;
	document.getElementById("zmtb-prefconnect").addEventListener("command", function(){This.connect()}, false);
	window.addEventListener("unload", function(){This.connect()}, false);
	document.getElementById("zmtb-clearrecent").addEventListener("command", function(){This.resetRecent()}, false);
	//If updates are received before the user changes account settings, we don't want to display a connected message
	this._startConnect = false;
	this._statusLabel = document.getElementById("zmtb-connectionStatus");
	this._prefManager = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
	//Passwords are stored separately in the loginManager
	this._passField = document.getElementById("zmtb-passwordField");
	//Connection timeout
	this._timeout = null;
	this._menuList = document.getElementById("ZMTB-Default-Folder");
	//Register for updates
	var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"].getService(Components.interfaces.nsIWindowMediator);
	var enumerator = wm.getEnumerator("navigator:browser");
	while(enumerator.hasMoreElements())
	{
		var win = enumerator.getNext();
		if(win.com_zimbra_tb)
		{
			this._zmtb = win.com_zimbra_tb;
			this._zmtb.getRequestManager().addUpdateListener(this);
			this._zmtb.getFolderManager().registerListener(this);
			window.addEventListener("unload", function(){This._zmtb.getFolderManager().removeListener(This)}, false);
			this._folderMan = this._zmtb.getFolderManager();
			this._localStrings = this._zmtb.getLocalStrings();
			this.updateFolders();
		}
	}
	
	var password;
  	var myLoginManager = Components.classes["@mozilla.org/login-manager;1"].getService(Components.interfaces.nsILoginManager);
	var pm = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
	var logins = myLoginManager.findLogins({}, 'chrome://zimbratb', null, 'Zimbra Login');
	for (var i = 0; i < logins.length; i++)
	{
		if (logins[i].username == pm.getCharPref("extensions.zmtb.username"))
		{
		   password = logins[i].password;
		   break;
		}
	}
	if(!password)
		this._passField.value = "";
	else	
		this._passField.value = password;
}

ZMTB_Prefs.prototype.updateFolders = function()
{
	var folders = this._folderMan.getFolders("default");
	this.resetFolderList();
	this._populateList(folders);
}

ZMTB_Prefs.prototype.resetFolderList = function()
{
	this._menuList.removeAllItems();
}

ZMTB_Prefs.prototype._populateList = function(folders)
{
	///Using new folder manager
	var list = this._menuList;
	for (var i=0; i < folders.length; i++)
		this._addToMenu(folders[i], "ZimTB-"+folders[i].name+"-Folder");
	list.selectedIndex = 0;
	var pm = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
	var df = pm.getCharPref("extensions.zmtb.defaultWatch");
	for (var i=0; i < this._menuList.itemCount; i++)
		if(this._menuList.getItemAtIndex(i).value == df)
			this._menuList.selectedIndex = i;
	if(!this._menuList.selectedIndex)
		this._menuList.selectedIndex = 0;
	if(this._menuList.selectedItem)
		this._menuList.className = this._menuList.selectedItem.className;
	var This = this;
	this._menuList.addEventListener("command", function(){This._menuList.className = This._menuList.selectedItem.className}, false)
}

ZMTB_Prefs.prototype._addToMenu = function(folder, class)
{
	var mL = (this._folderMan.getDepth(folder.id)*20).toString()+"px";
	var m = this._menuList.appendItem(folder.name, folder.id);
	m.style.marginLeft = mL;
	if(class)
		m.className = class + " menuitem-iconic ZimTB-Mail-Folder";
	if(folder.rss)
		m.className = "ZimTB-RSS-Folder menuitem-iconic";
	else if(folder.query)
		m.className = "ZMTB-Search-Folder menuitem-iconic";
}

ZMTB_Prefs.prototype.receiveUpdate = function(responseObj)
{
	// if(responseObj.code)
	// {
	// 	if(this._statusLabel.value == "Connected to server!")
	// 		return;
	// 	switch(responseObj.code)
	// 	{
	// 		case "NETWORK_ERROR":
	// 			this._statusLabel.value = "Could not connect to server."
	// 			break;
	// 		case "account.CHANGE_PASSWORD":
	// 			this._statusLabel.value = "Your password has expired. You must change it."
	// 			break;
	// 		case "account.AUTH_FAILED":
	// 			this._statusLabel.value = "You have entered an incorrect username or password."
	// 			break;
	// 	}
	// }
	clearTimeout(this._timeout);
	if(responseObj.Body.Fault && responseObj.Body.Fault.Detail.Error.Code)
	{
		if(this._statusLabel.value == this._localStrings.getString("preferences_status_connected"))
			return;
		switch(responseObj.Body.Fault.Detail.Error.Code)
		{
			case "NETWORK_ERROR":
				this.setStatus(this._localStrings.getString("preferences_status_noconnect"));
				break;
			case "account.CHANGE_PASSWORD":
				this.setStatus(this._localStrings.getString("preferences_status_passexp"));
				break;
			case "account.AUTH_FAILED":
				this.setStatus(this._localStrings.getString("preferences_status_noauth"));
				break;
		}
	}
	else
	{
		clearTimeout(this._timeout);
		if(this._startConnect)
			this.setStatus(this._localStrings.getString("preferences_status_connected"))
	}
}

ZMTB_Prefs._checkURL = function(URL)
{
	URL = URL.replace(/^\s*(\S*(\s+\S+)*)\s*$/, "$1");
	if(URL.match(/^(?:(?:ht|f)tp(?:s?)\:\/\/|~\/|\/)?(?:\w+:\w+@)?(?:(?:[-\w]+\.)+(?:com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum|travel|[a-z]{2}))(?::[\d]{1,5})?(?:(?:(?:\/(?:[-\w~!$+|.,=]|%[a-f\d]{2})+)+|\/)+|\?|#)?(?:(?:\?(?:[-\w~!$+|.,*:]|%[a-f\d{2}])+=(?:[-\w~!$+|.,*:=]|%[a-f\d]{2})*)(?:&(?:[-\w~!$+|.,*:]|%[a-f\d{2}])+=(?:[-\w~!$+|.,*:=]|%[a-f\d]{2})*)*)*(?:#(?:[-\w~!$+|.,*:=]|%[a-f\d]{2})*)?$/gi))
	{
		if(URL.charAt(URL.length-1) != "/")
			return URL.concat("/");
		else
			return URL;
	}
	else
		return null;
}

ZMTB_Prefs.prototype.reset = function(){}

ZMTB_Prefs.prototype.connect = function()
{
	this._startConnect = true;
	this._zmtb.reset();
	this._zmtb.disable();
	var passwordManager = Components.classes["@mozilla.org/login-manager;1"]
	                                .getService(Components.interfaces.nsILoginManager);
	var nsLoginInfo = new Components.Constructor("@mozilla.org/login-manager/loginInfo;1", Components.interfaces.nsILoginInfo, "init");
	var wm = Components.classes["@mozilla.org/appshell/window-mediator;1"].getService(Components.interfaces.nsIWindowMediator);
	var pm = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefBranch);
	
	if(pm.getCharPref("extensions.zmtb.hostname")=="")
	{
		this.setStatus(this._localStrings.getString("preferences_status_invalidurl"));
		return;
	}
	else if(pm.getCharPref("extensions.zmtb.username")=="")
	{
		this.setStatus(this._localStrings.getString("preferences_status_needusername"));
		return;
	}
	else if(this._passField.value=="")
	{
		this.setStatus(this._localStrings.getString("preferences_status_needpassword"));
		return;
	}

	var loginInfo = new nsLoginInfo('chrome://zimbratb', null, 'Zimbra Login', pm.getCharPref("extensions.zmtb.username"), this._passField.value, "", "");
	var logins = passwordManager.findLogins({}, 'chrome://zimbratb', null, 'Zimbra Login');
	for (var i = 0; i < logins.length; i++)
	{
		if (logins[i].username == pm.getCharPref("extensions.zmtb.username"))
		{
			passwordManager.removeLogin(logins[i]);
		    break;
		}
	}
	passwordManager.addLogin(loginInfo);
	if(ZMTB_Prefs._checkURL(pm.getCharPref("extensions.zmtb.hostname")))
		pm.setCharPref("extensions.zmtb.hostname", ZMTB_Prefs._checkURL(pm.getCharPref("extensions.zmtb.hostname")))
	else
	{
		this.setStatus(this._localStrings.getString("preferences_status_invalidurl"));
		return;
	}
	var enumerator = wm.getEnumerator("navigator:browser");
	while(enumerator.hasMoreElements())
	{
	  var win = enumerator.getNext();
		if(win.com_zimbra_tb && win.com_zimbra_tb.getRequestManager())
				win.com_zimbra_tb.getRequestManager().newServer(pm.getCharPref("extensions.zmtb.hostname"), pm.getCharPref("extensions.zmtb.username"), this._passField);
	}
	this._statusLabel.value = "";
	this._timeout = setTimeout(this.setStatus, 5000, this, this._localStrings.getString("preferences_status_noconnect"));
}
ZMTB_Prefs.prototype.setStatus = function(message, This)
{
	if(This)
		This._statusLabel.value = message;
	else
		this._statusLabel.value = message;		
}
ZMTB_Prefs.prototype.resetRecent = function()
{
	this._prefManager.setCharPref("extensions.zmtb.recentSearch", "");
}