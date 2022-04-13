package tpl.compiler.untyped.tree.other;

import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.other.TypedDummy;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.util.Position;

import java.util.List;

public class UntypedType extends UntypedStructure
{
    private final List<String> generics;
    private final String name;
    private final List<UntypedField> fields;

    public UntypedType(Position position, List<String> generics, String name, List<UntypedField> fields)
    {
        super(position);
        this.generics = generics;
        this.name = name;
        this.fields = fields;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        env.addType(getPosition(), this);
        return new TypedDummy(null);
    }

    public List<String> getGenerics()
    {
        return this.generics;
    }

    public String getName()
    {
        return this.name;
    }

    public List<UntypedField> getFields()
    {
        return this.fields;
    }
}
