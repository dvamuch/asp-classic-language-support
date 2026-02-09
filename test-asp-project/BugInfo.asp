<%@LANGUAGE="VBSCRIPT" CODEPAGE="65001"%>
<!--#include file="../Connections/connection.asp" -->
<!--#include file="../inc/functions.asp" -->
<%
Dim qsBugId : qsBugId = -1
If (Request("BugID") <> "") Then 
  qsBugId = CLng(Request("BugID"))
End If

Set rsBug = Server.CreateObject("ADODB.Recordset")
rsBug.ActiveConnection = MM_connection_STRING
rsBug.Source = "SELECT Bugs.*, isnull(Bugs.EstimatedTime,0) as EstimatedTime, isnull(Bugs.PercentOfDone,0) as PercentOfDone, Systems.SystemName, TroubleLevels.TroubleLevelName, BugStatuses.TroubleStatusName, Responsibles.LastName as ResponsibleName, Responsibles.EMail as EMail, OPeners.LastName as OpenerName, Updaters.LastName as UpdaterName, Managers.LastName as ManagerName, Bt.BugTypeName, BV.BugVersionName, BP.BP_Name  FROM dbo.Bugs left Join Systems On Bugs.SystemID=Systems.SystemID Left Join TroubleLevels On Bugs.TroubleLevelID=TroubleLevels.TroubleLevelID Left Join BugStatuses On Bugs.TroubleStatusID=BugStatuses.TroubleStatusID Left Join vwUsersFIO as Responsibles On Bugs.ResponsibleID=Responsibles.UserID Left Join vwUsersFIO as Openers On Bugs.CreatedBy=Openers.UserID Left Join vwUsersFIO as Updaters On Bugs.UpdatedBy=Updaters.UserID Left Join vwUsersFIO as Managers On Bugs.ManagerID=Managers.UserID Left Join BugTypes BT on Bugs.BugTypeID=BT.BugTypeID Left Join BugsVersions BV On Bugs.BugVersionID=BV.BugVersionID left join dbo.BizProcessesList BP on BP.BP_ID = Bugs.BizProcessID WHERE Bugs.BugID = " & qsBugId
rsBug.CursorType = 0
rsBug.CursorLocation = 3
rsBug.LockType = 1
rsBug.Open()

BugTitle = rsBug("BugName")

Set rsMessages = Server.CreateObject("ADODB.Recordset")
rsMessages.ActiveConnection = MM_connection_STRING
rsMessages.Source = "SELECT MSGS.*, IsNULL(Deleted,0) As Deleted FROM dbo.MSGS  WHERE MSGSubSectionID = " & qsBugId & " and MSGS.MSGSectionID=14  ORDER BY MSGID DESC"
rsMessages.CursorType = 0
rsMessages.CursorLocation = 3
rsMessages.LockType = 1
rsMessages.Open()

Set rsFiles = Server.CreateObject("ADODB.Recordset")
rsFiles.ActiveConnection = MM_connection_STRING
rsFiles.Source = "SELECT Files.*, Users.LastName  FROM dbo.Files Left Join vwUsersFIO as Users On Files.CreatedBy=Users.UserID  WHERE SubsectionID = " & qsBugId & " AND Files.SectionID = 14 ORDER BY FileID DESC"
rsFiles.CursorType = 0
rsFiles.CursorLocation = 3
rsFiles.LockType = 1
rsFiles.Open()

Set rsViewers = Server.CreateObject("ADODB.Recordset")
rsViewers.ActiveConnection = MM_connection_STRING
rsViewers.Source = "SELECT BV.UserID, FIO.LastName, Comments FROM dbo.BugViewers BV Left join vwUsersFIO FIO on BV.UserID = FIO.UserID WHERE BugID = " & qsBugId & " Order by LastName"
rsViewers.CursorType = 0
rsViewers.CursorLocation = 3
rsViewers.LockType = 1
' Run Query
rsViewers.Open()
%>
<!DOCTYPE html>
<html>
<HEAD>
  <title>
    <%
    If Not rsBug.EOF Or Not rsBug.BOF Then
      %>
      Bug:<%=rsBug("BugID")%> - <%=BugTitle%>
      <%
    End If ' end Not rsBug.EOF Or NOT rsBug.BOF
    %>
  </title>

  <script language="JavaScript" type="text/JavaScript">
    function MM_openBrWindow(theURL,winName,features) { //v2.0
      window.open(theURL, winName, features);
    }
  </script>
  <link href="../style.css" rel=stylesheet type=text/css>
  <meta content=text/html;charset=utf-8 http-equiv=Content-Type>
