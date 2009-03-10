<%@ tag body-content="empty" %>
<%@ attribute name="context" rtexprvalue="true" required="true" type="com.zimbra.cs.taglib.tag.SearchContext" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="com.zimbra.i18n" %>
<%@ taglib prefix="mo" uri="com.zimbra.mobileclient" %>
<%@ taglib prefix="zm" uri="com.zimbra.zm" %>
<%@ taglib prefix="app" uri="com.zimbra.htmlclient" %>
<mo:handleError>
    <zm:getMailbox var="mailbox"/>
    <mo:searchTitle var="title" context="${context}"/>
</mo:handleError>
<c:set var="context_url" value="${requestScope.baseURL!=null?requestScope.baseURL:'zmain'}"/>
<zm:currentResultUrl var="actionUrl" value="${context_url}" context="${context}" refresh="${true}"/>
<c:set var="title" value="${zm:truncate(context.shortBackTo,20,true)}" scope="request"/>
<form id="zForm" action="${fn:escapeXml(actionUrl)}" method="post">
    <input type="hidden" name="crumb" value="${fn:escapeXml(mailbox.accountInfo.crumb)}"/>
    <input type="hidden" name="doBriefcaseAction" value="1"/>
    <input name="moreActions" type="hidden" value="<fmt:message key="actionGo"/>"/>
   <mo:briefcaseToolbar context="${context}" urlTarget="${context_url}" isTop="true" mailbox="${mailbox}"/>
   <c:forEach items="${context.searchResult.hits}" var="hit" varStatus="status">
        <c:set var="bchit" value="${hit.wikiHit}"/>
        <div class="list-row row" id="cn${bchit.id}">
            <c:set value=",${hit.id}," var="stringToCheck"/>
            <c:set var="ctype" value="${fn:split(bchit.document.contentType,';')}" />
            <c:choose>
    <c:when test="${ctype[0] eq 'application'}">
        <c:set var="class" value="ImgExeDoc" scope="request"/>
        
    </c:when>
    <c:when test="${ctype[0] eq 'application/pdf'}">
        <c:set var="mimeImg" value="ImgPDFDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/postscript'}">
        <c:set var="mimeImg" value="ImgGenericDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/exe'}">
        <c:set var="mimeImg" value="ImgExeDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/x-msdownload'}">
        <c:set var="mimeImg" value="ImgExeDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/vnd.ms-excel'}">
        <c:set var="mimeImg" value="ImgMSExcelDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/vnd.ms-powerpoint'}">
        <c:set var="mimeImg" value="ImgMSPowerpointDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/vnd.ms-project'}">
        <c:set var="mimeImg" value="ImgMSProjectDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/vnd.visio'}">
        <c:set var="mimeImg" value="ImgMSVisioDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/msword'}">
        <c:set var="mimeImg" value="ImgMSWordDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/octet-stream'}">
        <c:set var="mimeImg" value="ImgUnknownDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'application/zip'}">
        <c:set var="mimeImg" value="ImgZipDoc" scope="request"/>

    </c:when>
    <c:when test="${zm:contains(ctype[0],'audio')}">
        <c:set var="mimeImg" value="ImgAudioDoc" scope="request"/>

    </c:when>
    <c:when test="${zm:contains(ctype[0],'video')}">
        <c:set var="mimeImg" value="ImgVideoDoc" scope="request"/>

    </c:when>
    <c:when test="${zm:contains(ctype[0],'image')}">
        <c:set var="mimeImg" value="ImgImageDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'message/rfc822'}">
        <c:set var="mimeImg" value="ImgMessageDoc" scope="request"/>

    </c:when>
    <c:when test="${zm:contains(ctype[0],'text')}">
        <c:set var="mimeImg" value="ImgGenericDoc" scope="request"/>

    </c:when>
    <c:when test="${ctype[0] eq 'text/html'}">
        <c:set var="mimeImg" value="ImgHtmlDoc" scope="request"/>

    </c:when>
    <c:otherwise>
        <c:set var="mimeImg" value="ImgUnknownDoc" scope="request"/>

    </c:otherwise>
