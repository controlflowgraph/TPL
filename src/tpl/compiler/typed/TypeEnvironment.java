package tpl.compiler.typed;

import tpl.compiler.type.tree.*;
import tpl.compiler.typed.tree.function.TypedFunction;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.untyped.tree.function.UntypedFunction;
import tpl.compiler.untyped.tree.other.UntypedField;
import tpl.compiler.untyped.tree.other.UntypedType;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.*;

public class TypeEnvironment
{
    private final Map<String, List<TypedFunction>> implementations = new HashMap<>();

    private static final Map<String, String> BINARY_OPERATORS = loadBinaryOperations();

    private static Map<String, String> loadBinaryOperations()
    {
        Map<String, String> operations = new HashMap<>();
        String[] lines = """
                byte,short,int,long,float,double +,-,*,/,%,&,|,^ byte,short,int,long,float,double
                byte,short,int,long,float,double <,>,<=,>=,!=,== boolean,boolean,boolean,boolean,boolean,boolean
                char <,>,<=,>=,==,!= boolean
                boolean &&,||,^^ boolean
                """.split("\n");

        for(String line : lines)
        {
            String[] parts = line.split(" ");
            String[] inputs = parts[0].split(",");
            String[] operators = parts[1].split(",");
            String[] outputs = parts[2].split(",");
            for(int i = 0; i < inputs.length; i++)
            {
                for (String operator : operators)
                {
                    operations.put(operator + inputs[i], outputs[i]);
                }
            }
        }

        return operations;
    }

    private static final Map<String, String> UNARY_OPERATORS = loadUnaryOperations();

    private static Map<String, String> loadUnaryOperations()
    {
        String[] lines = """
                byte,short,int,long,float,double +,- byte,short,int,long,float,double
                boolean ! boolean
                """.split("\n");

        Map<String, String> operations = new HashMap<>();
        for(String line : lines)
        {
            String[] parts = line.split(" ");
            String[] inputs = parts[0].split(",");
            String[] operators = parts[1].split(",");
            String[] outputs = parts[2].split(",");
            for(int i = 0; i < inputs.length; i++)
            {
                for (String operator : operators)
                {
                    operations.put(operator + inputs[i], outputs[i]);
                }
            }
        }
        return operations;
    }

    public TypeEnvironment()
    {
        addType(null, new UntypedType(
                null,
                List.of(),
                "Array",
                List.of(
                        new UntypedField(null, true, new SingleType(null, "int"), "length")
                )
        ));
    }

    public Type getOperatorResult(Position position, String operator, Type left, Type right)
    {
        if(operator.equals("==") && left.matches(right))
            return new SingleType(position, "boolean");

        if (!left.isPrimitive() || !left.matches(right))
            throw new TPLPositionedException("Binary operations only supported for primitives of the same type!", position);

        if(!BINARY_OPERATORS.containsKey(operator + left))
            throw new TPLPositionedException("Binary operator '" + operator + "' not supported for type '" + left + "'!", position);

        return new SingleType(position, BINARY_OPERATORS.get(operator + left));
    }

    public Type getOperatorResult(Position position, String operator, Type source)
    {
        if (!source.isPrimitive())
            throw new TPLPositionedException("Unary operations only supported for primitives of the same type!", position);

        if(!UNARY_OPERATORS.containsKey(operator + source))
            throw new TPLPositionedException("Unary operator '" + operator + "' not supported for type '" + source + "'!", position);

        return new SingleType(position, UNARY_OPERATORS.get(operator + source));
    }

    private record Scope(Map<String, TypedField> variables)
    {
        public Scope()
        {
            this(new HashMap<>());
        }

        public void define(TypedField field)
        {
            this.variables.put(field.name(), field);
        }

        public TypedField get(String name)
        {
            return this.variables.get(name);
        }

        public boolean isDefine(String name)
        {
            return this.variables.containsKey(name);
        }

        public List<TypedField> getAll()
        {
            return this.variables.values().stream().toList();
        }
    }

    private static int test = 0;
    private static class Area
    {
        private final int id;
        private final List<Integer> depths;
        private final Map<String, Type> substitutes;
        private final Stack<Scope> scopes = new Stack<>();
        private final Set<String> definitions = new HashSet<>();

        public Area(int id, List<Integer> depths, Map<String, Type> substitutes)
        {
            this.id = id;
            this.depths = depths;
            this.substitutes = substitutes;
            //this.scopes.push(new Scope());
        }

        public void enter()
        {
            this.scopes.push(new Scope());
        }

