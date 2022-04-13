package tpl.compiler.ir;

import tpl.compiler.type.tree.Type;

import java.util.List;

public class IRInstruction
{
    private final String instruction;
    private final String attribute;
    private final String destination;
    private final List<String> sources;
    private final List<Type> types;
    private final boolean meta;

    public IRInstruction(String instruction, String attribute, String destination, List<String> sources, List<Type> types)
    {
        this(instruction, attribute, destination, sources, types, false);
    }

    public IRInstruction(String instruction, String attribute, String destination, List<String> sources, List<Type> types, boolean meta)
    {
        this.instruction = instruction;
        this.attribute = attribute;
        this.destination = destination;
        this.sources = sources;
        this.types = types;
        this.meta = meta;
    }

    public IRInstruction(String instruction, List<String> sources, List<Type> types)
    {
        this(instruction, null, null, sources, types, true);
    }

    public IRInstruction(String instruction, String destination, List<String> sources, List<Type> types)
    {
        this(instruction, null, destination, sources, types);
    }

    public String getInstruction()
    {
        return this.instruction;
    }

    public String getAttribute()
    {
        return this.attribute;
    }

    public String getDestination()
    {
        return this.destination;
    }

    public List<String> getSources()
    {
        return this.sources;
    }

    public List<Type> getTypes()
    {
        return this.types;
    }

    public boolean isStandard()
    {
        return !this.meta;
    }

    @Override
    public String toString()
    {
        return this.destination + " := " + this.instruction + " {" + this.attribute + "} " + this.sources + " => " + this.types;
    }
}
