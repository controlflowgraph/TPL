package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.GenericType;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.typed.tree.segment.TypedTuple;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.ArrayList;
import java.util.List;

public class UntypedTuple extends UntypedSegment
{
    private final List<UntypedSegment> sources;

    public UntypedTuple(Position position, List<UntypedSegment> sources)
    {
        super(position);
        this.sources = sources;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        // TODO: add position to the typed structures / segments
        List<TypedSegment> sources = new ArrayList<>();
        List<Type> types = new ArrayList<>();
        for (UntypedSegment source : this.sources)
        {
            TypedSegment segment = source.checkType(env);
            sources.add(segment);
            types.add(segment.getType());
        }
        return new TypedTuple(new GenericType(getPosition(), new SingleType(getPosition(), "Tuple"), types), sources);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to tuple!", getPosition());
    }
}
