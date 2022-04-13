package tpl.compiler.untyped.tree.other;

import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.typed.tree.other.TypedIfStatement;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.expression.UntypedExpression;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.List;

public class UntypedIfStatement extends UntypedStructure
{
    private static final Type BOOLEAN_TYPE = new SingleType(null, "boolean");

    private final UntypedExpression condition;
    private final UntypedStructure body;
    private final UntypedStructure follower;

    public UntypedIfStatement(Position position, UntypedExpression condition, UntypedStructure body, UntypedStructure follower)
    {
        super(position);
        this.condition = condition;
        this.body = body;
        this.follower = follower;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        TypedExpression condition = this.condition.checkType(env);

        if(!BOOLEAN_TYPE.matches(condition.getType()))
            throw new TPLPositionedException("Expected boolean value in condition!", getPosition());

        env.enterScope();
        TypedStructure body = this.body.checkType(env);
        List<TypedField> scoped1 = env.leaveScope();

        env.enterScope();
        TypedStructure follower = this.follower == null ? null : this.follower.checkType(env);
        List<TypedField> scoped2 = env.leaveScope();
        return new TypedIfStatement(null, condition, body, follower, scoped1, scoped2);
    }
}
