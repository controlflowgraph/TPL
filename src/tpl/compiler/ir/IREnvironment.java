package tpl.compiler.ir;

import tpl.compiler.type.tree.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IREnvironment
{
    private final List<IRInstruction> instructions = new ArrayList<>();
    private final Map<String, Integer> labels = new HashMap<>();

    private int tempCounter;
    private int labelCounter;
    private final String prefix;
    private List<Type> parameters;

    public void setParameters(List<Type> parameters)
    {
        this.parameters = parameters;
    }

    public IREnvironment(Map<String, IRType> concreteTypes)
    {
        this(concreteTypes, "label_");
    }

    public IREnvironment(Map<String, IRType> concreteTypes, String prefix)
    {
        this.concreteTypes = concreteTypes;
        this.prefix = prefix;
    }

    public void setLabel(String label)
    {
        this.labels.put(label, this.instructions.size() - 1);
    }

    public String getLabel()
    {
        return this.prefix + this.labelCounter++;
    }

    public void addInstruction(IRInstruction instruction)
    {
        this.instructions.add(instruction);
    }

    public void addJump(String label)
    {
        this.instructions.add(new IRInstruction("jump", label, null, List.of(), List.of()));
    }

    public void addBranch(String label, String cond)
    {
        this.instructions.add(new IRInstruction("branch", label, null, List.of(cond), List.of()));
    }

    private Type returnType;

    public void setReturnType(Type type)
    {
        this.returnType = type;
    }

    public IRFragment toFragment()
    {
        return new IRFragment(this.returnType, this.parameters, this.instructions, this.labels);
    }

    public void print()
    {
        System.out.println("=========[ INSTRUCTIONS ]=========");
        this.instructions.forEach(System.out::println);
        System.out.println("=========[ LABELS ]=========");
        this.labels.forEach((a, b) -> System.out.println(a + ": " + b));
    }

    public String getTemp()
    {
        return "$t" + ++this.tempCounter;
    }

    public String getLastTemp()
    {
        return "$t" + this.tempCounter;
    }

    private final Map<String, IRType> concreteTypes;

    public void addType(Type type, List<String> attributes, List<Type> types)
    {
        String key = type.toString();
        if(!this.concreteTypes.containsKey(key))
        {
            this.concreteTypes.put(key, new IRType(type, attributes, types));
        }
    }
}
