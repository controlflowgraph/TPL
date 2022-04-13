package tpl.compiler.untyped.tree.other;

import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.other.TypedReturn;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.expression.UntypedExpression;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedReturn extends UntypedStructure
{
    private final UntypedExpression expression;

    public UntypedReturn(Position position, UntypedExpression expression)
    {
        super(position);
        this.expression = expression;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        if(this.expression == null)
        {
            return new TypedReturn(VOID, null);
        }

        TypedExpression expression = this.expression.checkType(env);
        Type type = expression.getType();
        Type resultType = env.getCurrentResultType();

        if(!type.matches(resultType))
            throw new TPLPositionedException("Expected return type " + resultType + " but got " + type + "!", getPosition());

        return new TypedReturn(resultType, expression);
    }

    private static final Type VOID = new SingleType(null, "void");
}
