<%@ include file="/WEB-INF/jsp/00-header.jsp" %>
<script type="text/javascript">
$(document).ready(function() {
<%@ include file="/WEB-INF/jsp/00-js-snippets/datepicker-future.js" %>
<%@ include file="/WEB-INF/jsp/00-js-snippets/getConnectorIdsZeroAllowed.js" %>
});
</script>
<div class="left-menu">
<ul>
	<li><a href="/steve/manager/operations/v1.5/ChangeAvailability">Change Availability</a></li>
	<li><a href="/steve/manager/operations/v1.5/ChangeConfiguration">Change Configuration</a></li>
	<li><a href="/steve/manager/operations/v1.5/ClearCache">Clear Cache</a></li>
	<li><a href="/steve/manager/operations/v1.5/GetDiagnostics">Get Diagnostics</a></li>
	<li><a href="/steve/manager/operations/v1.5/RemoteStartTransaction">Remote Start Transaction</a></li>
	<li><a href="/steve/manager/operations/v1.5/RemoteStopTransaction">Remote Stop Transaction</a></li>
	<li><a href="/steve/manager/operations/v1.5/Reset">Reset</a></li>
	<li><a href="/steve/manager/operations/v1.5/UnlockConnector">Unlock Connector</a></li>
	<li><a href="/steve/manager/operations/v1.5/UpdateFirmware">Update Firmware</a></li>
	<hr>
	<li><a class="highlight" href="/steve/manager/operations/v1.5/ReserveNow">Reserve Now</a></li>
	<li><a href="/steve/manager/operations/v1.5/CancelReservation">Cancel Reservation</a></li>
	<li><a href="/steve/manager/operations/v1.5/DataTransfer">Data Transfer</a></li>
	<li><a href="/steve/manager/operations/v1.5/GetConfiguration">Get Configuration</a></li>
	<li><a href="/steve/manager/operations/v1.5/GetLocalListVersion">Get Local List Version</a></li>
	<li><a href="/steve/manager/operations/v1.5/SendLocalList">Send Local List</a></li>
</ul>
</div>
<div class="op15-content">
<form method="POST" action="/steve/manager/operations/v1.5/ReserveNow">
<%@ include file="00-cp-single.jsp" %>
<section><span>Parameters</span></section>
<table class="userInput">
	<tr><td>Connector ID:</td>
		<td><select name="connectorId" id="connectorId" required disabled></select></td>
	</tr>
	<tr><td>Expiry Date/Time (ex: 2011-12-21 at 11:30):</td>
		<td>
			<input type="text" name="expiryDate" class="datepicker" required> at 
			<input type="text" name="expiryTime" class="timepicker" placeholder="optional">
		</td>
	</tr>
	<tr><td>User ID Tag:</td>
	<td>
		<select name="idTag" required>
		<option selected="selected" disabled="disabled" style="display:none;">Choose...</option>
		<%-- Start --%>
		<c:forEach items="${userList}" var="user">
		<option value="${user.idTag}">${user.idTag}</option>
		</c:forEach>
		<%-- End --%>
		</select>
	</td></tr>
	<tr><td>Parent ID Tag:</td>
	<td>
		<select name="parentIdTag">
		<option value="" selected="selected">-- Empty --</option>
		<%-- Start --%>
		<c:forEach items="${userList}" var="user">
		<option value="${user.idTag}">${user.idTag}</option>
		</c:forEach>
		<%-- End --%>
		</select>
	</td></tr>
</table>
<div class="submit-button"><input type="submit" value="Perform"></div>
</form>
</div>
<%@ include file="/WEB-INF/jsp/00-footer.jsp" %>