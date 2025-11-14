package com.dmitry.aspclassic.psi

import com.dmitry.aspclassic.AspFileType
import com.dmitry.aspclassic.AspLanguage
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * ASP PSI File
 */
class AspFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AspLanguage.INSTANCE) {
    override fun getFileType(): FileType = AspFileType.INSTANCE

    override fun toString(): String = "ASP Classic File"
}

