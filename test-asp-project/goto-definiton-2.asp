<%
Set rsBug = Server.CreateObject("ADODB.Recordset")
rsBug.ActiveConnection = MM_connection_STRING
rsBug.Source = "SELECT Bugs.*, isnull(Bugs.EstimatedTime,0) as EstimatedTime, isnull(Bugs.PercentOfDone,0) as PercentOfDone, Systems.SystemName, TroubleLevels.TroubleLevelName, BugStatuses.TroubleStatusName, Responsibles.LastName as ResponsibleName, Responsibles.EMail as EMail, OPeners.LastName as OpenerName, Updaters.LastName as UpdaterName, Managers.LastName as ManagerName, Bt.BugTypeName, BV.BugVersionName, BP.BP_Name  FROM dbo.Bugs left Join Systems On Bugs.SystemID=Systems.SystemID Left Join TroubleLevels On Bugs.TroubleLevelID=TroubleLevels.TroubleLevelID Left Join BugStatuses On Bugs.TroubleStatusID=BugStatuses.TroubleStatusID Left Join vwUsersFIO as Responsibles On Bugs.ResponsibleID=Responsibles.UserID Left Join vwUsersFIO as Openers On Bugs.CreatedBy=Openers.UserID Left Join vwUsersFIO as Updaters On Bugs.UpdatedBy=Updaters.UserID Left Join vwUsersFIO as Managers On Bugs.ManagerID=Managers.UserID Left Join BugTypes BT on Bugs.BugTypeID=BT.BugTypeID Left Join BugsVersions BV On Bugs.BugVersionID=BV.BugVersionID left join dbo.BizProcessesList BP on BP.BP_ID = Bugs.BizProcessID WHERE Bugs.BugID = " & qsBugId
rsBug.CursorType = 0
rsBug.CursorLocation = 3
rsBug.LockType = 1
rsBug.Open()
%>