package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;

import java.util.ArrayList;
import java.util.List;

public class TypedTuple extends TypedSegment
{
    private final List<TypedSegment> sources;
    public TypedTuple(Type type, List<TypedSegment> sources)
    {
        super(type);
        this.sources = sources;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        // TODO: refine with correct new instruction
        String temp = env.getTemp();

        List<Type> fields = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (TypedSegment source : this.sources)
        {
            fields.add(source.getType());
            names.add("_" + names.size());
        }

        env.addType(getType(), names, fields);
        env.addInstruction(new IRInstruction("new-object", temp, List.of(), List.of(getType())));



        List<String> sources = new ArrayList<>();
        sources.add(temp);
        for (TypedSegment source : this.sources)
        {
            sources.add(source.toIR(env));
        }

        List<Type> types = new ArrayList<>();
        types.add(getType());
        types.addAll(fields);

        env.addInstruction(new IRInstruction("init-object",  null, sources, types));

        return temp;
    }
}
