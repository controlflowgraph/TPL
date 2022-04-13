package tpl.compiler.typed.tree.function;

import tpl.compiler.ir.IREnvironment;
import tpl.compiler.ir.IRFragment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.ir.IRType;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.util.TPLException;

import java.util.List;
import java.util.Map;

public class TypedFunction extends TypedStructure
{
    private final boolean external;
    private final String name;
    private final int id;
    private final List<String> names;
    private final List<Type> parameters;
    private final TypedStructure body;
    private final List<TypedField> scoped;

    public TypedFunction(boolean external, Type result, String name, int id, List<String> names, List<Type> parameters, TypedStructure body, List<TypedField> scoped)
    {
        super(result);
        this.external = external;
        this.name = name;
        this.id = id;
        this.names = names;
        this.parameters = parameters;
        this.body = body;
        this.scoped = scoped;
    }

    public TypedFunction(boolean external, Type result, String name, int id, List<Type> parameters, TypedStructure body, List<TypedField> scoped)
    {
        this(external, result, name, id, null, parameters, body, scoped);
    }

    public int getId()
    {
        return this.id;
    }

    public boolean isExternal()
    {
        return this.external;
    }

    public String getName()
    {
        return this.name;
    }

    public List<Type> getParameters()
    {
        return this.parameters;
    }

    public boolean matchesArguments(List<Type> types)
    {
        if(this.parameters.size() != types.size()) return false;
        for(int i = 0; i < this.parameters.size(); i++)
        {
            if(!this.parameters.get(i).matches(types.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isExiting()
    {
        return false;
    }

    @Override
    public String toIR(IREnvironment env)
    {
        if(this.body != null)
        {
            List<String> names = this.scoped.stream().map(TypedField::name).toList();
            List<Type> types = this.scoped.stream().map(TypedField::type).toList();
            env.addInstruction(new IRInstruction("scope", names, types));

            for (int i = 0; i < this.names.size(); i++)
            {
                env.addInstruction(new IRInstruction("set-arg", this.names.get(i), List.of(Integer.toString(i)), List.of(this.parameters.get(i))));
            }

            this.body.toIR(env);
            env.addInstruction(new IRInstruction("unscope", names, types));
        }
        env.setReturnType(getType());
        return null;
    }

    private static final Type VOID_TYPE = new SingleType(null, "void");

    public void checkExiting()
    {
        // TODO: add position
        if(!VOID_TYPE.matches(getType()) && this.body != null && !this.body.isExiting())
        {
            throw new TPLException("Missing return statement at the end of " + this.name);
        }
    }

    public IRFragment toFragment(Map<String, IRType> concreteTypes)
    {
        IREnvironment env = new IREnvironment(concreteTypes);
        toIR(env);
        env.setParameters(this.parameters);
        return env.toFragment();
    }
}
