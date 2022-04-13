package tpl.compiler.type;

import tpl.compiler.lexer.Token;
import tpl.compiler.lexer.TokenType;
import tpl.compiler.type.tree.*;
import tpl.compiler.untyped.Provider;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLException;
import tpl.compiler.util.TPLPositionedException;

import java.util.ArrayList;
import java.util.List;

public class TypeParser
{
    public static Type parse(Provider provider, List<String> context)
    {
        return new TypeParser(provider, context).parse();
    }

    private final Provider provider;
    private final List<String> context;

    public TypeParser(Provider provider, List<String> context)
    {
        this.provider = provider;
        this.context = context;
    }

    private Type parse()
    {
        /*
        (t1, t2, t3)
        something
        something[]
        something<t1, t2>
        something<t1, t2>[]
        */
        if(this.provider.isNext("("))
        {
            return parseTuple();
        }
        else if(this.provider.isNext(TokenType.IDENTIFIER))
        {
            return parseStandard();
        }
        else
        {
            return fail();
        }
    }

    private Type parseTuple()
    {
        Position position = this.provider.assertNext("(").position();
        List<Type> types = new ArrayList<>();
        boolean required = true;
        while(this.provider.hasNext() && required)
        {
            types.add(TypeParser.parse(this.provider, this.context));

            if(!this.provider.isNext(","))
            {
                required = false;
            }
            else
            {
                this.provider.next();
            }
        }
        if(required)
        {
            throw new TPLPositionedException("Expected another type in tuple!", position);
        }
        this.provider.assertNext(")");
        return new GenericType(position, new SingleType(position, "Tuple"), types);
    }

    private boolean isPlaceholder(String name)
    {
        return this.context != null && this.context.contains(name);
    }

    private Type parseStandard()
    {
        Type type = getSimple();
        if(this.provider.isNext("<"))
        {
            type = parseGeneric(type);
        }
        else if(this.provider.isNext("<>"))
        {
            this.provider.assertNext("<>");
            type = new GenericType(type.getPosition(), type, List.of());
        }
        if(this.provider.isNext("["))
        {
            type = parseArray(type);
        }
        return type;
    }

    private Type getSimple()
    {
        Token single = this.provider.assertNext(TokenType.IDENTIFIER);
        if(isPlaceholder(single.text()))
        {
            return new PlaceholderType(single.position(), single.text());
        }
        else
        {
            return new SingleType(single.position(), single.text());
        }
    }

    private Type parseGeneric(Type base)
    {
        List<Type> generics = new ArrayList<>();
        this.provider.assertNext("<");

        boolean required = true;
        while(this.provider.hasNext() && required)
        {
            if(this.provider.isNext(">"))
            {
                required = false;
            }
            else
            {
                generics.add(TypeParser.parse(this.provider, this.context));
                if(this.provider.isNext(","))
                {
                    this.provider.next();
                }
                else
                {
                    required = false;
                }
            }
        }
        this.provider.assertNext(">");

        return new GenericType(base.getPosition(), base, generics);
    }

    private Type parseArray(Type base)
    {
        this.provider.assertNext("[");
        this.provider.assertNext("]");
        return new ArrayType(base.getPosition(), base);
    }

    private Type fail()
    {
        if(!this.provider.hasNext())
        {
            throw new TPLException("Expected type but EOF reached!");
        }
        else
        {
            Token token = this.provider.next();
            throw new TPLPositionedException("Expected type but found '" + token.text() + "'!", token.position());
        }
    }
}