        public List<TypedField> leave()
        {
            return this.scopes.pop().getAll();
        }

        public boolean isDefined(String name)
        {
            for(int i = this.scopes.size() - 1; i >= 0; i--)
            {
                if(this.scopes.get(i).isDefine(name))
                {
                    return true;
                }
            }
            return false;
        }

        public TypedField get(String name)
        {
            for(int i = this.scopes.size() - 1; i >= 0; i--)
            {
                if(this.scopes.get(i).isDefine(name))
                {
                    return this.scopes.get(i).get(name);
                }
            }
            return null;
        }

        public void define(TypedField field)
        {
            this.definitions.add(field.name());
            this.scopes.peek().define(field);
        }

        public boolean isDeclared(String name)
        {
            return !this.definitions.remove(name);
        }

        public List<Integer> getDepths()
        {
            return this.depths;
        }
    }

    public boolean isDeclared(String name)
    {
        return this.areas.isEmpty() ? this.global.isDeclared(name) : this.areas.peek().isDeclared(name);
    }

    private final Stack<Area> areas = new Stack<>();
    private final Area global = new Area(-1, List.of(), Map.of());

    public void enterArea(int id, List<Integer> depths, Map<String, Type> substitutes)
    {
        this.areas.push(new Area(id, depths, substitutes));
    }

    public void leaveArea()
    {
        this.areas.pop();
    }

    public void enterScope()
    {
        if(this.areas.isEmpty())
        {
            this.global.enter();
        }
        else
        {
            this.areas.peek().enter();
        }
    }

    public List<TypedField> leaveScope()
    {
        if(this.areas.isEmpty())
        {
            return this.global.leave();
        }
        else
        {
            return this.areas.peek().leave();
        }
    }

    public void define(TypedField field)
    {
        if(this.areas.isEmpty())
        {
            this.global.define(field);
        }
        else
        {
            this.areas.peek().define(field);
        }
    }

    public boolean isDefined(String name)
    {
        return this.global.isDefined(name) || !this.areas.isEmpty() && this.areas.peek().isDefined(name);
    }

    public Type get(String name)
    {
        return getField(name).type();
    }

    public TypedField getField(String name)
    {
        TypedField type = this.areas.isEmpty() ? null : this.areas.peek().get(name);
        if(type == null)
        {
            type = this.global.get(name);
        }
        return type;
    }

    public boolean isConstant(String name)
    {
        return getField(name).constant();
    }

    public boolean isLocallyDefined(String name)
    {
        return this.areas.isEmpty() ? this.global.isDefined(name) : this.areas.peek().isDefined(name);
    }

    public boolean isGlobal(String name)
    {
        return (this.areas.isEmpty() || !this.areas.peek().isDefined(name)) && this.global.scopes.get(0).isDefine(name);
    }

    private final Map<String, List<UntypedFunction>> functions = new HashMap<>();

    // TODO: make sure to check that new functions are not ambiguous
    public void addFunction(UntypedFunction function)
    {
        if(!this.functions.containsKey(function.getName())) this.functions.put(function.getName(), new ArrayList<>());
        this.functions.get(function.getName()).add(function);
    }

    private int id;
    private final Stack<Integer> idStack = new Stack<>();
    private final Stack<Type> resultTypes = new Stack<>();

    public void setResultType(Type type)
    {
        this.resultTypes.push(type);
    }

    public void removeResultType()
    {
        this.resultTypes.pop();
    }

    public Type getCurrentResultType()
    {
        return this.resultTypes.peek();
    }

    public TypedFunction getFunction(Position position, String name, List<Type> types)
    {
        List<TypedFunction> implementations = this.implementations.get(name);
        if(implementations != null)
        {
            for(TypedFunction function : implementations)
            {
                if(function.matchesArguments(types))
                {
                    return function;
                }
            }
        }

        List<UntypedFunction> functions = this.functions.get(name);
        if(functions == null)
            throw new TPLPositionedException("No " + name + " function defined!", position);

        for (UntypedFunction function : functions)
        {
            if(function.matchesArguments(types))
            {
                assureDepth(position, types, function);

                int id = this.id++;
                if(!this.implementations.containsKey(name))
                {
                    this.implementations.put(name, new ArrayList<>());
                }

                this.idStack.push(function.getId());
                TypedFunction func = function.apply(this, id, types);
                this.idStack.pop();

                replace(func);

                return func;
            }
        }
        throw new TPLPositionedException("No matching function " + name + " and parameters " + types + " found!", position);
    }

    public void setMock(TypedFunction function)
    {
        this.implementations.get(function.getName()).add(function);
    }

