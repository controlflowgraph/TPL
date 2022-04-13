package tpl.compiler.typed.tree.other;

import tpl.compiler.type.tree.Type;
import tpl.compiler.util.Position;

public record TypedField(Position position, boolean constant, Type type, String name)
{
}
