package tpl.interpreter.value;

import java.util.HashMap;
import java.util.Map;

public class ObjectValue extends Value
{
    private final Map<String, Value> attributes = new HashMap<>();

    public Value get(String attr)
    {
        return this.attributes.get(attr);
    }

    public void set(String attr, Value value)
    {
        this.attributes.put(attr, value);
    }

    @Override
    public String toString()
    {
        return "ObjectValue{" +
                "attributes=" + attributes +
                '}';
    }
}
