package tpl.compiler.untyped;

import tpl.compiler.lexer.Token;
import tpl.compiler.lexer.TokenType;
import tpl.compiler.type.TypeParser;
import tpl.compiler.type.tree.NullType;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.untyped.tree.expression.UntypedExpression;
import tpl.compiler.untyped.tree.segment.*;
import tpl.compiler.util.TPLException;
import tpl.compiler.util.TPLPositionedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExpressionParser
{
    public static UntypedSegment parse(List<Token> tokens, List<String> context)
    {
        return parse(new Provider(tokens), context);
    }

    private static UntypedSegment parse(Provider provider, List<String> context)
    {
        return new ExpressionParser(provider, context).parse();
    }

    private final Provider provider;
    private final List<String> context;

    private ExpressionParser(Provider provider, List<String> context)
    {
        this.provider = provider;
        this.context = context;
    }

    private UntypedSegment parse()
    {
        return reduceSegments(parseSegments());
    }

    private static final Set<String> UNARY_OPERATORS = Set.of("+", "-", "!");
    private static final List<Set<String>> PRECEDENCE = List.of(
            Set.of("%", "*", "/"),
            Set.of("+", "-"),
            Set.of("<<", ">>", "<<<", ">>>"),
            Set.of("&"),
            Set.of("^"),
            Set.of("|"),
            Set.of("<", ">", "<=", ">=", "==", "!="),
            Set.of("&&"),
            Set.of("^^"),
            Set.of("||")
    );
    private static final Set<String> ASSIGNMENTS = Set.of(
            "=",
            "+=",
            "-=",
            "*=",
            "/=",
            "%=",
            "<<=",
            ">>=",
            "<<<=",
            ">>>=",
            "&=",
            "^=",
            "|=",
            "&&=",
            "^^=",
            "||="
    );

    private UntypedSegment reduceSegments(List<UntypedSegment> segments)
    {
        // TODO: rework this to not use instanceof
        //       - kind of like the shunting yard algorithm
        //       - if the last token that was pushed on the operation stack
        //         and another operator is found then treat it like a unary operator
        //       - use stack of operators when parsing through
        //       - when a new operator is found check if the precedence is lower
        //       - if it is lower check the stack and create a new operation node
        //       - it is safe to assume that all required values have already been resolved
        List<UntypedSegment> unary = resolveUnaryOperators(segments);
        List<UntypedSegment> binary = resolveBinaryOperators(unary);

        if(binary.size() == 3 && binary.get(1) instanceof UntypedOperator o && ASSIGNMENTS.contains(o.getOperator()))
        {
            if(o.getOperator().equals("="))
            {
                binary = List.of(new UntypedAssignment(o.getPosition(), binary.get(0), binary.get(2)));
            }
            else
            {
                String op = o.getOperator().substring(0, o.getOperator().length() - 1);
                UntypedSegment right = new UntypedBinaryOperator(o.getPosition(), op, binary.get(0), binary.get(2));
                binary = List.of(new UntypedAssignment(o.getPosition(), binary.get(0), right));
            }
        }

        if(binary.size() > 1)
            throw new TPLException("Unable to resolve expression! " + binary);

        return binary.size() >= 1 ? binary.get(0) : null;
    }

    private List<UntypedSegment> resolveUnaryOperators(List<UntypedSegment> segments)
    {
        boolean change = true;
        while(change)
        {
            change = false;
            List<UntypedSegment> result = new ArrayList<>();
            for(int i = 0; i < segments.size(); i++)
            {
                if((i == 0 || segments.get(i - 1) instanceof UntypedOperator) && segments.get(i) instanceof UntypedOperator o && UNARY_OPERATORS.contains(o.getOperator()))
                {
                    if(i + 1 == segments.size())
                        throw new TPLPositionedException("Expected value after unary operator!", o.getPosition());

                    if(!(segments.get(i + 1) instanceof UntypedUnaryOperator))
                    {
                        change = true;
                        result.add(new UntypedUnaryOperator(o.getPosition(), o.getOperator(), segments.get(i++ + 1)));
                    }
                    else
                    {
                        result.add(segments.get(i));
                    }
                }
                else
                {
                    result.add(segments.get(i));
                }
            }
            segments = result;
        }
        return segments;
    }

    private List<UntypedSegment> resolveBinaryOperators(List<UntypedSegment> segments)
    {
        for(Set<String> precedence : PRECEDENCE)
        {
            List<UntypedSegment> result = new ArrayList<>();
            for(int i = 0; i < segments.size(); i++)
            {
                if(segments.get(i) instanceof UntypedOperator o && precedence.contains(o.getOperator()))
                {
                    if(result.size() == 0)
                        throw new TPLPositionedException("Expected value on the left side of a binary operator!", o.getPosition());
                    if(result.get(result.size() - 1) instanceof UntypedOperator u)
                        throw new TPLPositionedException("Unresolved operator on the left of binary operator!", u.getPosition());
                    UntypedSegment left = result.remove(result.size() - 1);
                    UntypedSegment right = segments.get(i++ + 1);
                    result.add(new UntypedBinaryOperator(o.getPosition(), o.getOperator(), left, right));
                }
                else
                {
                    result.add(segments.get(i));
                }
            }
            segments = result;
        }
        return segments;
    }

    private List<UntypedSegment> parseSegments()
    {
        List<UntypedSegment> segments = new ArrayList<>();
        UntypedSegment current = null;
        while (this.provider.hasNext())
        {
            Token token = this.provider.next();
            if(token.type() == TokenType.OPERATOR)
            {
                if(token.text().equals("."))
                {
                    String attribute = this.provider.assertNext(TokenType.IDENTIFIER).text();
                    current = new UntypedSelection(token.position(), current, attribute);
                }
                else
                {
                    if(current != null)
                    {
                        segments.add(current);
                        current = null;
                    }
                    segments.add(new UntypedOperator(token.position(), token.text()));
                }
            }
            else if(token.type() == TokenType.IDENTIFIER)
            {
                if(current != null)
                    throw new TPLPositionedException("Unexpected identifier '" + token.text() + "' found!", current.getPosition());

                if(token.text().equals("null"))
                {
                    current = new UntypedValue(token.position(), "null", new NullType(token.position()));
                }
                else if(this.provider.isNext("("))
                {
                    this.provider.assertNext("(");
                    current = new UntypedCall(token.position(), token.text(), parseParameters());
                }
                else
                {
                    current = new UntypedIdentifier(token.position(), token.text());
                }
            }
            else if(token.type() == TokenType.INT_NUMBER)
            {
                current = getValue("int", token);
            }
            else if(token.type() == TokenType.FP_NUMBER)
            {
                current = getValue("double", token);
            }
            else if(token.type() == TokenType.STRING)
            {
                current = getValue("String", token);
            }
            else if(token.type() == TokenType.BOOLEAN)
            {
                current = getValue("boolean", token);
            }
            else if(token.type() == TokenType.CHAR)
            {
                current = getValue("char", token);
            }
            else if(token.text().equals("new"))
            {
                Type type = TypeParser.parse(this.provider, this.context);
                this.provider.assertNext("(");
                List<UntypedSegment> parameters = parseParameters();
                current = new UntypedConstructor(token.position(), type, parameters);
            }
            else if(token.text().equals("to"))
            {
                Type type = TypeParser.parse(this.provider, this.context);
                current = new UntypedCast(token.position(), current, type);
            }
            else if(token.text().equals("("))
            {
                List<UntypedSegment> subs = parseParameters();
                if(subs.size() == 1)
                {
                    current = subs.get(0);
                }
                else
                {
                    current = new UntypedTuple(token.position(), subs);
                }
            }
            else if(token.text().equals("["))
            {
                UntypedSegment key = parseKey();
                current = new UntypedIndex(token.position(), current, key);
            }
            else
            {
                throw new TPLPositionedException("Unknown token in expression (" + token.text() + ")", token.position());
            }
        }

        if(current != null)
        {
            segments.add(current);
        }
        return segments;
    }

    private UntypedSegment getValue(String type, Token token)
    {
        return new UntypedValue(token.position(), token.text(), new SingleType(token.position(), type));
    }
    // TODO: rework expression parser to directly use the provider stream of the structure parser
    private List<UntypedSegment> parseParameters()
    {
        List<UntypedSegment> parameters = new ArrayList<>();
        List<Token> parameter = new ArrayList<>();
        int indentation = 0;
        while(this.provider.hasNext())
        {
            if(this.provider.isNext("("))
            {
                indentation++;
                parameter.add(this.provider.next());
            }
            if(this.provider.isNext(")"))
            {
                if(indentation == 0)
                {
                    if(parameter.size() > 0)
                    {
                        parameters.add(ExpressionParser.parse(parameter, this.context));
                    }
                    break;
                }
                parameter.add(this.provider.next());
                indentation--;
            }
            else if(this.provider.isNext(",") && indentation == 0)
            {
                parameters.add(ExpressionParser.parse(parameter, this.context));
                this.provider.assertNext(",");
                parameter = new ArrayList<>();
            }
            else
            {
                parameter.add(this.provider.next());
            }
        }
        this.provider.assertNext(")");
        return parameters;
    }

    private UntypedSegment parseKey()
    {
        List<Token> segment = new ArrayList<>();
        int indentation = 0;
        while(this.provider.hasNext())
        {
            if(this.provider.isNext("["))
            {
                indentation++;
                segment.add(this.provider.next());
            }
            else if(this.provider.isNext("]"))
            {
                if(indentation == 0)
                {
                    break;
                }
                indentation--;
                segment.add(this.provider.next());
            }
            else
            {
                segment.add(this.provider.next());
            }
        }
        this.provider.assertNext("]");
        return ExpressionParser.parse(segment, this.context);
    }
}
