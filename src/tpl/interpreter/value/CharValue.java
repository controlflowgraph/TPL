package tpl.interpreter.value;

public class CharValue extends Value
{
    private final char value;

    public CharValue(char value)
    {
        this.value = value;
    }

    public char getValue()
    {
        return this.value;
    }
}
