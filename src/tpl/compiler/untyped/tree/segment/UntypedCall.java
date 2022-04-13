package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.function.TypedFunction;
import tpl.compiler.typed.tree.segment.TypedCall;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.ArrayList;
import java.util.List;

public class UntypedCall extends UntypedSegment
{
    private final String name;
    private final List<UntypedSegment> parameters;

    public UntypedCall(Position position, String name, List<UntypedSegment> parameters)
    {
        super(position);
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        List<TypedSegment> parameters = this.parameters.stream().map(p -> p.checkType(env)).toList();
        List<Type> types = parameters.stream().map(TypedSegment::getType).toList();
        TypedFunction function = env.getFunction(getPosition(), this.name, types);
        return new TypedCall(function.getType(), function, parameters);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to function call!", getPosition());
    }
}
