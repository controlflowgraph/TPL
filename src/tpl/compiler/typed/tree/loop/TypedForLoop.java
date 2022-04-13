package tpl.compiler.typed.tree.loop;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedDeclaration;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.other.TypedField;

import java.util.List;

public class TypedForLoop extends TypedStructure
{
    private final TypedDeclaration init;
    private final TypedExpression condition;
    private final TypedExpression advancement;
    private final TypedStructure body;
    private final List<TypedField> scoped;

    public TypedForLoop(Type type, TypedDeclaration init, TypedExpression condition, TypedExpression advancement, TypedStructure body, List<TypedField> scoped)
    {
        super(type);
        this.init = init;
        this.condition = condition;
        this.advancement = advancement;
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
        this.init.toIR(env);
        String start = env.getLabel();
        String condition = env.getLabel();
        env.addJump(condition);
        //env.addInstruction(new IRInstruction("jump", "_", List.of(), List.of()));
        env.setLabel(start);
        this.body.toIR(env);
        this.advancement.toIR(env);
        env.setLabel(condition);
        String temp = this.condition.toIR(env);
        env.addBranch(start, temp);
        env.addInstruction(new IRInstruction("unscope", names, types));
        return null;
    }
}
