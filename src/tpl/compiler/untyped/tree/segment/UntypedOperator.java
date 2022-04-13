package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedOperator extends UntypedSegment
{
    private final String operator;

    public UntypedOperator(Position position, String operator)
    {
        super(position);
        this.operator = operator;
    }

    public String getOperator()
    {
        return this.operator;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        throw new RuntimeException("Should not occur");
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new RuntimeException("Should not occur");
    }
}