<body>
<table border=0 cellPadding=8 cellSpacing=0 width="100%" >
  <tr>
    <td vAlign=top> 
	    <table border=0 cellPadding=1 cellSpacing=2 width="100%">
		    <tr class="noprint"> 
          <td>
            <span class="header">
              <!--#include virtual="/admin/ssi-TopMenuMain.asp" -->    
            </span>
          </td>
        </tr>
        <tr>  
          <td>
            <!-- #BeginLibraryItem "/Library/BugsTopMenu.lbi" -->
              <table width="100%" border="0" cellpadding="1" cellspacing="1" class="tableboxUP">
                <tr class="boxtop">
                  <td><a href="../Systems/index.asp">Все системы</a></td>
                  <td><a href="index.asp">Все задачи</a></td>
                  <td><a href="OpenBugs.asp?BugTypeID=1">Открытые задачи (ошибки)</a></td>
                  <td><a href="OpenBugs.asp?BugTypeID=2">Открытые задачи (доработки)</a></td>
                  <td><a href="PersonalOpenedBugs.asp">Открытые лично</a></td>
                  <td><a href="PersonalBugs.asp">Личные</a></td>
                </tr>
              </table>
            <!-- #EndLibraryItem -->
            <%
            If Not rsBug.EOF Or Not rsBug.BOF Then
              %>
              <table width="100%" border="0" align="center" cellpadding="1" cellspacing="1" class="tablebox">
                <tr> 
                  <td colspan="4" class="article-top">Информация о задаче&nbsp; 
                    <%=rsBug("BugID")%> &nbsp;
                    <a href="../Bugs/RelatedBugAdd.asp?RelatedBugID=<%=rsBug("BugID")%>" title="Добавить связанную задачу">
                      <img src="../images/plus2.gif" width="23" height="23" border="0" align="absmiddle">
                    </a>
                  </td>
                </tr>
                <tr class="RowOverOdd"> 
                  <td align="right">Задача:&nbsp;</td>
                  <td colspan="3"><%=rsBug("BugName")%></td>
                </tr>
                <%
                if rsBug("BugTypeID")=2 then
                  %>
                  <tr class="RowOverEven"> 
                    <td align="right">Цель:&nbsp;</td>
                    <td colspan="3"><%=rsBug("Target")%></td>
                  </tr>
                  <tr class="RowOverEven"> 
                    <td align="right">Ожидаемый эффект:&nbsp;</td>
                    <td colspan="3"><%=rsBug("Rezult")%></td>
                  </tr>
                  <tr class="RowOverEven"> 
                    <td align="right">Бизнесс-процесс:&nbsp;</td>
                    <td colspan="3"><%=rsBug("BP_Name")%></td>
                  </tr>
                  <%
                end if
                %>
                <tr class="RowOverOdd">
                  <td align="right">Тип:&nbsp;</td>
                  <td><%=rsBug("BugTypeName")%></td>
                  <td align="right">Спринт:&nbsp;</td>
                  <td>
                    &nbsp;<%=rsBug("BugVersionName")%>&nbsp;
                    <img src="../images/arrow_switch.png" title="Изменить спринт" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" onClick="MM_openBrWindow('BugVersionEdit.asp?BugID=<%=rsBug("BugID")%>&System=<%=rsBug("SystemID")%>','priority','scrollbars=yes,resizable=yes,width=430,height=320')">
                  </td>
                </tr>
                <tr class="RowOverOdd"> 
                  <td align="right">Приоритет:&nbsp;</td>
                  <td bgcolor="<%=GetLevelColor(rsBug("TroubleLevelID"))%>">
                    <%=(rsBug("TroubleLevelName"))%>
                    <%
                    If (rsBug("TroubleLevelID"))=1 then
                      %>
                      <img src="../images/alert.gif" align="absmiddle">
                      <%
                    end if
                    'запрет изменения приоритета всем кроме хелпдеска, администратора, it, ip
                    If  (Session("MM_UserAuthorization") = "admin") OR (Session("MM_UserAuthorization") = "it") OR (Session("MM_UserAuthorization") = "inet") OR (Session("MM_UserAuthorization")= "helpdesk") Then
                      If Session("MM_Username")<>"" Then
                        If Not(rsBug("Closed") = 1)  OR IsNull(rsBug("Closed")) Then
                          %>
                          <img src="../images/arrow_switch.png" title="Изменить приоритет" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" onClick="MM_openBrWindow('bugpriority.asp?BugID=<%=rsBug("BugID")%>','priority','scrollbars=yes,resizable=yes,width=430,height=320')">
                          <%
                        End if
                      End if
                      'окончание запрета на изменение приоритета
                    End if
                    %>
                    </td>
                    <td align="right">Система:&nbsp;</td>
                    <td>
                      <a href="../Systems/SystemInfo.asp?SystemID=<%=rsBug("SystemID")%>">
                        <%=rsBug("SystemName")%>
                      </a>
                      &nbsp;
                      <a title="Перейти на список ошибок/доработок" href="../Systems/SystemBugs.asp?SystemID=<%=rsBug("SystemID")%>">
                        <img src="../images/bug.png" width="16" height="16" border="0" align="absmiddle">
                      </a>
                    </td>
                  </tr>
                  <tr class="RowOverEven"> 
                    <td align="right">Статус:&nbsp;</td>
                    <td  bgcolor="<%=GetStatusColor(rsBug("TroubleStatusID"))%>">
                      <%=(rsBug("TroubleStatusName"))%>
                      <%
                      If (Not(rsBug("Closed") = 1)) Or IsNull(rsBug("Closed")) Then
                        If rsBug("TroubleStatusID") <> 7 Then
                          %>
                          <input name="button3" type="button" class="noprint" style="cursor=pointer" title="Изменить статус ошибки" onClick="window.open('BugChangeStatus.asp?BugID=<%=rsBug("BugID")%>','troubleclose','width=490,height=440,resizable=yes')" value="Изменить">
                          <%
                        End If
                      End If
                      %>
                    </td>
                    <td align="right">Закрыта:&nbsp;</td>
                    <td>
                      &nbsp;<%=rsBug("ClosedAt")%>
                      <%
                      If  Not(rsBug("Closed") = 1)  OR IsNull(rsBug("Closed")) Then
                        If Session("UserID") = rsBug("ManagerID") or Session("MM_UserAuthorization") = "admin" Then
                          %>
                          <input name="button" type="button" class="noprint" onClick="window.open('BugClose.asp?BugID=<%=rsBug("BugID")%>','troubleclose','width=540,height=470,resizable=yes')" value="Закрыть" style="cursor:pointer">
                          <%
                        End If
                      End If
                      %>
                    </td>
                  </tr>
                  <tr class="RowOverEven"> 
                    <td align="right">Ответственный:</td>
                    <td>
                      <a href="../users/personal.asp?UserID=<%=rsBug("ResponsibleID")%>">
                        <%=rsBug("ResponsibleName")%>
                      </a>
                      <%
                      If  Not (rsBug("Closed") = 1)  OR IsNull(rsBug("Closed")) Then
                        %>
                        <input name="button2" type="button" class="noprint" onClick="window.open('BugAssign.asp?BugID=<%=qsBugId%>','troubleclose','width=390,height=400,resizable=yes')" value="Назначить"  title="Назначить ответственного" style="cursor:pointer"> 
                        <img src="../images/basket_put.png" title="Взять на себя ответственность и принять в работу" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" onClick="window.open('BugAssign.asp?BugID=<%=qsBugId%>&NewStatusID=1','troubleclose','width=390,height=400,resizable=yes')" value="Назначить" >
                        <%
                      end if
                      If rsBug("ResponsibleID") > 0 then
                        If Application("SMTP") <> "" Then
                          %>
                          <img src="../images/sendmessage.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Отправить сообщение" onClick="window.open('../users/sendmail.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsBug("ResponsibleID")%>','createmark','width=720,height=380,resizable=yes')">
                          &nbsp;
                          <img src="../images/sendmark.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Оценка" onClick="window.open('../users/createmark.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsBug("ResponsibleID")%>','createmark','width=360,height=300,resizable=yes')">
                          <%
                        End if 'Application(SMTP)
                      End If
                      %>
                    </td>
                    <td align="right">
                      <a href="BugAssignmanager.asp?BugID=<%=rsBug("BugID")%>">
                        <img title="Взять на себя роль заказчика" style="cursor:pointer" src="../images/cart_put.png" width="16" height="16" border="0" align="absmiddle" class="noprint">
                      </a> 
                      Заказчик:&nbsp;
                    </td>
                    <td>
                      <a href="../users/personal.asp?UserID=<%=rsBug("ManagerID")%>">
                        <%=rsBug("ManagerName")%>
                      </a>
                      <%
                      If Application("SMTP")<>"" Then
                        %>
                        <img src="../images/sendmessage.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Отправить сообщение" onClick="window.open('../users/sendmail.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsBug("ManagerID")%>','createmark','width=720,height=380,resizable=yes')">
                        <img src="../images/sendmark.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Оценка" onClick="window.open('../users/createmark.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsBug("ManagerID")%>','troubleclose','width=360,height=300,resizable=yes')"> 
                      <%
                      end if 'Application(SMTP)
                      %>
                    </td>
                  </tr>
                  <tr class="RowOverOdd"> 
                    <td align="right">Комментарии:&nbsp;</td>
                    <td colspan="3"><%=FormatCrLfStrUrl(rsBug("Comments"))%></td>
                  </tr>
                  <tr class="RowOverEven"> 
                    <td align="right">Решение:&nbsp;</td>
                    <td colspan="3"><%=FormatCrLfStrUrl(rsBug("Solution"))%></td>
                  </tr>
                  <tr class="RowOverOdd"> 
                    <td align="right">Создана:&nbsp;</td>
                    <td>
                      <%=rsBug("CreatedAt")%> &nbsp;
                      <a href="../users/personal.asp?UserID=<%=rsBug("CreatedBy")%>">
                        <%=rsBug("OpenerName")%>
                      </a>
                    </td>
                    <td align="right">Изменена:&nbsp;</td>
                    <td>
                      <%=rsBug("UpdatedAt")%> &nbsp;
                      <a href="../users/personal.asp?UserID=<%=rsBug("UpdatedBy")%>">
                        <%=rsBug("UpdaterName")%>
                      </a>
                    </td>
                  </tr>
                  <tr class="RowOverOdd">
                    <td align="right">Наблюдатели:&nbsp;<img src="../images/plus2.gif" width="15" height="15" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Добавить наблюдателя(ей)" onClick="window.open('../Bugs/AddViewer.asp?SectionID=14&BugID=<%=rsBug("BugID")%>','troubleclose','width=550,height=370, resizable=yes')" >
                    </td>
                    <td colspan="3">
                      &nbsp;
                      <%
                      If not rsViewers.BOF and not rsViewers.EOF then
                        While NOT rsViewers.EOF
                          %>
                          <div style="cursor:pointer; display:inline-block" title="Удалить" onClick="window.open('../Bugs/DeleteViewer.asp?SectionID=14&BugID=<%=rsBug("BugID")%>&UserID=<%=rsViewers("UserID")%>','troubleclose','width=360,height=200,resizable=yes')">
                            <%=rsViewers("LastName")%>
                          </div>
                          <img src="../images/sendmessage.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Отправить сообщение" onClick="window.open('../users/sendmail.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsViewers("UserID")%>','createmark','width=720,height=380,resizable=yes')">
                          &nbsp;
                          <% 
                          rsViewers.MoveNext
                        Wend
                      End If
                      %>
                    </td>
                  </tr>
                  <tr class="RowOverOdd"><td colspan="3" align="right">Сложность задачи:&nbsp;</td>
                    <td>
                      <b>
                        <%=rsBug("Complexity")%>
                      </b>	
                      &nbsp;
                      <img src="../images/arrow_switch.png" title="Изменить сложность задачи" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" onClick="MM_openBrWindow('BugComplexityEdit.asp?BugID=<%=rsBug("BugID")%>&System=<%=rsBug("SystemID")%>','priority','scrollbars=yes,resizable=yes,width=400,height=200')">
                    </td>
                  </tr>             
                  <%
                  ' Основная  задача
                  If rsBug("RelatedBugID")<>"" then

                    Dim rsRelatedBug
                    Dim rsRelatedBug_numRows

                    Set rsRelatedBug = Server.CreateObject("ADODB.Recordset")
                    rsRelatedBug.ActiveConnection = MM_connection_STRING
                    rsRelatedBug.Source = "SELECT Bugs.*, BugStatuses.TroubleStatusName, Users.LastName, Systems.SystemName  FROM dbo.Bugs Left Join BugStatuses On Bugs.TroubleStatusID=BugStatuses.TroubleStatusID Left Join Users On Bugs.ResponsibleID=Users.UserID Left Join Systems On Bugs.SystemID=Systems.SystemID  WHERE BugID = " & rsBug("RelatedBugID")
                    rsRelatedBug.CursorType = 0
                    rsRelatedBug.CursorLocation = 3
                    rsRelatedBug.LockType = 1
                    rsRelatedBug.Open()
                    If Not rsRelatedBug.EOF Or Not rsRelatedBug.BOF Then
                      %>
                      <tr class="RowOverOdd">
                        <td align="right"><b>Основная задача:</b> </td>
                        <td colspan="3" align="left">
                          <table width="80%" border="0" cellspacing="0">
                            <% 
                            While NOT rsRelatedBug.EOF
                              %>
                          
                              <tr  class="RowOverOdd"> 
                                <td width="7%" align="center" valign="top"  bgcolor="<%=GetStatusColor(rsRelatedBug("TroubleStatusID"))%>"><A HREF="../Bugs/Buginfo.asp?<%="BugID=" & rsRelatedBug("BugID") %>"><%=(rsRelatedBug("BugID"))%></A></td>
                                <td colspan="2" valign="top"><a href="../Bugs/Buginfo.asp?<%="BugID=" & rsRelatedBug("BugID") %>"><%=(rsRelatedBug("BugName"))%></a></td>
                                <td valign="top"><%=rsRelatedBug("SystemName")%></td>
                                <td valign="top"><%=rsRelatedBug("TroubleStatusName")%></td>
                                <td> &nbsp;
                                  <a href="../users/personal.asp?UserID=<%=rsRelatedBug("ResponsibleID")%>">
                                    <%=(rsRelatedBug("LastName"))%>
                                  </a>
                                </td>
                              </tr>
                              <% 
                              rsRelatedBug.MoveNext()
                            Wend
                            %>
                          </table>
                        </td>
                      </tr>
                      <%
                    End If ' end Not rsRelatedBug.EOF Or NOT 
                  End If

                  ' Связанная заявка
                  If rsBug("RelatedOrderID")<>"" then
                    Dim rsRelatedOrders
                    Dim rsRelatedOrders_numRows

                    Set rsRelatedOrders = Server.CreateObject("ADODB.Recordset")
                    rsRelatedOrders.ActiveConnection = MM_connection_STRING
                    rsRelatedOrders.Source = "SELECT Orders.*, BugStatuses.TroubleStatusName, Users.LastName, Customers.CustomerName  FROM dbo.Orders Left Join BugStatuses On Orders.TroubleStatusID=BugStatuses.TroubleStatusID Left Join Users On Orders.ResponsibleID=Users.UserID Left Join Customers On Orders.CustomerID=Customers.CustomerID  WHERE OrderID = " & rsBug("RelatedOrderID")
                    rsRelatedOrders.CursorType = 0
                    rsRelatedOrders.CursorLocation = 3
                    rsRelatedOrders.LockType = 1
                    rsRelatedOrders.Open()
                    If Not rsRelatedOrders.EOF Or Not rsRelatedOrders.BOF Then
                      %>
                      <tr class="RowOverOdd">
                        <td align="right">Связанная заявка: </td>
                        <td colspan="3" align="left">
                          <table width="80%" border="0" cellspacing="0">
                            <% 
                            While NOT rsRelatedOrders.EOF
                              %>
                              <tr class="class="RowOverOdd""> 
                                <td width="7%" align="center" valign="top"  bgcolor="<%=GetStatusColor(rsRelatedOrders("TroubleStatusID"))%>"><A HREF="../Customers/orderinfo.asp?<%="OrderID=" & rsRelatedOrders("OrderID") %>"><%=(rsRelatedOrders("OrderID"))%></A></td>
                                <td colspan="2" valign="top"><a href="../Customers/orderinfo.asp?<%="OrderID=" & rsRelatedOrders("OrderID") %>"><%=(rsRelatedOrders("Descr"))%></a></td>
                                <td valign="top"><A HREF="../Customers/customerinfo.asp?<%="CustomerID=" & rsRelatedOrders("CustomerID") %>"><%=(rsRelatedOrders("CustomerName"))%></A> </td>
                                <td valign="top"><%=(rsRelatedOrders("TroubleStatusName"))%></td><td> &nbsp;<A HREF="../users/personal.asp?<%="UserID=" & rsRelatedOrders("ResponsibleID") %>"><%=(rsRelatedOrders("LastName"))%></A></td>
                              </tr>
                              <% 
                              rsRelatedOrders.MoveNext()
                            Wend
                            %>
                          </table>
                        </td>
                      </tr>
                      <%
                    End If ' end Not rsRelatedOrders.EOF Or NOT 
                  End If

                  ' Связанные задачи
                  Dim rsRelatedBugs
                  Dim rsRelatedBugs_numRows

                  Set rsRelatedBugs = Server.CreateObject("ADODB.Recordset")
                  rsRelatedBugs.ActiveConnection = MM_connection_STRING
                  rsRelatedBugs.Source = "SELECT Bugs.*, BugStatuses.TroubleStatusName, Users.LastName, Systems.SystemName  FROM dbo.Bugs Left Join BugStatuses On Bugs.TroubleStatusID=BugStatuses.TroubleStatusID Left Join Users On Bugs.ResponsibleID=Users.UserID Left Join Systems On Bugs.SystemID=Systems.SystemID  WHERE RelatedBugID = " & rsBug("BugID")
                  rsRelatedBugs.CursorType = 0
                  rsRelatedBugs.CursorLocation = 3
                  rsRelatedBugs.LockType = 1
                  rsRelatedBugs.Open()
                  If Not rsRelatedBugs.EOF Or Not rsRelatedBugs.BOF Then
                    %>
                    <tr class="RowOverOdd">
                      <td align="right">Связанные задачи: </td>
                      <td colspan="3" align="left">
                        <table width="80%" border="0" cellspacing="0">
                          <% 
                          While NOT rsRelatedBugs.EOF
                            %>
                            <tr  class="RowOverOdd"> 
                              <td width="7%" align="center" valign="top"  bgcolor="<%=GetStatusColor(rsRelatedBugs("TroubleStatusID"))%>"><A HREF="../Bugs/Buginfo.asp?BugID=<%=rsRelatedBugs("BugID")%>"><%=rsRelatedBugs("BugID")%></A></td>
                              <td colspan="2" valign="top"><a href="../Bugs/Buginfo.asp?BugID=<%=rsRelatedBugs("BugID")%>"><%=rsRelatedBugs("BugName")%></a></td>
                              <td valign="top"><%=rsRelatedBugs("SystemName")%></td>
                              <td valign="top"><%=rsRelatedBugs("TroubleStatusName")%></td><td> &nbsp;<a href="../users/personal.asp?<%="UserID=" & rsRelatedBugs("ResponsibleID") %>"><%=rsRelatedBugs("LastName")%></A></td>
                            </tr>
                            <% 
                            rsRelatedBugs.MoveNext()
                          Wend
                          %>
                        </table>
                      </td>
                    </tr>
                    <%
                  End If ' end Not rsRelatedBugs.EOF Or NOT 
                  %>
                <tr class="RowOverSelected">
                    <td>
                      <a href="BugEdit.asp?BugID=<%=rsBug("BugID")%>">Изменить задачу</a>
                    </td>
                    <td colspan="3"></td>
                </tr>
              </table>
              <table width="100%" border="0" cellpadding="4" cellspacing="0" class="tablebox">
                <tr id="supaDVPDropZoneFileListTitle"> 
                  <td height="27" colspan="6"  class="article-top">
                    <link href="/javascript/dvp_plupload.css" rel="stylesheet" type="text/css" />
                    <script src="https://code.jquery.com/jquery-3.2.1.min.js" integrity="sha256-hwg4gsxgFZhOsEEamdOYGBf13FyQuiTwlAQgxVSNgt4=" crossorigin="anonymous"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/plupload/2.3.6/plupload.full.min.js"></script>
                    <script src="/javascript/dvp_plupload.js"></script>
                    <script>
                      dvpInitializeNewPluploadZOne(<%=qsBugId%>, "<%=Session("UserID")%>", "<%=Session("UserName")%>", 14);
                    </script>				
                    <div id="supaDVPDropZone" class="dvpPluploadStuff" style="position: relative; z-index: 1;">

                      <div class="cimg">
                        <div class="img"></div>
                      </div>
                      <div class="text">
                        <div class="first">Выберите файлы</div>
                        <div class="second">или перетащите в эту область</div>
                      </div>

                    </div>
                  </td>
                </tr>
                <%
                If Not rsFiles.EOF Or Not rsFiles.BOF Then
                  %>
                  <tr class="boxtop"> 
                    <td width="16"></td>
                    <td valign="top">ID</td>
                    <td valign="top">Название</td>
                    <td colspan="2" valign="top">Комментарий</td>
                    <td valign="top">Добавлен</td>
                  </tr>
                  <tbody class="zebra0">
                    <%
                    While (NOT rsFiles.EOF)
                      %>
                      <tr data-id="<%=rsFiles("FileID")%>">
                        <td>
                          <img style="cursor:pointer;" title="Скопировать адрес в буфер обмена" alt="Скопировать адрес в буфер обмена" src="/images/copy2buff.gif" width="16" height="16" onClick="copylinkToClipboard(this)" link="http://tts.naukanet.ru/files/filedownload.asp?<%="FileID=" & rsFiles("FileID") %>">
                        </td>
                        <td width="70"><%=rsFiles("FileID")%></td>
                        <td width="50%"> 
                          <a href="../files/filedownload.asp?<%="FileID=" & rsFiles("FileID") %>" title="<%=DequoteStr(rsFiles("Comments"))%>" target="_blank">
                            <%=rsFiles("FileName")%>
                          </a>
                          [<a href="../files/fileinfo.asp?<%="FileID=" & rsFiles("FileID") %>">?</a>]
                        </td>  
                        <td width="16">
                          <%
                          If Session("MM_UserAuthorization")="admin" or  session("UserID")= rsFiles("CreatedBy") then
                            %> 				   
                            <div class="dvpFileCommEdit"><img title="Редактировать комментарий" alt="Редактировать комментарий" src="/images/b_edit.png"></div>
                            <%
                          End If
                          %>
                        </td>
                        <td style="padding:0px;"> 
                          <%
                          If Session("MM_UserAuthorization")="admin" or  session("UserID")= rsFiles("CreatedBy") then
                            %>
                            <div class="dvpFileContentEditable"  contenteditable="true"><%=FormatCrLfStrURL(rsFiles("Comments"))%></div> 				
                            <%
                          Else
                            %>
                            <div style="padding:2px;"><%=FormatCrLfStrURL(rsFiles("Comments"))%></div>
                            <%
                          End If
                          %>
                        </td>
                        <td width="350"><%=(rsFiles("CreatedAt"))%>&nbsp; &nbsp;<a href="../users/personal.asp?<%="UserID=" & rsFiles("CreatedBy") %>"><%=rsFiles("LastName")%></a>
                          <%
                          If Session("MM_UserAuthorization")="admin" or  session("UserID")= rsFiles("CreatedBy") then
                            %>
                            <div class="dvpFileDelete"><img title="Удалить файл" alt="Удалить файл" src="/images/b_drop.png"></div>
                            <%
                          End If
                          %>
                        </td>
                      </tr>
                      <%
                      rsFiles.MoveNext()
                    Wend
                    %>
                  </tbody>
                  <%
                End If ' end Not rsFiles.EOF Or NOT rsFiles.BOF
                %>
              </table>
              <style>
                .dvp_msg_del_td {
                position: relative;
                }	

                .dvp_msg_delete_wrap {
                  min-width: 150px; 
                height:23px; 	
                }

                .dvp_msg_delete_wrap form {
                  display: none;	
                }
                
                .dvp_msg_delete_wrap a {
                line-height: 23px;
                display: block;
                float: left;
                margin-left: 2px;
                }
                
                
                .dvp_msg_delete_wrap img {
                display: block;
                float: right;
                margin-top: 6px;
                margin-right:4px;
                cursor: pointer;
                }
                
                .dvp_msg_confirm_wrap {
                display:none;
                position: absolute;
                top: 0px;
                width: 100%;
                  min-width: 150px; 
                height:23px; 
                margin-left: -2px;
                }
                
                .dvp_msg_confirm_button {
                margin-left: 2px;
                line-height: 21px;
                border: 1px solid #d60000;
                background: #ef4949;	
                color: #5f0000;
                font-weight: bold;
                cursor: pointer;
                }
              </style>	
              <script>
                $(function() {
                  $(".dvp_msg_delete_wrap img").click(function() {
                    var $this = $(this),
                      $td   = $this.closest("td");
                      
                      $td.find(".dvp_msg_confirm_wrap").fadeIn(150);
                      
                    setTimeout(function() {
                      $(document).one("click", function (e) {
                      var $target = $(e.target);

                      if (!$target.closest(".dvp_msg_confirm_button").length && !$target.closest(".dvp_msg_delete_wrap img").length) {
                        $(".dvp_msg_confirm_wrap").fadeOut(150);
                      }
                      });
                    }, 150);
                  });
                  
                  $(".dvp_msg_confirm_button").click(function() {
                    var $this = $(this),
                      $tr   = $this.closest("tr"),
                      $td   = $this.closest("td");
                  
                    $.ajax( {
                      url:  $td.attr("data-action"),
                      type:  "POST",
                      data:  {update: 1},
                      cache: false,
                      success: function() {			  
                        $("td#"+$td.attr("data-id")).html('<div style="max-width:790px; word-wrap:break-word">Удалено автором</div>');
                        
                        $td.find(".dvp_msg_confirm_wrap").remove();
                        $td.find("img").remove();
                      }
                    });			  
                  });
                });
                
                function copyValueToClipboard(element) {
                  var $temp = $("<input>");
                  $("body").append($temp);
                  $temp.val($(element).attr("value")).select();
                  document.execCommand("copy");
                  $temp.remove();
                alert ("Скопировано.");
                }
              </script>		

              <table width="100%" border="0" cellpadding="2" cellspacing="1" class="tablebox">
                <thead>
                  <tr> 
                    <td colspan="4" class="article-top">
                      Сообщения
                      <img src="../images/plus2.gif" width="21" height="21" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Добавить сообщение" onClick="window.open('../Bugs/msgadd2bug.asp?SectionID=14&<%="SubsectionID=" & rsBug("BugID") & "&EMail=" & rsBug("EMail") %>','troubleclose','width=720,height=380,resizable=yes')" >
                      &nbsp;
                      <img src="../images/sendmessage.png" width="17" height="17" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Отправить сообщение пользователям, не участвующим в проекте" onClick="window.open('../users/sendmail-b.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>','createmark','width=720,height=380,resizable=yes')">
                    </td>
                  </tr>
                </thead>
                <tbody class="zebra0">
                  <%
                  If Not rsMessages.EOF Or Not rsMessages.BOF Then
                    While (NOT rsMessages.EOF)
                      %>
                      <tr valign="top"> 
                        <td nowrap width="20%"> 
                          <%=(rsMessages("MSGDate"))%> &nbsp;
                          <br>
                          <%
                          If rsMessages("CreatedBy")>0 Then%>
                            <A HREF="../users/personal.asp?<%="UserID=" & rsMessages("CreatedBy") %>"> 
                              <%=(rsMessages("MSGAuthor"))%>
                            </A>
                            <%
                            If Application("SMTP")<>"" Then
                              %>
                              <img src="../images/sendmessage.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Отправить сообщение" onClick="window.open('../users/sendmail.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsMessages("CreatedBy")%>','createmark','width=720,height=380,resizable=yes')">
                              &nbsp;
                              <img src="../images/sendmark.png" width="16" height="16" border="0" align="absmiddle" class="noprint" style="cursor:pointer" title="Оценка"onClick="window.open('../users/createmark.asp?SectionID=14&SubSectionID=<%=rsBug("BugID")%>&UserID=<%=rsMessages("CreatedBy")%>','createmark','width=360,height=300,resizable=yes')">
                              <%
                            End if 'Application(SMTP)
                          End If
                          %>
                        </td>
                        <td colspan="2" id="<%=rsMessages("MSGID")%>">
                          <%
                          If rsMessages("Deleted") <> 1 then
                            If Instr(rsMessages("MSGText"),"<pre>")>0 Then
                              %>
                              <div style="width:950px;overflow-x:scroll;">
                                <code><%=FormatCrLfStrLen(rsMessages("MSGText"), 180)%></code>
                              </div>
                              <%
                            Else
                              %> 
                              <div style="max-width:950px; word-wrap:break-word">
                                <%=FormatCrLfStrUrl(rsMessages("MSGText"))%>
                              </div>
                              <%
                            End If
                          Else
                            %>
                            <div style="max-width:790px; word-wrap:break-word">
                              Удалено автором &nbsp;<%=rsMessages("DeletedAt")%>
                            </div> 
                            <%
                          End If
                          %>
                        </td>
                        <td valign="top" nowrap align="center" width="12%" class="dvp_msg_del_td" data-id="<%=rsMessages("MSGID")%>" data-action="/Users/MSGDelete.asp?MSGID=<%=rsMessages("MSGID")%>">
                          <img src="/images/copy2buff.gif" width="16" height="16" onClick="copyValueToClipboard(this)" title="Копировать #MSG: в буфер " value="#MSG:<%=rsMessages("MSGID")%>" style="vertical-align:middle;cursor:pointer;">
                          <div class="dvp_msg_delete_wrap" style="display:inline">
                            <font size="-1">
                              <a href="../Bugs/Buginfo.asp?BugID=<%=rsBug("BugID")%>#<%=rsMessages("MSGID")%>">
                                #MSG: <%=rsMessages("MSGID")%>
                              </a>
                            </font>
                            <%
                            If rsMessages("CreatedBy")=Session("UserID") and rsMessages("Deleted")=0 Then
                              %>
                              <img src="/images/b_drop.png" width="12" height="12" title="Удалить сообщение">	    
                              <%
                            End if
                            %>
                          </div>
                          <div class="dvp_msg_confirm_wrap"><div class="dvp_msg_confirm_button">Подтвердить</div></div>
                        </td>               
                      </tr>
                      <%
                      rsMessages.MoveNext()
                    Wend
                  End If ' end Not rsMessages.EOF Or NOT rsMessages.BOF
                  %>
                </tbody>
                <tfoot>
                  <tr class="RowOverSelected" style="height=1em;">
                    <td colspan="4"></td>
                  </tr>
                </tfoot>
              </table>
              <%
            End If ' end Not rsBug.EOF Or NOT rsBug.BOF
          
            If rsBug.EOF And rsBug.BOF Then
              %>
              Задачи с таким идентификатором в базе не обнаружено! 
              <%
            End If ' end rsBug.EOF And rsBug.BOF
            %>
          </td>
        </tr>
      </table>
    </td>
  </tr> 
</table>

<div align="center">
  <font size="1">
    Время: <%=Now()%>
  </font>
</div>
</body>
</html>
<%
rsBug.Close()
Set rsBug = Nothing
%>
<%
rsMessages.Close()
Set rsMessages = Nothing
%>
<%
rsFiles.Close()
Set rsFiles = Nothing
%>
<%
rsViewers.Close()
Set rsViewers = Nothing
%>
