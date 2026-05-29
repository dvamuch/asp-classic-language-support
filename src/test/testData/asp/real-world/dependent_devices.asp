<%@LANGUAGE="VBSCRIPT" CODEPAGE="65001"%>
<!--#include file="../core/dals/deviceDal.inc" -->
<!--#include file="../core/helpers.inc" -->
<%
Response.Charset = "utf-8"
Response.ContentType = "text/html"
Response.AddHeader "Content-Type", "text/html;charset=utf-8"
Response.AddHeader "Cache-Control", "no-cache, no-store, must-revalidate"
Response.CodePage = 65001

function IsInteger(value)
    on error resume next
    dim num : num = CLng(value)
    if Err.Number = 0 And IsNumeric(value) And InStr(value, ".") = 0 then
        IsInteger = True
    else
        IsInteger = False
    end if
    on error goto 0
end function

' Валидация SwitchID
Dim rawSwitchId : rawSwitchId = Request.QueryString("SwitchID")
if rawSwitchId = "" or IsEmpty(rawSwitchId) or IsNull(rawSwitchId) then
    Response.Write("Параметр SwitchID не указан!")
    Response.End()
end if
if Not IsInteger(rawSwitchId) then
    Response.Write("Параметр SwitchID должен быть целым числом!")
    Response.End()
end if
Dim switchId : switchId = CLng(rawSwitchId)

' Валидация pagenUmber
dim pageNumber : pageNumber = Request.QueryString("page_number")
if pageNumber = "" or IsEmpty(pageNumber) or IsNull(pageNumber) or pageNumber <= 0 then
  pageNumber = 1
end if

dim rowsPerPage : rowsPerPage = 100

set deviceDalInstance = new DeviceDal
set deviceRs = deviceDalInstance.getDownlinksWithServiceAndClientExceptTelephony(switchId, pageNumber, rowsPerPage)
dim totalRows : totalRows = deviceDalInstance.getDownlinksCount(switchId)
set currentDeviceRs = deviceDalInstance.getById(switchId)

dim totalPages : totalPages = max(1, totalRows \ rowsPerPage + (totalRows mod rowsPerPage + rowsPerPage - 1) \ rowsPerPage)

' Формирую url с теми же queryParams, но убираю page_number.
dim queryStringWithoutPageNumber : queryStringWithoutPageNumber = "?"
dim addAmpersand : addAmpersand = false

for each queryParam in Request.QueryString
  if queryParam <> "page_number" and Request.QueryString(queryParam) <> "" then
    if addAmpersand = true then
      queryStringWithoutPageNumber = queryStringWithoutPageNumber & "&"
    end if

    queryStringWithoutPageNumber = queryStringWithoutPageNumber & queryParam & "=" & Server.URLencode(Request.QueryString(queryParam))
    addAmpersand = true
  end if
next


if addAmpersand = true then
  queryStringWithoutPageNumber = queryStringWithoutPageNumber & "&"
end if

' Формирую ссылки на перую, предыдущю, следующую, последнюю страницы.
dim firstPageLink : firstPageLink =  Request.ServerVariables("URL") & queryStringWithoutPageNumber & "page_number=1"
dim previousPageLink : previousPageLink =  Request.ServerVariables("URL") & queryStringWithoutPageNumber & "page_number=" & (pageNumber - 1)
dim nextPageLink : nextPageLink =  Request.ServerVariables("URL") & queryStringWithoutPageNumber & "page_number=" & (pageNumber + 1)
dim lastPageLink : lastPageLink =  Request.ServerVariables("URL") & queryStringWithoutPageNumber & "page_number=" & totalPages

dim rowNumberStartOnCurrentPage : rowNumberStartOnCurrentPage = (pageNumber - 1) * rowsPerPage + 1
dim rowNumberFinishOnCurrentPage : rowNumberFinishOnCurrentPage = Min(rowNumberStartOnCurrentPage + rowsPerPage - 1, totalRows)

