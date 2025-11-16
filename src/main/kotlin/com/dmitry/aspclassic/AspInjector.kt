package com.dmitry.aspclassic

import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl
import com.intellij.psi.impl.source.xml.XmlTextImpl

class AspInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        // Инжектируем HTML во все текстовые узлы XML
        when (context) {
            is XmlTextImpl -> injectHtml(registrar, context)
            is XmlAttributeValueImpl -> injectHtml(registrar, context)
            // Добавьте другие типы элементов по необходимости
        }
    }

    private fun injectHtml(registrar: MultiHostRegistrar, host: PsiLanguageInjectionHost) {
        registrar.startInjecting(HTMLLanguage.INSTANCE)
            .addPlace(null, null, host, ElementManipulators.getValueTextRange(host))
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement?>?> {
        return listOf(
            XmlTextImpl::class.java,
            XmlAttributeValueImpl::class.java
        )
    }

}