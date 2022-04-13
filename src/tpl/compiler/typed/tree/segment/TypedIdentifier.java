package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedIdentifier extends TypedSegment
{
    private final boolean global;
    private final String name;

    public TypedIdentifier(Type type, boolean global, String name)
    {
        super(type);
        this.global = global;
        this.name = name;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String temp = env.getTemp();
        String g = this.global ? "global" : "local";
        env.addInstruction(new IRInstruction("load-" + g, temp, List.of(this.name), List.of(getType())));
        return temp;
    }
}
