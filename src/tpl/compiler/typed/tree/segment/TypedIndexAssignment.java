package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedIndexAssignment extends TypedSegment
{
    private final TypedSegment source;
    private final TypedSegment key;
    private final TypedSegment value;

    public TypedIndexAssignment(Type type, TypedSegment source, TypedSegment key, TypedSegment value)
    {
        super(type);
        this.source = source;
        this.key = key;
        this.value = value;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String source = this.source.toIR(env);
        String key = this.key.toIR(env);
        String value = this.value.toIR(env);
        env.addInstruction(new IRInstruction("set-index", null, List.of(source, key, value), List.of(this.source.getType(), getType())));
        return null;
    }
}
