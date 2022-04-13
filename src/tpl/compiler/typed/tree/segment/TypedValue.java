package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;

import java.util.List;

public class TypedValue extends TypedSegment
{
    private final String value;

    public TypedValue(Type type, String value)
    {
        super(type);
        this.value = value;
    }

    public String getValue()
    {
        return this.value;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String temp = env.getTemp();
        String name = getType().getName().toLowerCase();
        env.addInstruction(new IRInstruction("load-constant", name, temp, List.of(this.value), List.of()));
        return temp;
    }
}
