package tpl.compiler.untyped;

import tpl.compiler.lexer.Token;
import tpl.compiler.lexer.TokenType;
import tpl.compiler.util.TPLException;
import tpl.compiler.util.TPLPositionedException;

import java.util.List;

public class Provider
{
    private final List<Token> tokens;
    private int index;

    public Provider(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    public Token assertNext(String text)
    {
        if(!hasNext())
            throw new TPLException("Expected '" + text + "' but EOF reached!");
        Token token = next();
        if(!token.text().equals(text))
            throw new TPLPositionedException("Expected '" + text + "' but got '" + token.text() + "'!", token.position());
        return token;
    }

    public Token assertNext(TokenType type)
    {
        if(!hasNext())
            throw new TPLException("Expected '" + type + "' token but EOF reached!");
        Token token = next();
        if(!token.type().equals(type))
            throw new TPLPositionedException("Expected '" + type + "' token but got '" + token.text() + "'!", token.position());
        return token;
    }

    public boolean isNext(String text)
    {
        return hasNext() && peek().text().equals(text);
    }

    public boolean isNext(TokenType type)
    {
        return hasNext() && peek().type().equals(type);
    }

    public Token next()
    {
        return this.tokens.get(this.index++);
    }

    public Token peek()
    {
        return this.tokens.get(this.index);
    }

    public boolean hasNext()
    {
        return this.index < this.tokens.size();
    }
}
