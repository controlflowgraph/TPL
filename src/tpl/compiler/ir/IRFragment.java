package tpl.compiler.ir;

import tpl.compiler.type.tree.Type;

import java.util.List;
import java.util.Map;

public class IRFragment
{
    private final Type returnType;
    private final List<Type> parameters;
    private final List<IRInstruction> instructions;
    private final Map<String, Integer> labels;

    public IRFragment(Type returnType, List<Type> parameters, List<IRInstruction> instructions, Map<String, Integer> labels)
    {
        this.returnType = returnType;
        this.parameters = parameters;
        this.instructions = instructions;
        this.labels = labels;
    }

    public Type getReturnType()
    {
        return this.returnType;
    }

    public List<IRInstruction> getInstructions()
    {
        return instructions;
    }

    public Map<String, Integer> getLabels()
    {
        return labels;
    }

    public List<Type> getParameters()
    {
        return this.parameters;
    }
}
