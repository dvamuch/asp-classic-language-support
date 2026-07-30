package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import java.util.Locale

internal enum class VbObjectMemberKind(val label: String) {
    PROPERTY("property"),
    METHOD("method"),
    COLLECTION("collection")
}

internal data class VbObjectParameter(
    val name: String,
    val typeText: String? = null,
    val optional: Boolean = false
) {
    fun presentation(): String {
        val value = if (typeText == null) name else "$name As $typeText"
        return if (optional) "[$value]" else value
    }
}

internal data class VbObjectMember(
    val name: String,
    val kind: VbObjectMemberKind,
    val returnType: String? = null,
    val parameters: List<VbObjectParameter>? = null
) {
    fun signatureText(): String? {
        return parameters?.joinToString(prefix = "(", postfix = ")") { it.presentation() }
    }
}

internal data class VbObjectType(
    val id: String,
    val displayName: String,
    val aliases: Set<String>,
    val members: List<VbObjectMember>
) {
    fun member(name: String): VbObjectMember? {
        return members.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}

/**
 * A deliberately finite catalog of documented COM objects commonly used by Classic ASP code.
 * Custom and third-party ProgIDs stay unknown instead of receiving guessed members.
 */
internal object VbComTypeCatalog {
    private val types = listOf(
        objectType(
            id = "ado.connection",
            displayName = "ADO Connection",
            aliases = setOf("ADODB.Connection"),
            properties = listOf(
                "Attributes", "CommandTimeout", "ConnectionString", "ConnectionTimeout",
                "CursorLocation", "DefaultDatabase", "IsolationLevel", "Mode", "Provider",
                "State", "Version"
            ),
            collections = mapOf("Errors" to "ado.errors", "Properties" to null),
            methods = mapOf(
                "BeginTrans" to null,
                "Cancel" to null,
                "Close" to null,
                "CommitTrans" to null,
                "Execute" to "ado.recordset",
                "Open" to null,
                "OpenSchema" to "ado.recordset",
                "RollbackTrans" to null
            ),
            methodParameters = mapOf(
                "Execute" to listOf(
                    required("CommandText", "String"),
                    optional("RecordsAffected", "Long"),
                    optional("Options", "Long")
                ),
                "Open" to listOf(
                    optional("ConnectionString", "String"),
                    optional("UserID", "String"),
                    optional("Password", "String"),
                    optional("Options", "Long")
                ),
                "OpenSchema" to listOf(
                    required("QueryType", "SchemaEnum"),
                    optional("Criteria", "Variant"),
                    optional("SchemaID", "String")
                )
            )
        ),
        objectType(
            id = "ado.command",
            displayName = "ADO Command",
            aliases = setOf("ADODB.Command"),
            properties = listOf(
                "ActiveConnection", "CommandStream", "CommandText", "CommandTimeout",
                "CommandType", "Dialect", "Name", "NamedParameters", "Prepared", "State"
            ),
            collections = mapOf("Parameters" to "ado.parameters", "Properties" to null),
            methods = mapOf(
                "Cancel" to null,
                "CreateParameter" to "ado.parameter",
                "Execute" to "ado.recordset"
            ),
            methodParameters = mapOf(
                "CreateParameter" to listOf(
                    optional("Name", "String"),
                    optional("Type", "DataTypeEnum"),
                    optional("Direction", "ParameterDirectionEnum"),
                    optional("Size", "Long"),
                    optional("Value", "Variant")
                ),
                "Execute" to listOf(
                    optional("RecordsAffected", "Long"),
                    optional("Parameters", "Variant"),
                    optional("Options", "Long")
                )
            )
        ),
        objectType(
            id = "ado.recordset",
            displayName = "ADO Recordset",
            aliases = setOf("ADODB.Recordset"),
            properties = listOf(
                "AbsolutePage", "AbsolutePosition", "ActiveCommand", "ActiveConnection", "BOF",
                "Bookmark", "CacheSize", "CursorLocation", "CursorType", "DataMember", "DataSource",
                "EditMode", "EOF", "Filter", "Index", "LockType", "MarshalOptions", "MaxRecords",
                "PageCount", "PageSize", "RecordCount", "Sort", "Source", "State", "Status",
                "StayInSync"
            ),
            collections = mapOf("Fields" to "ado.fields", "Properties" to null),
            methods = mapOf(
                "AddNew" to null,
                "Cancel" to null,
                "CancelBatch" to null,
                "CancelUpdate" to null,
                "Clone" to "ado.recordset",
                "Close" to null,
                "CompareBookmarks" to null,
                "Delete" to null,
                "Find" to null,
                "GetRows" to null,
                "GetString" to null,
                "Move" to null,
                "MoveFirst" to null,
                "MoveLast" to null,
                "MoveNext" to null,
                "MovePrevious" to null,
                "NextRecordset" to "ado.recordset",
                "Open" to null,
                "Requery" to null,
                "Resync" to null,
                "Save" to null,
                "Seek" to null,
                "Supports" to null,
                "Update" to null,
                "UpdateBatch" to null
            ),
            methodParameters = mapOf(
                "AddNew" to listOf(optional("FieldList", "Variant"), optional("Values", "Variant")),
                "Delete" to listOf(optional("AffectRecords", "AffectEnum")),
                "Find" to listOf(
                    required("Criteria", "String"),
                    optional("SkipRecords", "Long"),
                    optional("SearchDirection", "SearchDirectionEnum"),
                    optional("Start", "Variant")
                ),
                "GetRows" to listOf(
                    optional("Rows", "Long"),
                    optional("Start", "Variant"),
                    optional("Fields", "Variant")
                ),
                "GetString" to listOf(
                    optional("StringFormat", "StringFormatEnum"),
                    optional("NumRows", "Long"),
                    optional("ColumnDelimiter", "String"),
                    optional("RowDelimiter", "String"),
                    optional("NullExpr", "String")
                ),
                "Move" to listOf(required("NumRecords", "Long"), optional("Start", "Variant")),
                "NextRecordset" to listOf(optional("RecordsAffected", "Long")),
                "Open" to listOf(
                    optional("Source", "Variant"),
                    optional("ActiveConnection", "Variant"),
                    optional("CursorType", "CursorTypeEnum"),
                    optional("LockType", "LockTypeEnum"),
                    optional("Options", "Long")
                ),
                "Requery" to listOf(optional("Options", "Long")),
                "Save" to listOf(
                    optional("Destination", "Variant"),
                    optional("PersistFormat", "PersistFormatEnum")
                ),
                "Seek" to listOf(
                    required("KeyValues", "Variant"),
                    optional("SeekOption", "SeekEnum")
                ),
                "Supports" to listOf(required("CursorOptions", "CursorOptionEnum")),
                "Update" to listOf(optional("Fields", "Variant"), optional("Values", "Variant"))
            )
        ),
        objectType(
            id = "ado.stream",
            displayName = "ADO Stream",
            aliases = setOf("ADODB.Stream"),
            properties = listOf(
                "Charset", "EOS", "LineSeparator", "Mode", "Position", "Size", "State", "Type"
            ),
            methods = mapOf(
                "Cancel" to null,
                "Close" to null,
                "CopyTo" to null,
                "Flush" to null,
                "LoadFromFile" to null,
                "Open" to null,
                "Read" to null,
                "ReadText" to null,
                "SaveToFile" to null,
                "SetEOS" to null,
                "SkipLine" to null,
                "Write" to null,
                "WriteText" to null
            ),
            methodParameters = mapOf(
                "CopyTo" to listOf(
                    required("Dest", "Stream"),
                    optional("CharNumber", "Long")
                ),
                "LoadFromFile" to listOf(required("FileName", "String")),
                "Open" to listOf(
                    optional("Source", "Variant"),
                    optional("Mode", "ConnectModeEnum"),
                    optional("OpenOptions", "StreamOpenOptionsEnum"),
                    optional("UserName", "String"),
                    optional("Password", "String")
                ),
                "Read" to listOf(optional("NumBytes", "Long")),
                "ReadText" to listOf(optional("NumChars", "Long")),
                "SaveToFile" to listOf(
                    required("FileName", "String"),
                    optional("SaveOptions", "SaveOptionsEnum")
                ),
                "Write" to listOf(required("Buffer", "Variant")),
                "WriteText" to listOf(
                    required("Data", "String"),
                    optional("Options", "StreamWriteEnum")
                )
            )
        ),
        objectType(
            id = "ado.parameter",
            displayName = "ADO Parameter",
            properties = listOf(
                "Attributes", "Direction", "Name", "NumericScale", "Precision", "Size", "Type", "Value"
            ),
            collections = mapOf("Properties" to null),
            methods = mapOf("AppendChunk" to null),
            methodParameters = mapOf("AppendChunk" to listOf(required("Value", "Variant")))
        ),
        objectType(
            id = "ado.parameters",
            displayName = "ADO Parameters",
            properties = listOf("Count"),
            propertiesWithTypes = mapOf("Item" to "ado.parameter"),
            methods = mapOf("Append" to null, "Delete" to null, "Refresh" to null),
            methodParameters = mapOf(
                "Append" to listOf(required("Object", "Parameter")),
                "Delete" to listOf(required("Index", "Variant"))
            )
        ),
        objectType(
            id = "ado.field",
            displayName = "ADO Field",
            properties = listOf(
                "ActualSize", "Attributes", "DataFormat", "DefinedSize", "Name", "NumericScale",
                "OriginalValue", "Precision", "Status", "Type", "UnderlyingValue", "Value"
            ),
            collections = mapOf("Properties" to null),
            methods = mapOf("AppendChunk" to null, "GetChunk" to null),
            methodParameters = mapOf(
                "AppendChunk" to listOf(required("Data", "Variant")),
                "GetChunk" to listOf(required("Length", "Long"))
            )
        ),
        objectType(
            id = "ado.fields",
            displayName = "ADO Fields",
            properties = listOf("Count"),
            propertiesWithTypes = mapOf("Item" to "ado.field"),
            methods = mapOf(
                "Append" to null,
                "CancelUpdate" to null,
                "Delete" to null,
                "Refresh" to null,
                "Resync" to null,
                "Update" to null
            )
        ),
        objectType(
            id = "ado.error",
            displayName = "ADO Error",
            properties = listOf(
                "Description", "HelpContext", "HelpFile", "NativeError", "Number", "Source", "SQLState"
            )
        ),
        objectType(
            id = "ado.errors",
            displayName = "ADO Errors",
            properties = listOf("Count"),
            propertiesWithTypes = mapOf("Item" to "ado.error"),
            methods = mapOf("Clear" to null, "Refresh" to null)
        ),
        objectType(
            id = "scripting.dictionary",
            displayName = "Scripting Dictionary",
            aliases = setOf("Scripting.Dictionary"),
            properties = listOf("CompareMode", "Count", "Item", "Key"),
            methods = mapOf(
                "Add" to null,
                "Exists" to null,
                "Items" to null,
                "Keys" to null,
                "Remove" to null,
                "RemoveAll" to null
            ),
            methodParameters = mapOf(
                "Add" to listOf(required("Key", "Variant"), required("Item", "Variant")),
                "Exists" to listOf(required("Key", "Variant")),
                "Remove" to listOf(required("Key", "Variant"))
            )
        ),
        objectType(
            id = "scripting.filesystemobject",
            displayName = "Scripting FileSystemObject",
            aliases = setOf("Scripting.FileSystemObject"),
            collections = mapOf("Drives" to null),
            methods = mapOf(
                "BuildPath" to null,
                "CopyFile" to null,
                "CopyFolder" to null,
                "CreateFolder" to "scripting.folder",
                "CreateTextFile" to "scripting.textstream",
                "DeleteFile" to null,
                "DeleteFolder" to null,
                "DriveExists" to null,
                "FileExists" to null,
                "FolderExists" to null,
                "GetAbsolutePathName" to null,
                "GetBaseName" to null,
                "GetDrive" to "scripting.drive",
                "GetDriveName" to null,
                "GetExtensionName" to null,
                "GetFile" to "scripting.file",
                "GetFileName" to null,
                "GetFolder" to "scripting.folder",
                "GetParentFolderName" to null,
                "GetSpecialFolder" to "scripting.folder",
                "GetTempName" to null,
                "MoveFile" to null,
                "MoveFolder" to null,
                "OpenTextFile" to "scripting.textstream"
            ),
            methodParameters = mapOf(
                "BuildPath" to listOf(required("Path", "String"), required("Name", "String")),
                "CopyFile" to listOf(
                    required("Source", "String"),
                    required("Destination", "String"),
                    optional("Overwrite", "Boolean")
                ),
                "CopyFolder" to listOf(
                    required("Source", "String"),
                    required("Destination", "String"),
                    optional("Overwrite", "Boolean")
                ),
                "CreateFolder" to listOf(required("FolderSpec", "String")),
                "CreateTextFile" to listOf(
                    required("FileName", "String"),
                    optional("Overwrite", "Boolean"),
                    optional("Unicode", "Boolean")
                ),
                "DeleteFile" to listOf(required("FileSpec", "String"), optional("Force", "Boolean")),
                "DeleteFolder" to listOf(required("FolderSpec", "String"), optional("Force", "Boolean")),
                "DriveExists" to listOf(required("DriveSpec", "String")),
                "FileExists" to listOf(required("FileSpec", "String")),
                "FolderExists" to listOf(required("FolderSpec", "String")),
                "GetAbsolutePathName" to listOf(required("PathSpec", "String")),
                "GetBaseName" to listOf(required("Path", "String")),
                "GetDrive" to listOf(required("DriveSpec", "String")),
                "GetDriveName" to listOf(required("Path", "String")),
                "GetExtensionName" to listOf(required("Path", "String")),
                "GetFile" to listOf(required("FileSpec", "String")),
                "GetFileName" to listOf(required("PathSpec", "String")),
                "GetFolder" to listOf(required("FolderSpec", "String")),
                "GetParentFolderName" to listOf(required("Path", "String")),
                "GetSpecialFolder" to listOf(required("FolderSpec", "SpecialFolderConst")),
                "MoveFile" to listOf(required("Source", "String"), required("Destination", "String")),
                "MoveFolder" to listOf(required("Source", "String"), required("Destination", "String")),
                "OpenTextFile" to listOf(
                    required("FileName", "String"),
                    optional("IOMode", "IOMode"),
                    optional("Create", "Boolean"),
                    optional("Format", "Tristate")
                )
            )
        ),
        objectType(
            id = "scripting.textstream",
            displayName = "Scripting TextStream",
            properties = listOf("AtEndOfLine", "AtEndOfStream", "Column", "Line"),
            methods = mapOf(
                "Close" to null,
                "Read" to null,
                "ReadAll" to null,
                "ReadLine" to null,
                "Skip" to null,
                "SkipLine" to null,
                "Write" to null,
                "WriteBlankLines" to null,
                "WriteLine" to null
            ),
            methodParameters = mapOf(
                "Read" to listOf(required("Characters", "Long")),
                "Skip" to listOf(required("Characters", "Long")),
                "Write" to listOf(required("String", "String")),
                "WriteBlankLines" to listOf(required("Lines", "Long")),
                "WriteLine" to listOf(optional("String", "String"))
            )
        ),
        objectType(
            id = "scripting.file",
            displayName = "Scripting File",
            properties = listOf(
                "Attributes", "DateCreated", "DateLastAccessed", "DateLastModified", "Drive", "Name",
                "ParentFolder", "Path", "ShortName", "ShortPath", "Size", "Type"
            ),
            methods = mapOf(
                "Copy" to null,
                "Delete" to null,
                "Move" to null,
                "OpenAsTextStream" to "scripting.textstream"
            ),
            methodParameters = mapOf(
                "Copy" to listOf(required("Destination", "String"), optional("Overwrite", "Boolean")),
                "Delete" to listOf(optional("Force", "Boolean")),
                "Move" to listOf(required("Destination", "String")),
                "OpenAsTextStream" to listOf(
                    optional("IOMode", "IOMode"),
                    optional("Format", "Tristate")
                )
            )
        ),
        objectType(
            id = "scripting.folder",
            displayName = "Scripting Folder",
            properties = listOf(
                "Attributes", "DateCreated", "DateLastAccessed", "DateLastModified", "Drive", "Files",
                "IsRootFolder", "Name", "ParentFolder", "Path", "ShortName", "ShortPath", "Size",
                "SubFolders", "Type"
            ),
            methods = mapOf(
                "Copy" to null,
                "CreateTextFile" to "scripting.textstream",
                "Delete" to null,
                "Move" to null
            ),
            methodParameters = mapOf(
                "Copy" to listOf(required("Destination", "String"), optional("Overwrite", "Boolean")),
                "CreateTextFile" to listOf(
                    required("FileName", "String"),
                    optional("Overwrite", "Boolean"),
                    optional("Unicode", "Boolean")
                ),
                "Delete" to listOf(optional("Force", "Boolean")),
                "Move" to listOf(required("Destination", "String"))
            )
        ),
        objectType(
            id = "scripting.drive",
            displayName = "Scripting Drive",
            properties = listOf(
                "AvailableSpace", "DriveLetter", "DriveType", "FileSystem", "FreeSpace", "IsReady",
                "Path", "RootFolder", "SerialNumber", "ShareName", "TotalSize", "VolumeName"
            )
        ),
        objectType(
            id = "msxml.document",
            displayName = "MSXML DOMDocument",
            aliases = setOf(
                "Microsoft.XMLDOM", "MSXML2.DOMDocument", "MSXML2.DOMDocument.3.0", "MSXML2.DOMDocument.6.0"
            ),
            properties = listOf(
                "async", "doctype", "implementation", "nodeName", "nodeType", "nodeValue", "parsed",
                "preserveWhiteSpace", "readyState", "resolveExternals", "url", "validateOnParse", "xml"
            ),
            propertiesWithTypes = mapOf(
                "documentElement" to "msxml.node",
                "parseError" to "msxml.parseerror"
            ),
            methods = mapOf(
                "abort" to null,
                "appendChild" to "msxml.node",
                "cloneNode" to "msxml.node",
                "createAttribute" to "msxml.node",
                "createCDATASection" to "msxml.node",
                "createComment" to "msxml.node",
                "createElement" to "msxml.node",
                "createNode" to "msxml.node",
                "createProcessingInstruction" to "msxml.node",
                "createTextNode" to "msxml.node",
                "getElementsByTagName" to "msxml.nodelist",
                "load" to null,
                "loadXML" to null,
                "save" to null,
                "selectNodes" to "msxml.nodelist",
                "selectSingleNode" to "msxml.node",
                "transformNode" to null,
                "validate" to "msxml.parseerror"
            ),
            methodParameters = mapOf(
                "appendChild" to listOf(required("NewChild", "IXMLDOMNode")),
                "cloneNode" to listOf(required("Deep", "Boolean")),
                "createAttribute" to listOf(required("Name", "String")),
                "createCDATASection" to listOf(required("Data", "String")),
                "createComment" to listOf(required("Data", "String")),
                "createElement" to listOf(required("TagName", "String")),
                "createNode" to listOf(
                    required("Type", "Variant"),
                    required("Name", "String"),
                    required("NamespaceURI", "String")
                ),
                "createProcessingInstruction" to listOf(
                    required("Target", "String"),
                    required("Data", "String")
                ),
                "createTextNode" to listOf(required("Data", "String")),
                "getElementsByTagName" to listOf(required("TagName", "String")),
                "load" to listOf(required("XMLSource", "Variant")),
                "loadXML" to listOf(required("XML", "String")),
                "save" to listOf(required("Destination", "Variant")),
                "selectNodes" to listOf(required("QueryString", "String")),
                "selectSingleNode" to listOf(required("QueryString", "String")),
                "transformNode" to listOf(required("Stylesheet", "IXMLDOMNode"))
            )
        ),
        objectType(
            id = "msxml.node",
            displayName = "MSXML DOM Node",
            properties = listOf(
                "attributes", "baseName", "dataType", "namespaceURI", "nodeName", "nodeType", "nodeValue",
                "ownerDocument", "parentNode", "parsed", "prefix", "specified", "text", "xml"
            ),
            propertiesWithTypes = mapOf(
                "childNodes" to "msxml.nodelist",
                "firstChild" to "msxml.node",
                "lastChild" to "msxml.node",
                "nextSibling" to "msxml.node",
                "previousSibling" to "msxml.node"
            ),
            methods = mapOf(
                "appendChild" to "msxml.node",
                "cloneNode" to "msxml.node",
                "hasChildNodes" to null,
                "insertBefore" to "msxml.node",
                "removeChild" to "msxml.node",
                "replaceChild" to "msxml.node",
                "selectNodes" to "msxml.nodelist",
                "selectSingleNode" to "msxml.node",
                "transformNode" to null
            ),
            methodParameters = mapOf(
                "appendChild" to listOf(required("NewChild", "IXMLDOMNode")),
                "cloneNode" to listOf(required("Deep", "Boolean")),
                "insertBefore" to listOf(
                    required("NewChild", "IXMLDOMNode"),
                    required("RefChild", "Variant")
                ),
                "removeChild" to listOf(required("ChildNode", "IXMLDOMNode")),
                "replaceChild" to listOf(
                    required("NewChild", "IXMLDOMNode"),
                    required("OldChild", "IXMLDOMNode")
                ),
                "selectNodes" to listOf(required("QueryString", "String")),
                "selectSingleNode" to listOf(required("QueryString", "String")),
                "transformNode" to listOf(required("Stylesheet", "IXMLDOMNode"))
            )
        ),
        objectType(
            id = "msxml.nodelist",
            displayName = "MSXML DOM NodeList",
            properties = listOf("length"),
            propertiesWithTypes = mapOf("item" to "msxml.node"),
            methods = mapOf("nextNode" to "msxml.node", "reset" to null)
        ),
        objectType(
            id = "msxml.parseerror",
            displayName = "MSXML ParseError",
            properties = listOf("errorCode", "filepos", "line", "linepos", "reason", "srcText", "url")
        ),
        objectType(
            id = "msxml.serverxmlhttp",
            displayName = "MSXML ServerXMLHTTP",
            aliases = setOf(
                "MSXML2.ServerXMLHTTP", "MSXML2.ServerXMLHTTP.3.0", "MSXML2.ServerXMLHTTP.6.0"
            ),
            properties = listOf(
                "readyState", "responseBody", "responseStream", "responseText", "status", "statusText"
            ),
            propertiesWithTypes = mapOf("responseXML" to "msxml.document"),
            methods = mapOf(
                "abort" to null,
                "getAllResponseHeaders" to null,
                "getOption" to null,
                "getResponseHeader" to null,
                "open" to null,
                "send" to null,
                "setOption" to null,
                "setProxy" to null,
                "setProxyCredentials" to null,
                "setRequestHeader" to null,
                "setTimeouts" to null,
                "waitForResponse" to null
            ),
            methodParameters = mapOf(
                "getOption" to listOf(required("Option", "ServerXMLHTTPOptionEnum")),
                "getResponseHeader" to listOf(required("Header", "String")),
                "open" to listOf(
                    required("Method", "String"),
                    required("URL", "String"),
                    optional("Async", "Boolean"),
                    optional("User", "String"),
                    optional("Password", "String")
                ),
                "send" to listOf(optional("Body", "Variant")),
                "setOption" to listOf(
                    required("Option", "ServerXMLHTTPOptionEnum"),
                    required("Value", "Variant")
                ),
                "setProxy" to listOf(
                    required("ProxySetting", "SXH_PROXY_SETTING"),
                    optional("ProxyServer", "Variant"),
                    optional("BypassList", "Variant")
                ),
                "setProxyCredentials" to listOf(
                    required("UserName", "String"),
                    required("Password", "String")
                ),
                "setRequestHeader" to listOf(
                    required("Header", "String"),
                    required("Value", "String")
                ),
                "setTimeouts" to listOf(
                    required("ResolveTimeout", "Long"),
                    required("ConnectTimeout", "Long"),
                    required("SendTimeout", "Long"),
                    required("ReceiveTimeout", "Long")
                ),
                "waitForResponse" to listOf(optional("TimeoutInSeconds", "Variant"))
            )
        ),
        objectType(
            id = "winhttp.request",
            displayName = "WinHTTP Request",
            aliases = setOf("WinHttp.WinHttpRequest.5.1"),
            properties = listOf(
                "Option", "ResponseBody", "ResponseStream", "ResponseText", "Status", "StatusText"
            ),
            methods = mapOf(
                "Abort" to null,
                "GetAllResponseHeaders" to null,
                "GetResponseHeader" to null,
                "Open" to null,
                "Send" to null,
                "SetAutoLogonPolicy" to null,
                "SetClientCertificate" to null,
                "SetCredentials" to null,
                "SetProxy" to null,
                "SetRequestHeader" to null,
                "SetTimeouts" to null,
                "WaitForResponse" to null
            ),
            methodParameters = mapOf(
                "GetResponseHeader" to listOf(required("Header", "String")),
                "Open" to listOf(
                    required("Method", "String"),
                    required("URL", "String"),
                    optional("Async", "Boolean")
                ),
                "Send" to listOf(optional("Body", "Variant")),
                "SetAutoLogonPolicy" to listOf(required("Policy", "AutoLogonPolicy")),
                "SetClientCertificate" to listOf(required("ClientCertificate", "String")),
                "SetCredentials" to listOf(
                    required("UserName", "String"),
                    required("Password", "String"),
                    required("Flags", "HTTPREQUEST_SETCREDENTIALS_FLAGS")
                ),
                "SetProxy" to listOf(
                    required("ProxySetting", "HTTPREQUEST_PROXY_SETTING"),
                    optional("ProxyServer", "Variant"),
                    optional("BypassList", "Variant")
                ),
                "SetRequestHeader" to listOf(
                    required("Header", "String"),
                    required("Value", "String")
                ),
                "SetTimeouts" to listOf(
                    required("ResolveTimeout", "Long"),
                    required("ConnectTimeout", "Long"),
                    required("SendTimeout", "Long"),
                    required("ReceiveTimeout", "Long")
                ),
                "WaitForResponse" to listOf(optional("Timeout", "Variant"))
            )
        ),
        objectType(
            id = "vbscript.regexp",
            displayName = "VBScript RegExp",
            aliases = setOf("VBScript.RegExp", "RegExp"),
            properties = listOf("Global", "IgnoreCase", "Multiline", "Pattern"),
            methods = mapOf(
                "Execute" to "vbscript.matches",
                "Replace" to null,
                "Test" to null
            ),
            methodParameters = mapOf(
                "Execute" to listOf(required("SourceString", "String")),
                "Replace" to listOf(
                    required("SourceString", "String"),
                    required("ReplaceString", "String")
                ),
                "Test" to listOf(required("SourceString", "String"))
            )
        ),
        objectType(
            id = "vbscript.matches",
            displayName = "VBScript MatchCollection",
            properties = listOf("Count"),
            propertiesWithTypes = mapOf("Item" to "vbscript.match")
        ),
        objectType(
            id = "vbscript.match",
            displayName = "VBScript Match",
            properties = listOf("FirstIndex", "Length", "SubMatches", "Value")
        )
    )

    private val typesById = types.associateBy { it.id }
    private val typesByAlias = buildMap {
        types.forEach { type ->
            type.aliases.forEach { alias -> put(normalize(alias), type) }
        }
    }

    fun byProgId(progId: String): VbObjectType? = typesByAlias[normalize(progId)]

    fun byNewExpression(className: String): VbObjectType? = typesByAlias[normalize(className)]

    fun byId(id: String): VbObjectType? = typesById[id]

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
}

private fun objectType(
    id: String,
    displayName: String,
    aliases: Set<String> = emptySet(),
    properties: List<String> = emptyList(),
    propertiesWithTypes: Map<String, String?> = emptyMap(),
    collections: Map<String, String?> = emptyMap(),
    methods: Map<String, String?> = emptyMap(),
    methodParameters: Map<String, List<VbObjectParameter>> = emptyMap()
): VbObjectType {
    val members = buildList {
        properties.forEach { add(VbObjectMember(it, VbObjectMemberKind.PROPERTY)) }
        propertiesWithTypes.forEach { (name, returnType) ->
            add(VbObjectMember(name, VbObjectMemberKind.PROPERTY, returnType))
        }
        collections.forEach { (name, returnType) ->
            add(VbObjectMember(name, VbObjectMemberKind.COLLECTION, returnType))
        }
        methods.forEach { (name, returnType) ->
            add(VbObjectMember(name, VbObjectMemberKind.METHOD, returnType, methodParameters[name]))
        }
    }.sortedBy { it.name.lowercase(Locale.ROOT) }
    return VbObjectType(id, displayName, aliases, members)
}

private fun required(name: String, typeText: String? = null) =
    VbObjectParameter(name, typeText, optional = false)

private fun optional(name: String, typeText: String? = null) =
    VbObjectParameter(name, typeText, optional = true)
