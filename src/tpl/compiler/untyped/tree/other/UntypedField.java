package tpl.compiler.untyped.tree.other;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.util.Position;

import java.util.Map;

public record UntypedField(Position position, boolean constant, Type type, String name)
{
    public TypedField getTypedField(Map<String, Type> substitute)
    {
        return new TypedField(this.position, this.constant, this.type.substitute(substitute), this.name);
    }
}
