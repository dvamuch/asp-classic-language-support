package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import java.util.Locale

internal object VbBuiltInSymbols {
    val globals = listOf(
        "Request", "Response", "Session", "Application", "Server", "ObjectContext", "Err", "WScript",
        "Array", "Asc", "AscB", "AscW", "CBool", "CByte", "CCur", "CDate", "CDbl", "Chr",
        "ChrB", "ChrW", "CInt", "CLng", "CSng", "CStr", "Date", "DateAdd", "DateDiff",
        "DatePart", "DateSerial", "DateValue", "Day", "Eval", "Filter", "FormatCurrency",
        "FormatDateTime", "FormatNumber", "FormatPercent", "GetLocale", "GetObject", "Hex",
        "Hour", "InStr", "InStrB", "InStrRev", "IsArray", "IsDate", "IsEmpty", "IsNull",
        "IsNumeric", "IsObject", "Join", "LBound", "LCase", "Left", "LeftB", "Len", "LenB",
        "Log", "LTrim", "Mid", "MidB", "Minute", "Month", "MonthName", "Now", "Oct",
        "Replace", "Right", "RightB", "Rnd", "Round", "RTrim", "Second", "SetLocale",
        "Space", "Split", "Sqr", "StrComp", "String", "StrReverse", "Time", "Timer",
        "TimeSerial", "TimeValue", "Trim", "TypeName", "UBound", "UCase", "VarType",
        "Weekday", "WeekdayName", "Year", "Abs", "Atn", "Cos", "Exp", "Fix", "Int",
        "CreateObject", "InputBox", "LoadPicture", "MsgBox", "Randomize", "RGB", "ScriptEngine",
        "ScriptEngineBuildVersion",
        "ScriptEngineMajorVersion", "ScriptEngineMinorVersion", "Sin", "Tan", "GetRef"
    )

    val members = mapOf(
        "request" to listOf(
            "BinaryRead", "ClientCertificate", "Cookies", "Form", "QueryString",
            "ServerVariables", "TotalBytes"
        ),
        "response" to listOf(
            "AddHeader", "AppendToLog", "BinaryWrite", "Buffer", "CacheControl", "Charset",
            "Clear", "ContentType", "Cookies", "End", "Expires", "ExpiresAbsolute", "Flush",
            "IsClientConnected", "PICS", "Redirect", "Status", "Write"
        ),
        "server" to listOf(
            "CreateObject", "Execute", "GetLastError", "HTMLEncode", "MapPath", "ScriptTimeout",
            "Transfer", "URLEncode"
        ),
        "session" to listOf(
            "Abandon", "CodePage", "Contents", "LCID", "SessionID", "StaticObjects", "Timeout"
        ),
        "application" to listOf("Contents", "Lock", "StaticObjects", "Unlock"),
        "objectcontext" to listOf("SetAbort", "SetComplete"),
        "err" to listOf("Clear", "Description", "HelpContext", "HelpFile", "Number", "Raise", "Source"),
        "wscript" to listOf(
            "Arguments", "CreateObject", "DisconnectObject", "Echo", "FullName", "GetObject",
            "Interactive", "Name", "Path", "Quit", "ScriptFullName", "ScriptName", "Sleep",
            "StdErr", "StdIn", "StdOut", "Version"
        )
    )

    private val constants = setOf(
        "vbBlack", "vbRed", "vbGreen", "vbYellow", "vbBlue", "vbMagenta", "vbCyan", "vbWhite",
        "vbCr", "vbCrLf", "vbFormFeed", "vbLf", "vbNewLine", "vbNullChar", "vbNullString",
        "vbTab", "vbVerticalTab", "vbEmpty", "vbNull", "vbInteger", "vbLong", "vbSingle",
        "vbDouble", "vbCurrency", "vbDate", "vbString", "vbObject", "vbError", "vbBoolean",
        "vbVariant", "vbDataObject", "vbDecimal", "vbByte", "vbArray", "vbUseDefault",
        "vbTrue", "vbFalse", "vbBinaryCompare", "vbTextCompare", "vbDatabaseCompare",
        "vbSunday", "vbMonday", "vbTuesday", "vbWednesday", "vbThursday", "vbFriday",
        "vbSaturday", "vbUseSystemDayOfWeek", "vbFirstJan1", "vbFirstFourDays",
        "vbFirstFullWeek", "vbGeneralDate", "vbLongDate", "vbShortDate", "vbLongTime",
        "vbShortTime", "vbOKOnly", "vbOKCancel", "vbAbortRetryIgnore", "vbYesNoCancel",
        "vbYesNo", "vbRetryCancel", "vbCritical", "vbQuestion", "vbExclamation",
        "vbInformation", "vbDefaultButton1", "vbDefaultButton2", "vbDefaultButton3",
        "vbDefaultButton4", "vbApplicationModal", "vbSystemModal", "vbOK", "vbCancel",
        "vbMsgBoxHelpButton", "vbMsgBoxSetForeground", "vbMsgBoxRight", "vbMsgBoxRtlReading",
        "vbAbort", "vbRetry", "vbIgnore", "vbYes", "vbNo", "vbObjectError", "vbNormal", "vbReadOnly",
        "vbHidden", "vbSystem", "vbVolume", "vbDirectory", "vbArchive", "vbAlias",
        "ForReading", "ForWriting", "ForAppending", "TristateUseDefault", "TristateTrue",
        "TristateFalse"
    ).mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }

    private val normalizedGlobals = globals.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }

    fun isKnownGlobal(name: String): Boolean {
        val normalized = name.lowercase(Locale.ROOT)
        return normalized in normalizedGlobals || normalized in constants || normalized == "me" || normalized == "regexp"
    }
}