%>
<html>
  <head>
    <title>Зависимое оборудование</title>
    <link href="../style.css" rel=stylesheet type=text/css>
    <meta content=text/html;charset=utf-8 http-equiv=Content-Type>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js" type="text/javascript"></script>
    <script src="../javascript/jquery-ui.js" type="text/javascript"></script>
    <link rel="stylesheet" href="../javascript/jquery-ui.css" />
    <style>
    .service_highlight {
      color: #0000CC;
    }
    .content a {
      text-decoration: underline;
    }
    </style>
  </head>
  <body>
    <table border=0 cellPadding=8 cellSpacing=0 width="100%">
      <tr> 
        <td vAlign=top> 
          <span class="header">
            <table width="100%" border=0 cellPadding=3 cellSpacing=0 class="tableboxUp">
              <tr>
                <td><!--#include virtual="/admin/ssi-TopMenuMain.asp" --> </td>
              </tr>
            </table>
          </span>
        </td>
      </tr>
      <tr>
        <td>
          <table  width="100%" border=0 cellPadding=4 cellSpacing=1 class="tablebox content">
            <thead>
              <tr>
                <td class="article-top" colspan=2>
                  <div style="display: flex; align-items: center; justify-content: space-between;">
                    <div>Клиенты на зависимых устройствах для <a  href="/devices/deviceinfo.asp?SwitchID=<%=currentDeviceRs("device_id")%>"><%=currentDeviceRs("device_name")%></a></div>
                    <div><a  href="/devices/dependent_devices_telephony.asp?SwitchID=<%=currentDeviceRs("device_id")%>">Зависимые устройства с телефонией</a></div>
                  </div>
                </td>
              </tr>
              <tr>
                <td class="article-top" colspan=2>
                  <a target="_blank"  href="https://wiki.naukanet.ru/doku.php?id=tts:devices_dependent_devices">Об этой странице в Вики</a>
                <td>
              </tr>
              <tr class="boxtop">
                <td>
                  <b>(Записи <%=rowNumberStartOnCurrentPage%> - <%=rowNumberFinishOnCurrentPage%> из <%=totalRows%>)</b>
                </td>
                <td>
                  <table border="0" width="50%" align="center">
                    <tr>
                      <% if pageNumber <> 1 then %>
                        <td align="center" width="40">
                          <a href="<%=firstPageLink%>">
                            <img src="../images/First.gif" width="18" height="13" border=0>
                          </a>
                        </td>
                        <td align="center" width="40">
                          <a href="<%=previousPageLink%>">
                            <img src="../images/Previous.gif" width="14" height="13" border=0>
                          </a>
                        </td>
                      <% else %>
                        <td align="center" width="40"><img src="../images/First_.gif" width="18" height="13" border=0></td>
                        <td align="center" width="40"><img src="../images/Previous_.gif" width="14" height="13" border=0></td>
                      <% end if %>
                      <% if Not pageNumber = totalPages then %>
                        <td align="center" width="40">
                          <a href="<%=nextPageLink%>">
                            <img src="../images/Next.gif" width="14" height="13" border=0>
                          </a>
                        </td>
                        <td align="center" width="40">
                          <a href="<%=lastPageLink%>">
                            <img src="../images/Last.gif" width="18" height="13" border=0>
                          </a>
                        </td>
                      <% else %>
                        <td align="center" width="40"><img src="../images/Next_.gif" width="14" height="13" border=0></td>
                        <td align="center" width="40"><img src="../images/Last_.gif" width="18" height="13" border=0></td>
                      <% end if %>
                    </tr>
                  </table>
                </td>
              </tr>
              <tr class="boxtop">
                <th class="device-column">Устройство</th>
                <th class="info-column">Информация о клиенте и услуге</th>
              </tr>
            </thead>
            <tbody>
              <%
              while not deviceRs.EOF
                %>
                <tr class="autoRowEvenOrOdd">
                  <td>
                    <a href="/devices/deviceinfo.asp?SwitchID=<%=deviceRs("device_id")%>"><%=deviceRs("device_name")%></a>
                  </td>
                  <td>
                    <%
                    dim servicesXML : servicesXML = deviceRs("service_xml")
                    if not isNull(servicesXML) and servicesXML <> "" then
                      ' Создаем объект для работы с XML
                      Set xmlDoc = Server.CreateObject("MSXML2.DOMDocument")
                      xmlDoc.async = False
                      xmlDoc.loadXML(servicesXML)
                      ' Проверяем, успешно ли загружен XML
                      if xmlDoc.parseError.errorCode <> 0 then
                          Err.Raise vbObjectError + 1, "XML Parsing Error", "Ошибка при загрузке XML: " & xmlDoc.parseError.reason & " XML: " & servicesXML
                      else
                          Set serviceNodes = xmlDoc.selectNodes("//service")
                          for each serviceNode in serviceNodes
                              serviceItemId = serviceNode.selectSingleNode("service_item_id").text
                              serviceItemComment = serviceNode.selectSingleNode("service_item_comment").text
                              serviceName = serviceNode.selectSingleNode("service_name").text
                              customerId = serviceNode.selectSingleNode("customer_id").text
                              customerName = serviceNode.selectSingleNode("customer_name").text

                              customerPhone = getSingleXMLNodeTextOrNull(serviceNode, "customer_phone")
                              customerEmail = getSingleXMLNodeTextOrNull(serviceNode, "customer_email")
                              set addressNodes = serviceNode.selectNodes(".//address")

                              customerIsVip = serviceNode.selectSingleNode("customer_is_vip").text
                              if serviceNode.selectSingleNode("customer_is_vip").text = "1" then
                                customerIsVipTag = "<span style=""color: #9d840f; background-color: #e8bf05; margin: 0 8px;"">[VIP]</span>"
                              end if
                              %>
                              <div style="border: solid; padding: 4px; margin: 4px;">
                                <div>
                                  <b class="service_highlight">Клиент:</b> <a href="http://tts.naukanet.ru/customers/customerinfo.asp?CustomerID=<%=customerId%>"><%=customerName%></a><%=customerIsVipTag%>
                                  <%
                                  if not isNull(customerPhone) then
                                    %>
                                    | Телефон: <%=customerPhone%>
                                    <%
                                  end if
                                  if not isNull(customerEmail) then
                                    %>
                                    | Email: <%=customerEmail%>
                                    <%
                                  end if
                                  %>
                                </div>
                                <hr>
                                <div>
                                  <b class="service_highlight">Услуга:</b> <a href="http://tts.naukanet.ru/services/serviceiteminfo.asp?ServiceItemID=<%=serviceItemId%>"><%=serviceName%> (<%=serviceItemId%>) </a>
                                  | <b class="service_highlight">Описание:</b> <%=serviceItemComment%><br>
                                  <b class="service_highlight">Адреса:</b> 
                                  <%
                                  For Each addressNode In addressNodes
                                    %>
                                    <%=addressNode.text%>;
                                    <%
                                  Next
                                  %>
                                </div>
                              </div>
                              <%
                          next
                      end if

                      ' Освобождаем объект XML
                      Set xmlDoc = Nothing
                    end if
                    %>
                  </td>
                </tr>
                <%
                deviceRs.moveNext()
              wend
              deviceRs.close()
              set deviceRs = nothing
              %>
            </tbody>
          </table>
        </td>
      </tr>   
    </table>

    <!--#include virtual="/admin/ssi-Footer.asp" --> 
  </body>
</html>
