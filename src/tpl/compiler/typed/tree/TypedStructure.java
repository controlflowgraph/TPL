package tpl.compiler.typed.tree;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.type.tree.Type;

public abstract class TypedStructure
{
    private final Type type;

    public TypedStructure(Type type)
    {
        this.type = type;
    }

    public Type getType()
    {
        return this.type;
    }

    public abstract boolean isExiting();

    public abstract String toIR(IREnvironment env);
}
