package tpl.compiler;

import tpl.compiler.analysis.IRAnalyzer;
import tpl.compiler.bc.Generator;
import tpl.compiler.ir.*;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.TypedStructure;
import tpl.compiler.typed.tree.function.TypedFunction;
import tpl.compiler.typed.tree.other.TypedField;
import tpl.compiler.untyped.Parser;
import tpl.compiler.untyped.UntypedFragment;
import tpl.compiler.untyped.tree.UntypedStructure;
import tpl.compiler.util.TPLException;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TPLC
{
    public static void main(String[] args)
    {
        if(args.length != 2)
        {
            System.out.println("Expected 2 argument (the source file and output location)");
            System.exit(1);
        }
        else
        {
            compile(args[0], args[1]);
        }
    }

    public static void compile(String base, String out)
    {
        try
        {
            Map<Path, UntypedFragment> fragments = loadFragments(base);
            List<UntypedFragment> ordered = orderFragments(fragments);
            List<UntypedStructure> structures = fuseStructures(ordered);
            TypeEnvironment env = new TypeEnvironment();
            env.enterScope();
            List<TypedStructure> typed = structures.stream().map(s -> s.checkType(env)).toList();
            List<TypedField> scoped = env.leaveScope();

            env.getImplementations().forEach((a, b) -> b.forEach(TypedFunction::checkExiting));

            Map<String, IRType> concreteTypes = new HashMap<>();
            IREnvironment environment = new IREnvironment(concreteTypes);

            environment.setReturnType(new SingleType(null, "void"));
            List<String> names = scoped.stream().map(TypedField::name).toList();
            List<Type> types = scoped.stream().map(TypedField::type).toList();
            environment.addInstruction(new IRInstruction("scope-global", names, types));
            typed.forEach(t -> t.toIR(environment));
            environment.addInstruction(new IRInstruction("unscope-global", names, types));
            List<TypedFunction> external = env.getImplementations()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(TypedFunction::isExternal)
                    .toList();
            Map<Integer, IRFragment> procedures = env.getImplementations()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(f -> !f.isExternal())
                    .collect(Collectors.toMap(TypedFunction::getId, f -> f.toFragment(concreteTypes)));



            IRProgram program = new IRProgram(concreteTypes.values().stream().toList(), procedures, environment.toFragment(), external);

//            Generator.generate(out, program);
            IRAnalyzer.analyze(program);
            System.out.println("Successfully compiled program!");
            System.exit(0);
        }
        catch (TPLException e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<UntypedStructure> fuseStructures(List<UntypedFragment> fragments)
    {
        return fragments.stream()
                .flatMap(f -> f.getStructures().stream())
                .toList();
    }

    private static List<UntypedFragment> orderFragments(Map<Path, UntypedFragment> fragments)
    {
        List<UntypedFragment> order = new ArrayList<>();
        Set<Path> visited = new HashSet<>();
        List<UntypedFragment> open = fragments.values().stream().toList();
        while (open.size() > 0)
        {
            List<UntypedFragment> unclosed = new ArrayList<>();
            for (UntypedFragment fragment : open)
            {
                if (hasOpenDependency(visited, fragment))
                {
                    unclosed.add(fragment);
                }
                else
                {
                    visited.add(fragment.getPath());
                    order.add(fragment);
                }
            }

            if (unclosed.size() == open.size())
                throw new TPLException("Cycle in include graph!");

            open = unclosed;
        }
        return order;
    }

    private static boolean hasOpenDependency(Set<Path> visited, UntypedFragment fragment)
    {
        for (Path include : fragment.getIncludes())
        {
            if (!visited.contains(include))
            {
                return true;
            }
        }
        return false;
    }

    private static Map<Path, UntypedFragment> loadFragments(String base)
    {
        Path basePath = Path.of(base).toAbsolutePath();

        Set<Path> enqueued = new HashSet<>();
        enqueued.add(basePath);

        Queue<Path> queue = new PriorityQueue<>();
        queue.offer(basePath);

        Map<Path, UntypedFragment> fragments = new HashMap<>();
        while (!queue.isEmpty())
        {
            Path path = queue.remove();
            UntypedFragment fragment = Parser.parseFile(path);
            Set<Path> includes = fragment.getIncludes();
            for (Path include : includes)
            {
                if (!enqueued.contains(include))
                {
                    queue.add(include);
                    enqueued.add(include);
                }
            }
            fragments.put(path, fragment);
        }
        return fragments;
    }
}
