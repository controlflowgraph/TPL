package tpl.interpreter;

import java.util.Arrays;

public class LocalsManager
{
    private final boolean[] locals;
    private final String[] names;

    public LocalsManager(int capacity)
    {
        this.locals = new boolean[capacity];
        this.names = new String[capacity];
    }

    public int get(String name)
    {
        for(int i = 0; i < this.locals.length; i++)
        {
            if(!this.locals[i])
            {
                this.locals[i] = true;
                this.names[i] = name;
                return i;
            }
        }
        throw new RuntimeException("Capacity too small! (" + this.locals.length + ") " + Arrays.toString(this.names));
    }

    public void give(int index)
    {
        this.locals[index] = false;
        this.names[index] = null;
    }
}
