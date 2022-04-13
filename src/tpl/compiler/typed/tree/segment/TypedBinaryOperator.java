package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.List;

public class TypedBinaryOperator extends TypedSegment
{
    private final String operator;
    private final TypedSegment left;
    private final TypedSegment right;

    public TypedBinaryOperator(Type type, String operator, TypedSegment left, TypedSegment right)
    {
        super(type);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String left = this.left.toIR(env);
        String right = this.right.toIR(env);
        String temp = env.getTemp();
        env.addInstruction(new IRInstruction(this.operator, getType().toString(), temp, List.of(left, right), List.of(getType(), this.left.getType(), this.right.getType())));
        return temp;
    }
}
