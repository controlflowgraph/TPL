package tpl.compiler.typed.tree.other;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedExpression;

import java.util.List;

public class TypedIfStatement extends TypedStructure
{
    private final TypedExpression condition;
    private final TypedStructure body;
    private final TypedStructure follower;
    private final List<TypedField> scoped1;
    private final List<TypedField> scoped2;

    public TypedIfStatement(Type type, TypedExpression condition, TypedStructure body, TypedStructure follower, List<TypedField> scoped1, List<TypedField> scoped2)
    {
        super(type);
        this.condition = condition;
        this.body = body;
        this.follower = follower;
        this.scoped1 = scoped1;
        this.scoped2 = scoped2;
    }

    @Override
    public boolean isExiting()
    {
        return this.body.isExiting() && this.follower != null && this.follower.isExiting();
    }

    @Override
    public String toIR(IREnvironment env)
    {
        List<String> names1 = this.scoped1.stream().map(TypedField::name).toList();
        List<Type> types1 = this.scoped1.stream().map(TypedField::type).toList();

        env.addInstruction(new IRInstruction("scope", names1, types1));
        String temp = this.condition.toIR(env);
        String next = env.getLabel();
        String end = env.getLabel();
        String cond = env.getTemp();
        env.addInstruction(new IRInstruction("neg", cond, List.of(temp), List.of()));
        env.addBranch(next, cond);
        this.body.toIR(env);
        env.addJump(end);
        env.addInstruction(new IRInstruction("unscope", names1, types1));

        env.setLabel(next);
        if(this.follower != null)
        {
            List<String> names2 = this.scoped2.stream().map(TypedField::name).toList();
            List<Type> types2 = this.scoped2.stream().map(TypedField::type).toList();
            env.addInstruction(new IRInstruction("scope", names2, types2));
            this.follower.toIR(env);
            env.addInstruction(new IRInstruction("unscope", names2, types2));
        }

        env.setLabel(end);
        return null;
    }
}
