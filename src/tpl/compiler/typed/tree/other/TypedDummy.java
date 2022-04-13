package tpl.compiler.typed.tree.other;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;

public class TypedDummy extends TypedStructure
{
    public TypedDummy(Type type)
    {
        super(type);
    }

    @Override
    public boolean isExiting()
    {
        return false;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        return null;
    }
}
