package com.dvamuch.asp;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import static com.dvamuch.asp.AspTypes.*;

%%

%{
  public _AspLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _AspLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%state ASP_CODE

WHITE_SPACE=[\ \t\f\r\n]+
ASP_TAG_START="<%"
ASP_TAG_END="%>"
IDENTIFIER=[a-zA-Z_][a-zA-Z0-9_]*
STRING=\"([^\"\\]|\\[^\n])*\"|'([^'\\]|\\[^\n])*'
NUMBER=[0-9]+(\.[0-9]*)?
COMMENT='[^\n\r]*

%%

<YYINITIAL> {
    {ASP_TAG_START}   { yybegin(ASP_CODE); return ASP_TAG_START; }
    .                 { return HTML_CONTENT; }
}

<ASP_CODE> {
    // Встроенные объекты ASP
    "Response"        { return RESPONSE; }
    "Request"         { return REQUEST; }
    "Session"         { return SESSION; }
    "Application"     { return APPLICATION; }
    "Server"          { return SERVER; }

    // Ключевые слова VBScript
    "If"              { return IF; }
    "Then"            { return THEN; }
    "Else"            { return ELSE; }
    "End"             { return END; }
    "For"             { return FOR; }
    "Next"            { return NEXT; }
    "While"           { return WHILE; }
    "Wend"            { return WEND; }
    "Do"              { return DO; }
    "Loop"            { return LOOP; }
    "Dim"             { return DIM; }
    "Set"             { return SET; }
    "Function"        { return FUNCTION; }
    "Sub"             { return SUB; }

    // Операторы
    "="               { return EQUALS; }
    ","               { return COMMA; }
    "("               { return LPAREN; }
    ")"               { return RPAREN; }

    // Литералы
    {STRING}          { return STRING; }
    {NUMBER}          { return NUMBER; }
    {IDENTIFIER}      { return IDENTIFIER; }
    {COMMENT}         { return COMMENT; }

    {ASP_TAG_END}     { yybegin(YYINITIAL); return ASP_TAG_END; }
    {WHITE_SPACE}     { return TokenType.WHITE_SPACE; }
}

[^] { return TokenType.BAD_CHARACTER; }