package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedIdentifierAssignment;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedAssignment extends UntypedSegment
{
    private final UntypedSegment destination;
    private final UntypedSegment value;

    public UntypedAssignment(Position position, UntypedSegment destination, UntypedSegment value)
    {
        super(position);
        this.destination = destination;
        this.value = value;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        TypedSegment value = this.value.checkType(env);
        Type valueType = value.getType();

        TypedSegment destination = this.destination.checkAssignment(env, value);
        Type destinationType = destination.getType();

        if(!valueType.matches(destinationType))
            throw new TPLPositionedException("Value doesn't match assigning variable! (" + destinationType + " <-> " + valueType + ")", getPosition());

        return destination;
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to assignment!", getPosition());
    }
}
