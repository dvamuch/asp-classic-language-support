package dvamuch.aspclassiclanguagesupport2.lang.vbscript;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static dvamuch.aspclassiclanguagesupport2.lang.vbscript.psi.VbTypes.*;

%%

%class VbScriptLexer
%implements FlexLexer
%unicode
%ignorecase
%function advance
%type IElementType

%state IN_PAREN
%state AFTER_DOT

%{
  private int parenDepth = 0;
%}

EOL = \r\n|\r|\n
WS = [ \t\f]+

IDENTIFIER = [A-Za-z_][A-Za-z0-9_]*
NUMBER = [0-9]+
FLOAT = [0-9]+"."[0-9]+
HEX_NUMBER = "&"[Hh][0-9A-Fa-f]+
OCT_NUMBER = "&"[Oo][0-7]+
STRING = \"([^\"\r\n]|\"\")*\"
SINGLE_STRING = \'([^\'\r\n]|\'\')*\'
DATE = \#[^#\r\n]*\#

LINE_CONT = "_"[ \t]*{EOL}
COMMENT = "'"[^\r\n]*
REM_COMMENT = "rem"[ \t][^\r\n]*

%%

{WS} { return WHITE_SPACE; }
{EOL} { return EOL; }
{LINE_CONT} { return WHITE_SPACE; }
<YYINITIAL>{COMMENT} { return COMMENT; }
{REM_COMMENT} { return COMMENT; }

<AFTER_DOT>{WS} { return WHITE_SPACE; }
<AFTER_DOT>{IDENTIFIER} {
  if (parenDepth > 0) yybegin(IN_PAREN); else yybegin(YYINITIAL);
  return IDENTIFIER;
}
<AFTER_DOT>. {
  if (parenDepth > 0) yybegin(IN_PAREN); else yybegin(YYINITIAL);
  yypushback(1);
}

<YYINITIAL,IN_PAREN>"(" { parenDepth++; yybegin(IN_PAREN); return LPAREN; }
<YYINITIAL,IN_PAREN>")" { if (parenDepth > 0) parenDepth--; if (parenDepth == 0) yybegin(YYINITIAL); return RPAREN; }
"," { return COMMA; }
":" { return COLON; }
"." { yybegin(AFTER_DOT); return DOT; }
"=" { return EQ; }
"<>" { return NEQ; }
"<=" { return LE; }
">=" { return GE; }
"<" { return LT; }
">" { return GT; }
"+" { return PLUS; }
"-" { return MINUS; }
"*" { return STAR; }
"/" { return SLASH; }
"\\" { return IDIV; }
"^" { return POW; }
"&" { return AMP; }

"option" { return OPTION; }
"explicit" { return EXPLICIT; }
"dim" { return DIM; }
"const" { return CONST; }
"public" { return PUBLIC_KW; }
"private" { return PRIVATE_KW; }
"class" { return CLASS; }
"end" { return END; }
"function" { return FUNCTION; }
"sub" { return SUB; }
"property" { return PROPERTY; }
"get" { return GET; }
"let" { return LET; }
"set" { return SET; }
"if" { return IF; }
"then" { return THEN; }
"else" { return ELSE; }
"elseif" { return ELSEIF; }
"select" { return SELECT; }
"case" { return CASE; }
"for" { return FOR; }
"each" { return EACH; }
"in" { return IN; }
"to" { return TO; }
"step" { return STEP; }
"next" { return NEXT; }
"do" { return DO; }
"loop" { return LOOP; }
"while" { return WHILE; }
"until" { return UNTIL; }
"wend" { return WEND; }
"with" { return WITH; }
"exit" { return EXIT; }
"on" { return ON; }
"error" { return ERROR; }
"resume" { return RESUME; }
"goto" { return GOTO; }
"redim" { return REDIM; }
"preserve" { return PRESERVE; }
"erase" { return ERASE; }
"execute" { return EXECUTE; }
"executeglobal" { return EXECUTEGLOBAL; }
"call" { return CALL; }
"new" { return NEW; }
"byval" { return BYVAL; }
"byref" { return BYREF; }
"optional" { return OPTIONAL; }

"true" { return TRUE; }
"false" { return FALSE; }
"null" { return NULL; }
"empty" { return EMPTY; }
"nothing" { return NOTHING; }

"and" { return AND; }
"or" { return OR; }
"not" { return NOT; }
"xor" { return XOR; }
"eqv" { return EQV; }
"imp" { return IMP; }
"is" { return IS; }
"mod" { return MOD; }

{FLOAT} { return FLOAT; }
{HEX_NUMBER} { return HEX_NUMBER; }
{OCT_NUMBER} { return OCT_NUMBER; }
{NUMBER} { return NUMBER; }
{STRING} { return STRING; }
<IN_PAREN>{SINGLE_STRING} { return STRING; }
{DATE} { return DATE; }

{IDENTIFIER} { return IDENTIFIER; }

. { return BAD_CHARACTER; }
