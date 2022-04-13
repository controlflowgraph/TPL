package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.typed.tree.segment.TypedValue;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedValue extends UntypedSegment
{
    private final String value;
    private final Type type;

    public UntypedValue(Position position, String value, Type type)
    {
        super(position);
        this.value = value;
        this.type = type;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        return new TypedValue(this.type, this.value);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to value!", getPosition());
    }
}
