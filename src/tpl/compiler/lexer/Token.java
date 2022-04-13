package tpl.compiler.lexer;

import tpl.compiler.util.Position;

public record Token(String text, TokenType type, Position position)
{
}
