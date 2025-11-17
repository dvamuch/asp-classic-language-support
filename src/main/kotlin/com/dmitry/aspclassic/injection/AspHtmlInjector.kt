package com.dmitry.aspclassic.injection

import com.intellij.lang.html.HTMLLanguage
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.dmitry.aspclassic.psi.AspContent

class AspHtmlInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        println("INJECTOR: Called for: ${context.javaClass.simpleName} '${context.text.take(50)}...'")

        if (context is AspContent) {
            println("INJECTOR: Found AspContent, isValidHost: ${context.isValidHost()}")

            if (context.isValidHost()) {
                val text = context.text
                println("INJECTOR: Injecting HTML into: '${text.take(50)}...'")

                registrar.startInjecting(HTMLLanguage.INSTANCE)
                    .addPlace(null, null, context, TextRange(0, text.length))
                    .doneInjecting()

                println("INJECTOR: HTML injection completed")
            }
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> {
        return listOf(AspContent::class.java)
    }
}