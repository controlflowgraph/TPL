package tpl.compiler.bc;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import tpl.compiler.typed.tree.function.TypedFunction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class NativeFunctionConstructor
{
    private final Map<String, Method> generators = new HashMap<>();
    private final Map<String, String[]> dependencies = new HashMap<>();

    private final ClassWriter writer;

    public NativeFunctionConstructor(ClassWriter writer)
    {
        this.writer = writer;
        fillMaps();
    }

    private void fillMaps()
    {
        Method[] methods = NativeFunctionConstructor.class.getDeclaredMethods();
        for (Method method : methods)
        {
            if(method.isAnnotationPresent(NativeFunction.class))
            {
                NativeFunction annotation = method.getAnnotation(NativeFunction.class);
                this.generators.put(annotation.signature(), method);
                this.dependencies.put(annotation.signature(), annotation.dependencies());
            }
        }
    }

    private final Set<String> included = new HashSet<>();

    public void include(TypedFunction function)
    {
        String signature = function.getName() + function.getParameters();
        include(signature);
    }

    private void include(String signature)
    {
        if(this.included.contains(signature))
            return;

        this.included.add(signature);
        Method method = this.generators.get(signature);
        String[] dependencies = this.dependencies.get(signature);
        try
        {
            method.invoke(this);
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred while generating bytecode for native function " + signature + "!");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        for (String dependency : dependencies)
        {
            include(dependency);
        }
    }

    @NativeFunction(signature = "print[String]")
    private void printString()
    {
        // print function for internal strings
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "print", "(LString;)V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, "Program", "writer", "Ljava/io/PrintWriter;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "String", "chars", "[C");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintWriter", "println", "([C)V");
        mv.visitFieldInsn(GETSTATIC, "Program", "writer", "Ljava/io/PrintWriter;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintWriter", "flush", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    @NativeFunction(signature = "random[]")
    private void randomDouble()
    {
        // random function
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "random", "()D", null, null);
        mv.visitCode();
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "random", "()D");
        mv.visitInsn(DRETURN);
        mv.visitMaxs(2, 0);
        mv.visitEnd();
    }

    @NativeFunction(signature = "str[double]", dependencies = { "tstr" })
    private void toStringDouble()
    {
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "str", "(D)LString;", null, null);
        mv.visitCode();
        mv.visitVarInsn(DLOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "toString", "(D)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "Program", "tstr", "(Ljava/lang/String;)LString;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 3);
        mv.visitEnd();
    }

    @NativeFunction(signature = "str[int]", dependencies = { "tstr" })
    private void toStringInteger()
    {
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "str", "(I)LString;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "Program", "tstr", "(Ljava/lang/String;)LString;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    @NativeFunction(signature = "str[char]", dependencies = { "tstr" })
    private void toStringCharacter()
    {
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "str", "(C)LString;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "toString", "(I)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "Program", "tstr", "(Ljava/lang/String;)LString;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    @NativeFunction(signature = "str[boolean]", dependencies = { "tstr" })
    private void toStringBoolean()
    {
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "str", "(Z)LString;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ILOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "toString", "(Z)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "Program", "tstr", "(Ljava/lang/String;)LString;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    @NativeFunction(signature = "str[long]", dependencies = { "tstr" })
    private void toStringLong()
    {
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "str", "(J)LString;", null, null);
        mv.visitCode();
        mv.visitVarInsn(LLOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "toString", "(J)Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESTATIC, "Program", "tstr", "(Ljava/lang/String;)LString;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

    @NativeFunction(signature = "tstr")
    private void toStringJString()
    {
        MethodVisitor mv = this.writer.visitMethod(ACC_PUBLIC + ACC_STATIC, "tstr", "(Ljava/lang/String;)LString;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, "String");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C");
        mv.visitMethodInsn(INVOKESPECIAL, "String", "<init>", "(I[C)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }
}
