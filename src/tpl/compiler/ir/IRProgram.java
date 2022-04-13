package tpl.compiler.ir;

import tpl.compiler.typed.tree.function.TypedFunction;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class IRProgram
{
    private final List<IRType> types;
    private final Map<Integer, IRFragment> procedures;
    private final IRFragment base;
    private final List<TypedFunction> nativeFunctions;

    public IRProgram(List<IRType> types, Map<Integer, IRFragment> procedures, IRFragment base, List<TypedFunction> nativeFunctions)
    {
        this.types = types;
        this.procedures = procedures;
        this.base = base;
        this.nativeFunctions = nativeFunctions;
    }

    public IRFragment getBase()
    {
        return this.base;
    }

    public IRFragment getFragment(int id)
    {
        return this.procedures.get(id);
    }

    public Map<Integer, IRFragment> getProcedures()
    {
        return this.procedures;
    }

    public List<IRType> getTypes()
    {
        return this.types;
    }

    public List<TypedFunction> getNativeFunctions()
    {
        return this.nativeFunctions;
    }

    public void dump(String location)
    {
        try
        {
            PrintWriter writer = new PrintWriter(location);
            this.base.getInstructions().forEach(writer::println);
            this.procedures.forEach((a, b) -> {
                writer.println(a + "----------------------------------------");
                b.getInstructions().forEach(writer::println);
            });
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
