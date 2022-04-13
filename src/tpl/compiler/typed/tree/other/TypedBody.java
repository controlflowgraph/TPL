package tpl.compiler.typed.tree.other;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.util.TPLException;
import tpl.compiler.util.TPLPositionedException;

import java.util.List;

public class TypedBody extends TypedStructure
{
    private final List<TypedStructure> content;

    public TypedBody(Type type, List<TypedStructure> content)
    {
        super(type);
        this.content = content;
    }

    // TODO: add positions to the typed tree

    @Override
    public boolean isExiting()
    {
        for (int i = 0; i < this.content.size(); i++)
        {
            if(this.content.get(i).isExiting())
            {
                if(i + 1 != this.content.size())
                    throw new TPLException("Unreachable statements after return!");
                return true;
            }
        }
        return false;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        // TODO!
        for (TypedStructure typedStructure : this.content)
        {
            typedStructure.toIR(env);
        }
        return null;
    }
}
