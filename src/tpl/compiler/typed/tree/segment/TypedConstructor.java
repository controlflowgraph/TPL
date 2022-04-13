package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.ArrayList;
import java.util.List;

public class TypedConstructor extends TypedSegment
{
    private final boolean array;
    private final List<String> attributes;
    private final List<TypedSegment> arguments;

    public TypedConstructor(Type type, List<TypedSegment> arguments)
    {
        this(type, true, null, arguments);
    }

    public TypedConstructor(Type type, List<String> attributes, List<TypedSegment> arguments)
    {
        this(type, false, attributes, arguments);
    }

    private TypedConstructor(Type type, boolean array, List<String> attributes, List<TypedSegment> arguments)
    {
        super(type);
        this.array = array;
        this.arguments = arguments;
        this.attributes = attributes;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        String temp = env.getTemp();
        if(this.array)
        {
            List<String> sources = new ArrayList<>();
            for (TypedSegment argument : this.arguments)
            {
                sources.add(argument.toIR(env));
            }
            env.addInstruction(new IRInstruction("new-array", temp, sources, List.of(getType())));
        }
        else
        {
            String typeName = null;
            env.addType(getType(), this.attributes, this.arguments.stream().map(TypedSegment::getType).toList());
            env.addInstruction(new IRInstruction("new-object", typeName, temp, List.of(), List.of(getType())));

            List<String> sources = new ArrayList<>();
            sources.add(temp);
            List<Type> types = new ArrayList<>();
            types.add(getType());
            for (int i = 0; i < this.attributes.size(); i++)
            {
                sources.add(this.arguments.get(i).toIR(env));
                types.add(this.arguments.get(i).getType());
            }
            env.addInstruction(new IRInstruction("init-object",  null, sources, types));
        }
        return temp;
    }
}
