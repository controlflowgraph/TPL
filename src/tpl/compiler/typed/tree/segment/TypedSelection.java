package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedSelection extends TypedSegment
{
    private final TypedSegment source;
    private final String attribute;

    public TypedSelection(Type type, TypedSegment source, String attribute)
    {
        super(type);
        this.source = source;
        this.attribute = attribute;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String source = this.source.toIR(env);
        String temp = env.getTemp();
        env.addInstruction(new IRInstruction("get-attribute", this.attribute, temp, List.of(source), List.of(this.source.getType(), getType())));
        return temp;
    }
}
