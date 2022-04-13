package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.typed.tree.segment.TypedSelection;
import tpl.compiler.typed.tree.segment.TypedSelectionAssignment;
import tpl.compiler.untyped.tree.other.UntypedField;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.HashMap;
import java.util.Map;

public class UntypedSelection extends UntypedSegment
{
    private final UntypedSegment source;
    private final String attribute;

    public UntypedSelection(Position position, UntypedSegment source, String attribute)
    {
        super(position);
        this.source = source;
        this.attribute = attribute;
    }

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        TypedSegment source = this.source.checkType(env);
        TypedField attributeField = env.getAttributeField(getPosition(), source.getType(), this.attribute);
        Type typeOfTypeStruct = env.getTypeOfTypeStruct(source.getType().getName());
        Map<String, Type> subs = new HashMap<>();
        typeOfTypeStruct.collectGenericTypes(source.getType(), subs);
        return new TypedSelection(attributeField.type(), source, this.attribute);
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment value)
    {
        TypedSegment source = this.source.checkType(env);
        TypedField attributeType = env.getAttributeField(getPosition(), source.getType(), this.attribute);
        Type result = env.getSubstituted(attributeType.type(), value.getType());

        if(!value.getType().matches(result))
            throw new TPLPositionedException("Attribute of type " + attributeType + " but received " + value.getType(), getPosition());

        if(attributeType.constant())
            throw new TPLPositionedException("Attribute " + this.attribute + " of type " + source.getType() + " is constant!", getPosition());

        return new TypedSelectionAssignment(result, source, this.attribute, value);
    }
}
