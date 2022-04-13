package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedIdentifierAssignment;
import tpl.compiler.typed.tree.segment.TypedIdentifier;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedIdentifier extends UntypedSegment
{
    private final String name;

    public UntypedIdentifier(Position position, String name)
    {
        super(position);
        this.name = name;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        if(!env.isDefined(this.name))
            throw new TPLPositionedException("Variable '" + this.name + "' not defined!", getPosition());
        Type type = env.get(this.name).substitute(env.getCurrentSubstituteMapping());
        boolean global = env.isGlobal(this.name);
        return new TypedIdentifier(type, global, this.name);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        if(!env.isDefined(this.name))
            throw new TPLPositionedException("Variable '" + this.name + "' not defined!", getPosition());

        Type expected = env.get(this.name).substitute(env.getCurrentSubstituteMapping());
        if(!type.getType().matches(expected))
            throw new TPLPositionedException("Expected type " + expected + " but got " + type.getType(), getPosition());

        if(env.isDeclared(this.name) && env.isConstant(this.name))
            throw new TPLPositionedException("Variable '" + this.name + "' is constant!", getPosition());

        boolean global = env.isGlobal(this.name);

        return new TypedIdentifierAssignment(expected, global, this.name, type);
    }
}
