package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedCast;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedCast extends UntypedSegment
{
    private final UntypedSegment segment;
    private final Type type;

    public UntypedCast(Position position, UntypedSegment segment, Type type)
    {
        super(position);
        this.segment = segment;
        this.type = type;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        TypedSegment segment = this.segment.checkType(env);
        Type type = this.type.substitute(env.getCurrentSubstituteMapping());

        if(!type.isPrimitive() || !segment.getType().isPrimitive())
            throw new TPLPositionedException("Unable to perform cast " + segment.getType() + " -> " + type, getPosition());

        return new TypedCast(type, segment);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        return null;
    }
}
