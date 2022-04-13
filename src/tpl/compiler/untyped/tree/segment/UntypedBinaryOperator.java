package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.segment.TypedBinaryOperator;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedBinaryOperator extends UntypedSegment
{
    private final String operator;
    private final UntypedSegment left;
    private final UntypedSegment right;

    public UntypedBinaryOperator(Position position, String operator, UntypedSegment left, UntypedSegment right)
    {
        super(position);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        TypedSegment left = this.left.checkType(env);
        TypedSegment right = this.right.checkType(env);
        Type result = env.getOperatorResult(getPosition(), this.operator, left.getType(), right.getType());
        return new TypedBinaryOperator(result, this.operator, left, right);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to assignment!", getPosition());
    }
}
