package tpl.compiler.untyped.tree.expression;

import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.segment.UntypedSegment;
import tpl.compiler.util.Position;

public class UntypedExpression extends UntypedStructure
{
    private final UntypedSegment segment;

    public UntypedExpression(Position position, UntypedSegment segment)
    {
        super(position);
        this.segment = segment;
    }

    public UntypedSegment getSegment()
    {
        return this.segment;
    }

    @Override
    public TypedExpression checkType(TypeEnvironment env)
    {
        TypedSegment segment = this.segment.checkType(env);
        return new TypedExpression(segment.getType(), segment);
    }

    private static final Type VOID = new SingleType(null, "void");
}
