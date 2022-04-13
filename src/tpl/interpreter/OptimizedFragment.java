package tpl.interpreter;

import tpl.compiler.type.tree.Type;

import java.util.List;
import java.util.Map;

public class OptimizedFragment
{
    private final int size;
    private final List<Type> parameters;
    private final List<OptimizedInstruction> instructions;
    private final Map<String, Integer> labels;

    public OptimizedFragment(int size, List<Type> parameters, List<OptimizedInstruction> instructions, Map<String, Integer> labels)
    {
        this.size = size;
        this.parameters = parameters;
        this.instructions = instructions;
        this.labels = labels;
    }

    public int getSize()
    {
        return this.size;
    }

    public List<OptimizedInstruction> getInstructions()
    {
        return instructions;
    }

    public Map<String, Integer> getLabels()
    {
        return labels;
    }
}
