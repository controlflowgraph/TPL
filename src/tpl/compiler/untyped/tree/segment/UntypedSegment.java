package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;

public abstract class UntypedSegment
{
    private final Position position;

    public UntypedSegment(Position position)
    {
        this.position = position;
    }

    public Position getPosition()
    {
        return this.position;
    }

    public abstract TypedSegment checkType(TypeEnvironment env);

    public abstract TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type);
}
