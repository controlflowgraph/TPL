package tpl.interpreter.value;

public class BooleanValue extends Value
{
    private final boolean value;

    public BooleanValue(boolean value)
    {
        this.value = value;
    }

    public boolean isValue()
    {
        return this.value;
    }
}
