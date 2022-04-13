package tpl.interpreter.value;

public class ArrayValue extends Value
{
    private final Value[] values;

    public ArrayValue(int size)
    {
        this.values = new Value[size];
    }

    public Value get(int index)
    {
        return this.values[index];
    }

    public void set(int index, Value value)
    {
        this.values[index] = value;
    }
}
