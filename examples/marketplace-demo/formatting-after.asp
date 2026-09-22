<%
If showOrders Then
    %>
    <section class="orders">
        <h2><%= pageTitle %></h2>
        <% If orderCount > 0 Then %>
            <p><%= orderCount %> orders are ready.</p>
        <% Else %>
            <p>No orders are ready.</p>
        <% End If %>
    </section>
    <%
End If
%>
