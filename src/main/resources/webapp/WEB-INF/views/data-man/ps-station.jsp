<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"  %>
<table class="userInput">
    <thead><tr><th>PlugSurfing</th><th></th></thead>
    <tr><td>Enable PlugSurfing?</td><td><form:checkbox path="plugSurfing"/></td></tr>
    <tr><td>Open 24-Hours</td><td><form:checkbox path="open24"/></td></tr>
    <tr><td>Reservable</td><td><form:checkbox path="reservable"/></td></tr>
    <tr><td>Free of Charge</td><td><form:checkbox path="freeCharge"/></td></tr>
    <tr><td>Green Power</td><td><form:checkbox path="greenPowerAvailable"/></td></tr>
    <tr><td>Plugin Charge</td><td><form:checkbox path="pluginCharge"/></td></tr>
    <tr><td>Roofed</td><td><form:checkbox path="roofed"/></td></tr>
    <tr><td>Private</td><td><form:checkbox path="privatelyOwned"/></td></tr>
    <tr>
        <td>Number of Connectors</td>
        <td>
            <%--
                Once set, numberOfConnectors cannot be changed later. But existing local stations
                can be upgraded to a PlugSurfing station. That's why we should allow setting
                numberOfConnectors in details/update page. But only if it was not set before.
            --%>
            <c:choose>
                <c:when test="${empty chargePointForm.numberOfConnectors}">
                    <form:input path="numberOfConnectors" readonly="false"/>
                </c:when>
                <c:otherwise>
                    <form:input path="numberOfConnectors" readonly="true"/>
                </c:otherwise>
            </c:choose>
            <a class="tooltip" href="#"><img src="${ctxPath}/static/images/info.png" style="vertical-align:middle">
                <span>
                    PlugSurfing does not allow to change the number of connectors on an existing station.
                    Because of that this field is set only once, and cannot be changed later.
                </span>
            </a>
        </td>
    </tr>
    <tr><td>Floor Level</td><td><form:input path="floorLevel"/></td></tr>
    <tr><td>Parking Spots</td><td><form:input path="totalParking"/></td></tr>
</table>