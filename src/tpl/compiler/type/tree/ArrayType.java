package tpl.compiler.type.tree;

import tpl.compiler.util.Position;

import java.util.Map;
import java.util.Set;

public class ArrayType extends Type
{
    private final Position position;
    private final Type contentType;

    public ArrayType(Position position, Type contentType)
    {
        this.position = position;
        this.contentType = contentType;
    }

    @Override
    public boolean isPrimitive()
    {
        return false;
    }

    @Override
    public boolean matches(Type type)
    {
        if(!(type instanceof ArrayType a)) return type instanceof NullType;
        return this.contentType.matches(a.contentType);
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
        ArrayType array = (ArrayType) type;
        this.contentType.collectGenericTypes(array.contentType, types);
    }

    @Override
    public void collectGenerics(Set<String> generics)
    {
        this.contentType.collectGenerics(generics);
    }

    @Override
    public Type substitute(Map<String, Type> substitutes)
    {
        return new ArrayType(this.position, this.contentType.substitute(substitutes));
    }

    public Type getContentType()
    {
        return this.contentType;
    }

    @Override
    public int getDepth()
    {
        return this.contentType.getDepth() + 1;
    }

    @Override
    public String getName()
    {
        return "Array";
    }

    @Override
    public String toSignature()
    {
        return "[" + this.contentType.toSignature();
    }

    @Override
    public int getByteSize()
    {
        return 4;
    }

    @Override
    public String toString()
    {
        return this.contentType + "[]";
    }
}
