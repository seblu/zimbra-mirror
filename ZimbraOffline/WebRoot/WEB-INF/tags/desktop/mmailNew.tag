<%@ tag body-content="empty" %>
<%@ attribute name="uri" required="true" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="zdf" uri="com.zimbra.cs.offline.jsp" %>
<%@ taglib prefix="fmt" uri="com.zimbra.i18n" %>

<fmt:setBundle basename="/desktop/ZdMsg" scope="request"/>

<script type="text/javascript">
<!--
function InitScreen() {
    if (!zd.isChecked("smtpAuth")) {
        zd.hide("smtpAuthSettingsRow");
    }
    SetPort();
    SetSmtpPort();
}

function SetPort() {
    if (zd.isDisabled("port")) {
        if (zd.isChecked("ssl")) {
            zd.set("port", "993");
        } else {
            zd.set("port", "143");
        }
    }
}

function SetSmtpPort() {
    if (zd.isDisabled("smtpPort")) {
        if (zd.isChecked("smtpSsl")) {
            zd.set("smtpPort", "465");
        } else {
            zd.set("smtpPort", "25");
        }
    }
}

function OnPickType() {
    window.location = '/zimbra/desktop/new.jsp';
}

function OnCancel() {
    window.location = '/zimbra/desktop/console.jsp';
}

function OnSubmit() {
    beforeSubmit();
    zd.enable("port");
    zd.enable("smtpPort");
    mmailNew.submit();
}

function beforeSubmit() {
    disableButtons();
    zd.set("whattodo", "<span class='ZOfflineNotice'><fmt:message key='Processing'/></span>");
}

function disableButtons() {
    zd.disable("typeButton");
    zd.disable("cancelButton");
    zd.disable("saveButton");
}
//-->
</script>


<div id="newService" class="ZWizardPage">
	<div class="ZWizardPageTitle">
		<fmt:message key='MSESetupTitle'/>
	</div>
<span class="padding">
<c:choose>
    <c:when test="${not empty bean.error}" >
        <p class='ZOfflineError'>${bean.error}</p>
    </c:when>
    <c:when test="${not bean.allValid}" >
        <p class='ZOfflineError'><fmt:message key='PlsCorrectInput'/></p>
    </c:when>
	<c:otherwise>
		<p id='instructions'>* <fmt:message key='RequiredField'/><br>
		                        <fmt:message key='IfYouNotSure'/>
		</p>
	</c:otherwise>
