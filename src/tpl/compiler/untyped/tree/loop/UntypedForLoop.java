package tpl.compiler.untyped.tree.loop;

import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedDeclaration;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.loop.TypedForLoop;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.expression.UntypedDeclaration;
import tpl.compiler.untyped.tree.expression.UntypedExpression;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.List;

public class UntypedForLoop extends UntypedStructure
{
    private static final Type BOOLEAN_TYPE = new SingleType(null, "boolean");

    private final UntypedDeclaration declaration;
    private final UntypedExpression condition;
    private final UntypedExpression advancement;
    private final UntypedStructure body;

    public UntypedForLoop(Position position, UntypedDeclaration declaration, UntypedExpression condition, UntypedExpression advancement, UntypedStructure body)
    {
        super(position);
        this.declaration = declaration;
        this.condition = condition;
        this.advancement = advancement;
        this.body = body;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        env.enterScope();
        TypedDeclaration init = this.declaration.checkType(env);
        TypedExpression condition = this.condition.checkType(env);

        if(!BOOLEAN_TYPE.matches(condition.getType()))
            throw new TPLPositionedException("Expected boolean value in condition!", getPosition());

        TypedExpression advancement = this.advancement.checkType(env);
        TypedStructure body = this.body.checkType(env);
        List<TypedField> scoped = env.leaveScope();
        return new TypedForLoop(null, init, condition, advancement, body, scoped);
    }
}
