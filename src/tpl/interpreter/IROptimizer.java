package tpl.interpreter;

import tpl.compiler.ir.IRFragment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.ir.IRProgram;

import java.util.*;

public class IROptimizer
{
    public static OptimizedProgram optimize(IRProgram program)
    {
        IROptimizer optimizer = new IROptimizer();
        OptimizedFragment base = optimize(optimizer, program.getBase());
        Map<Integer, OptimizedFragment> fragments = new HashMap<>();
        for (Map.Entry<Integer, IRFragment> entry : program.getProcedures().entrySet())
        {
            fragments.put(entry.getKey(), optimize(optimizer, entry.getValue()));
        }
        return new OptimizedProgram(fragments, base, optimizer.strings, optimizer.integers, optimizer.doubles, optimizer.booleans, optimizer.characters, optimizer.nulls);
    }

    private static OptimizedFragment optimize(IROptimizer optimizer, IRFragment fragment)
    {
        return optimizer.run(fragment);
    }

    private final Pool<String> strings = new Pool<>();
    private final Pool<Integer> integers = new Pool<>();
    private final Pool<Double> doubles = new Pool<>();
    private final Pool<Boolean> booleans = new Pool<>();
    private final Pool<Character> characters = new Pool<>();
    private final Pool<Object> nulls = new Pool<>();

    private IROptimizer()
    {

    }

    private boolean global = true;
    private Map<String, Integer> globalMapping;
    private LocalsManager manager;
    private Map<String, Integer> slotMapping;

    private OptimizedFragment run(IRFragment fragment)
    {
        this.slotMapping = new HashMap<>();
        this.globalMapping = this.global ? this.slotMapping : this.globalMapping;

        OptimizedFragment frag = execute(fragment);

        this.global = false;
        return frag;
    }

    private OptimizedFragment execute(IRFragment fragment)
    {
        // compute map of last used indices
        // compute the required number of local variables
        // iterate over all instructions
        // - if there is a variable that can be released by the manager release it
        // - get a variable
        // - resolve all variables
        List<IRInstruction> instructions = fragment.getInstructions();
        Map<String, int[]> bounds = getBounds(instructions);
        Map<Integer, List<String>> ends = getEnds(bounds);
        int[] requirements = getRequirements(instructions.size(), bounds);
        int required = max(requirements);

        LocalsManager manager = new LocalsManager(required);
        if(this.manager == null)
            this.manager = manager;

        List<OptimizedInstruction> optimized = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++)
        {
            IRInstruction instruction = instructions.get(i);
            String operation = instruction.getInstruction();
            int[] resolved = getResolved(instruction);

            List<String> variables = ends.get(i);
            if(!operation.equals("unscope-global"))
            {
                if(variables != null)
                {
                    for (String variable : variables)
                    {
                        if(this.slotMapping.containsKey(variable))
                        {
                            manager.give(this.slotMapping.remove(variable));
                        }
                    }
                }
            }

            String destination = instruction.getDestination();
            // if the variable is used later
            int[] bound = bounds.get(destination);
            int dest = -1;
            if(destination != null)
            {
                if(!this.global && instruction.getInstruction().equals("set-global"))
                {
                    dest = this.globalMapping.get(destination);
                }
                else if(bound[0] != bound[1])
                {
                    if(!this.slotMapping.containsKey(destination))
                    {
                        this.slotMapping.put(destination, manager.get(destination));
                    }
                    dest = this.slotMapping.get(destination);
                }
            }

            optimized.add(new OptimizedInstruction(instruction, dest, resolved));
        }

