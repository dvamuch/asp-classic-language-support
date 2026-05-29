<html>
<body>
<%
Dim total
total = 10
%>
<p>Value: <%= total %></p>
<%-- keep comment scriptlet ignored by injector --%>
<%
total = total + 1
Response.Write total
%>
</body>
</html>
