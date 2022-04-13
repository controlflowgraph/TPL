package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedSelectionAssignment extends TypedSegment
{
    private final TypedSegment source;
    private final String attribute;
    private final TypedSegment value;

    public TypedSelectionAssignment(Type type, TypedSegment source, String attribute, TypedSegment value)
    {
        super(type);
        this.source = source;
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String source = this.source.toIR(env);
        String value = this.value.toIR(env);
        env.addInstruction(new IRInstruction("set-attribute", this.attribute, null, List.of(source, value), List.of(this.source.getType(), getType())));
        return null;
    }
}