    private void replace(TypedFunction function)
    {
        List<TypedFunction> typedFunctions = this.implementations.get(function.getName());
        for (int i = 0; i < typedFunctions.size(); i++)
        {
            if(typedFunctions.get(i).getId() == function.getId())
            {
                typedFunctions.set(i, function);
                return;
            }
        }
        if(function.isExternal())
        {
            typedFunctions.add(function);
        }
    }
    private void assureDepth(Position position, List<Type> types, UntypedFunction function)
    {
        List<Integer> depths1 = new ArrayList<>();
        for (Type type : types)
        {
            depths1.add(type.getDepth());
        }
        for(int i = this.idStack.size() - 1; i >= 0; i--)
        {
            if(this.idStack.get(i) == function.getId())
            {
                List<Integer> depths2 = this.areas.get(i).getDepths();
                for(int k = 0; k < depths1.size(); k++)
                {
                    if(depths2.get(k) < depths1.get(k))
                    {
                        throw new TPLPositionedException("Parameter " + (k + 1) + " is expanding!", position);
                    }
                }
                return;
            }
        }
    }

    public void print()
    {
        System.out.println(this.implementations);
        for (Map.Entry<String, List<TypedFunction>> stringListEntry : this.implementations.entrySet())
        {
            System.out.println(stringListEntry.getKey() + ":");
            for (TypedFunction typedFunction : stringListEntry.getValue())
            {
                System.out.println(typedFunction.getName() + " " + typedFunction.getId() + " -> " + typedFunction.getParameters() + " -> " + typedFunction.getType());
            }
        }
    }

    public Map<String, List<TypedFunction>> getImplementations()
    {
        return this.implementations;
    }

    public Map<String, Type> getCurrentSubstituteMapping()
    {
        return this.areas.isEmpty() ? null : this.areas.peek().substitutes;
    }

    private record TypeStruct(String name, List<String> generics, List<UntypedField> fields) { }

    private final Map<String, TypeStruct> types = new HashMap<>();

    public TypedField getAttributeField(Position position, Type type, String attribute)
    {
        String name = type.getName();
        TypeStruct struct = this.types.get(name);

        if(struct == null)
            throw new TPLPositionedException("No type named '" + name + "' defined!", position);

        int index = -1;
        for (int i = 0; i < struct.fields.size() && index == -1; i++)
        {
            if(struct.fields.get(i).name().equals(attribute))
            {
                index = i;
            }
        }
        Map<String, Type> sub = new HashMap<>();
        if(type instanceof GenericType g)
        {
            for (int i = 0; i < struct.generics.size(); i++)
            {
                sub.put(struct.generics.get(i), g.getGenerics().get(i));
            }
        }

        if(index == -1)
            throw new TPLPositionedException("No attribute named '" + attribute + "' in type '" + name + "'!", position);

        UntypedField field = struct.fields.get(index);
        return new TypedField(null, field.constant(), field.type().substitute(sub), field.name());
    }

    public Type getSubstituted(Type base, Type actual)
    {
        Map<String, Type> generics = new HashMap<>();
        base.collectGenericTypes(actual, generics);
        return base.substitute(generics);
    }

    public void addType(Position position, UntypedType type)
    {
        if(this.types.containsKey(type.getName()))
            throw new TPLPositionedException("Type '" + type.getName() + "' already defined!", position);
        this.types.put(type.getName(), new TypeStruct(type.getName(), type.getGenerics(), type.getFields()));
    }

    public Type getTypeOfTypeStruct(String name)
    {
        List<Type> placeholders = new ArrayList<>();
        List<String> lst = this.types.get(name).generics;
        if(lst == null)
        {
            return new SingleType(null, name);
        }

        for (String generic : this.types.get(name).generics)
        {
            placeholders.add(new PlaceholderType(null, generic));
        }
        if(name.equals("Array"))
        {
            return new ArrayType(null, new NullType(null));
        }
        return new GenericType(null, new SingleType(null, name), placeholders);
    }

    public List<Type> getTypesOfFields(String name)
    {
        List<Type> types = new ArrayList<>();
        for (UntypedField field : this.types.get(name).fields)
        {
            types.add(field.type());
        }
        return types;
    }

    public List<String> getNamesOfFields(String name)
    {
        List<String> attributes = new ArrayList<>();
        for (UntypedField field : this.types.get(name).fields)
        {
            attributes.add(field.name());
        }
        return attributes;
    }

    public boolean isTypeDefined(String name)
    {
        return this.types.containsKey(name);
    }
}