</c:choose>
            <span class="cell f">
                    <input class="chk" type="checkbox" ${requestScope.select ne 'none' && (fn:contains(requestScope._selectedIds,stringToCheck) || requestScope.select eq 'all') ? 'checked="checked"' : ''}
                           name="id" value="${bchit.id}"/>
            <span class="SmlDocIcnHldr ${mimeImg}">&nbsp;</span>
            </span>
            <span class="cell m" onclick='return zClickLink("a${bchit.id}")'>
                <c:set var="briefUrl" value="/service/home/~/?id=${bchit.id}&auth=co"/>
                <a id="a${bchit.id}" href="${briefUrl}" target="_blank">
                <div>
                    <strong><c:out escapeXml="true" value="${zm:truncate(bchit.document.name,100,true)}"/></strong>
                </div>
                </a>
                <c:set var="cname" value="${fn:split(bchit.document.creator,'@')}" />
               <div class="Email from-span">
                    <a href="${briefUrl}" target="_blank">
                    ${fn:escapeXml(bchit.document.creator)}
                    </a>
                </div>
                <a href="${briefUrl}" target="_blank">
                <div class="frag-span small-gray-text">
                    <c:set var="cname" value="${fn:split(bchit.document.editor,'@')}" />
                    <fmt:message key="modified"/>&nbsp;<fmt:message key="by"/>&nbsp;${cname[0]}&nbsp;<fmt:message key="on"/>&nbsp;${fn:escapeXml(zm:displayDate(pageContext, bchit.modifiedDate))}&nbsp;
                    <%--<fmt:message key="modified"/>&nbsp;<fmt:message key="by"/>&nbsp;${fn:split(bchit.document.editor,'@')[0]}&nbsp;<fmt:message key="on"/>&nbsp;${fn:escapeXml(zm:displayDate(pageContext, bchit.modifiedDate))}--%>
                </div>
                </a>
            </span>
            <span class="cell l" onclick='return zClickLink("a${bchit.id}")'>
                <fmt:formatDate timeZone="${mailbox.prefs.timeZone}" var="on_dt" pattern="yyyyMMdd" value="${bchit.createdDate}"/>
                <a <c:if test="${mailbox.features.calendar}">href='${context_url}?st=cal&amp;view=month&amp;date=${on_dt}'</c:if>>
                    <fmt:parseDate var="mdate" value="${on_dt}" pattern="yyyyMMdd" timeZone="${mailbox.prefs.timeZone}"/>
                    ${fn:escapeXml(zm:displayMsgDate(pageContext, bchit.createdDate))}
                </a><br/>
                <span class='small-gray-text'>(${fn:escapeXml(zm:displaySize(pageContext, bchit.document.size))})</span>
                <c:if test="${!empty bchit.document.tagIds}">
                <div>
                <mo:miniTagImage
                                ids="${bchit.document.tagIds}"/>
                </div>
                </c:if>
            </span>
        </div>
    </c:forEach>
    <%--c:import url="/m/zmview">
        <c:param name="sfi" value="${context.sfi}"/>
        <c:param name="st" value="briefcase"/>
        <c:param name="top_stb" value="0"/>
        <c:param name="btm_stb" value="0"/>
        <c:param name="top_tb" value="0"/>
        <c:param name="btm_tb" value="0"/>
        <c:param name="supressNoRes" value="1"/>
    </c:import--%>
   <c:if test="${empty context || empty context.searchResult or context.searchResult.size eq 0}">
        <div class='table'>
                <div class="table-row">
                    <div class="table-cell zo_noresults">
                        <fmt:message key="noResultsFound"/>
                     </div>
                </div>
            </div>
    </c:if>
    <mo:briefcaseToolbar context="${context}" urlTarget="${context_url}" isTop="false" mailbox="${mailbox}"/>
</form>
