package tpl.compiler.ir;

import tpl.compiler.type.tree.Type;

import java.util.List;

public class IRType
{
    private final Type type;
    private final List<String> names;
    private final List<Type> types;

    public IRType(Type type, List<String> names, List<Type> types)
    {
        this.type = type;
        this.names = names;
        this.types = types;
    }

    public Type getType()
    {
        return this.type;
    }

    public List<String> getNames()
    {
        return this.names;
    }

    public List<Type> getTypes()
    {
        return this.types;
    }

    @Override
    public String toString()
    {
        return this.type.toString() + this.types + this.names;
    }
}
