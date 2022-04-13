package tpl.interpreter.value;

public class LongValue extends Value
{
    private final long value;

    public LongValue(long value)
    {
        this.value = value;
    }

    public long getValue()
    {
        return this.value;
    }
}
