package tpl.compiler.bc;

import tpl.compiler.type.tree.Type;

import java.util.HashMap;
import java.util.Map;

public class LocalsIndexManager
{
    private int counter = 0;
    private final Map<String, Integer> locals = new HashMap<>();

    public int getIndexOrCreate(String name, Type type)
    {
        // TODO: this doesnt take into account when there are two variables that are called the same (differentiate with type)
        if(!this.locals.containsKey(name))
        {
            this.locals.put(getIdentifier(name, type), this.counter);
            this.counter += Math.max(1, type.getByteSize() / 4);
        }

        return getIndex(name, type);
    }

    public int getIndex(String name, Type type)
    {
        return this.locals.get(getIdentifier(name, type));
    }

    private String getIdentifier(String name, Type type)
    {
        return name;
    }

    public int getCount()
    {
        return this.counter;
    }
}
