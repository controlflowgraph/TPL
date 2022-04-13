package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;
import java.util.Map;

public class TypedUnaryOperator extends TypedSegment
{
    private final String operator;
    private final TypedSegment source;
    public TypedUnaryOperator(Type type, String operator, TypedSegment source)
    {
        super(type);
        this.operator = operator;
        this.source = source;
    }

    private static final Map<String, String> NAMES = Map.of(
            "!", "neg",
            "-", "neg"
    );

    @Override
    public String toIR(IREnvironment env)
    {
        String source = this.source.toIR(env);
        String temp = env.getTemp();
        env.addInstruction(new IRInstruction(NAMES.get(this.operator), temp, List.of(source), List.of()));
        return temp;
    }
}
