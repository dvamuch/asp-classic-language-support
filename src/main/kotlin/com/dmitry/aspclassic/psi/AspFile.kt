package com.dmitry.aspclassic.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.dmitry.aspclassic.AspFileType
import com.dmitry.aspclassic.AspLanguage

/**
 * Root PSI element for ASP files
 */
class AspFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, AspLanguage.INSTANCE) {

    override fun getFileType(): FileType {
        return AspFileType.INSTANCE
    }

    override fun toString(): String {
        return "ASP Classic File"
    }
}