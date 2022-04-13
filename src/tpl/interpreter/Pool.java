package tpl.interpreter;

import java.util.ArrayList;
import java.util.List;

public class Pool <T>
{
    private final List<T> values = new ArrayList<>();

    public int add(T value)
    {
        int index = this.values.indexOf(value);
        if(index == -1)
        {
            index = this.values.size();
            this.values.add(value);
        }
        return index;
    }

    public T get(int index)
    {
        return this.values.get(index);
    }

    public int size()
    {
        return this.values.size();
    }

    @Override
    public String toString()
    {
        return this.values.toString();
    }
}
