package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.ArrayType;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.TypedConstructor;
import tpl.compiler.typed.tree.segment.TypedSegment;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.*;

public class UntypedConstructor extends UntypedSegment
{
    private final Type type;
    private final List<UntypedSegment> parameters;

    public UntypedConstructor(Position position, Type type, List<UntypedSegment> parameters)
    {
        super(position);
        this.type = type;
        this.parameters = parameters;
    }

    private static final Type INTEGER_TYPE = new SingleType(null, "int");

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        List<TypedSegment> parameters = this.parameters.stream().map(p -> p.checkType(env)).toList();
        if(this.type.getName().equals("Array"))
        {
            Type type = this.type;
            int depth = 0;
            while(type instanceof ArrayType a)
            {
                depth++;
                type = a.getContentType();
            }

            if(parameters.size() > depth)
                throw new TPLPositionedException("Expected at most " + depth + " dimensions but got " + parameters.size() + "!", getPosition());

            for (TypedSegment parameter : parameters)
            {
                if(!INTEGER_TYPE.matches(parameter.getType()))
                    throw new TPLPositionedException("Expected only int values as dimensions!", getPosition());
            }

            type = this.type.substitute(env.getCurrentSubstituteMapping());
            return new TypedConstructor(type, parameters);
        }
        else
        {
            if(!env.isTypeDefined(this.type.getName()))
                throw new TPLPositionedException("Type '" + this.type.getName() + "' is not defined!", getPosition());

            Type base = env.getTypeOfTypeStruct(this.type.getName());
            List<Type> fieldTypes = env.getTypesOfFields(this.type.getName());
            List<String> attributes = env.getNamesOfFields(this.type.getName());

            if(fieldTypes.size() != parameters.size())
                throw new TPLPositionedException("Expected " + fieldTypes.size() + " parameters but got " + parameters.size() + "!", getPosition());

            for(int i = 0; i < fieldTypes.size(); i++)
            {
                if(!fieldTypes.get(i).matches(parameters.get(i).getType()))
                {
                    throw new TPLPositionedException("Expected type of parameter " + (i + 1) + " is " + fieldTypes.get(i) + " but was " + parameters.get(i).getType(), getPosition());
                }
            }

            Map<String, Type> test = env.getCurrentSubstituteMapping();
            Map<String, Type> subs = test == null ? new HashMap<>() : new HashMap<>();
            for(int i = 0; i < fieldTypes.size(); i++)
            {
                fieldTypes.get(i).collectGenericTypes(parameters.get(i).getType(), subs);
            }

            Set<String> generics = new HashSet<>();
            base.collectGenerics(generics);

            for (String generic : generics)
            {
                if(!subs.containsKey(generic))
                    throw new TPLPositionedException("Unable to infer generic parameter " + generic + "!", getPosition());
            }

            Type type = base.substitute(subs);

            return new TypedConstructor(type, attributes, parameters);
        }
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment type)
    {
        throw new TPLPositionedException("Unable to assign to constructor call!", getPosition());
    }
}
