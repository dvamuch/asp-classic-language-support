<%@LANGUAGE="VBSCRIPT" CODEPAGE="65001"%>
<!--#include file="../Connections/connection.asp" -->
<!--#include file="../inc/functions.asp" -->
<!--#include file="../inc/sendmailwithlog.asp" -->
<!--#include file="../inc/messagelog.asp" -->
<%
Server.ScriptTimeout = 1600
'Response.Buffer=False
%>

<%
sAuthor = Session("UserName")
'sAuthor = Replace(sAuthor, CHR(10), "")
'sAuthor = Replace(sAuthor, CHR(13), "")
'sSignature = "С уважением," & vbCrLf & sAuthor & vbCrLf & "ООО Наука-Связь"
'sSignature = Session("UserSignature")
Dim rsSignature

Set rsSignature = Server.CreateObject("ADODB.Recordset")
rsSignature.ActiveConnection = MM_connection_STRING
rsSignature.Source = "SELECT dbo.Users.Signature FROM dbo.Users WHERE UserID = " & Session("UserID")
rsSignature.Open()
 sSignature = rsSignature("Signature")
 if sSignature <> "" then
 sSignature = Replace(sSignature, """", "&quot;")
 sSignature = Replace(sSignature, "'", "&prime;")
 sSignature = Replace(sSignature, "<", "&lsaquo;")
 sSignature = Replace(sSignature, ">", "&rsaquo;")
 end if
rsSignature.Close()
Set rsSignature = Nothing
%>
<%
Dim rsSignatureRucelcotele

Set rsSignatureRucelcotele = Server.CreateObject("ADODB.Recordset")
rsSignatureRucelcotele.ActiveConnection = MM_connection_STRING
rsSignatureRucelcotele.Source = "SELECT dbo.Users.SignatureRucelcotele FROM dbo.Users WHERE UserID = " & Session("UserID")
rsSignatureRucelcotele.Open()
 sSignatureRucelcotele = rsSignatureRucelcotele("SignatureRucelcotele")
 if sSignatureRucelcotele <> "" then
 sSignatureRucelcotele = Replace(sSignatureRucelcotele, """", "&quot;")
 sSignatureRucelcotele = Replace(sSignatureRucelcotele, "'", "&prime;")
 sSignatureRucelcotele = Replace(sSignatureRucelcotele, "<", "&lsaquo;")
 sSignatureRucelcotele = Replace(sSignatureRucelcotele, ">", "&rsaquo;")
 end if 
 
rsSignatureRucelcotele.Close()
Set rsSignatureRucelcotele = Nothing
%>
<%
sTextCopy = ""
sTextCopyRucelcotele = ""

IF Request("MSGID")<>"" AND Request("MSGType")<>"" Then ' Если нужно копировать сообщение


Dim rsMSGS

Set rsMSGS = Server.CreateObject("ADODB.Recordset")
rsMSGS.ActiveConnection = MM_connection_STRING

 Select Case Request("MSGType")
 Case 1
  TableName = "dbo.MSGS"
  ID = "MSGID"
  Author = "MSGAuthor"
  MSGText = "MSGText"
'rsMSGS.Source = "SELECT " & MSGText & " AS Text, " & Author & " AS Author FROM " & TableName & " WHERE " & ID & "=" & Request("MSGID")
 Case 2
  TableName = "dbo.SrvMSGS"
  ID = "SrvMSGID"
  Author = "SrvMSGAuthor"
  MSGText = "SrvMSGText"
'rsMSGS.Source = "SELECT " & MSGText & " AS Text, " & Author & " AS Author FROM " & TableName & " WHERE " & ID & "=" & Request("MSGID")
 Case 3
rsMSGS.Source = "SELECT dbo.CstMSGS.CstMSGID, dbo.CstMSGS.CstMSGTitle,  dbo.CstMSGS.CstMSGFrom, dbo.CstMSGS.CstMSGTo, dbo.CstMSGS.CstMessage, dbo.CstMSGS.CstMSGText FROM dbo.CstMSGS WHERE CstMSGID = " & Request("MSGID")

rsMSGS.Open()
'Response.Write(rsMSGS.Source)
sAddFrom = rsMSGS("CstMSGFrom")
    IF rsMSGS("CstMSGTo")<>""  Then
    sAddFrom = sAddFrom  & ", " & Replace(rsMSGS("CstMSGTo"), " ",", ")
   End If 
    IF sAddFrom<>""  Then
   sAddFrom = replace(sAddFrom,"""", "")
   sAddFrom = replace(sAddFrom,"'", "")
   sAddFrom = replace(sAddFrom,"<", "")
   sAddFrom = replace(sAddFrom,">", "")
    End If
  ' sAddFrom=""
'sText = rsMSGS("CstMessage")
sText = Replace(rsMSGS("CstMSGText"),"Клиенту отправлено сообщение:","")

    IF rsMSGS("CstMSGTitle")<>""  Then
sTitleCopy = "Re: " & Replace(rsMSGS("CstMSGTitle"), """", "&quot;")
    End If
'sText =  Replace(sText, CHR(10) & CHR(13), CHR(10) & CHR(13) & ">")
sText = Replace(sText, CHR(10), CHR(10) & ">")
sText = Replace(sText, """", "&quot;")
sText = Replace(sText, "'", "&prime;")
sText = Replace(sText, "<", "&lsaquo;")
sText = Replace(sText, ">", "&rsaquo;")

sTextCopy = vbCrLf & vbCrLf & "   -------- Исходное сообщение --------"  & vbCrLf & ">" & sText 
sTextCopyRucelcotele = vbCrLf & vbCrLf & "   -------- Исходное сообщение --------"  & vbCrLf & ">" & sText
rsMSGS.Close()
  
End Select

Set rsMSGS=Nothing

End If ' Копирование

sFrom = Request("From")

sTextCopy = vbCrLf & vbCrLf & vbCrLf & sSignature & sTextCopy
sTextCopyRucelcotele = vbCrLf & vbCrLf & vbCrLf & sSignatureRucelcotele & sTextCopyRucelcotele

%>
<%
CustomersList=Split(Request("CustomersList"),",") 

Dim rsCustomers

Set rsCustomers = Server.CreateObject("ADODB.Recordset")
rsCustomers.ActiveConnection = MM_connection_STRING


TextTO1=""

For each Customer in CustomersList
If Trim(Customer)<>"" Then
rsCustomers.Source = "SELECT CustomerID, CustomerName, CustomerEmail FROM  dbo.Customers WHERE (CustomerEmail <> N'') AND CustomerID=" & Customer
' Response.Write(rsCustomers.Source)
 
 rsCustomers.Open()

If NOT(rsCustomers.EOF AND rsCustomers.BOF) Then

CustomerEmail=Replace(Trim(rsCustomers("CustomerEmail"))," ",",") 
CustomerEmail=Replace(CustomerEmail,"; ",",")
CustomerEmail=Replace(CustomerEmail,",,",",")

TextTO1=TextTO1 & CustomerEmail & "," & vbCrLf
End If
 rsCustomers.Close()
End If
next

'Response.Write(TextTO1)
%>
<%
TroubleList=Split(Request("TroubleList"),",") 

TextTO3=""

For each Customer in TroubleList
If Trim(Customer)<>"" Then
rsCustomers.Source = "SELECT CustomerID, CustomerName, TroubleMail FROM  dbo.Customers WHERE (TroubleMail <> N'') AND CustomerID=" & Customer
' Response.Write(rsCustomers.Source)
 
 rsCustomers.Open()

If NOT(rsCustomers.EOF AND rsCustomers.BOF) Then

TroubleMail=Replace(Trim(rsCustomers("TroubleMail"))," ",",") 
TroubleMail=Replace(TroubleMail,"; ",",")
TroubleMail=Replace(TroubleMail,",,",",")

TextTO3=TextTO3 & TroubleMail & "," & vbCrLf
End If
 rsCustomers.Close()
End If
next

'Response.Write(TextTO3)
%>

<%
ContactList=Split(Request("ContactList"),",") 

Dim rsContacts

Set rsContacts = Server.CreateObject("ADODB.Recordset")
rsContacts.ActiveConnection = MM_connection_STRING


TextTO2=""

For each Contact in ContactList
If Trim(Contact)<>"" Then
rsContacts.Source = "SELECT ContactID, CustomerID, ConTypeID, LastName, FirstName, Status, Email, Active FROM  dbo.Contacts WHERE (Email <> N'') AND ContactID=" & Contact
' Response.Write ("Contact= " & Contact & "</br>")
' Response.Write(rsContacts.Source)
 rsContacts.Open()

Email=Replace(Trim(rsContacts("Email"))," ",",") 
Email=Replace(Email,"; ",",")
Email=Replace(Email,",,",",")

TextTO2=TextTO2 & Email & "," & vbCrLf
 rsContacts.Close()
End If
next

 IF Request("AddFrom")<>"" Then

   TextTO2=TextTO2 & sAddFrom

 End If 
 

'Response.Write(TextTO2)
%>
<% ' Отправить письма 

  select case Request("SectionID")
'  application.
  case 3 ' инциденты
  sSubject = "[NSV_TTS][TT:"& Request("SubsectionID") & "][MSG] "
  case 6
  sSubject = "[NSV_TTS][ORD:"& Request("SubsectionID") & "][MSG] "
  case 10
  sSubject = "[NSV_TTS][WRK:"& Request("SubsectionID") & "][MSG] "
  case else
  sSubject = "[NSV_TTS][TTS:"& Request("SubsectionID") & "][MSG] "
  end select 

 

If Request("Send")<>""  or Request("SubmForm") <> "" Then
'Response.Write "OK"

sTo = CheckEMails(Request("Contacts"))
'sTo = "a.terehov@naukanet.ru"
' Response.Write(sTo & "</br>")


IF InStr(Request("Subject"),"[NSV_TTS]")>0 Then
  sSubject= Request("Subject")
Else
  sSubject= sSubject &  Request("Subject")
End If

sText= Request("Message")
sText = Replace(sText, "&quot;", """")
sText = Replace(sText, "&prime;",  "'")
sText = Replace(sText, "&lsaquo;",  "<")
sText = Replace(sText, "&rsaquo;",  ">")

sTitle = Request("Subject")
sMessage = Request("Message")



'If sTo<>"" AND InStr(sTo,"@")>2 Then
If sTo<>"" Then

 if Request("withFiles") = "" then
    sResult = SMTPSendMail (sTo, sFrom, sSubject, sText)
  end if
' Теперь все это еще добавим в историю инциденты/заявки
  
 sBody = "Клиенту отправлено сообщение:" & vbCrLf & _ 
 "Тема: " & Request.Form("Subject") & vbCrLf & _
 "От:   " & Request.Form("From") & vbCrLf & _
 "Кому: " & sTo & vbCrLf & _ 
 "Дата: " & Now() & vbCrLf & vbCrLf & sText


 sSubject = "Клиенту отправлено сообщение"

 

sTmpCst = LogCstMessage(Request("SectionID"), Request("SubsectionID"), sAuthor, sSubject, sBody,  Request("CustomerID"), 1, sTitle, sMessage, sTo, sFrom, MM_connection_STRING)

 sBody = "Клиенту отправлено сообщение:" & vbCrLf & _ 
 "Отправлено на: " & sTo & vbCrLf & _ 
 "Отправлено от: " & Request("From")

sTmp = LogMessage(Request("SectionID"), Request("SubsectionID"), sAuthor, sSubject, sBody, MM_connection_STRING)

 ''''' Доработка 5696. Сохранение информации о загруженных файлах в базу данных

  if Request("withFiles") <> "" then
      
      set conn = Server.CreateObject("ADODB.Connection")
       conn.ConnectionString = MM_connection_STRING
       conn.Open
       secID = Request("SectionID")
       ssecID = Request("SubsectionID") 
       fnamearr = split(Request("ShortName"),",")
       fcontarr = split(Request("contentLength"),",")
       ftypearr = split(Request("contentType"),",")

     for i = 0 to Ubound(fnamearr)
        sql = "insert into MailAttaches (MessageID, FilePath,Filename,ContentType, ContentLength, CreatedBy, CreatedAt) values (" & sTmpCst & ",'" &  server.mappath("../docs/data/sent/" & secID & "_" & ssecID & "/")   &  "','" & fnamearr(i) & "', '"  &  ftypearr(i) & "','" & fcontarr(i) & "', "  & Session("UserID") & ", getdate())" 
       conn.execute(sql)
     next
     conn.close
     set conn = nothing

  end if
  '''''

%>
     <script language="Javascript">
	    opener.location.reload(); 	self.close()
	</script>
<%
Else
%> <script type="text/javascript">
//  alert("Проверьте контакты")
</script>
<%
End If
End If
%>
<html><!-- #BeginTemplate "/Templates/TTSTemplateTopMenu.dwt" --><!-- DW6 -->
<head>
<!--#include virtual="/admin/ssi-Header.asp" -->
<!-- #BeginEditable "doctitle" -->
<LINK href="../style.css" rel=stylesheet type=text/css>
<title>Почтовая рассылка</title>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js" type="text/javascript"></script>
<script src="../javascript/jquery-ui.js" type="text/javascript"></script>
<link rel="stylesheet" href="../javascript/jquery-ui.css" />
<script src="https://code.jquery.com/jquery-latest.min.js" type="text/javascript"></script>
<script src="/javascript/jquery.simplemodal.js" type="text/javascript" ></script>
<!--<script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false"></script>-->
<style type="text/css">
    #simplemodal-overlay {background-color:#000;}
    #simplemodal-container {background-color:#999; border:1px solid gray; padding:12px; color:White;}
    #simplemodal-container a.modalCloseImg {
	background:url(/images/x.png) no-repeat; 
	width:25px;
	height:29px;
	display:inline;
	z-index:3200;
	position:absolute;
	top:-15px;
	right:-18px;
	cursor:pointer;
}
</style>
<script language="Javascript">

  $(document).ready(function () {

     $('#hidden_upload').load(function () { result = $('#hidden_upload').contents().find('div').text(); jsonProcess(); });
  
  });
   
    function CheckMail() {
        if ($('#From').val().indexOf("datalahti") > 0) {
            $('#Message').html('');
            $('#Message').html($('#textRucelcotele').val());

        }
        else {
            $('#Message').html('');
            $('#Message').html($('#textNormal').val());
        }
    }

    var rowNumber = 1;

    
    function addRow(button) {
      newRow = $(button).parent("div.addFile").clone();
      
      $(newRow).appendTo("#filerows");
      $(newRow).find($("input[type='file']")).val("");
      $(newRow).css("display", "table");
      ++rowNumber;
    }

   function delRow(button) {
      if (rowNumber > 1) {
        $(button).parent("div.addFile").remove();
        --rowNumber;
      }
    }

   
    var result, namesStr, guidsStr, shortNameStr, names, guids, contentTypes, contentLengths;

    result = "";
    namesStr = "";
    guidsStr = "";
    shortNameStr = "";
    contentTypeStr = "";
    contentLengthStr = "";
    function jsonProcess() {

      if (result != "") {
        var resArray = JSON.parse(result);



        if (resArray.Names.length > 0) {

          $.map(resArray.Names, function (val) {
            if (val != null) {

              namesStr = namesStr + "<span>" + val + "&nbsp;<img src='../images/b_drop.png' title='Удалить файл' style='cursor:pointer;'  Onclick='deleteAFile(\"" + val + "\")' /></span>" + ",";
              shortNameStr = shortNameStr + val + ",";
            }
          });

          names = namesStr.substring(0, namesStr.length - 1);
          shortNameStr = shortNameStr.substring(0, shortNameStr.length - 1);

          $.map(resArray.Guids, function (vl) {
            if (vl != null) {
              guidsStr = guidsStr + vl + ",";
            }
          });

          guids = guidsStr.substring(0, guidsStr.length - 1);


          $.map(resArray.ContentTypes, function (ct) {
            if (ct != null) {
              contentTypeStr = contentTypeStr + ct + ",";
            }
          });

          contentTypes = contentTypeStr.substring(0, contentTypeStr.length - 1);

          $.map(resArray.ContentLengths, function (cl) {
            if (cl != null) {
              contentLengthStr = contentLengthStr + cl.toString() + ",";
            }
          });

          contentLengths = contentLengthStr.substring(0, contentLengthStr.length - 1);


          if ($("#Names").val() == "") {
            $("#Names").val(names);
            $("#Guids").val(guids);
            $("#ShortName").val(shortNameStr);
            $("#contentType").val(contentTypes);
            $("#contentLength").val(contentLengths);
          }
          else {
            $("#Names").val($("#Names").val() + "," + names);
            $("#Guids").val($("#Guids").val() + "," + guids);
            $("#ShortName").val($("#ShortName").val() + "," + shortNameStr);
            $("#contentType").val($("#contentType").val() + "," + contentTypes);
            $("#contentLength").val($("#contentLength").val() + "," + contentLengths);
          }
          $("#fUploaded").val("true");

          $("#loadedFiles").html($("#Names").val());
          $("#incFiles").css("display", "block");
        }

      
        $("#FileBody").val("");
        namesStr = "";
        guidsStr = "";
        shortNameStr = "";
        contentTypeStr = "";
        contentLengthStr = "";
      }
    }


    function SendEmailWithFiles() {

      Sent = true;
      SubmContr = false;
      if ($("#fUploaded").val() != "") {

        $("#withFiles").val("true");

        var sTo, sFrom, sSubject, sText, Sent;

        sTo = $("#Contacts").val().replace(/\n/g, "").replace(/, ,/g, ",").replace(/,,/g, ",");
        
        if (sTo.slice(-1) == ",") {
          sTo = sTo.substring(0, sTo.length - 1);  
        }

        sFrom = $("#From").val();  
      

        var sectionID = parseInt($("#SectionID").val());
        switch (sectionID) {
          case 3:
              sSubject = "[NSV_TTS][TT:" + $("#SubsectionID").val() + "][MSG] ";
            break;
          case 6:
            sSubject = "[NSV_TTS][ORD:" + $("#SubsectionID").val() + "][MSG] ";
            break;
          case 10:
            sSubject = "[NSV_TTS][WRK:" + $("#SubsectionID").val() + "][MSG] ";
            break;
          default:
            sSubject = "[NSV_TTS][TTS:" + $("#SubsectionID").val() + "][MSG] ";
         }
    
	 sSubject = sSubject + $("#Subject").val();

        sText = $("#Message").val().replace(/&quot;/g, '"').replace(/&prime;/g, "'").replace(/&lsaquo;/g, "<").replace(/&rsaquo;/g, ">");
    
  
     try {
       $.ajax({
         url: "/apitts/API.asmx/SendMailWithFiles",
         type: "post",
         async: false,
         data: { "Names": $("#ShortName").val(), "Guids": $("#Guids").val(), "To": sTo, "From": sFrom, "Subject": sSubject, "Text": sText, "SectionID": $("#SectionID").val(), "SubsectionID": $("#SubsectionID").val() },
         cache: false,
         success: function (data) {
           var json = JSON.parse(data);
           if (json.code != 200) {
             alert(json.message);
             Sent = false;
           }
         }
       });
         }
         catch (e) {
           alert(e.Description);
           Sent = false;
         }

       }
    

       if ($("#fUploaded").val() != "" &&  Sent == false) {
         SubmContr = true;
       }

       if (SubmContr == false) {
         $("form[name='form1']").submit();
       }

     }

     function CheckUploadName() {

       var uploadName =  "" ;
       var duplControl = false;
       uploadName = $('#FileBody').val().replace(/\s/g, "_")
                                        .replace(/#/g, "_")
                                        .replace(/\$/g, "_")
                                        .replace(/'/g, "_")
                                        .replace(/\\/g, "_")
                                        .replace(/\</g, "_")
                                        .replace(/\>/g, "_")
                                        .replace(/\^/g, "_")
                                        .replace(/\&/g, "_")
                                        .replace(/@/g, "_")
                                        .replace(/\//g, "_")
                                        .replace(/\|/g, "_")
                                        .replace(/\?/g, "_")
                                        .replace(/\!/, "_");

       var contrArr = $("#ShortName").val().split(",");

       if ($.inArray(uploadName, contrArr) > -1 || $('#FileBody').val() == '') {

         alert("Такой файл уже есть!");
         $('#FileBody').val("");
         duplControl = true;
       }
      if (duplControl == false) {
         $("form[name='filesAttach']").submit();
         $('#FileBody').val("");
       }

     }

    
     function deleteAFile(name) {
       var i, guids, guid;
       var nameArr = $("#ShortName").val().split(",");
       i = nameArr.indexOf(name);
       guids = $("#Guids").val().split(",");
       guid = guids[i];
      
       // Удаление файла из временной папки
       try {
         $.ajax({
           url: "/apitts/API.asmx/deleteTempFile",
           type: "post",
           async: false,
           data: { "Guid": guid },
           cache: false,
           success: function (data) {
             var json = JSON.parse(data);
             if (json.code == 200) {

               // Удалить файл из списка и стереть его название со страницы ???

               newGuids = $("#Guids").val().split(",");
               newGuids.splice(i, 1);
               newGuids.toString();
               $("#Guids").val(newGuids);

               newNames = $("#Names").val().split(",");
               newNames.splice(i, 1);
               newNames.toString();
               $("#Names").val(newNames);

               newShortName = $("#ShortName").val().split(",");
               newShortName.splice(i, 1);
               newShortName.toString();
               $("#ShortName").val(newShortName);

               newcontentLength = $("#contentLength").val().split(",");
               newcontentLength.splice(i, 1);
               newcontentLength.toString();
               $("#contentLength").val(newcontentLength);


               newcontentType = $("#contentType").val().split(",");
               newcontentType.splice(i, 1);
               newcontentType.toString();
               $("#contentType").val(newcontentType);

          
               $("#loadedFiles").html($("#Names").val());

               //
             }
             else {
               alert(json.message);
             }
           }
         });
       }
       catch (e) {
         alert(e.Description);
       }
    }
    
</script>

<SCRIPT LANGUAGE="JavaScript">
// Проверка правильности заполнения e-mail:
// проверка по длинне
// проверка на наличие пробелов
// проверка на наличие "@" и отсуттствие 2-х "@"
// проверка точки перед расширением

function fnCheckEMail (cc){

s = document.getElementById(cc).value;
    document.MM_returnValue = true;
var ss = s.split(",");
for (var i in ss) {
var ss4 = (ss[i].substring(ss[i].length-5))
ss[i] = ss[i].trim()
ss[i] = ss[i].replace('/n','')
//alert (ss[i] +' '+ ss[i].length);
//alert(ss4)
//if(((ss[i].length>1)&&(ss[i].length<6))||(ss[i].indexOf(' ',1)>1)||(ss[i].indexOf('@')<1)||(ss[i].indexOf('@', ss[i].indexOf('@')+1)>0)||(ss4.indexOf('.')<0)){
if(((ss[i].length>1)&&(ss[i].length<6))||(ss[i].indexOf(' ',1)>1)||(ss[i].indexOf('@')<1)||(ss[i].indexOf('@', ss[i].indexOf('@')+1)>0)){
	
      alert ('Проверьте контакт ' + ss[i]);	
        document.MM_returnValue = false;
  }
//alert (document.MM_returnValue);	
//   document.write(document.MM_returnValue);
//    document.write("<br/>");
}
if (document.MM_returnValue == true) {
 
 javascript:SendEmailWithFiles(); 
 
 //        $("form[name='form1']").submit();
       }
}
</script>

<!-- #EndEditable -->
<!--#include virtual="/admin/ssi-JSFunctions.asp" -->
<body>
<TABLE border=0 cellPadding=8 cellSpacing=0 width="100%" >
  <TR> 
    <TD vAlign=top> 
	  <TABLE border=0 cellPadding=1 cellSpacing=2 width="100%">
		<tr class="noprint"> 
          <td><span class="header">
<!--#include virtual="/admin/ssi-TopMenuMain.asp" -->         
          </span>
		  </td>
        </tr>
        <TR>  
          <TD><!-- #BeginEditable "main" --> 
 <form method="POST" action="<%=MM_editAction%>" name="form1" id="MainForm" >
 <table border="0" cellpadding="1" cellspacing="1" class="tablebox" width="100%">
<tr class="article-top">
  <td colspan="2"><font size="+1">Почтовое сообщение</font></td></tr>
<tr class="boxtop">
  <td width="70%">
    <font size="+1">
	      <strong>ОТ:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</strong>
    </font> 
    <select name="From" id="From" style="width:220" onChange="javascript:CheckMail();">
      <option value="helpdesk@naukanet.ru">helpdesk@naukanet.ru</option>
      <option value="helpdesk@datalahti.fi">helpdesk@datalahti.fi</option> 
      <option value="<%=Session("UserEmail")%>"><%=Session("UserEmail")%></option>
    </select>
  </td>
  <td align="center" style="width:200px">
    <font size="+1">
      <strong> КОМУ:</strong>
    </font>
  </td>
</tr>
<tr class="boxtop"><td><font size="+1"> <strong>ТЕМА:&nbsp; </strong></font>
<input name="Subject" type="text" id="Subject" style="width:75%" value="<%=sTitleCopy%>"> </td> 
<td rowspan="4" style="width:200px"> 
<font color="#FF0000" size="-2">Важно!</br>
Адреса указываются через запятую.</br>
После последнего адреса запятая не ставится.</font>
<textarea name="Contacts" rows="30" id="Contacts" style="width:95%" title="Важно!
Адреса указываются через запятую.
После последнего адреса запятая не ставится."><%=CheckEMails(TextTO1 & TextTO3 & TextTO2)%></textarea>  
</td> </tr>
<tr class="boxtop"><td align="center"><font size="+1"> <strong>ТЕКСТ:&nbsp; </strong></font> </td> </tr>
<tr class="boxtop" align="center"><td> <textarea name="Message" rows="27" id="Message" style="width:99%"><%=sTextCopy%></textarea>
<input type="hidden" id="textRucelcotele" value="<%=sTextCopyRucelcotele%>" />
<input type="hidden" id="textNormal" value="<%=sTextCopy%>" />
<input type="hidden" id="Guids" value="" />
<input type="hidden" id="Names" value="" />
<input type="hidden" id="fUploaded" value="" />
<input type="hidden" id="withFiles" name="withFiles" value="" />
<input type="hidden" id="SectionID" name="SectionID" value="<%=Request("SectionID") %>" />
<input type="hidden" id="SubsectionID" name="SubsectionID" value="<%=Request("SubsectionID") %>" />
<input type="hidden" id="ShortName" name="ShortName" value="" />
<input type="hidden" id="contentLength"  name="contentLength" value="" />
<input type="hidden" id="contentType" name="contentType" value="" />
<input type="hidden" id="SubmForm" name="SubmForm" value="Yes" />

 </td> </tr>
<tr class="boxtop" align="center"><td colspan="2"> <input name="Send" type="button" id="Send" value="Отправить"  style="width:100" OnClick="fnCheckEMail('Contacts'); return document.MM_returnValue;"></td></tr>
<tr class="boxtop" id="incFiles" style="display:none;"><td><font size="+1"> <strong>Прикрепленные файлы:&nbsp; </strong></font>
<span id="loadedFiles"></span></td> </tr>
</form>
<tr  class="boxtop" align="center">
<td colspan="2"> 
<div id="addFile" >
<form action="/apitts/AttachFileToMail.aspx" method="post" enctype='multipart/form-data' accept-charset="UTF-8" target="hidden_upload" id="filesAttach" name="filesAttach">
<div id="filerows">
<div class="addFile" style="margin-left:10px;margin-top:10px;display:table;float:left;">Прикрепить файл:&nbsp;&nbsp;<input style="width:300px;" type="file" name="FileBody" id="FileBody" onChange="javascript: return CheckUploadName(); " /></div>
</div>
</form>
</div>
</td>
</tr>
</Table>
<iframe  name="hidden_upload" src=""  id="hidden_upload" style="width:0px;height:0px;border:1px solid black" ></iframe>
<!-- #EndEditable --></TD>
        </TR>
      </TABLE></TD>
  </TR> 
</TABLE>
<!--#include virtual="/admin/ssi-Footer.asp" -->        
</body>
<!-- #EndTemplate --></html>

<%
Set rsContacts=Nothing

%>
