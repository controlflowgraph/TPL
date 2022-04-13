package tpl.compiler.untyped.tree.function;

import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.function.TypedFunction;
import tpl.compiler.typed.tree.other.TypedDummy;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.untyped.tree.other.UntypedField;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UntypedFunction extends UntypedStructure
{
    private final boolean external;
    private final List<String> generics;
    private final Type result;
    private final String name;
    private final int id;
    private final List<UntypedField> fields;
    private final UntypedStructure body;

    public UntypedFunction(Position position, boolean external, List<String> generics, Type result, String name, int id, List<UntypedField> fields, UntypedStructure body)
    {
        super(position);
        this.external = external;
        this.generics = generics;
        this.result = result;
        this.name = name;
        this.id = id;
        this.fields = fields;
        this.body = body;
    }

    public TypedFunction apply(TypeEnvironment env, int id, List<Type> types)
    {
        List<Type> results = new ArrayList<>();
        List<TypedField> scoped = new ArrayList<>();

        Type result = this.result;
        TypedStructure body = null;
        if(this.external)
        {
            for (UntypedField field : this.fields)
            {
                results.add(field.type());
            }
        }
        else
        {
            Map<String, Type> substitute = getSubstitutes(types);
            List<Integer> depths = getDepth(types);

            env.enterArea(this.id, depths, substitute);
            env.enterScope();

            for (UntypedField field : this.fields)
            {
                Type type = field.type().substitute(substitute);
                env.define(field.getTypedField(substitute));
                results.add(type);
            }

            if(this.generics != null)
            {
                for (String generic : this.generics)
                {
                    if(!substitute.containsKey(generic))
                        throw new TPLPositionedException("Unable to derive generic parameter '" + generic + "'!", getPosition());
                }

                if(this.generics.size() != substitute.size())
                    throw new TPLPositionedException(substitute.size() + " generic parameters specified and " + this.generics.size() + " required!", getPosition());
            }

            result = result.substitute(substitute);

            env.setResultType(result);
            env.setMock(new TypedFunction(false, result, this.name, id, results, null, null));
            body = this.body.checkType(env);
            env.removeResultType();
            scoped = env.leaveScope();
            env.leaveArea();
        }

        List<String> parameter = this.fields.stream().map(UntypedField::name).toList();

        return new TypedFunction(this.external, result, this.name, id, parameter, results, body, scoped);
    }

    public Map<String, Type> getSubstitutes(List<Type> types)
    {
        Map<String, Type> substitute = new HashMap<>();
        for (int i = 0; i < this.fields.size(); i++)
        {
            this.fields.get(i).type().collectGenericTypes(types.get(i), substitute);
        }
        return substitute;
    }

    public List<Integer> getDepth(List<Type> types)
    {
        List<Integer> depths = new ArrayList<>();
        for (int i = 0; i < this.fields.size(); i++)
        {
            depths.add(types.get(i).getDepth());
        }
        return depths;
    }

    public boolean matchesArguments(List<Type> types)
    {
        if(this.fields.size() != types.size()) return false;

        for(int i = 0; i < types.size(); i++)
        {
            if(!this.fields.get(i).type().matches(types.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public TypedStructure checkType(TypeEnvironment env)
    {
        env.addFunction(this);
        return new TypedDummy(null);
    }

    public String getName()
    {
        return this.name;
    }

    public int getId()
    {
        return this.id;
    }

    public boolean isExternal()
    {
        return this.external;
    }

    public Type getResult()
    {
        return this.result;
    }

    public List<UntypedField> getFields()
    {
        return this.fields;
    }

    public UntypedStructure getBody()
    {
        return this.body;
    }
}
