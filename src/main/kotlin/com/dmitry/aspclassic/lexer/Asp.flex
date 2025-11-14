package com.dmitry.aspclassic.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.dmitry.aspclassic.psi.AspTypes;
import com.intellij.psi.TokenType;

%%

%class AspLexer
%public
%implements FlexLexer
%unicode
%ignorecase
%function advance
%type IElementType
%eof{  return;
%eof}

%state IN_ASP_TAG

CRLF = \r\n | \r | \n
WHITE_SPACE = [ \t\f]
LINE_COMMENT = "'" [^\r\n]*
HTML_CONTENT = [^<]+
ASP_START = "<%"
ASP_END = "%>"
ASP_OUTPUT_START = "<%="
ASP_DIRECTIVE_START = "<%@"

// VBScript keywords
KEYWORD = "Dim" | "Set" | "If" | "Then" | "Else" | "ElseIf" | "End" | "For" | "To" | "Step" | "Next" |
          "While" | "Wend" | "Do" | "Loop" | "Until" | "Function" | "Sub" | "Exit" | "Call" |
          "Select" | "Case" | "With" | "New" | "Class" | "Public" | "Private" | "Property" |
          "Get" | "Let" | "Default" | "On" | "Error" | "Resume" | "GoTo" | "ReDim" | "Preserve" |
          "And" | "Or" | "Not" | "Xor" | "Eqv" | "Imp" | "Mod" | "Is" | "Nothing" | "Empty" | "Null" |
          "True" | "False" | "Option" | "Explicit" | "ByVal" | "ByRef" | "Const"

// ASP Objects and their members
ASP_OBJECT = "Request" | "Response" | "Session" | "Application" | "Server" | "ObjectContext"

// String literals
STRING = \"([^\"\r\n]|\"\")*\" | '([^'\r\n]|'')*'

// Numbers
NUMBER = [0-9]+(\.[0-9]+)?

// Identifiers
IDENTIFIER = [a-zA-Z_][a-zA-Z0-9_]*

// Operators
OPERATOR = "=" | "+" | "-" | "*" | "/" | "\\" | "^" | "&" | "<" | ">" | "<=" | ">=" | "<>" | "=>"

%%

<YYINITIAL> {
    {ASP_OUTPUT_START}      { yybegin(IN_ASP_TAG); return AspTypes.ASP_OUTPUT_START; }
    {ASP_DIRECTIVE_START}   { yybegin(IN_ASP_TAG); return AspTypes.ASP_DIRECTIVE_START; }
    {ASP_START}             { yybegin(IN_ASP_TAG); return AspTypes.ASP_START; }
    "<"                     { return AspTypes.HTML_CONTENT; }
    {HTML_CONTENT}          { return AspTypes.HTML_CONTENT; }
    {CRLF}                  { return AspTypes.CRLF; }
    {WHITE_SPACE}+          { return TokenType.WHITE_SPACE; }
}

<IN_ASP_TAG> {
    {ASP_END}               { yybegin(YYINITIAL); return AspTypes.ASP_END; }
    {LINE_COMMENT}          { return AspTypes.COMMENT; }
    {KEYWORD}               { return AspTypes.KEYWORD; }
    {ASP_OBJECT}            { return AspTypes.ASP_OBJECT; }
    {STRING}                { return AspTypes.STRING; }
    {NUMBER}                { return AspTypes.NUMBER; }
    {IDENTIFIER}            { return AspTypes.IDENTIFIER; }
    {OPERATOR}              { return AspTypes.OPERATOR; }
    "("                     { return AspTypes.LPAREN; }
    ")"                     { return AspTypes.RPAREN; }
    "{"                     { return AspTypes.LBRACE; }
    "}"                     { return AspTypes.RBRACE; }
    ","                     { return AspTypes.COMMA; }
    "."                     { return AspTypes.DOT; }
    ":"                     { return AspTypes.COLON; }
    "_"                     { return AspTypes.LINE_CONTINUATION; }
    {CRLF}                  { return AspTypes.CRLF; }
    {WHITE_SPACE}+          { return TokenType.WHITE_SPACE; }
}

[^]                         { return TokenType.BAD_CHARACTER; }

