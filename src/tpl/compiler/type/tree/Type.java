package tpl.compiler.type.tree;

import tpl.compiler.util.Position;

import java.util.Map;
import java.util.Set;

public abstract class Type
{
    public abstract boolean isPrimitive();
    public abstract boolean matches(Type type);
    public abstract Position getPosition();

    public abstract void collectGenericTypes(Type type, Map<String, Type> types);
    public abstract void collectGenerics(Set<String> generics);
    public abstract Type substitute(Map<String, Type> substitutes);
    public abstract int getDepth();
    public abstract String getName();
    public abstract String toSignature();
    public abstract int getByteSize();
}
