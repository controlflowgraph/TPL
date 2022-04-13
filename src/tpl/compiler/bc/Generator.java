package tpl.compiler.bc;

import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import tpl.compiler.ir.IRFragment;
import tpl.compiler.ir.IRInstruction;
import tpl.compiler.ir.IRProgram;
import tpl.compiler.ir.IRType;
import tpl.compiler.type.tree.ArrayType;
import tpl.compiler.type.tree.GenericType;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.tree.function.TypedFunction;

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class Generator
{
    public static void generate(String out, IRProgram program)
    {
        new Generator(out, program).generate();
    }

    private final String out;
    private final IRProgram program;
    private final ClassWriter writer;
    private final Stack<MethodVisitor> visitors = new Stack<>();

    private Generator(String out, IRProgram program)
    {
        this.out = Path.of(out).toAbsolutePath() + "/";
        this.program = program;
        createConcreteTypes();

        this.writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        this.writer.visit(61, ACC_PUBLIC + ACC_SUPER, "Program", null, "java/lang/Object", null);

        // add default constructor
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void generate()
    {
        FieldVisitor fv = this.writer.visitField(ACC_PRIVATE + ACC_STATIC, "writer", "Ljava/io/PrintWriter;", null, null);
        fv.visitEnd();

        MethodVisitor main = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.visitCode();
        main.visitTypeInsn(NEW, "java/io/PrintWriter");
        main.visitInsn(DUP);
        main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        main.visitMethodInsn(INVOKESPECIAL, "java/io/PrintWriter", "<init>", "(Ljava/io/OutputStream;)V");
        main.visitFieldInsn(PUTSTATIC, "Program", "writer", "Ljava/io/PrintWriter;");

        main.visitMethodInsn(INVOKESTATIC, "Program", "run", "()V");
        main.visitInsn(RETURN);
        main.visitEnd();
        main.visitMaxs(4, 1);

        createNativeFunctions();

        MethodVisitor run = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "run", "()V", null, null);

        this.visitors.push(run);
        MethodVisitor peek = this.visitors.peek();
        peek.visitCode();
        generate(this.program.getBase());

        peek.visitEnd();

        for (Map.Entry<Integer, IRFragment> fragment : this.program.getProcedures().entrySet())
        {
            String signature = getFunctionSignature(fragment.getValue());
            MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "m_" + fragment.getKey(), signature, null, null);
            this.visitors.push(mv);
            generate(fragment.getValue());
            mv.visitEnd();
        }

        byte[] arr = this.writer.toByteArray();
        ClassLoader loader = null;
        try
        {
            loader = new URLClassLoader(new URL[]{
                    new URL("file:///" + this.out)
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        CheckClassAdapter.verify(new ClassReader(arr), loader, false, new PrintWriter(System.out));
        write(this.out + "Program.class", arr);
    }

    private void write(String loc, byte[] arr)
    {
        try
        {
            Files.write(Path.of(loc), arr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void createNativeFunctions()
    {
        List<TypedFunction> nativeFunctions = this.program.getNativeFunctions();
        NativeFunctionConstructor constructor = new NativeFunctionConstructor(this.writer);
        for (TypedFunction nativeFunction : nativeFunctions)
        {
            constructor.include(nativeFunction);
        }
    }

    private void createConcreteTypes()
    {
        for (IRType type : this.program.getTypes())
        {
            String name = replace(getTypeSubSignature(type.getType()));
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            writer.visit(61, ACC_SUPER, name, null, "java/lang/Object", null);
            List<Type> types = type.getTypes();
            List<String> names = type.getNames();
            StringBuilder signature = new StringBuilder();
            for (int i = 0; i < types.size(); i++)
            {
                String sig = getTypeSignature(types.get(i));
                writer.visitField(ACC_PUBLIC, names.get(i), sig, null, null)
                        .visitEnd();
                signature.append(sig);
            }

            String constructor = "(" + signature + ")V";

            MethodVisitor methodVisitor = writer.visitMethod(ACC_PUBLIC, "<init>", constructor, null, null);
            methodVisitor.visitCode();
            // initialize super class
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

            int counter = 1;
            for (int i = 0; i < types.size(); i++)
            {
                // initialize field
                String sig = getTypeSignature(types.get(i));
                methodVisitor.visitVarInsn(ALOAD, 0);
                methodVisitor.visitVarInsn(switch (sig)
                                                   {
                                                       case "I", "Z", "C" -> ILOAD;
                                                       case "J" -> LLOAD;
                                                       case "D" -> DLOAD;
                                                       case "F" -> FLOAD;
                                                       default -> ALOAD;
                                                   }, counter);
                methodVisitor.visitFieldInsn(PUTFIELD, name, names.get(i), sig);
                counter += Math.max(1, types.get(i).getByteSize() / 4);
            }

            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(2, counter);
            methodVisitor.visitEnd();

            byte[] arr = writer.toByteArray();
            CheckClassAdapter.verify(new ClassReader(arr), false, new PrintWriter(System.out));
            write(this.out + name + ".class", arr);
        }
    }

    private String getTypeSignature(Type type)
    {
        if (type instanceof SingleType s)
        {
            return s.toSignature();
        }
        else if (type instanceof ArrayType a)
        {
            String s = getTypeSignature(a.getContentType());
            String sub = a.getContentType().isPrimitive() ? s : "L" + s + ";";
            return "[" + sub;
        }
        return replace(getTypeSubSignature(type));
    }

    private String getTypeSubSignature(Type type)
    {
        return switch (type)
                {
                    case SingleType s -> s.getName();
                    case ArrayType a -> "Array<" + getTypeSubSignature(a.getContentType()) + ">";
                    case GenericType g -> {
                        StringBuilder builder = new StringBuilder(g.getName());
                        builder.append('<');
                        List<Type> generics = g.getGenerics();
                        for (int i = 0; i < generics.size(); i++)
                        {
                            builder.append(getTypeSubSignature(generics.get(i)));
                            if (i + 1 < generics.size())
                            {
                                builder.append(',');
                            }
                        }
                        builder.append('>');
                        yield builder.toString();
                    }
                    case null, default -> throw new RuntimeException("Unsupported type '" + type + "'!");
                };
    }

    private MethodVisitor current()
    {
        return this.visitors.peek();
    }

    private final Set<String> globalVariables = new HashSet<>();

    private void generate(IRFragment fragment)
    {
        Map<String, Integer> labelLines = fragment.getLabels();
        Map<Integer, Label> labels = labelLines
                .values()
                .stream()
                .distinct()
                .collect(Collectors.toMap(i -> i, i -> new Label()));
        LocalsIndexManager manager = new LocalsIndexManager();
        List<IRInstruction> instructions = fragment.getInstructions();

        int stackSize = 0;
        int stackMax = 0;
        for (int i = 0; i < instructions.size(); i++)
        {
            IRInstruction instruction = instructions.get(i);
            int stackDelta = switch (instruction.getInstruction())
                    {
                        case "scope-global", "unscope-global", "scope", "unscope" -> 0;
                        case "set-arg" -> setArg(instruction, manager);
                        case "load-constant" -> loadConstant(instruction);
                        case "set-global" -> setGlobal(instruction);
                        case "load-global" -> loadGlobal(instruction);
                        case "set-local" -> setLocal(instruction, manager);
                        case "load-local" -> loadLocal(instruction, manager);
                        case "jump" -> jump(instruction, labelLines, labels);
                        case "branch" -> branch(instruction, labelLines, labels);
                        case "+" -> plus(instruction);
                        case "-" -> minus(instruction);
                        case "*" -> multiply(instruction);
                        case "<" -> less(instruction);
                        case "<=" -> lessEqual(instruction);
                        case ">=" -> greaterEqual(instruction);
                        case "==" -> equal(instruction);
                        case "!=" -> unequal(instruction);
                        case "cast" -> cast(instruction);
                        case "call" -> call(instruction);
                        case "call-native" -> callNative(instruction);
                        case "neg" -> neg(instruction);
                        case "return" -> give(instruction);
                        case "new-array" -> createArray(instruction);
                        case "new-object" -> createObject(instruction);
                        case "init-object" -> initializeObject(instruction);
                        case "get-attribute" -> getAttribute(instruction);
                        case "set-attribute" -> setAttribute(instruction);
                        case "set-index" -> setIndex(instruction);
                        case "get-index" -> getIndex(instruction);
                        default -> throw new RuntimeException("Unsupported instruction " + instruction);
                    };

            stackSize += stackDelta;
            stackMax = Math.max(stackMax, stackSize);

            if (labels.containsKey(i))
            {
                current().visitLabel(labels.get(i));
            }
        }

        if (fragment.getReturnType().matches(new SingleType(null, "void")))
        {
            current().visitInsn(RETURN);
        }

        current().visitMaxs(stackMax, manager.getCount());
    }

    private int loadConstant(IRInstruction instruction)
    {
        String type = instruction.getAttribute();
        switch (type)
        {
            case "int" -> {
                current().visitLdcInsn(Integer.parseInt(instruction.getSources().get(0)));
                return 1;
            }
            case "long" -> {
                current().visitLdcInsn(Long.parseLong(instruction.getSources().get(0)));
                return 2;
            }
            case "float" -> {
                current().visitLdcInsn(Float.parseFloat(instruction.getSources().get(0)));
                return 1;
            }
            case "double" -> {
                current().visitLdcInsn(Double.parseDouble(instruction.getSources().get(0)));
                return 2;
            }
            case "string" -> {
                String data = instruction.getSources().get(0);
                data = data.substring(1, data.length() - 1);
                current().visitTypeInsn(NEW, "String");
                current().visitInsn(DUP);
                current().visitLdcInsn(data.length());
                current().visitLdcInsn((java.lang.String) data);
                current().visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C");
                current().visitMethodInsn(INVOKESPECIAL, "String", "<init>", "(I[C)V");

                return 4;
            }
            case "null" -> {
                current().visitInsn(ACONST_NULL);
                return 1;
            }
            case "char" -> {
                // TODO: fix \' etc
                current().visitLdcInsn(instruction.getSources().get(0).charAt(1));
                return 1;
            }
            case "boolean" -> {
                if (instruction.getSources().get(0).equals("true"))
                {
                    current().visitInsn(ICONST_1);
                }
                else
                {
                    current().visitInsn(ICONST_0);
                }
                return 1;
            }
            default -> throw new RuntimeException("Unsupported constant type '" + type + "'! (" + instruction + ")");
        }
    }

    private int setGlobal(IRInstruction instruction)
    {
        String name = instruction.getDestination();
        Type type = instruction.getTypes().get(0);
        String declaration = getTypeSignature(type);
        if (!type.isPrimitive())
        {
            declaration = "L" + declaration + ";";
        }

        if (!this.globalVariables.contains(name))
        {
            this.writer.visitField(ACC_PUBLIC + ACC_STATIC, name, declaration, null, null).visitEnd();
            this.globalVariables.add(name);
        }
        current().visitFieldInsn(PUTSTATIC, "Program", name, declaration);
        return -Math.max(1, type.getByteSize() / 4);
    }

    private int loadGlobal(IRInstruction instruction)
    {
        String name = instruction.getSources().get(0);
        Type type = instruction.getTypes().get(0);
        String declaration = replace(getTypeSignature(type));
        if (!type.isPrimitive())
        {
            declaration = "L" + declaration + ";";
        }
        current().visitFieldInsn(GETSTATIC, "Program", name, declaration);
        return Math.max(1, type.getByteSize() / 4);
    }

    private int setLocal(IRInstruction instruction, LocalsIndexManager manager)
    {
        String name = instruction.getDestination();
        Type type = instruction.getTypes().get(0);
        switch (type.toString())
        {
            case "boolean", "int" -> {
                current().visitVarInsn(ISTORE, manager.getIndexOrCreate(name, type));
                return -1;
            }
            case "byte", "char", "double", "float", "long" -> {
                throw new RuntimeException("Store of type '" + type + "' not supported! (" + instruction + ")");
            }
            default -> {
                current().visitVarInsn(ASTORE, manager.getIndexOrCreate(name, type));
                return -1;
            }
        }
    }

    private int loadLocal(IRInstruction instruction, LocalsIndexManager manager)
    {
        String name = instruction.getSources().get(0);
        Type type = instruction.getTypes().get(0);
        switch (type.toString())
        {
            case "boolean", "int" -> {
                current().visitVarInsn(ILOAD, manager.getIndex(name, type));
                return 1;
            }
            case "byte", "char", "float", "long" -> {
                throw new RuntimeException("Load of type '" + type + "' not supported! (" + instruction + ")");
            }
            case "double" -> {
                System.out.println("LOADING DOUBLE");
                current().visitVarInsn(DLOAD, manager.getIndex(name, type));
                return 2;
            }
            default -> {
                current().visitVarInsn(ALOAD, manager.getIndex(name, type));
                return 1;
            }
        }
    }

    private int plus(IRInstruction instruction)
    {
        String type = instruction.getAttribute();
        switch (type)
        {
            case "int" -> {
                current().visitInsn(IADD);
                return -1;
            }
            case "long" -> {
                current().visitInsn(LADD);
                return -2;
            }
            case "double" -> {
                current().visitInsn(DADD);
                return -2;
            }
            default -> throw new RuntimeException("Addition of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int minus(IRInstruction instruction)
    {
        String type = instruction.getAttribute();
        switch (type)
        {
            case "int" -> {
                current().visitInsn(ISUB);
                return -1;
            }
            case "long" -> {
                current().visitInsn(LSUB);
                return -2;
            }
            case "double" -> {
                current().visitInsn(DSUB);
                return -2;
            }
            default -> throw new RuntimeException("Addition of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int multiply(IRInstruction instruction)
    {
        String type = instruction.getAttribute();
        switch (type)
        {
            case "int" -> {
                current().visitInsn(IMUL);
                return -1;
            }
            case "long" -> {
                current().visitInsn(LMUL);
                return -2;
            }
            case "double" -> {
                current().visitInsn(DMUL);
                return -2;
            }
            default -> throw new RuntimeException("Addition of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int less(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(1).toString();
        switch (type)
        {
            case "int" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ICMPGE, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return -1;
            }
            default -> throw new RuntimeException("Less of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int lessEqual(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(1).toString();
        switch (type)
        {
            case "int" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ICMPGT, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return -1;
            }
            default -> throw new RuntimeException("Less equal of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int greaterEqual(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(1).toString();
        switch (type)
        {
            case "int" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ICMPLT, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return -1;
            }
            default -> throw new RuntimeException("Less equal of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int equal(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(1).toString();
        switch (type)
        {
            case "int" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ICMPNE, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return -1;
            }
            case "List<int>" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IFNONNULL, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_APPEND, 1, new Object[]{"List_int_"}, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return 4;
            }
            case "List<double>" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ACMPNE, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_APPEND, 1, new Object[]{"List_double_"}, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return 4;
            }
            case "String" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ACMPNE, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_APPEND, 1, new Object[]{"String"}, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return 4;
            }
            default -> throw new RuntimeException("Equal of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int unequal(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(1).toString();
        switch (type)
        {
            case "char", "int" -> {
                Label l0 = new Label();
                current().visitJumpInsn(IF_ICMPEQ, l0);
                current().visitInsn(ICONST_1);
                Label l1 = new Label();
                current().visitJumpInsn(GOTO, l1);
                current().visitLabel(l0);
                current().visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                current().visitInsn(ICONST_0);
                current().visitLabel(l1);
                current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
                return -1;
            }
            default -> throw new RuntimeException("Equal of type '" + type + "' not supported! (" + instruction + ")");
        }
    }

    private int jump(IRInstruction instruction, Map<String, Integer> labelLines, Map<Integer, Label> labels)
    {
        current().visitJumpInsn(GOTO, labels.get(labelLines.get(instruction.getAttribute())));
        return 0;
    }

    private int branch(IRInstruction instruction, Map<String, Integer> labelLines, Map<Integer, Label> labels)
    {
        current().visitJumpInsn(IFNE, labels.get(labelLines.get(instruction.getAttribute())));
        return -1;
    }

    private int cast(IRInstruction instruction)
    {
        String conversion = instruction.getAttribute();
        switch (conversion)
        {
            case "int->long" -> {
                current().visitInsn(I2L);
                return 1;
            }
            case "int->double" -> {
                current().visitInsn(I2D);
                return 1;
            }
            case "double->int" -> {
                current().visitInsn(D2I);
                return -1;
            }
            default -> throw new RuntimeException("Conversion " + conversion + " not supported! (" + instruction + ")");
        }
    }

    private int call(IRInstruction instruction)
    {
        int id = Integer.parseInt(instruction.getAttribute());
        IRFragment fragment = this.program.getProcedures().get(id);
        int parametersSlots = Math.max(1, fragment.getReturnType().getByteSize() / 4);
        for (Type parameter : fragment.getParameters())
        {
            parametersSlots -= Math.max(1, parameter.getByteSize() / 4);
        }

        String signature = getFunctionSignature(fragment);

        current().visitMethodInsn(INVOKESTATIC, "Program", "m_" + id, signature);

        return -parametersSlots;
    }

    private String getFunctionSignature(IRFragment fragment)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (Type parameter : fragment.getParameters())
        {
            String dec = replace(getTypeSignature(parameter));
            if (!parameter.isPrimitive())
            {
                dec = "L" + dec + ";";
            }
            builder.append(dec);
        }
        builder.append(")");
        String s = replace(getTypeSignature(fragment.getReturnType()));
        if (!fragment.getReturnType().isPrimitive())
        {
            s = "L" + s + ";";
        }
        builder.append(s);
        return builder.toString();
    }

    private int setArg(IRInstruction instruction, LocalsIndexManager manager)
    {
        manager.getIndexOrCreate(instruction.getDestination(), instruction.getTypes().get(0));
        return 0;
    }

    private int neg(IRInstruction instruction)
    {
        Label l0 = new Label();
        current().visitJumpInsn(IFNE, l0);
        current().visitInsn(ICONST_1);
        Label l1 = new Label();
        current().visitJumpInsn(GOTO, l1);
        current().visitLabel(l0);
        current().visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        current().visitInsn(ICONST_0);
        current().visitLabel(l1);
        current().visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});
        return 0;
    }

    private int give(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(0).toString();
        switch (type)
        {
            case "char", "boolean", "byte", "short", "int" -> current().visitInsn(IRETURN);
            case "long" -> current().visitInsn(LRETURN);
            case "float" -> current().visitInsn(FRETURN);
            case "double" -> current().visitInsn(DRETURN);
            case "void" -> current().visitInsn(RETURN);
            default -> current().visitInsn(ARETURN);
        }
        return 0;
    }

    private int createArray(IRInstruction instruction)
    {
        // IDEA: Encoding of classes:
        // replace '<' with ' '
        // replace '>' with '='
        // e.g. Map<Integer, String> will become
        // Map Integer,String=

//        int T_BOOLEAN = 4;
//        int T_CHAR = 5;
//        int T_FLOAT = 6;
//        int T_DOUBLE = 7;
//        int T_BYTE = 8;
//        int T_SHORT = 9;
//        int T_INT = 10;
//        int T_LONG = 11;
//        "java/lang/String";
        String type = ((ArrayType) instruction.getTypes().get(0)).getContentType().toString();
        switch (type)
        {
            case "int" -> current().visitIntInsn(NEWARRAY, T_INT);
            case "char" -> current().visitIntInsn(NEWARRAY, T_CHAR);
            case "double" -> current().visitIntInsn(NEWARRAY, T_DOUBLE);
            case "float", "boolean", "long", "byte", "short" -> throw new RuntimeException("Not supported! (" + type + ")");
            default -> current().visitTypeInsn(ANEWARRAY, type);
        }
        return 0;
    }

    private int createObject(IRInstruction instruction)
    {
        String type = replace(getTypeSubSignature(instruction.getTypes().get(0)));
        current().visitTypeInsn(NEW, type);
        current().visitInsn(DUP);
        // TODO: add branch true and branch false to avoid neg instruction due to overly complex structure
        // TODO: add more precise stack requirements (so that i can return 1 but actually use 2 stack slots)
        return 2;
    }

    private int initializeObject(IRInstruction instruction)
    {
        List<Type> types = instruction.getTypes();
        String name = getClassName(types.get(0));

        StringBuilder builder = new StringBuilder();
        int consumption = 1;
        for (int i = 1; i < types.size(); i++)
        {
            builder.append(getTypeSignature(types.get(i)));
            consumption += Math.max(1, types.get(i).getByteSize() / 4);
        }

        current().visitMethodInsn(INVOKESPECIAL, name, "<init>", "(" + builder + ")V");

        return -consumption; // TODO: compute the correct delta
    }

    private String getClassName(Type type)
    {
        return replace(type.toString());
    }

    private String replace(String s)
    {
        return s.replace(" ", "")
                .replace(",", "_")
                .replace("<", "_")
                .replace(">", "_");
    }

    private int getAttribute(IRInstruction instruction)
    {
        if (instruction.getTypes().get(0) instanceof ArrayType a)
        {
            current().visitInsn(ARRAYLENGTH);
            return 1;
        }
        else
        {
            String name = getTypeSignature(instruction.getTypes().get(0));
            String declaration = getTypeSignature(instruction.getTypes().get(1));
            String attribute = instruction.getAttribute();
            current().visitFieldInsn(GETFIELD, name, attribute, declaration);
            return Math.max(1, instruction.getTypes().get(1).getByteSize() / 4) - 1;
        }
    }

    private int setAttribute(IRInstruction instruction)
    {
        String name = getTypeSignature(instruction.getTypes().get(0));
        String declaration = getTypeSignature(instruction.getTypes().get(1));
        String attribute = instruction.getAttribute();
        current().visitFieldInsn(PUTFIELD, name, attribute, declaration);
        return -Math.max(1, instruction.getTypes().get(1).getByteSize() / 4) - 1;
    }

    private int setIndex(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(0).toString();
        switch (type)
        {
            case "int[]" -> {
                current().visitInsn(IASTORE);
                return -3;
            }
            case "char[]" -> {
                current().visitInsn(CASTORE);
                return -3;
            }
            case "double[]" -> {
                current().visitInsn(DASTORE);
                return -4;
            }
            case "boolean[]", "byte[]", "short[]", "float[]" -> throw new RuntimeException("Assignment into '" + type + "'! (" + instruction + ")");
            default -> {
                current().visitInsn(AASTORE);
                return -3;
            }
        }
    }

    private int getIndex(IRInstruction instruction)
    {
        String type = instruction.getTypes().get(0).toString();
        switch (type)
        {
            case "int[]" -> {
                current().visitInsn(IALOAD);
                return -1;
            }
            case "char[]" -> {
                current().visitInsn(CALOAD);
                return -1;
            }
            case "boolean[]", "byte[]", "short[]", "float[]" -> throw new RuntimeException("Loading from '" + type + "' not supported! (" + instruction + ")");
            case "double[]" -> {
                current().visitInsn(DALOAD);
                return 0;
            }
            default -> {
                if (instruction.getTypes().get(0) instanceof GenericType g && g.getName().equals("Tuple"))
                {
                    // TODO: find good way to calculate the exact size of the required stack
                    System.out.println(instruction);
                    current().visitFieldInsn(GETFIELD, type, "Test", "[C");
                    return Math.max(1, instruction.getTypes().get(1).getByteSize() / 4) - 1;
                }
                else
                {
                    current().visitInsn(AALOAD);
                    return -1;
                }
            }
        }
    }

    private int callNative(IRInstruction instruction)
    {
        switch (instruction.getAttribute())
        {
            case "print[String]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "print", "(LString;)V");
                return -1;
            }
            case "str[boolean]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "str", "(Z)LString;");
                return -1;
            }
            case "str[char]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "str", "(C)LString;");
                return -1;
            }
            case "str[int]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "str", "(I)LString;");
                return -1;
            }
            case "str[double]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "str", "(D)LString;");
                return -1;
            }
            case "str[long]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "str", "(J)LString;");
                return -1;
            }
            case "random[]" -> {
                current().visitMethodInsn(INVOKESTATIC, "Program", "random", "()D");
                return 2;
            }
            default -> throw new RuntimeException("Unsupported native function '" + instruction.getAttribute() + "'! (" + instruction + ")");
        }
    }
}
