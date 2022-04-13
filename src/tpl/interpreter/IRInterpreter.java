package tpl.interpreter;

import tpl.interpreter.value.Value;
import tpl.compiler.ir.IRFragment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.ir.IRProgram;
import tpl.interpreter.value.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class IRInterpreter
{
    public static void run(IRProgram program)
    {
        program.dump("data/dump.txt");
        new IRInterpreter(program).run();
    }

    private final IRProgram program;
    private final Stack<Scope> scopes = new Stack<>();

    private final Map<String, Function<List<Value>, Value>> external = new HashMap<>();

    private IRInterpreter(IRProgram program)
    {
        this.program = program;
        this.external.put("print[String]", args -> {

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
        for(int i = 0; i < length; i++)
        {
            builder.append(((CharValue) arr.get(Integer.toString(i))).getValue());
        }
        return builder.toString();
    }

    private void run()
    {
        long t1 = System.currentTimeMillis();
        this.scopes.push(new Scope());
        run(this.program.getBase());
        long t2 = System.currentTimeMillis();
        System.out.println("---------[ EXECUTION RESULTS ]---------");
        System.out.println("Instructions: " + this.instructionCounter + " | Time: " + (t2 - t1) / 1000.0f + " s");
//        System.out.println("---------[ INSTRUCTION HISTOGRAM ]---------");
//        this.histogram.entrySet()
//                .stream()
//                .sorted(Map.Entry.comparingByValue())
//                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
    }

    private int instructionCounter;
    private final Map<String, Integer> histogram = new HashMap<>();

    private Value run(IRFragment frag)
    {
        int pointer = 0;
        List<IRInstruction> instructions = frag.getInstructions();
        while(0 <= pointer && pointer < instructions.size())
        {
            this.instructionCounter++;
            IRInstruction instruction = instructions.get(pointer);
            this.histogram.put(instruction.getInstruction(), this.histogram.getOrDefault(instruction.getInstruction(), 0) + 1);
            if(instruction.isStandard())
            {
                switch (instruction.getInstruction())
                {
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
                        BooleanValue cond = (BooleanValue) this.scopes.peek().get(instruction.getSources().get(0));
                        if(cond.isValue())
                        {
                            pointer = frag.getLabels().get(instruction.getAttribute());
                        }
                    }
                    case "return" -> { return this.scopes.peek().get(instruction.getSources().get(0)); }
                    case "get-index" -> getIndex(instruction);
                    case "set-index" -> setIndex(instruction);
                    case "cast" -> cast(instruction);
                    case "neg" -> neg(instruction);
                    case "set-global" -> setGlobal(instruction);
                    case "load-global" -> loadGlobal(instruction);
                    case "set-arg" -> setArg(instruction);
                    default -> throw new RuntimeException("Unexpected instruction '" + instruction.getInstruction() + "'!");
                }
            }
            pointer++;
        }
        return null;
    }

    private void loadConst(IRInstruction instruction)
    {
        String type = instruction.getAttribute();
        final String val = instruction.getSources().get(0);
        this.scopes.peek().set(instruction.getDestination(), switch (type)
                {
                    case "int" -> new IntValue(Integer.parseInt(val));
                    case "double" -> new DoubleValue(Double.parseDouble(val));
                    case "boolean" -> new BooleanValue(Boolean.parseBoolean(val));
                    case "char" -> new CharValue(switch (val)
                                                         {
                                                             case "'\\n'" -> '\n';
                                                             case "'\\r'" -> '\r';
                                                             case "'\\t'" -> '\t';
                                                             default -> val.charAt(1);
                                                         });
                    case "string" -> str(val.substring(1, val.length() - 1));
                    case "null" -> null;
                    default -> throw new RuntimeException("Unsupported constant type '" + type + "'!");
                });
    }

    private void createObj(IRInstruction instruction)
    {
        ObjectValue obj = new ObjectValue();
        this.scopes.peek().set(instruction.getDestination(), obj);
    }

    private void setAttribute(IRInstruction instruction)
    {
        String source = instruction.getSources().get(0);
        String attr = instruction.getAttribute();
        String val = instruction.getSources().get(1);
        ((ObjectValue) this.scopes.peek().get(source)).set(attr, this.scopes.peek().get(val));
    }

    private void setLocal(IRInstruction instruction)
    {
        this.scopes.peek().set(instruction.getDestination(), this.scopes.peek().get(instruction.getSources().get(0)));
    }

    private void loadLocal(IRInstruction instruction)
    {
        this.scopes.peek().set(instruction.getDestination(), this.scopes.peek().get(instruction.getSources().get(0)));
    }

    private void callNative(IRInstruction instruction)
    {
        List<Value> args = new ArrayList<>();
        for (String source : instruction.getSources())
        {
            args.add(this.scopes.peek().get(source));
        }
        if(!this.external.containsKey(instruction.getAttribute()))
        {
            throw new RuntimeException("Native function " + instruction.getAttribute() + " not implemented!");
        }
        Value res = this.external.get(instruction.getAttribute()).apply(args);

        this.scopes.peek().set(instruction.getDestination(), res);
    }

    private void call(IRInstruction instruction)
    {
        Scope current = this.scopes.peek();
        Scope next = new Scope();
        List<String> args = instruction.getSources();
        for(int i = 0; i < args.size(); i++)
        {
            next.set(Integer.toString(i), current.get(args.get(i)));
        }
        this.scopes.push(next);
        Value val = run(this.program.getFragment(Integer.parseInt(instruction.getAttribute())));
        this.scopes.pop();
        current.set(instruction.getDestination(), val);
    }

    private void getAttribute(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value val = ((ObjectValue) scope.get(sources.get(0))).get(instruction.getAttribute());
        scope.set(instruction.getDestination(), val);
    }

    private void plus(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res;
        if(v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new IntValue(i1.getValue() + i2.getValue());
        }
        else if(v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new DoubleValue(d1.getValue() + d2.getValue());
        }
        else if(v1 instanceof LongValue d1 && v2 instanceof LongValue d2)
        {
            res = new LongValue(d1.getValue() + d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type! " + v1.getClass() + " " + v2.getClass());
        }
        scope.set(instruction.getDestination(), res);
    }

    private void createArray(IRInstruction instruction)
    {
        IntValue size = (IntValue) this.scopes.peek().get(instruction.getSources().get(0));
        ObjectValue arr = new ObjectValue();
        for(int i = 0; i < size.getValue(); i++)
        {
            arr.set("" + i, null);
        }
        arr.set("length", new IntValue(size.getValue()));
        this.scopes.peek().set(instruction.getDestination(), arr);
    }

    private void less(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        IntValue v1 = (IntValue) this.scopes.peek().get(sources.get(0));
        IntValue v2 = (IntValue) this.scopes.peek().get(sources.get(1));
        Value val = new BooleanValue(v1.getValue() < v2.getValue());
        this.scopes.peek().set(instruction.getDestination(), val);
    }

    private void getIndex(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        ObjectValue obj = (ObjectValue) this.scopes.peek().get(sources.get(0));
        IntValue index = (IntValue) this.scopes.peek().get(sources.get(1));
        this.scopes.peek().set(instruction.getDestination(), obj.get(Integer.toString(index.getValue())));
    }

    private void setIndex(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        ObjectValue obj = (ObjectValue) this.scopes.peek().get(sources.get(0));
        IntValue index = (IntValue) this.scopes.peek().get(sources.get(1));
        Value value = this.scopes.peek().get(sources.get(2));
        obj.set(Integer.toString(index.getValue()), value);
    }

    private Value str(String v)
    {
        ObjectValue value = new ObjectValue();
        for(int i = 0; i < v.length(); i++)
        {
            value.set(Integer.toString(i), new CharValue(v.charAt(i)));
        }
        value.set("length", new IntValue(v.length()));
        ObjectValue obj = new ObjectValue();
        obj.set("length", new IntValue(v.length()));
        obj.set("chars", value);
        return obj;
    }

    private void cast(IRInstruction instruction)
    {
        Value val = this.scopes.peek().get(instruction.getSources().get(0));
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
        if(attr.endsWith("int"))
        {
            res = new IntValue((int) v);
        }
        else if(attr.endsWith("boolean"))
        {
            res = new BooleanValue(v > 0);
        }
        else if(attr.endsWith("char"))
        {
            res = new CharValue((char) v);
        }
        else if(attr.endsWith("double"))
        {
            res = new DoubleValue(v);
        }
        else if(attr.endsWith("long"))
        {
            res = new LongValue((long) v);
        }
        else
        {
            throw new RuntimeException("Unsupported type! " + attr);
        }
        this.scopes.peek().set(instruction.getDestination(), res);
    }

    private void minus(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res;
        if(v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new IntValue(i1.getValue() - i2.getValue());
        }
        else if(v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new DoubleValue(d1.getValue() - d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope.set(instruction.getDestination(), res);
    }

    private void times(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res;
        if(v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new IntValue(i1.getValue() * i2.getValue());
        }
        else if(v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new DoubleValue(d1.getValue() * d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope.set(instruction.getDestination(), res);
    }

    private void equal(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res = switch (v1)
                {
                    case IntValue i1 && v2 instanceof IntValue i2 -> new BooleanValue(i1.getValue() == i2.getValue());
                    case DoubleValue d1 && v2 instanceof DoubleValue d2 -> new BooleanValue(d1.getValue() == d2.getValue());
                    case CharValue d1 && v2 instanceof CharValue d2 -> new BooleanValue(d1.getValue() == d2.getValue());
                    case BooleanValue d1 && v2 instanceof BooleanValue d2 -> new BooleanValue(d1.isValue() == d2.isValue());
                    case null, default -> new BooleanValue(v1 == v2);
                };
        scope.set(instruction.getDestination(), res);
    }

    private void unequal(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res = switch (v1)
                {
                    case IntValue i1 && v2 instanceof IntValue i2 -> new BooleanValue(i1.getValue() != i2.getValue());
                    case DoubleValue d1 && v2 instanceof DoubleValue d2 -> new BooleanValue(d1.getValue() != d2.getValue());
                    case CharValue d1 && v2 instanceof CharValue d2 -> new BooleanValue(d1.getValue() != d2.getValue());
                    case BooleanValue d1 && v2 instanceof BooleanValue d2 -> new BooleanValue(d1.isValue() != d2.isValue());
                    case null, default -> throw new RuntimeException("Unsupported type!");
                };
        scope.set(instruction.getDestination(), res);
    }

    private void neg(IRInstruction instruction)
    {
        Value v = this.scopes.peek().get(instruction.getSources().get(0));
        Value res;
        if(v instanceof BooleanValue b)
        {
            res = new BooleanValue(!b.isValue());
        }
        else if(v instanceof IntValue i)
        {
            res = new IntValue(- i.getValue());
        }
        else
        {
            throw new RuntimeException("UNSUPPORTED!" + v);
        }
        this.scopes.peek().set(instruction.getDestination(), res);
    }

    private void greaterEqual(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res;
        if(v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new BooleanValue(i1.getValue() >= i2.getValue());
        }
        else if(v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new BooleanValue(d1.getValue() >= d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope.set(instruction.getDestination(), res);
    }

    private void setGlobal(IRInstruction instruction)
    {
        this.scopes.get(0).set(instruction.getDestination(), this.scopes.peek().get(instruction.getSources().get(0)));
    }

    private void loadGlobal(IRInstruction instruction)
    {
        this.scopes.peek().set(instruction.getDestination(), this.scopes.get(0).get(instruction.getSources().get(0)));
    }

    private void lessEqual(IRInstruction instruction)
    {
        List<String> sources = instruction.getSources();
        Scope scope = this.scopes.peek();
        Value v1 = scope.get(sources.get(0));
        Value v2 = scope.get(sources.get(1));
        Value res;
        if(v1 instanceof IntValue i1 && v2 instanceof IntValue i2)
        {
            res = new BooleanValue(i1.getValue() <= i2.getValue());
        }
        else if(v1 instanceof DoubleValue d1 && v2 instanceof DoubleValue d2)
        {
            res = new BooleanValue(d1.getValue() <= d2.getValue());
        }
        else
        {
            throw new RuntimeException("Unsupported type!");
        }
        scope.set(instruction.getDestination(), res);
    }

    private void setArg(IRInstruction instruction)
    {
        Scope scope = this.scopes.peek();
        scope.set(instruction.getDestination(), scope.get(instruction.getSources().get(0)));
    }
}
