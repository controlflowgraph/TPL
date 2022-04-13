package tpl.compiler.typed.tree.other;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedExpression;

import java.util.List;

public class TypedReturn extends TypedStructure
{
    private final TypedExpression expression;

    public TypedReturn(Type type, TypedExpression expression)
    {
        super(type);
        this.expression = expression;
    }

    @Override
    public boolean isExiting()
    {
        return true;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        if(this.expression == null)
        {
            env.addInstruction(new IRInstruction("return", null, List.of(), List.of(getType())));
        }
        else
        {
            String temp = this.expression.toIR(env);
            env.addInstruction(new IRInstruction("return", null, List.of(temp), List.of(getType())));
        }

        return null;
    }
}
