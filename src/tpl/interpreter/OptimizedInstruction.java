package tpl.interpreter;

import tpl.compiler.ir.IRInstruction;

import java.util.Arrays;

public class OptimizedInstruction
{
    private final boolean standard;
    private final String instruction;
    private final String attribute;
    private final int destination;
    private final int[] sources;

    public OptimizedInstruction(IRInstruction instruction, int destination, int[] sources)
    {
        this(instruction.isStandard(), instruction.getInstruction(), instruction.getAttribute(), destination, sources);
    }

    public OptimizedInstruction(boolean standard, String instruction, String attribute, int destination, int[] sources)
    {
        this.standard = standard;
        this.instruction = instruction;
        this.attribute = attribute;
        this.destination = destination;
        this.sources = sources;
    }

    public String getInstruction()
    {
        return this.instruction;
    }

    public String getAttribute()
    {
        return this.attribute;
    }

    public int getDestination()
    {
        return this.destination;
    }

    public int[] getSources()
    {
        return this.sources;
    }

    public boolean isStandard()
    {
        return this.standard;
    }

    @Override
    public String toString()
    {
        return "{" +
                "instruction='" + instruction + '\'' +
                ", attribute='" + attribute + '\'' +
                ", destination='" + destination + '\'' +
                ", sources=" + Arrays.toString(sources) +
                '}';
    }
}
