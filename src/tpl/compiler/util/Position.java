package tpl.compiler.util;

import java.nio.file.Path;

public record Position(Path file, int line, int offset, int length)
{
}
