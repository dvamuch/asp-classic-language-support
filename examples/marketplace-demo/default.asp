<%@ Language="VBScript" %>
<%
Option Explicit
%>
<!-- #include file="includes/order-service.inc" -->
<%
Dim orderService
Dim recentOrders
Dim order

Set orderService = New OrderService
recentOrders = orderService.GetRecentOrders()
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title><%= orderService.PageTitle %></title>
</head>
<body>
<main class="orders">
    <h1><%= orderService.PageTitle %></h1>

    <% If IsArray(recentOrders) Then %>
        <table>
            <thead>
            <tr>
                <th>ID</th>
                <th>Customer</th>
                <th>Status</th>
            </tr>
            </thead>
            <tbody>
            <% For Each order In recentOrders %>
                <tr class="<%= orderService.StatusClass(order(2)) %>">
                    <td><%= order(0) %></td>
                    <td><%= Server.HTMLEncode(order(1)) %></td>
                    <td><%= order(2) %></td>
                </tr>
            <% Next %>
            </tbody>
        </table>
    <% Else %>
        <p>No recent orders.</p>
    <% End If %>
</main>
</body>
</html>
