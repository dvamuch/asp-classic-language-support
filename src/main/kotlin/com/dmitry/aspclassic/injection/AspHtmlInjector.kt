package com.dmitry.aspclassic.injection

import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.dmitry.aspclassic.psi.AspContent
import com.dmitry.aspclassic.parser.AspTokenType

/**
 * HTML language injector for ASP files
 * Injects HTML language into content outside ASP blocks
 */
class AspHtmlInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        println("INJECTOR: Checking element: ${context.javaClass.simpleName} '${context.text.take(50)}...'")
        println("INJECTOR: Node type: ${context.node.elementType}")

        if (context is AspContent && context.isValidHost) {
            val text = context.text
            if (text.isNotBlank() && !text.trim().startsWith("<%")) {
                println("INJECTOR: Injecting HTML into: '${text.take(50)}...'")
                injectHtmlIntoContent(registrar, context)
            } else {
                println("INJECTOR: Skipping - empty or ASP tag")
            }
        }
    }

    private fun injectHtmlIntoContent(registrar: MultiHostRegistrar, host: AspContent) {
        val text = host.text
        if (text.isBlank()) return

        registrar.startInjecting(HTMLLanguage.INSTANCE)
            .addPlace(null, null, host as PsiLanguageInjectionHost, TextRange(0, text.length))
            .doneInjecting()

        println("INJECTOR: Successfully injected HTML")
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(AspContent::class.java)
    }
}