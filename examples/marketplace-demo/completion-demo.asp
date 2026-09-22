<%@ Language="VBScript" %>
<%
Option Explicit
%>
<!-- #include file="includes/order-service.inc" -->
<%
Dim orderService
Set orderService = New OrderService

Response. orderService.PageTitle
Response.Write orderService.StatusClass("Ready")
%>
