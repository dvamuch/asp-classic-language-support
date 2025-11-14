package com.dmitry.aspclassic

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * ASP file type definition
 */
class AspFileType private constructor() : LanguageFileType(AspLanguage.INSTANCE) {
    companion object {
        @JvmStatic
        val INSTANCE = AspFileType()
    }

    override fun getName(): String = "ASP"

    override fun getDescription(): String = "ASP Classic file"

    override fun getDefaultExtension(): String = "asp"

    override fun getIcon(): Icon = AspIcons.FILE
}

