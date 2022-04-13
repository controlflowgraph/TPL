package tpl.compiler.untyped.tree.expression;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.expression.TypedDeclaration;
import tpl.compiler.typed.tree.expression.TypedExpression;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.other.UntypedField;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

public class UntypedDeclaration extends UntypedStructure
{
    private final UntypedField field;
    private final UntypedExpression expression;

    public UntypedDeclaration(Position position, UntypedField field, UntypedExpression expression)
    {
        super(position);
        this.field = field;
        this.expression = expression;
    }

    @Override
    public TypedDeclaration checkType(TypeEnvironment env)
    {
        if(env.isLocallyDefined(this.field.name()))
            throw new TPLPositionedException("Variable '" + this.field.name() + "' already defined!", getPosition());

        Type fieldType = this.field.type().substitute(env.getCurrentSubstituteMapping());
        TypedField typedField = new TypedField(getPosition(), this.field.constant(), fieldType, this.field.name());
        env.define(typedField);

        TypedExpression expression = this.expression.checkType(env);
        return new TypedDeclaration(null, typedField, expression);
    }
}
