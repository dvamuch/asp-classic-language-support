package com.dmitry.aspclassic

import com.intellij.lang.Commenter

/**
 * Commenter for ASP Classic (VBScript style comments)
 */
class AspCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "'"

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}

