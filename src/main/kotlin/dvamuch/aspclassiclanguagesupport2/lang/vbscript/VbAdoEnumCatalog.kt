package dvamuch.aspclassiclanguagesupport2.lang.vbscript

import java.util.Locale

internal object VbAdoEnumCatalog {
    private val valuesByType = mapOf(
        "datatypeenum" to setOf(
            "adEmpty", "adTinyInt", "adSmallInt", "adInteger", "adBigInt",
            "adUnsignedTinyInt", "adUnsignedSmallInt", "adUnsignedInt", "adUnsignedBigInt",
            "adSingle", "adDouble", "adCurrency", "adDecimal", "adNumeric", "adBoolean",
            "adError", "adUserDefined", "adVariant", "adIDispatch", "adIUnknown", "adGUID",
            "adDate", "adDBDate", "adDBTime", "adDBTimeStamp", "adBSTR", "adChar",
            "adVarChar", "adLongVarChar", "adWChar", "adVarWChar", "adLongVarWChar",
            "adBinary", "adVarBinary", "adLongVarBinary", "adChapter", "adFileTime",
            "adPropVariant", "adVarNumeric", "adArray"
        ),
        "parameterdirectionenum" to setOf(
            "adParamUnknown", "adParamInput", "adParamOutput", "adParamInputOutput",
            "adParamReturnValue"
        )
    ).mapValues { (_, values) -> values.mapTo(hashSetOf()) { normalize(it) } }

    fun contains(typeName: String?, constantName: String): Boolean {
        val values = valuesByType[normalize(typeName ?: return false)] ?: return false
        return normalize(constantName) in values
    }

    private fun normalize(value: String): String = value.lowercase(Locale.ROOT)
}
