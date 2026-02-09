<%
Dim qsBugId : qsBugId = -1
If (Request("BugID") <> "") Then
  qsBugId = CLng(Request("BugID"))
End If
%>