</c:choose>

	<form name="mmailNew" action="${uri}" method="POST">
		<input type="hidden" name="verb" value="add">

		<table class="ZWizardForm" border=0>
			<tr>
				<td class="${zdf:isValid(bean, 'accountName') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='Description'/>:</td>
				<td>
					<input style='width:200px' class="ZField" type="text" id="accountName" name="accountName" value="${bean.accountName}">
					<span id='service_hint' class='ZHint'><fmt:message key='DescHint2'/></span>
				</td>
			</tr>

			<tr>
				<td class="ZFieldLabel"><fmt:message key='FullName'/>:</td>
				<td><input style='width:200px' class="ZField" type="text" id="fromDisplay" name="fromDisplay" value="${bean.fromDisplay}"></td>
			</tr>

			<tr id='emailRow'>
				<td class="${zdf:isValid(bean, 'email') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='EmailAddr'/>:</td>
				<td><input style='width:200px' class="ZField" type="text" id="email" name="email" value="${bean.email}" onkeypress='zd.syncIdsOnTimer(this, "username", "smtpUsername")'></td>
			</tr>

			<tr id='receivingMailRow'><td colspan=2><div class='ZOfflineHeader'><fmt:message key='ReceivingMail'/></div></td></tr>

			<tr id='usernameRow'>
				<td class="${zdf:isValid(bean, 'username') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='UserName'/>:</td>
				<td><input style='width:200px' class="ZField" type="text" id="username" name="username" value="${bean.username}" onkeypress='zd.markElementAsManuallyChanged(this);zd.syncIdsOnTimer(this, "smtpUsername")'></td>
			</tr>

			<tr id='passwordRow'>
				<td class="${zdf:isValid(bean, 'password') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='Password'/>:</td>
				<td><input style='width:100px' class="ZField" type="password" id="password" name="password" value="${bean.password}" onkeypress='zd.syncIdsOnTimer(this, "smtpPassword")'></td>
			</tr>

			<tr id='mailServerRow'>
				<td class="${zdf:isValid(bean, 'host') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='InMailServer'/>:</td>
				<td>
					<table cellspacing=0 cellpadding=0>
						<tr>
							<td><input style='width:200px' class="ZField" type="text" id="host" name="host" value="${bean.host}"></td>
							<td>&nbsp;&nbsp;&nbsp;</td>
							<td class="${zdf:isValid(bean, 'port') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='Port'/>:</td>
							<td width=100%><input style='width:50px' class="ZField" disabled='true' type="text" id="port" name="port" value="${bean.port}">&nbsp;&nbsp;<a href="#" onclick="zd.enable('port');this.style.display='none'"><fmt:message key='Edit'/></a></td>
						</tr>
					</table>
				</td>
			</tr>

			<tr id='mailSecureRow'>
				<td class='ZCheckboxCell'><input type="checkbox" id="ssl" name="ssl" ${bean.ssl ? 'checked' : ''} onclick="SetPort()"></td>
				<td class="ZCheckboxLabel"><fmt:message key='UseSSL'/></td>
			</tr>

			<tr id='sendingMailRow'><td colspan=2><div class='ZOfflineHeader'><fmt:message key='SendingMail'/></div></td></tr>

			<tr id='smtpServerRow'>
				<td class="${zdf:isValid(bean, 'smtpHost') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='OutMailServer'/>:</td>
				<td>
					<table cellspacing=0 cellpadding=0>
						<tr>
							<td><input style='width:200px' class="ZField" type="text" id=smtpHost name="smtpHost" value="${bean.smtpHost}"></td>
							<td>&nbsp;&nbsp;&nbsp;</td>
							<td class="${zdf:isValid(bean, 'smtpPort') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='Port'/>:</td>
							<td width=100%><input style='width:50px' class="ZField" disabled='true' type="text" id="smtpPort" name="smtpPort" value="${bean.smtpPort}">&nbsp;&nbsp;<a href="#" onclick="zd.enable('smtpPort');this.style.display='none'"><fmt:message key='Edit'/></a></td>
						</tr>
					</table>
				</td>
			</tr>

			<tr id='smtpSecureRow'>
				<td class='ZCheckboxCell'><input type="checkbox" id="smtpSsl" name="smtpSsl" ${bean.smtpSsl ? 'checked' : ''} onclick="SetSmtpPort()"></td>
				<td class="ZCheckboxLabel"><fmt:message key='UseSSL'/></td>
			</tr>

			<tr id='smtpAuthRow'>
				<td class='ZCheckboxCell'><input type="checkbox" id="smtpAuth" name="smtpAuth" ${bean.smtpAuth ? 'checked' : ''} onclick='zd.toggle("smtpAuthSettingsRow", this.checked)'></td>
				<td class="ZCheckboxLabel"><fmt:message key='UsrPassForSend'/></td>
			</tr>

			<tr id='smtpAuthSettingsRow'>
				<td></td>
				<td>
					<table>
						<tr>
							<td class="${zdf:isValid(bean, 'smtpUsername') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='UserName'/>:</td>
							<td><input style='width:200px' class="ZField" type="text" id="smtpUsername" name="smtpUsername" value="${bean.smtpUsername}" onkeypress='zd.markElementAsManuallyChanged(this)'></td>
						</tr>
						<tr>
							<td class="${zdf:isValid(bean, 'smtpPassword') ? 'ZFieldLabel' : 'ZFieldError'}">*<fmt:message key='Password'/>:</td>
							<td><input style='width:100px' class="ZField" type="password" id="smtpPassword" name="smtpPassword" value="${bean.smtpPassword}" onkeypress='zd.markElementAsManuallyChanged(this)'></td>
						</tr>
					</table>
				</td>
			</tr>

			<tr id='replyToRow'>
				<td class="ZFieldLabel"><fmt:message key='ReplyTo'/>:</td>
				<td>
					<table>
						<tr>
							<td><fmt:message key='Name'/>:</td>
							<td><fmt:message key='EmailAddress'/>:</td>
						</tr>
						<tr>
							<td><input style='width:200px' class="ZField" type="text" id="replyToDisplay" name="replyToDisplay" value="${bean.replyToDisplay}" onkeypress='zd.markElementAsManuallyChanged(this)'></td>
							<td><input style='width:200px' class="ZField" type="text" id="replyTo" name="replyTo" value="${bean.replyTo}" onkeypress='zd.markElementAsManuallyChanged(this)'></td>
						</tr>
					</table>
				</td>
			</tr>

			<tr><td colspan=2><div class='ZOfflineHeader'><fmt:message key='DownloadingMail'/></div></td></tr>

			<tr>
	            <td class="ZFieldLabel"><fmt:message key='SyncFrequency'/>:</td>
	            <td>
	                <select class="ZSelect" id="syncFreqSecs" name="syncFreqSecs">
	                    <option value="-1" ${bean.syncFreqSecs == -1 ? 'selected' : ''}><fmt:message key='SyncManually'/></option>
	                    <option value="60" ${bean.syncFreqSecs == 60 ? 'selected' : ''}><fmt:message key='SyncEveryMin'/></option>
	                    <option value="300" ${bean.syncFreqSecs == 300 ? 'selected' : ''}><fmt:message key='SyncEvery5'/></option>
	                    <option value="900" ${bean.syncFreqSecs == 900 ? 'selected' : ''}><fmt:message key='SyncEvery15'/></option>
	                    <option value="1800" ${bean.syncFreqSecs == 1800 ? 'selected' : ''}><fmt:message key='SyncEvery30'/></option>
	                    <option value="3600" ${bean.syncFreqSecs == 3600 ? 'selected' : ''}><fmt:message key='SyncEvery1Hr'/></option>
	                    <option value="14400" ${bean.syncFreqSecs == 14400 ? 'selected' : ''}><fmt:message key='SyncEvery4Hr'/></option>
	                    <option value="43200" ${bean.syncFreqSecs == 43200 ? 'selected' : ''}><fmt:message key='SyncEvery12Hr'/></option>
	                </select>
	            </td>
			</tr>

            <tr>
                <td style='text-align:right'><input type="checkbox" id="syncAllServerFolders" name="syncAllServerFolders" ${bean.syncAllServerFolders ? 'checked' : ''}></td>
                <td class="ZCheckboxLabel"><fmt:message key='SyncAllFolders'/></td>
            </tr>

		</table>
	</form>

	<p><span id="whattodo"><fmt:message key='PressToVerify'><fmt:param><span class="ZWizardButtonRef"><fmt:message key='SaveSettings'/></span></fmt:param></fmt:message></span></p>
</span>
	<table class="ZWizardButtonBar" width="100%">
		<tr>
			<td class="ZWizardButton"><button id='typeButton' class='DwtButton' onclick="OnPickType()"><fmt:message key='UseDiffType'/></button></td>
			<td class="ZWizardButtonSpacer"><div></div></td>
			<td class="ZWizardButton" width="1%"><button id='cancelButton' class='DwtButton' onclick="OnCancel()"><fmt:message key='Cancel'/></button></td>
			<td class="ZWizardButton" width="1%"><button id='saveButton' class='DwtButton-focused' onclick="OnSubmit()"><fmt:message key='SaveSettings'/></button></td>
		</tr>
	</table>
</div>
<br>
