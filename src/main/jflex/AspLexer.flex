package com.dmitry.aspclassic.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.dmitry.aspclassic.parser.AspTokenType;

%%

%class _AspLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%public

%state ASP_CONTENT

%{
  public _AspLexer() {
    this((java.io.Reader)null);
  }
%}

ASP_OPEN_TAG="<%"
ASP_CLOSE_TAG="%>"
HTML_TEXT=([^<]|"<"[^%])+

%%

<YYINITIAL> {
  {ASP_OPEN_TAG}   { yybegin(ASP_CONTENT); return AspTokenType.ASP_OPEN_TAG; }
  {HTML_TEXT}      { return AspTokenType.HTML_CONTENT; }
}

<ASP_CONTENT> {
  {ASP_CLOSE_TAG}  { yybegin(YYINITIAL); return AspTokenType.ASP_CLOSE_TAG; }
  [^%]+            { return AspTokenType.ASP_SCRIPT_CONTENT; }
  "%" / [^>]       { return AspTokenType.ASP_SCRIPT_CONTENT; }
  .                { return AspTokenType.ASP_SCRIPT_CONTENT; }
}