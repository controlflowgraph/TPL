package tpl.compiler.type.tree;

import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.Map;
import java.util.Set;

public class PlaceholderType extends Type
{
    private final Position position;
    private final String name;

    public PlaceholderType(Position position, String name)
    {
        this.position = position;
        this.name = name;
    }

    @Override
    public boolean isPrimitive()
    {
        return false;
    }

    @Override
    public boolean matches(Type type)
    {
        return true;
    }

    @Override
    public Position getPosition()
    {
        return this.position;
    }

    @Override
    public void collectGenericTypes(Type type, Map<String, Type> types)
    {
        if(type instanceof NullType) return;
        if(types.containsKey(this.name))
        {
            if(!types.get(this.name).matches(type))
            {
                throw new TPLPositionedException("Mismatching generic type definitions! (" + types.get(this.name) + " <-> " + type + ")", this.position);
            }
        }
        types.put(this.name, type);
    }

    @Override
    public void collectGenerics(Set<String> generics)
    {
        generics.add(this.name);
    }

    @Override
    public Type substitute(Map<String, Type> substitutes)
    {
        return substitutes.get(this.name);
    }

    @Override
    public int getDepth()
    {
        return 1;
    }

    @Override
    public String getName()
    {
        throw new RuntimeException("Should not occur");
    }

    @Override
    public String toSignature()
    {
        throw new RuntimeException("Should not occur");
    }

    @Override
    public int getByteSize()
    {
        throw new RuntimeException("Should not occur");
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
