<!--#include file="../../Connections/connection.asp" -->
<!--#include file="../adovbs.inc" -->
<%
' Класс слоя доступа к данным для работы с устройствами.
' Таблица: dbo.Switches.
class DeviceDal
    ' Возвращает устройство по его ID.
    public function getById(deviceId)
        set cmd = Server.CreateObject("ADODB.Command")
        cmd.ActiveConnection = MM_connection_STRING
        cmd.CommandText = "SELECT s.SwitchID as device_id " & _
                            ", SwitchName as device_name " & _
                        "FROM Switches s " & _
                        "WHERE s.SwitchID = ?;"
        cmd.CommandType = 1
        cmd.Prepared = true
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId))

        set rs = cmd.Execute()

        set getById = rs
    end function

    ' Возвращает нижестоящие устройства (кроме VoIP и АТС) с услугой и информацией о клиенте.
    public function getDownlinksWithServiceAndClientExceptTelephony(deviceId, pageNumber, pageSize)
        dim offset : offset = (pageNumber - 1) * pageSize

        set cmd = Server.CreateObject("ADODB.Command")
        cmd.ActiveConnection = MM_connection_STRING
        cmd.CommandText = "WITH CurrentDevice AS (SELECT ? AS SwitchID) " & _
                        ", Downlinks AS (SELECT d.DownlinkSwitchID " & _
                                        "FROM dbo.fn_Graph_GetDLDevices(?, 2) AS d " & _
                                        "UNION ALL " & _
                                        "SELECT SwitchID AS DownlinkSwitchID " & _
                                        "FROM CurrentDevice) " & _
                        "SELECT d.DownlinkSwitchID                                AS device_id " & _
                            ", s.SwitchName                                      AS device_name " & _
                            ", (SELECT si.ServiceItemID                AS service_item_id " & _
                                    ", si.Comments                     AS service_item_comment " & _
                                    ", se.ServiceName                  AS service_name " & _
                                    ", c.CustomerID                    AS customer_id " & _
                                    ", c.CustomerName                  AS customer_name " & _
                                    ", c.CustomerPhone                 AS customer_phone " & _
                                    ", c.CustomerEMail                 AS customer_email " & _
                                    ", c.VIP                           AS customer_is_vip " & _
                                    ", (SELECT REPLACE(dbo.CleanAndTrim(NS.Place), '\', '/') " & _
                                        "FROM S2S_Links SL " & _
                                                "JOIN S2S_Templates ST ON ST.S2S_TemplateID = SL.S2S_TemplateID " & _
                                                "JOIN newServiceAddress NS ON NS.AddressID = SL.SourceTableKey " & _
                                                "LEFT JOIN saledesk.saleman.Locations L ON L.id = NS.LocationID " & _
                                                "LEFT JOIN saledesk.saleman.LocationAddresses LA ON LA.LocationAddressID = NS.LocationAddressID " & _
                                        "WHERE ST.S2S_SchemaID = 26 " & _
                                        "AND SL.ServiceItemID = si.ServiceItemID " & _
                                        "FOR XML PATH('address'), TYPE) AS addresses " & _
                                "FROM S2S_Templates s2s_t " & _
                                        "INNER JOIN S2S_Links s2s_l ON s2s_l.S2S_TemplateID = s2s_t.S2S_TemplateID " & _
                                        "INNER JOIN Services se ON se.ServiceID = s2s_t.ServiceID " & _
                                        "INNER JOIN ServiceItems si ON si.ServiceItemID = s2s_l.ServiceItemID " & _
                                        "INNER JOIN Customers c ON c.CustomerID = si.CustomerID " & _
                                "WHERE s2s_t.S2S_SchemaID = 6 " & _
                                "AND s2s_l.SourceTableKey = d.DownlinkSwitchID " & _
                                "FOR XML PATH('service'), ROOT('services'), TYPE) AS service_xml " & _
                        "FROM Downlinks AS d " & _
                                "LEFT JOIN dbo.Switches s ON d.DownlinkSwitchID = s.SwitchID " & _
                        "WHERE s.DeviceTypeID NOT IN (5, 7) " & _
                        "ORDER BY d.DownlinkSwitchID " & _
                        "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;"
        cmd.CommandType = 1
        cmd.Prepared = true
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId))
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId)) ' В запросе два раза используется этот параметр. Если бы не убогость драйвера, можно было бы делать именованные параметры.
        cmd.Parameters.Append(cmd.CreateParameter("offset", adInteger, adParamInput, 0, offset))
        cmd.Parameters.Append(cmd.CreateParameter("limit", adInteger, adParamInput, 0, pageSize))
        set rs = cmd.Execute()

        set getDownlinksWithServiceAndClientExceptTelephony = rs
    end function

    ' Количество даунлинков для данного устройства.
    ' Использовать для пагинации.
    public function getDownlinksCount(deviceId)
        set cmd = Server.CreateObject("ADODB.Command")
        cmd.ActiveConnection = MM_connection_STRING
        cmd.CommandText = "SELECT COUNT(1) as count " & _
                "FROM dbo.fn_Graph_GetDLDevices(?, 2) AS d " & _
                    "LEFT JOIN dbo.Switches s ON d.DownlinkSwitchID = s.SwitchID " & _
                "WHERE s.DeviceTypeID NOT IN (5, 7);"
        cmd.CommandType = adCmdText
        cmd.Prepared = true
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, , deviceId))

        set rs = cmd.Execute()

        ' Проверяем, есть ли данные
        if not rs.EOF then
            getDownlinksCount = rs("count")
        else
            getDownlinksCount = null
        end if

        ' Закрываем и освобождаем объекты
        rs.Close
        set rs = Nothing
        set cmd = Nothing
    end function

    ' Возвращает количество нижестоящих устройств, относящиеся к ТЕЛЕФОНИИ с услугой и информацией о клиенте.
    public function getTelephonyDownlinksCount(deviceId)

        set cmd = Server.CreateObject("ADODB.Command")
        cmd.ActiveConnection = MM_connection_STRING
        cmd.CommandText = "WITH CurrentDevice AS (SELECT ? AS SwitchID) " & _
                        ", Downlinks AS (SELECT d.DownlinkSwitchID " & _
                                        "FROM dbo.fn_Graph_GetDLDevices(?, 2) AS d " & _
                                        "UNION ALL " & _
                                        "SELECT SwitchID " & _
                                        "FROM CurrentDevice) " & _
                        ", ServiceData AS (SELECT s2s_l.SourceTableKey AS SwitchID " & _
                                                ", COUNT_BIG(*)         AS ServiceCount " & _
                                            "FROM S2S_Templates s2s_t " & _
                                                    "INNER JOIN S2S_Links s2s_l ON s2s_l.S2S_TemplateID = s2s_t.S2S_TemplateID " & _
                                                    "INNER JOIN ServiceItems si ON si.ServiceItemID = s2s_l.ServiceItemID " & _
                                            "WHERE s2s_t.S2S_SchemaID = 6 " & _
                                            "AND si.ServiceId IN (14, 145, 178) " & _
                                            "AND si.ServiceItemStatusID <> 3 " & _
                                            "GROUP BY s2s_l.SourceTableKey) " & _
                        "SELECT COUNT(1) AS count " & _
                        "FROM Downlinks d " & _
                                "LEFT JOIN dbo.Switches s ON d.DownlinkSwitchID = s.SwitchID " & _
                        "WHERE EXISTS (SELECT 1 " & _
                                    "FROM ServiceData sd " & _
                                    "WHERE sd.SwitchID = d.DownlinkSwitchID);"
        cmd.CommandType = 1
        cmd.Prepared = true
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId))
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId)) ' В запросе два раза используется этот параметр. Если бы не убогость драйвера, можно было бы делать именованные параметры.
        cmd.CommandTimeout = 300 ' 5 минут
        set rs = cmd.Execute()

        ' Проверяем, есть ли данные
        if not rs.EOF then
            getTelephonyDownlinksCount = rs("count")
        else
            getTelephonyDownlinksCount = null
        end if

        ' Закрываем и освобождаем объекты
        rs.Close
        set rs = Nothing
        set cmd = Nothing
    end function

    ' Возвращает нижестоящие устройства, относящиеся к ТЕЛЕФОНИИ с услугой и информацией о клиенте.
    public function getTelephonyDownlinksWithServiceAndClient(deviceId, pageNumber, pageSize)
        dim offset : offset = (pageNumber - 1) * pageSize

        set cmd = Server.CreateObject("ADODB.Command")
        cmd.ActiveConnection = MM_connection_STRING
        cmd.CommandText = "WITH CurrentDevice AS (SELECT ? AS SwitchID) " & _
                        ", Downlinks AS (SELECT d.DownlinkSwitchID " & _
                                        "FROM dbo.fn_Graph_GetDLDevices(?, 2) AS d " & _
                                        "UNION ALL " & _
                                        "SELECT SwitchID AS DownlinkSwitchID " & _
                                        "FROM CurrentDevice) " & _
                "SELECT * " & _
                "FROM (SELECT d.DownlinkSwitchID                                AS device_id " & _
                        ", s.SwitchName                                      AS device_name " & _
                        ", (SELECT si.ServiceItemID                AS service_item_id " & _
                                ", si.Comments                     AS service_item_comment " & _
                                ", se.ServiceName                  AS service_name " & _
                                ", c.CustomerID                    AS customer_id " & _
                                ", c.CustomerName                  AS customer_name " & _
                                ", c.CustomerPhone                 AS customer_phone " & _
                                ", c.CustomerEMail                 AS customer_email " & _
                                ", c.VIP                           AS customer_is_vip " & _
                                ", (SELECT REPLACE(dbo.CleanAndTrim(NS.Place), '\', '/') " & _
                                    "FROM S2S_Links SL " & _
                                            "JOIN S2S_Templates ST ON ST.S2S_TemplateID = SL.S2S_TemplateID " & _
                                            "JOIN newServiceAddress NS ON NS.AddressID = SL.SourceTableKey " & _
                                            "LEFT JOIN saledesk.saleman.Locations L ON L.id = NS.LocationID " & _
                                            "LEFT JOIN saledesk.saleman.LocationAddresses LA " & _
                                                        "ON LA.LocationAddressID = NS.LocationAddressID " & _
                                    "WHERE ST.S2S_SchemaID = 26 " & _
                                        "AND SL.ServiceItemID = si.ServiceItemID " & _
                                    "FOR XML PATH('address'), TYPE) AS addresses " & _
                            "FROM S2S_Templates s2s_t " & _
                                    "INNER JOIN S2S_Links s2s_l ON s2s_l.S2S_TemplateID = s2s_t.S2S_TemplateID " & _
                                    "INNER JOIN Services se ON se.ServiceID = s2s_t.ServiceID " & _
                                    "INNER JOIN ServiceItems si ON si.ServiceItemID = s2s_l.ServiceItemID " & _
                                    "INNER JOIN Customers c ON c.CustomerID = si.CustomerID " & _
                            "WHERE s2s_t.S2S_SchemaID = 6 " & _
                                "AND s2s_l.SourceTableKey = d.DownlinkSwitchID " & _
                                "AND si.ServiceId IN (14, 145, 178) " & _
                                "AND si.ServiceItemStatusID <> 3 " & _
                            "FOR XML PATH('service'), ROOT('services'), TYPE) AS service_xml " & _
                    "FROM Downlinks AS d " & _
                            "LEFT JOIN dbo.Switches s ON d.DownlinkSwitchID = s.SwitchID) AS alias " & _
                "WHERE alias.service_xml IS NOT NULL " & _
                "ORDER BY alias.device_id " & _
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY; "
        cmd.CommandType = 1
        cmd.Prepared = true
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId))
        cmd.Parameters.Append(cmd.CreateParameter("device_id", adInteger, adParamInput, 0, deviceId)) ' В запросе два раза используется этот параметр. Если бы не убогость драйвера, можно было бы делать именованные параметры.
        cmd.Parameters.Append(cmd.CreateParameter("offset", adInteger, adParamInput, 0, offset))
        cmd.Parameters.Append(cmd.CreateParameter("limit", adInteger, adParamInput, 0, pageSize))
        set rs = cmd.Execute()

        set getTelephonyDownlinksWithServiceAndClient = rs
    end function
end class
%>