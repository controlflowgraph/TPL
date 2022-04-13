package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedIndex extends TypedSegment
{
    private final TypedSegment source;
    private final TypedSegment key;
    public TypedIndex(Type type, TypedSegment source, TypedSegment key)
    {
        super(type);
        this.source = source;
        this.key = key;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String source = this.source.toIR(env);
        String key = this.key.toIR(env);
        String temp = env.getTemp();
        env.addInstruction(new IRInstruction("get-index", temp, List.of(source, key), List.of(this.source.getType(), getType())));
        return temp;
    }
}
