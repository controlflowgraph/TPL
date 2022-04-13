package tpl.compiler.untyped.tree.other;

import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.other.TypedBody;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.util.Position;

import java.util.ArrayList;
import java.util.List;

public class UntypedBody extends UntypedStructure
{
    private final List<UntypedStructure> content;

    public UntypedBody(Position position, List<UntypedStructure> content)
    {
        super(position);
        this.content = content;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        List<TypedStructure> content = new ArrayList<>();
        for (UntypedStructure untypedStructure : this.content)
        {
            content.add(untypedStructure.checkType(env));
        }
        return new TypedBody(null, content);
    }
}
