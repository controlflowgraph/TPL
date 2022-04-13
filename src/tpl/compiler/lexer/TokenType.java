package tpl.compiler.lexer;

public enum TokenType
{
    CHAR,
    STRING,
    BOOLEAN,
    INT_NUMBER,
    FP_NUMBER,
    OPERATOR,
    WORD_OPERATOR,
    SYNTAX_ELEMENT,
    IDENTIFIER,
    KEYWORD,
    SINGLE_COMMENT,
    MULTI_COMMENT,
    NEW_LINE,
    UNKNOWN
}
