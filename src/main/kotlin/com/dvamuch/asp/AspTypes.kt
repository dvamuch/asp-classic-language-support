package com.dvamuch.asp

import com.intellij.psi.tree.IElementType

class AspTokenType(debugName: String) : IElementType(debugName, AspLanguage.INSTANCE)

object AspTypes {
    // HTML Content
    val HTML_CONTENT = AspTokenType("HTML_CONTENT")

    // ASP Tags
    val ASP_TAG_START = AspTokenType("<%")
    val ASP_TAG_END = AspTokenType("%>")

    // Built-in Objects
    val RESPONSE = AspTokenType("Response")
    val REQUEST = AspTokenType("Request")
    val SESSION = AspTokenType("Session")
    val APPLICATION = AspTokenType("Application")
    val SERVER = AspTokenType("Server")

    // Keywords
    val IF = AspTokenType("If")
    val THEN = AspTokenType("Then")
    val ELSE = AspTokenType("Else")
    val END = AspTokenType("End")
    val FOR = AspTokenType("For")
    val NEXT = AspTokenType("Next")
    val WHILE = AspTokenType("While")
    val WEND = AspTokenType("Wend")
    val DO = AspTokenType("Do")
    val LOOP = AspTokenType("Loop")
    val DIM = AspTokenType("Dim")
    val SET = AspTokenType("Set")
    val FUNCTION = AspTokenType("Function")
    val SUB = AspTokenType("Sub")

    // Operators
    val EQUALS = AspTokenType("=")
    val COMMA = AspTokenType(",")
    val LPAREN = AspTokenType("(")
    val RPAREN = AspTokenType(")")

    // Literals
    val STRING = AspTokenType("STRING")
    val NUMBER = AspTokenType("NUMBER")
    val IDENTIFIER = AspTokenType("IDENTIFIER")
    val COMMENT = AspTokenType("COMMENT")
}