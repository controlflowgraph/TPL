package tpl.compiler.type.tree;

import tpl.compiler.util.Position;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenericType extends Type
{
    private final Position position;
    private final Type baseType;
    private final List<Type> generics;

    public GenericType(Position position, Type baseType, List<Type> generics)
    {
        this.position = position;
        this.baseType = baseType;
        this.generics = generics;
    }

    public List<Type> getGenerics()
    {
        return this.generics;
    }

    @Override
    public boolean isPrimitive()
    {
        return false;
    }

    @Override
    public boolean matches(Type type)
    {
        if(!(type instanceof GenericType g)) return type instanceof NullType;
        if(!this.baseType.matches(g.baseType)) return false;
        if(this.generics.size() != g.generics.size()) return false;
        for(int i = 0; i < this.generics.size(); i++)
        {
            if(!this.generics.get(i).matches(g.generics.get(i)))
            {
                return false;
            }
        }
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
        GenericType generic = (GenericType) type;
        this.baseType.collectGenericTypes(generic.baseType, types);
        for(int i = 0; i < this.generics.size(); i++)
        {
            this.generics.get(i).collectGenericTypes(generic.generics.get(i), types);
        }
    }

    @Override
    public void collectGenerics(Set<String> generics)
    {
        this.baseType.collectGenerics(generics);
        this.generics.forEach(g -> g.collectGenerics(generics));
    }

    @Override
    public Type substitute(Map<String, Type> substitutes)
    {
        Type base = this.baseType.substitute(substitutes);
        List<Type> types = this.generics.stream().map(t -> t.substitute(substitutes)).toList();
        return new GenericType(this.position, base, types);
    }

    @Override
    public int getDepth()
    {
        int max = this.baseType.getDepth();
        for (Type generic : this.generics)
        {
            max = Math.max(max, generic.getDepth());
        }
        return max + 1;
    }

    @Override
    public String getName()
    {
        return this.baseType.getName();
    }

    @Override
    public String toSignature()
    {
//        List<String> generics = this.generics.stream().map(Type::toSignature).toList();
//        return this.baseType.toSignature() + "_" + String.join("_", generics);
        return toString()
                .replace(" ", "")
                .replace("<", " ")
                .replace(">", "=");
    }

    @Override
    public int getByteSize()
    {
        return 4;
    }

    @Override
    public String toString()
    {
        return this.baseType + "<" + String.join(", ", this.generics.stream().map(Object::toString).toList()) + ">";
    }
}
