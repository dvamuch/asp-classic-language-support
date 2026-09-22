<%
if showOrders then
%>
<section class="orders">
<h2><%=pageTitle%></h2>
<%if orderCount>0 then%>
<p><%=orderCount%> orders are ready.</p>
<%else%>
<p>No orders are ready.</p>
<%end if%>
</section>
<%
end if
%>
