package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.typed.tree.segment.TypedUnaryOperator;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedUnaryOperator extends UntypedSegment
{
    private final String operator;
    private final UntypedSegment source;

    public UntypedUnaryOperator(Position position, String operator, UntypedSegment source)
    {
        super(position);
        this.operator = operator;
        this.source = source;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        TypedSegment source = this.source.checkType(env);
        Type type = env.getOperatorResult(getPosition(), this.operator, source.getType());
        return new TypedUnaryOperator(type, this.operator, source);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to unary operation!", getPosition());
    }
}
