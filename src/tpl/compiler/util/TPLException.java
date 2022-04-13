package tpl.compiler.util;

public class TPLException extends RuntimeException
{
    private final String msg;

    public TPLException(String msg)
    {
        this.msg = "Error: " + msg;
    }

    @Override
    public String getMessage()
    {
        return this.msg;
    }
}
