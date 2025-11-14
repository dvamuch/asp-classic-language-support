<%@ Language=VBScript %>
<!DOCTYPE html>
<html>
<head>
    <title>Test ASP Page</title>
</head>
<body>
<%
Dim userName
userName = "Test User"
Response.Write("Hello, " & userName)

Function TestFunction(x, y)
    TestFunction = x + y
End Function

For i = 1 To 10
    Response.Write(i)
Next
%>
</body>
</html>

