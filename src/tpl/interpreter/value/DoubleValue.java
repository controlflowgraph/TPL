package tpl.interpreter.value;

public class DoubleValue extends Value
{
    private final double value;

    public DoubleValue(double value)
    {
        this.value = value;
    }

    public double getValue()
    {
        return this.value;
    }
}
