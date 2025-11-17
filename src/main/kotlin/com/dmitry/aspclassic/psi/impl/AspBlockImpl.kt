package com.dmitry.aspclassic.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.dmitry.aspclassic.psi.AspBlock

/**
 * Implementation of ASP block PSI element
 */
class AspBlockImpl(node: ASTNode) : ASTWrapperPsiElement(node), AspBlock