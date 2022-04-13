package tpl.compiler.type.tree;

import tpl.compiler.util.Position;

import java.util.Map;
import java.util.Set;

public class SingleType extends Type
{
    private static final Map<String, String> PRIMITIVE_TYPES = Map.of(
            "boolean", "Z",
            "char", "C",
            "byte", "B",
            "short", "S",
            "int", "I",
            "long", "J",
            "float", "F",
            "double", "D",
            "void", "V"
    );

    private static final Map<String, Integer> SIZES = Map.of(
            "void", 0,
            "boolean", 1,
            "char", 1,
            "byte", 1,
            "short", 2,
            "int", 4,
            "long", 8,
            "float", 4,
            "double", 8
    );

    private final Position position;
    private final String name;

    public SingleType(Position position, String name)
    {
        this.position = position;
        this.name = name;
    }

    @Override
    public boolean isPrimitive()
    {
        return PRIMITIVE_TYPES.containsKey(this.name);
    }

    @Override
    public boolean matches(Type type)
    {
        if(!(type instanceof SingleType s)) return !isPrimitive() && type instanceof NullType;
        return this.name.equals(s.name);
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
        return this;
    }

    @Override
    public int getDepth()
    {
        return 1;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String toSignature()
    {
        if(isPrimitive())
        {
            return PRIMITIVE_TYPES.get(this.name);
        }
        return this.name; //"L" + this.name + ";";
    }

    @Override
    public int getByteSize()
    {
        return SIZES.getOrDefault(this.name, 4);
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
