package tpl.compiler.lexer;

import tpl.compiler.util.Position;
import tpl.compiler.util.TPLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer
{
    private static final List<Category> CATEGORIES = List.of(
            new Category(TokenType.SINGLE_COMMENT, Pattern.compile("//[^\n]*")),
            new Category(TokenType.MULTI_COMMENT, Pattern.compile("/\\*")),
            new Category(TokenType.STRING, Pattern.compile("\"(\\\\.|.)*\"")),
            new Category(TokenType.CHAR, Pattern.compile("'(\\\\.|.)'")),
            new Category(TokenType.IDENTIFIER, Pattern.compile("[_a-zA-Z][a-zA-Z0-9]*")),
            new Category(TokenType.FP_NUMBER, Pattern.compile("[0-9]+\\.[0-9]+")),
            new Category(TokenType.INT_NUMBER, Pattern.compile("[0-9]+")),
            new Category(TokenType.OPERATOR, Pattern.compile("[.+\\-*/%&|<=>?!]+")),
            new Category(TokenType.SYNTAX_ELEMENT, Pattern.compile("[;:,()\\[\\]{}]")),
            new Category(TokenType.NEW_LINE, Pattern.compile("\n")),
            new Category(TokenType.UNKNOWN, Pattern.compile("[^ \t\r\n]"))
    );

    private static final Set<String> WORD_OPERATORS = Set.of(
            "of",
            "to",
            "new"
    );

    private static final Set<String> KEYWORDS = Set.of(
            "include",
            "check",
            "assume",
            "function",
            "type",
            "for",
            "while",
            "if",
            "else",
            "return"
    );

    public static List<Token> lexFile(Path file)
    {
        try
        {
            return lex(file, Files.readString(file));
        }
        catch (IOException e)
        {
            throw new TPLException("Unable to read file '" + file + "'!");
        }
    }

    public static List<Token> lexString(String text)
    {
        return lex(null, text);
    }

    private static List<Token> lex(Path path, String text)
    {
        return new Lexer(path, text).lex();
    }

    private int offset;
    private int line = 1;
    private final int last;
    private final Path path;
    private final String text;
    private final List<Matcher> matchers;

    private Lexer(Path path, String text)
    {
        this.matchers = CATEGORIES.stream().map(c -> c.pattern().matcher(text)).toList();
        this.path = path;
        this.text = text;
        this.last = getLastRelevantCharacter();
    }

    private int getLastRelevantCharacter()
    {
        int last = text.length();
        while(--last >= 0)
        {
            char character = text.charAt(last);
            if(character != ' ' && character != '\n' && character != '\t')
            {
                break;
            }
        }
        return last;
    }

    private List<Token> lex()
    {
        List<Token> tokens = new ArrayList<>();
        int start = 0;
        while(start <= this.last)
        {
            int index = getBestMatcherIndex(start);
            Matcher matcher = this.matchers.get(index);
            TokenType type = CATEGORIES.get(index).type();
            String text = matcher.group();
            if(type == TokenType.UNKNOWN)
            {
                throw new TPLException("Unknown token '" + text + "'!");
            }
            else if(type == TokenType.NEW_LINE)
            {
                start = matcher.end();
                this.offset = start;
                this.line++;
            }
            else if(type == TokenType.SINGLE_COMMENT)
            {
                start = getNextNewLineStart(start);
            }
            else if(type == TokenType.MULTI_COMMENT)
            {
                start = getNextMultiCommentEnd(start);
            }
            else
            {
                if(type == TokenType.IDENTIFIER)
                {
                    if(WORD_OPERATORS.contains(text))
                    {
                        type = TokenType.WORD_OPERATOR;
                    }
                    else if(KEYWORDS.contains(text))
                    {
                        type = TokenType.KEYWORD;
                    }
                    else if(text.equals("true") || text.equals("false"))
                    {
                        type = TokenType.BOOLEAN;
                    }
                }
                Position position = new Position(this.path, this.line, matcher.start() - this.offset, text.length());
                tokens.add(new Token(text, type, position));
                start = matcher.end();
            }
        }
        return tokens;
    }

    private int getNextNewLineStart(int start)
    {
        while(++start <= this.last)
        {
            if(this.text.charAt(start) == '\n')
            {
                break;
            }
        }
        return start;
    }

    private int getNextMultiCommentEnd(int start)
    {
        while(++start + 1 <= this.last)
        {
            if(this.text.charAt(start) == '\n')
            {
                this.line++;
            }
            if(this.text.charAt(start) == '*' && this.text.charAt(start + 1) == '/')
            {
                return start + 2;
            }
        }
        throw new TPLException("Expected multi line comment end but EOF found!");
    }

    private int getBestMatcherIndex(int start)
    {
        int bestStart = Integer.MAX_VALUE;
        int index = -1;
        for(int i = 0; i < this.matchers.size(); i++)
        {
            Matcher matcher = this.matchers.get(i);
            if(matcher.find(start))
            {
                if(matcher.start() < bestStart)
                {
                    bestStart = matcher.start();
                    index = i;
                }
            }
        }
        return index;
    }
}
