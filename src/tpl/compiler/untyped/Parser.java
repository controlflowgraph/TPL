package tpl.compiler.untyped;

import tpl.compiler.lexer.Lexer;
import tpl.compiler.lexer.Token;
import tpl.compiler.lexer.TokenType;
import tpl.compiler.type.TypeParser;
import tpl.compiler.type.tree.Type;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.expression.UntypedDeclaration;
import tpl.compiler.untyped.tree.expression.UntypedExpression;
import tpl.compiler.untyped.tree.function.UntypedFunction;
import tpl.compiler.untyped.tree.loop.UntypedForLoop;
import tpl.compiler.untyped.tree.loop.UntypedWhileLoop;
import tpl.compiler.untyped.tree.other.*;
import tpl.compiler.untyped.tree.segment.UntypedAssignment;
import tpl.compiler.untyped.tree.segment.UntypedIdentifier;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Parser
{
    public static UntypedFragment parseFile(Path path)
    {
        List<Token> tokens = Lexer.lexFile(path);
        return new Parser(path, tokens).parse();
    }

    private final Path path;
    private final List<Token> tokens;
    private final Provider provider;
    private List<String> context;

    private Parser(Path path, List<Token> tokens)
    {
        this.path = path;
        this.tokens = tokens;
        this.provider = new Provider(tokens);
    }

    private UntypedFragment parse()
    {
        List<UntypedStructure> structures = new ArrayList<>();
        while(this.provider.hasNext())
        {
            structures.add(parseStructure());
        }
        return new UntypedFragment(this.path, structures);
    }

    private UntypedStructure parseStructure()
    {
        this.structureTotal++;
        if(this.provider.isNext("include"))
        {
            return parseInclude();
        }
        else if(this.provider.isNext("const"))
        {
            return parseDeclaration();
        }
        else if(this.provider.isNext("var"))
        {
            return parseDeclaration();
        }
        else if(this.provider.isNext("for"))
        {
            return parseForLoop();
        }
        else if(this.provider.isNext("{"))
        {
            return parseBody();
        }
        else if(this.provider.isNext("while"))
        {
            return parseWhileLoop();
        }
        else if(this.provider.isNext("if"))
        {
            return parseIfStatement();
        }
        else if(this.provider.isNext("return"))
        {
            return parseReturn();
        }
        else if(this.provider.isNext("native"))
        {
            return parseNativeFunction();
        }
        else if(this.provider.isNext("function"))
        {
            return parseFunction();
        }
        else if(this.provider.isNext("type"))
        {
            return parseType();
        }
        else
        {
            return parseExpression();
        }
    }

    private UntypedInclude parseInclude()
    {
        this.includeTotal++;
        Position position = this.provider.assertNext("include").position();

        if(!isInGlobal() || this.includeTotal != this.structureTotal)
            throw new TPLPositionedException("Includes only allowed at the beginning of a file in the global scope!", position);

        String path = this.provider.assertNext(TokenType.STRING).text();
        this.provider.assertNext(";");
        return new UntypedInclude(position, path.substring(1, path.length() - 1));
    }

    private UntypedForLoop parseForLoop()
    {
        Position position = this.provider.assertNext("for").position();
        this.provider.assertNext("(");
        UntypedDeclaration declaration = parseDeclaration();
        UntypedExpression condition = parseExpression();
        UntypedExpression advancement = parseExpressionUntil(")", "(");
        enterLoop();
        UntypedStructure body = parseStructure();
        exitLoop();
        return new UntypedForLoop(position, declaration, condition, advancement, body);
    }

    private UntypedDeclaration parseDeclaration()
    {
        Position position = this.provider.peek().position();
        UntypedField field = parseField();
        Position assignment = this.provider.assertNext("=").position();
        UntypedExpression expression = new UntypedExpression(assignment, new UntypedAssignment(assignment, new UntypedIdentifier(field.position(), field.name()), parseExpression().getSegment()));
        return new UntypedDeclaration(position, field, expression);
    }

    private UntypedField parseField()
    {
        Token modifier = this.provider.next();
        String con = modifier.text();
        if(!con.equals("const") && !con.equals("var"))
            throw new TPLPositionedException("Expected const / var modifier but got '" + modifier.text() + "'!", modifier.position());

        boolean constant = con.equals("const");
        Type type = TypeParser.parse(this.provider, this.context);
        Token token = this.provider.assertNext(TokenType.IDENTIFIER);
        Position position = token.position();
        String name = token.text();
        return new UntypedField(position, constant, type, name);
    }

    private UntypedExpression parseExpression()
    {
        return parseExpressionUntil(";", null);
    }

    private UntypedExpression parseExpressionUntil(String text, String in)
    {
        List<Token> tokens = new ArrayList<>();

        int indent = 0;
        boolean end = false;
        while(this.provider.hasNext() && !end)
        {
            if(this.provider.isNext(text))
            {
                if(indent == 0)
                {
                    end = true;
                }
                else
                {
                    indent--;
                    tokens.add(this.provider.next());
                }
            }
            else if(this.provider.isNext(in))
            {
                indent++;
                tokens.add(this.provider.next());
            }
            else
            {
                tokens.add(this.provider.next());
            }
        }

        //System.out.println(tokens);

        this.provider.assertNext(text);

        if(tokens.size() == 0)
        {
            return null;
        }

        return new UntypedExpression(null, ExpressionParser.parse(tokens, this.context));
    }

    private UntypedBody parseBody()
    {
        List<UntypedStructure> content = new ArrayList<>();

        Position position = this.provider.assertNext("{").position();
        enter();
        boolean found = false;
        while(this.provider.hasNext() && !found)
        {
            if(this.provider.isNext("}"))
            {
                found = true;
            }
            else
            {
                content.add(parseStructure());
            }
        }
        exit();
        this.provider.assertNext("}");

        return new UntypedBody(position, content);
    }

    private UntypedWhileLoop parseWhileLoop()
    {
        Position position = this.provider.assertNext("while").position();
        this.provider.assertNext("(");
        UntypedExpression condition = parseExpressionUntil(")", "(");
        enterLoop();
        UntypedStructure body = parseStructure();
        exitLoop();
        return new UntypedWhileLoop(position, condition, body);
    }

    private UntypedIfStatement parseIfStatement()
    {
        Position position = this.provider.assertNext("if").position();
        this.provider.assertNext("(");
        UntypedExpression condition = parseExpressionUntil(")", "(");
        enter();
        UntypedStructure body = parseStructure();
        exit();
        UntypedStructure follower = null;
        if(this.provider.isNext("else"))
        {
            this.provider.assertNext("else");
            enter();
            follower = parseStructure();
            exit();
        }
        return new UntypedIfStatement(position, condition, body, follower);
    }

    private UntypedReturn parseReturn()
    {
        Position position = this.provider.assertNext("return").position();
        if(!isInFunction())
            throw new TPLPositionedException("Return statement outside of function!", position);
        UntypedExpression expression = parseExpression();
        return new UntypedReturn(position, expression);
    }

    private UntypedFunction parseFunction()
    {
        Position position = this.provider.assertNext("function").position();
        List<String> generics = null;
        if(this.provider.isNext("<"))
        {
            generics = parseGenerics();
        }
        this.context = generics;

        Type result = TypeParser.parse(this.provider, this.context);
        String name = this.provider.assertNext(TokenType.IDENTIFIER).text();
        List<UntypedField> fields = parseFields();
        enterFunction();
        UntypedStructure body = parseStructure();
        exitFunction();
        this.context = null;
        return new UntypedFunction(position, false, generics, result, name, this.functionId++, fields, body);
    }

    private List<UntypedField> parseFields()
    {
        List<UntypedField> fields = new ArrayList<>();
        Position position = this.provider.assertNext("(").position();
        boolean found = false;
        while(this.provider.hasNext() && !found)
        {
            if (this.provider.isNext(")"))
            {
                found = true;
            }
            else
            {
                fields.add(parseField());
                if(this.provider.isNext(","))
                {
                    this.provider.assertNext(",");
                }
            }
        }

        if(!found)
            throw new TPLPositionedException("Expected another generic parameter!", position);

        this.provider.assertNext(")");
        return fields;
    }

    private List<String> parseGenerics()
    {
        List<String> generics = new ArrayList<>();
        Position position = this.provider.assertNext("<").position();
        boolean required = true;
        while(this.provider.hasNext() && required)
        {
            Token token = this.provider.assertNext(TokenType.IDENTIFIER);
            String generic = token.text();
            if(generics.contains(generic))
            {
                throw new TPLPositionedException("Generic parameter '" + generic + "' already defined!", token.position());
            }
            generics.add(generic);
            if (this.provider.isNext(","))
            {
                this.provider.assertNext(",");
            }
            else
            {
                required = false;
            }
        }

        if(required)
            throw new TPLPositionedException("Expected another generic parameter!", position);

        this.provider.assertNext(">");
        return generics;
    }

    private UntypedFunction parseNativeFunction()
    {
        Position position = this.provider.assertNext("native").position();
        this.provider.assertNext("function");
        Type result = TypeParser.parse(this.provider, this.context);
        String name = this.provider.assertNext(TokenType.IDENTIFIER).text();
        List<UntypedField> fields = parseFields();
        this.provider.assertNext(";");
        return new UntypedFunction(position, true, null, result, name, this.functionId++, fields, null);
    }

    private UntypedType parseType()
    {
        Position position = this.provider.assertNext("type").position();
        List<String> generics = null;
        if(this.provider.isNext("<"))
        {
            generics = parseGenerics();
        }
        String name = this.provider.assertNext(TokenType.IDENTIFIER).text();
        this.context = generics;
        List<UntypedField> fields = parseFields();
        this.provider.assertNext(";");
        return new UntypedType(position, generics, name, fields);
    }

    private int nesting;
    private int function;
    private int loops;
    private int structureTotal;
    private int includeTotal;
    private int functionId;

    public void enterLoop()
    {
        this.nesting++;
        this.loops++;
    }

    public void exitLoop()
    {
        this.loops--;
        this.nesting--;
    }

    public void enterFunction()
    {
        this.nesting++;
        this.function++;
    }

    public void exitFunction()
    {
        this.function--;
        this.nesting--;
    }

    public void enter()
    {
        this.nesting++;
    }

    public void exit()
    {
        this.nesting--;
    }

    public boolean isInGlobal()
    {
        return this.nesting == 0;
    }

    public boolean isInFunction()
    {
        return this.function > 0;
    }
}
