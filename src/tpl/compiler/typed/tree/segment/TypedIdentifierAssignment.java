package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedIdentifierAssignment extends TypedSegment
{
    private final boolean global;
    private final String variable;
    private final TypedSegment value;

    public TypedIdentifierAssignment(Type type, boolean global, String variable, TypedSegment value)
    {
        super(type);
        this.global = global;
        this.variable = variable;
        this.value = value;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String val = this.value.toIR(env);
        String g = this.global ? "global" : "local";
        env.addInstruction(new IRInstruction("set-" + g, this.variable, List.of(val), List.of(getType())));
        return null;
    }
}
