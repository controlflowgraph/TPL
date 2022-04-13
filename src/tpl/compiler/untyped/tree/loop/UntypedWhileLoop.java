package tpl.compiler.untyped.tree.loop;

import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.loop.TypedWhileLoop;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.expression.UntypedExpression;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.List;

public class UntypedWhileLoop extends UntypedStructure
{
    private static final Type BOOLEAN_TYPE = new SingleType(null, "boolean");

    private final UntypedExpression condition;
    private final UntypedStructure body;

    public UntypedWhileLoop(Position position, UntypedExpression condition, UntypedStructure body)
    {
        super(position);
        this.condition = condition;
        this.body = body;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        env.enterScope();
        TypedExpression condition = this.condition.checkType(env);

        if(!BOOLEAN_TYPE.matches(condition.getType()))
            throw new TPLPositionedException("Expected boolean value in condition!", getPosition());

        TypedStructure body = this.body.checkType(env);
        List<TypedField> scoped = env.leaveScope();
        return new TypedWhileLoop(null, condition, body, scoped);
    }
}
