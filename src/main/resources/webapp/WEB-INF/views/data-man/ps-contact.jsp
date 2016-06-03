<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<table class="userInput">
	<thead><tr><th>Contact</th><th></th></thead>
	<tr><td>Phone:</td><td><form:input path="contact.phone"/></td></tr>
	<tr><td>Fax:</td><td><form:input path="contact.fax"/></td></tr>
	<tr><td>Web Site:</td><td><form:input path="contact.website"/></td></tr>
	<tr><td>Email:</td><td><form:input path="contact.email"/></td></tr>

</table>