package tpl.compiler.lexer;

import java.util.regex.Pattern;

public record Category(TokenType type, Pattern pattern)
{
}
