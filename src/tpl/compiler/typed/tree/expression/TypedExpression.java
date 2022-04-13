package tpl.compiler.typed.tree.expression;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.segment.TypedSegment;

public class TypedExpression extends TypedStructure
{
    private final TypedSegment segment;

    public TypedExpression(Type type, TypedSegment segment)
    {
        super(type);
        this.segment = segment;
    }

    @Override
    public boolean isExiting()
    {
        return false;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        return this.segment.toIR(env);
    }
}
