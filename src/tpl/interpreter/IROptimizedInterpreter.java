package tpl.interpreter;

import com.sun.jdi.IntegerValue;
import tpl.compiler.ir.IRProgram;
import tpl.interpreter.value.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class IROptimizedInterpreter
{
    public static void run(IRProgram program)
    {
//        program.dump("data/dump.txt");
        OptimizedProgram p = IROptimizer.optimize(program);
        new IROptimizedInterpreter(p).run();
    }

    private final OptimizedProgram program;
    private final Stack<Value[]> scopes = new Stack<>();
    private Value[] current;
    private final SizedPool<String> strings;
    private final SizedPool<IntValue> integers;
    private final SizedPool<DoubleValue> doubles;
    private final SizedPool<BooleanValue> booleans;
    private final SizedPool<CharValue> characters;
    private final SizedPool<Object> nulls;

    private final Map<String, Function<List<Value>, Value>> external = new HashMap<>();

    private IROptimizedInterpreter(OptimizedProgram program)
    {
        this.program = program;
        this.strings = new SizedPool<>(program.getStrings(), i -> i, String[]::new);
        this.integers = new SizedPool<>(program.getIntegers(), IntValue::new, IntValue[]::new);
        this.doubles = new SizedPool<>(program.getDoubles(), DoubleValue::new, DoubleValue[]::new);
        this.booleans = new SizedPool<>(program.getBooleans(), BooleanValue::new, BooleanValue[]::new);
        this.characters = new SizedPool<>(program.getCharacters(), CharValue::new, CharValue[]::new);
        this.nulls = new SizedPool<>(program.getNulls(), i -> i, Object[]::new);

        System.out.println("strings: " + program.getStrings().size());
        System.out.println("doubles: " + program.getDoubles().size());
        System.out.println("integers: " + program.getIntegers().size());
        System.out.println("booleans: " + program.getBooleans().size());
        System.out.println("characters: " + program.getCharacters().size());
        System.out.println("nulls: " + program.getNulls().size());

        this.external.put("print[String]", args ->
        {
            System.out.println(fuse(args.get(0)));
            return null;
        });

        this.external.put("random[]", args -> new DoubleValue(Math.random()));
        this.external.put("round[double]", args -> new IntValue((int) Math.round(((DoubleValue) args.get(0)).getValue())));
        this.external.put("str[int]", args -> str(Integer.toString(((IntValue) args.get(0)).getValue())));
        this.external.put("str[double]", args -> str(Double.toString(((DoubleValue) args.get(0)).getValue())));
        this.external.put("str[char]", args -> str(Character.toString(((CharValue) args.get(0)).getValue())));
        this.external.put("str[boolean]", args -> str(Boolean.toString(((BooleanValue) args.get(0)).isValue())));
        this.external.put("str[long]", args -> str(Long.toString(((LongValue) args.get(0)).getValue())));

        this.external.put("read[String]", args ->
        {
            try
            {
                return str(Files.readString(Path.of(fuse(args.get(0)))));
            }
            catch (IOException e)
            {
                return null;
            }
        });
        this.external.put("write[String, String]", args ->
        {
            try
            {
                Files.writeString(Path.of(fuse(args.get(0))), fuse(args.get(1)));
            }
            catch (IOException ignored)
            {

            }
            return null;
        });
    }

    private String fuse(Value value)
    {
        ObjectValue str = (ObjectValue) value;
        int length = ((IntValue) str.get("length")).getValue();
        ObjectValue arr = ((ObjectValue) str.get("chars"));
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            builder.append(((CharValue) arr.get(Integer.toString(i))).getValue());
        }
        return builder.toString();
    }

    private void run()
    {

        long t1 = System.currentTimeMillis();
        this.current = new Value[this.program.getBase().getSize()];
        run(this.program.getBase());
        long t2 = System.currentTimeMillis();
        System.out.println("---------[ EXECUTION RESULTS ]---------");
        System.out.println("Instructions: " + this.instructionCounter + " | Time: " + (t2 - t1) / 1000.0f + " s");
        System.out.println("---------[ INSTRUCTION HISTOGRAM ]---------");
        this.histogram.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
    }

    private int instructionCounter;
    private final Map<String, Integer> histogram = new HashMap<>();

    private Value run(OptimizedFragment frag)
    {
        int pointer = 0;
        List<OptimizedInstruction> instructions = frag.getInstructions();
        while (0 <= pointer && pointer < instructions.size())
        {
            OptimizedInstruction instruction = instructions.get(pointer);
//            this.histogram.put(instruction.getInstruction(), this.histogram.getOrDefault(instruction.getInstruction(), 0) + 1);
//            this.instructionCounter++;
            switch (instruction.getInstruction())
            {
                case "scope-global", "unscope-global", "scope", "unscope" -> {}
                case "load-constant" -> loadConst(instruction);
                case "new-object" -> createObj(instruction);
                case "new-array" -> createArray(instruction);
                case "set-attribute" -> setAttribute(instruction);
                case "set-local" -> setLocal(instruction);
                case "load-local" -> loadLocal(instruction);
                case "call-native" -> callNative(instruction);
                case "call" -> call(instruction);
                case "get-attribute" -> getAttribute(instruction);
                case "+" -> plus(instruction);
                case "-" -> minus(instruction);
                case "*" -> times(instruction);
                case "<" -> less(instruction);
                case "==" -> equal(instruction);
                case "!=" -> unequal(instruction);
                case ">=" -> greaterEqual(instruction);
                case "<=" -> lessEqual(instruction);
                case "jump" -> pointer = frag.getLabels().get(instruction.getAttribute());
                case "branch" -> {
                    BooleanValue cond = (BooleanValue) this.current[instruction.getSources()[0]];
                    if (cond.isValue())
                    {
                        pointer = frag.getLabels().get(instruction.getAttribute());
                    }
                }
                case "return" -> {
                    return this.current[instruction.getSources()[0]];
                }
                case "get-index" -> getIndex(instruction);
                case "set-index" -> setIndex(instruction);
                case "cast" -> cast(instruction);
                case "neg" -> neg(instruction);
                case "set-global" -> setGlobal(instruction);
                case "load-global" -> loadGlobal(instruction);
                case "set-arg" -> setArg(instruction);
                default -> throw new RuntimeException("Unexpected instruction '" + instruction.getInstruction() + "'!");
            }
            pointer++;
        }
        return null;
    }

    private void loadConst(OptimizedInstruction instruction)
    {
        String type = instruction.getAttribute();
        int val = instruction.getSources()[0];
        int dest = instruction.getDestination();
        this.current[dest] = switch (type)
                {
                    case "int" -> this.integers.get(val);
                    case "double" -> this.doubles.get(val);
                    case "boolean" -> this.booleans.get(val);
                    case "char" -> this.characters.get(val);
                    case "string" -> str(this.strings.get(val));
                    case "null" -> null;
                    default -> throw new RuntimeException("Unsupported constant type '" + type + "'!");
                };
    }

    private void createObj(OptimizedInstruction instruction)
    {
        int dest = instruction.getDestination();
        this.current[dest] = new ObjectValue();
    }

    private void setAttribute(OptimizedInstruction instruction)
    {
        int source = instruction.getSources()[0];
        String attr = instruction.getAttribute();
        int val = instruction.getSources()[1];
        ((ObjectValue) this.current[source]).set(attr, this.current[val]);
    }

    private void setLocal(OptimizedInstruction instruction)
    {
        this.current[instruction.getDestination()] = this.current[instruction.getSources()[0]];
    }

    private void loadLocal(OptimizedInstruction instruction)
    {
        this.current[instruction.getDestination()] = this.current[instruction.getSources()[0]];
    }

    private void callNative(OptimizedInstruction instruction)
    {
        List<Value> args = new ArrayList<>();
        for (int source : instruction.getSources())
        {
            args.add(this.current[source]);
        }

        if (!this.external.containsKey(instruction.getAttribute()))
        {
            throw new RuntimeException("Native function " + instruction.getAttribute() + " not implemented!");
        }

        Value res = this.external.get(instruction.getAttribute()).apply(args);
        if(instruction.getDestination() != -1)
            this.current[instruction.getDestination()] = res;
    }

    private void call(OptimizedInstruction instruction)
    {
        OptimizedFragment fragment = this.program.getFragment(Integer.parseInt(instruction.getAttribute()));
        Value[] current = this.current;
        Value[] next = new Value[fragment.getSize()];
        int[] args = instruction.getSources();
        for (int i = 0; i < args.length; i++)
        {
            next[i] = current[args[i]];
        }
        this.scopes.push(this.current);
        this.current = next;
        Value val = run(fragment);
        this.current = this.scopes.pop();
        if(instruction.getDestination() != -1)
            current[instruction.getDestination()] = val;
    }

    private void getAttribute(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value val = ((ObjectValue) scope[sources[0]]).get(instruction.getAttribute());
        scope[instruction.getDestination()] = val;
    }

    private void plus(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res;
        if (v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new IntValue(i1.getValue() + i2.getValue());
        }
        else if (v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new DoubleValue(d1.getValue() + d2.getValue());
        }
        else if (v1 instanceof LongValue d1 && v2 instanceof LongValue d2)
        {
            res = new LongValue(d1.getValue() + d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type! " + v1.getClass() + " " + v2.getClass());
        }
        scope[instruction.getDestination()] = res;
    }

    private void createArray(OptimizedInstruction instruction)
    {
        IntValue size = (IntValue) this.current[instruction.getSources()[0]];
        ObjectValue arr = new ObjectValue();
        for (int i = 0; i < size.getValue(); i++)
        {
            arr.set("" + i, null);
        }
        arr.set("length", new IntValue(size.getValue()));
        this.current[instruction.getDestination()] = arr;
    }

    private void less(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        IntValue v1 = (IntValue) this.current[sources[0]];
        IntValue v2 = (IntValue) this.current[sources[1]];
        Value val = new BooleanValue(v1.getValue() < v2.getValue());
        this.current[instruction.getDestination()] = val;
    }

    private void getIndex(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        ObjectValue obj = (ObjectValue) this.current[sources[0]];
        IntValue index = (IntValue) this.current[sources[1]];
        this.current[instruction.getDestination()] = obj.get(Integer.toString(index.getValue()));
    }

    private void setIndex(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        ObjectValue obj = (ObjectValue) this.current[sources[0]];
        IntValue index = (IntValue) this.current[sources[1]];
        Value value = this.current[sources[2]];
        obj.set(Integer.toString(index.getValue()), value);
    }

    private Value str(String v)
    {
        ObjectValue value = new ObjectValue();
        for (int i = 0; i < v.length(); i++)
        {
            value.set(Integer.toString(i), new CharValue(v.charAt(i)));
        }
        value.set("length", new IntValue(v.length()));
        ObjectValue obj = new ObjectValue();
        obj.set("length", new IntValue(v.length()));
        obj.set("chars", value);
        return obj;
    }

    private void cast(OptimizedInstruction instruction)
    {
        Value val = this.current[instruction.getSources()[0]];
        double v = switch (val)
                {
                    case DoubleValue o -> o.getValue();
                    case IntValue o -> o.getValue();
                    case BooleanValue o -> o.isValue() ? 1 : 0;
                    case CharValue o -> o.getValue();
                    default -> throw new RuntimeException("Unexpected type! (" + val.getClass() + ")");
                };
        String attr = instruction.getAttribute();
        Value res;
        if (attr.endsWith("int"))
        {
            res = new IntValue((int) v);
        }
        else if (attr.endsWith("boolean"))
        {
            res = new BooleanValue(v > 0);
        }
        else if (attr.endsWith("char"))
        {
            res = new CharValue((char) v);
        }
        else if (attr.endsWith("double"))
        {
            res = new DoubleValue(v);
        }
        else if (attr.endsWith("long"))
        {
            res = new LongValue((long) v);
        }
        else
        {
            throw new RuntimeException("Unsupported type! " + attr);
        }
        this.current[instruction.getDestination()] = res;
    }

    private void minus(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res;
        if (v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new IntValue(i1.getValue() - i2.getValue());
        }
        else if (v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new DoubleValue(d1.getValue() - d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope[instruction.getDestination()] = res;
    }

    private void times(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res;
        if (v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new IntValue(i1.getValue() * i2.getValue());
        }
        else if (v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new DoubleValue(d1.getValue() * d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope[instruction.getDestination()] = res;
    }

    private void equal(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res = switch (v1)
                {
                    case IntValue i1 && v2 instanceof IntValue i2 -> new BooleanValue(i1.getValue() == i2.getValue());
                    case DoubleValue d1 && v2 instanceof DoubleValue d2 -> new BooleanValue(d1.getValue() == d2.getValue());
                    case CharValue d1 && v2 instanceof CharValue d2 -> new BooleanValue(d1.getValue() == d2.getValue());
                    case BooleanValue d1 && v2 instanceof BooleanValue d2 -> new BooleanValue(d1.isValue() == d2.isValue());
                    case null, default -> new BooleanValue(v1 == v2);
                };
        scope[instruction.getDestination()] = res;
    }

    private void unequal(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res = switch (v1)
                {
                    case IntValue i1 && v2 instanceof IntValue i2 -> new BooleanValue(i1.getValue() != i2.getValue());
                    case DoubleValue d1 && v2 instanceof DoubleValue d2 -> new BooleanValue(d1.getValue() != d2.getValue());
                    case CharValue d1 && v2 instanceof CharValue d2 -> new BooleanValue(d1.getValue() != d2.getValue());
                    case BooleanValue d1 && v2 instanceof BooleanValue d2 -> new BooleanValue(d1.isValue() != d2.isValue());
                    case null, default -> throw new RuntimeException("Unsupported type!");
                };
        scope[instruction.getDestination()] = res;
    }

    private void neg(OptimizedInstruction instruction)
    {
        Value v = this.current[instruction.getSources()[0]];
        Value res;
        if (v instanceof BooleanValue b)
        {
            res = new BooleanValue(!b.isValue());
        }
        else if (v instanceof IntValue i)
        {
            res = new IntValue(-i.getValue());
        }
        else
        {
            throw new RuntimeException("UNSUPPORTED!" + v);
        }
        this.current[instruction.getDestination()] = res;
    }

    private void greaterEqual(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res;
        if (v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new BooleanValue(i1.getValue() >= i2.getValue());
        }
        else if (v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new BooleanValue(d1.getValue() >= d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope[instruction.getDestination()] = res;
    }

    private void setGlobal(OptimizedInstruction instruction)
    {
        Value[] scope = this.scopes.size() > 0 ? this.scopes.get(0) : this.current;
        scope[instruction.getDestination()] = this.current[instruction.getSources()[0]];
    }

    private void loadGlobal(OptimizedInstruction instruction)
    {
        Value[] current = this.scopes.size() > 0 ? this.scopes.get(0) : this.current;
        this.current[instruction.getDestination()] = current[instruction.getSources()[0]];
    }

    private final BooleanValue TRUE = new BooleanValue(true);
    private final BooleanValue FALSE = new BooleanValue(false);

    private void lessEqual(OptimizedInstruction instruction)
    {
        int[] sources = instruction.getSources();
        Value[] scope = this.current;
        Value v1 = scope[sources[0]];
        Value v2 = scope[sources[1]];
        Value res;
        if (v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = i1.getValue() <= i2.getValue() ? TRUE : FALSE;
        }
        else if (v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = d1.getValue() <= d2.getValue() ? TRUE : FALSE;
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope[instruction.getDestination()] = res;
    }

    private void setArg(OptimizedInstruction instruction)
    {
        // can be ignored because the variables are already at the correct place
    }
}
