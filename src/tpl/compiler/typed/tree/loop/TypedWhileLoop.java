package tpl.compiler.typed.tree.loop;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.other.TypedField;

import java.util.List;

public class TypedWhileLoop extends TypedStructure
{
    private final TypedExpression condition;
    private final TypedStructure body;
    private final List<TypedField> scoped;

    public TypedWhileLoop(Type type, TypedExpression condition, TypedStructure body, List<TypedField> scoped)
    {
        super(type);
        this.condition = condition;
        this.body = body;
        this.scoped = scoped;
    }

    @Override
    public boolean isExiting()
    {
        return false;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        List<String> names = this.scoped.stream().map(TypedField::name).toList();
        List<Type> types = this.scoped.stream().map(TypedField::type).toList();
        env.addInstruction(new IRInstruction("scope", names, types));
        String start = env.getLabel();
        String condition = env.getLabel();

        env.addJump(condition);
        env.setLabel(start);
        this.body.toIR(env);
        env.setLabel(condition);
        String temp = this.condition.toIR(env);
        env.addBranch(start, temp);
        env.addInstruction(new IRInstruction("unscope", names, types));
        return null;
    }
}
