package com.dmitry.aspclassic

import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Определение типа файла ASP - на основе кастомного AspLanguage
 */
class AspFileType private constructor() : LanguageFileType(AspLanguage.INSTANCE) {
    companion object { // Игнорим ворнинг
        @JvmStatic
        val INSTANCE = AspFileType()
    }

    override fun getName(): String = "ASP Classic"

    override fun getDescription(): String = "ASP Classic file"

    override fun getDefaultExtension(): String = "asp"

    override fun getIcon(): Icon = AspIcons.FILE
}