        return new OptimizedFragment(
                required,
                fragment.getParameters(),
                optimized,
                fragment.getLabels()
        );
    }

    private int[] getResolved(IRInstruction instruction)
    {
        int[] resolved = new int[instruction.getSources().size()];
        if(instruction.getInstruction().equals("load-constant"))
        {
            convertConstant(instruction, resolved);
        }
        else if(instruction.getInstruction().equals("load-global"))
        {
            resolved[0] = this.globalMapping.get(instruction.getSources().get(0));
        }
        else if(instruction.getInstruction().equals("set-arg"))
        {
            resolved[0] = Integer.parseInt(instruction.getSources().get(0));
        }
        else if(instruction.isStandard())
        {
            for (int i = 0; i < instruction.getSources().size(); i++)
            {
                resolved[i] = this.slotMapping.get(instruction.getSources().get(i));
            }
        }
        return resolved;
    }

    private void convertConstant(IRInstruction instruction, int[] resolved)
    {
        List<String> sources = instruction.getSources();
        String type = instruction.getAttribute();
        String first = sources.get(0);
        resolved[0] = switch (type)
                {
                    case "string" -> this.strings.add(first.substring(1, first.length() - 1));
                    case "int" -> this.integers.add(Integer.parseInt(first));
                    case "double" -> this.doubles.add(Double.parseDouble(first));
                    case "boolean" -> this.booleans.add(Boolean.parseBoolean(first));
                    case "char" -> this.characters.add(switch (first)
                                                               {
                                                                   case "'\\n'" -> '\n';
                                                                   case "'\\r'" -> '\r';
                                                                   case "'\\t'" -> '\t';
                                                                   default -> first.charAt(1);
                                                               });
                    case "null" -> this.nulls.add(null);
                    default -> throw new RuntimeException("Type '" + type + "' not supported in optimization! (" + instruction + ")");
                };
    }

    private Map<Integer, List<String>> getEnds(Map<String, int[]> bounds)
    {
        Map<Integer, List<String>> ends = new HashMap<>();

        for (Map.Entry<String, int[]> entry : bounds.entrySet())
        {
            int end = entry.getValue()[1];
            if (!ends.containsKey(end))
            {
                ends.put(end, new ArrayList<>());
            }
            ends.get(end).add(entry.getKey());
        }

        return ends;
    }

    private int max(int[] requirements)
    {
        int max = 0;
        for (int requirement : requirements)
        {
            max = Math.max(max, requirement);
        }
        return max;
    }

    private int[] getRequirements(int size, Map<String, int[]> bounds)
    {
        int[] requirements = new int[size];

        for (Map.Entry<String, int[]> entry : bounds.entrySet())
        {
            for (int i = entry.getValue()[0]; i < entry.getValue()[1]; i++)
            {
                requirements[i]++;
            }
        }

        return requirements;
    }

    private Map<String, int[]> getBounds(List<IRInstruction> instructions)
    {
        Map<String, int[]> bounds = new HashMap<>();
        for (int i = 0; i < instructions.size(); i++)
        {
            String dest = instructions.get(i).getDestination();
            if (dest != null && !bounds.containsKey(dest) && (this.global || !instructions.get(i).getInstruction().equals("set-global")))
            {
                int max = i;
                for (int k = i; k < instructions.size(); k++)
                {
                    if (instructions.get(k).getSources().contains(dest))
                    {
                        max = k;
                    }
                }
                bounds.put(dest, new int[]{i, max});
            }
        }
        return bounds;
    }
}


/*
List<String> sources = instruction.getSources();
            int[] resolved = new int[sources.size()];
            boolean exclude = this.global && instruction.getInstruction().equals("set-global");
            if (instruction.getInstruction().equals("load-constant"))
            {
                convertConstant(instruction, resolved);
            }
            else if(instruction.getInstruction().equals("load-global"))
            {
                System.out.println("GLOBAL LOADING: " + instruction + " -> " + this.globalMapping.get(instruction.getSources().get(0)));
                resolved[0] = this.globalMapping.get(instruction.getSources().get(0));
            }
            else if(instruction.getInstruction().equals("set-global"))
            {
                System.out.println("GLOBAL LOADING: " + instruction + " -> " + this.globalMapping.get(instruction.getSources().get(0)));

            }
            else
            {
                if(instruction.isStandard())
                {
                    for (int a = 0; a < sources.size(); a++)
                    {
                        String source = sources.get(a);
                        if (this.slotMapping.containsKey(source))
                        {
                            resolved[a] = this.slotMapping.get(source);
                        }
                        else
                        {
                            if(this.global && this.globalMapping.containsKey(source))
                            {
                                resolved[a] = this.globalMapping.get(source);
                            }
                            else
                            {
                                resolved[a] = Integer.parseInt(source);
                            }
                        }
                    }
                }
                else
                {
                    for (int a = 0; a < sources.size(); a++)
                    {
                        resolved[a] = this.strings.add(sources.get(a));
                    }
                }
            }

            List<String> ending = ends.get(i);
            String destination = instruction.getDestination();
            if (ending != null)
            {
                for (String variable : ending)
                {
                    manager.give(this.slotMapping.remove(variable));
                }
            }

            if (!exclude && destination != null && !this.slotMapping.containsKey(destination) && !(ending != null && ending.contains(destination)))
            {
                System.out.println("FOR: " + destination);
                int v = manager.get();
                this.slotMapping.put(destination, v);
            }

            int dest = !exclude && destination != null ? this.slotMapping.get(destination) : -1;
*/