package tpl.compiler.untyped;

import tpl.compiler.untyped.tree.UntypedStructure;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UntypedFragment
{
    private final Path path;
    private final List<UntypedStructure> structures;
    private final Set<Path> includes = new HashSet<>();

    public UntypedFragment(Path path, List<UntypedStructure> structures)
    {
        Set<String> includes = new HashSet<>();
        for (UntypedStructure structure : structures)
        {
            structure.collectIncludePaths(includes);
        }

        Path parent = path.getParent();

        for (String include : includes)
        {
            this.includes.add(parent.resolve(Path.of(include)).toAbsolutePath());
        }

        this.path = path;
        this.structures = structures;
    }

    public Set<Path> getIncludes()
    {
        return this.includes;
    }

    public Path getPath()
    {
        return this.path;
    }

    public List<UntypedStructure> getStructures()
    {
        return this.structures;
    }
}
