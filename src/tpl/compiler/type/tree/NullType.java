package tpl.compiler.type.tree;

import tpl.compiler.util.Position;

import java.util.Map;
import java.util.Set;

public class NullType extends Type
{
    private final Position position;

    public NullType(Position position)
    {
        this.position = position;
    }

    @Override
    public boolean isPrimitive()
    {
        return false;
    }

    @Override
    public boolean matches(Type type)
    {
        return !type.isPrimitive();
    }

    @Override
    public Position getPosition()
    {
        return this.position;
    }

    @Override
    public void collectGenericTypes(Type type, Map<String, Type> types)
    {

    }

    @Override
    public void collectGenerics(Set<String> generics)
    {

    }

    @Override
    public Type substitute(Map<String, Type> substitutes)
    {
        return new NullType(getPosition());
    }

    @Override
    public int getDepth()
    {
        return 1;
    }

    @Override
    public String getName()
    {
        return "null";
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
        return "null";
    }
}
