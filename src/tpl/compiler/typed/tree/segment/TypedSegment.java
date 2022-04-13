package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;

public abstract class TypedSegment
{
    private final Type type;

    public TypedSegment(Type type)
    {
        this.type = type;
    }

    public Type getType()
    {
        return this.type;
    }

    public abstract String toIR(IREnvironment env);
}
