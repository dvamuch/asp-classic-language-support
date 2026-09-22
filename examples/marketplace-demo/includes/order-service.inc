<%
Class OrderService
    Private mPageTitle

    Private Sub Class_Initialize()
        mPageTitle = "Recent orders"
    End Sub

    Public Property Get PageTitle()
        PageTitle = mPageTitle
    End Property

    Public Function GetRecentOrders()
        GetRecentOrders = Array( _
                Array(1042, "Northwind Traders", "In progress"), _
                Array(1043, "Contoso Retail", "Ready"), _
                Array(1044, "Adventure Works", "New") _
        )
    End Function

    Public Function StatusClass(ByVal status)
        Select Case status
            Case "Ready"
                StatusClass = "status-ready"
            Case "In progress"
                StatusClass = "status-progress"
            Case Else
                StatusClass = "status-new"
        End Select
    End Function
End Class
%>
