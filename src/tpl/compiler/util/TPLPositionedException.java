package tpl.compiler.util;

public class TPLPositionedException extends TPLException
{
    private final Position position;

    public TPLPositionedException(String msg, Position position)
    {
        super(position.line() + " : " + msg + " (file: " + position.file() + ")");
        this.position = position;
    }

    public Position getPosition()
    {
        return this.position;
    }
}
