package com.dmitry.aspclassic

import com.intellij.lang.Language

/**
 * Основное определение языка ASP
 */
class AspLanguage : Language("ASP Classic") {
    companion object {
        @JvmStatic
        val INSTANCE = AspLanguage()
    }

    /**
     * Отображаемое имя в IDE.
     */
    override fun getDisplayName(): String = "ASP Classic"

    override fun isCaseSensitive(): Boolean = false
}