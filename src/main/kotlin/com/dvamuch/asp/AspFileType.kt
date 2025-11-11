package com.dvamuch.asp

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class AspFileType : LanguageFileType(AspLanguage.INSTANCE) {

    override fun getName(): String = "ASP Classic"

    override fun getDescription(): String = "ASP Classic file"

    override fun getDefaultExtension(): String = "asp"

    override fun getIcon(): Icon? = null

    companion object {
        val INSTANCE = AspFileType()
    }

}