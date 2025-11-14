package com.dmitry.aspclassic

import com.intellij.lang.Language

/**
 * ASP Classic language definition
 */
class AspLanguage private constructor() : Language("ASP") {
    companion object {
        @JvmStatic
        val INSTANCE = AspLanguage()
    }

    override fun getDisplayName(): String = "ASP Classic"

    override fun isCaseSensitive(): Boolean = false
}

