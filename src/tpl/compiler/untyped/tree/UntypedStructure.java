package tpl.compiler.untyped.tree;

import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.util.Position;

import java.util.Set;

public abstract class UntypedStructure
{
    private final Position position;

    public UntypedStructure(Position position)
    {
        this.position = position;
    }

    public void collectIncludePaths(Set<String> paths)
    {

    }

    public Position getPosition()
    {
        return this.position;
    }

    public abstract TypedStructure checkType(TypeEnvironment env);
}
