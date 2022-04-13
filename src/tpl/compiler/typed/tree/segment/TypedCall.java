package tpl.compiler.typed.tree.segment;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.function.TypedFunction;

import java.util.ArrayList;
import java.util.List;

public class TypedCall extends TypedSegment
{
    private final String name;
    private final int id;
    private final boolean external;
    private final List<TypedSegment> parameters;

    public TypedCall(Type type, TypedFunction function, List<TypedSegment> parameters)
    {
        super(type);
        this.name = function.getName();
        this.id = function.getId();
        this.external = function.isExternal();
        this.parameters = parameters;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        // TODO: load reference before loading the parameters
        List<String> sources = new ArrayList<>();
        for (TypedSegment parameter : this.parameters)
        {
            sources.add(parameter.toIR(env));
        }
        String temp = env.getTemp();
        String nat = this.external ? "-native" : "";
        String name = this.external ? this.name + this.parameters.stream().map(TypedSegment::getType).toList() : Integer.toString(this.id);
        env.addInstruction(new IRInstruction("call" + nat, name, temp, sources, List.of()));
        return temp;
    }
}
