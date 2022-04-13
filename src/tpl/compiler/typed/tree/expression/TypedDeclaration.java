package tpl.compiler.typed.tree.expression;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.other.TypedField;

public class TypedDeclaration extends TypedStructure
{
    private final TypedField field;
    private final TypedExpression expression;

    public TypedDeclaration(Type type, TypedField field, TypedExpression expression)
    {
        super(type);
        this.field = field;
        this.expression = expression;
    }

    @Override
    public boolean isExiting()
    {
        return false;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        this.expression.toIR(env);
        return null;
    }
}
