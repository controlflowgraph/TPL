package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedCast extends TypedSegment
{
    private final TypedSegment source;

    public TypedCast(Type type, TypedSegment source)
    {
        super(type);
        this.source = source;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String temp = env.getTemp();
        String source = this.source.toIR(env);
        String conversion = this.source.getType().getName() + "->" + getType();
        env.addInstruction(new IRInstruction("cast", conversion, temp, List.of(source), List.of()));
        return temp;
    }
}
