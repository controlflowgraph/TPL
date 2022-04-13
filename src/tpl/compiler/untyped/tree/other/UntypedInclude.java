package tpl.compiler.untyped.tree.other;

import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.other.TypedDummy;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.util.Position;

import java.util.Set;

public class UntypedInclude extends UntypedStructure
{
    private final String path;

    public UntypedInclude(Position position, String path)
    {
        super(position);
        this.path = path;
    }

    @Override
    public void collectIncludePaths(Set<String> paths)
    {
        paths.add(this.path);
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        return new TypedDummy(null);
    }
}
