package tpl.compiler.untyped.tree.segment;

import tpl.compiler.type.tree.ArrayType;
import tpl.compiler.type.tree.GenericType;
import tpl.compiler.type.tree.SingleType;
import tpl.compiler.type.tree.Type;
import tpl.compiler.typed.TypeEnvironment;
import tpl.compiler.typed.tree.segment.*;
import tpl.compiler.util.Position;
import tpl.compiler.util.TPLPositionedException;

import java.util.List;

public class UntypedIndex extends UntypedSegment
{
    private final UntypedSegment source;
    private final UntypedSegment key;

    public UntypedIndex(Position position, UntypedSegment source, UntypedSegment key)
    {
        super(position);
        this.source = source;
        this.key = key;
    }

    private static final Type INTEGER_TYPE = new SingleType(null, "int");

    @Override
    public TypedSegment checkType(TypeEnvironment env)
    {
        TypedSegment source = this.source.checkType(env);
        Type type = source.getType().substitute(env.getCurrentSubstituteMapping());
        TypedSegment key = this.key.checkType(env);

        if(!INTEGER_TYPE.matches(key.getType()))
            throw new TPLPositionedException("Expected type " + INTEGER_TYPE + " as key but got " + key.getType() + "!", getPosition());

        if(type.getName().equals("Array"))
        {
            return new TypedIndex(((ArrayType) type).getContentType(), source, key);
        }
        else if(type.getName().equals("Tuple"))
        {
            if(!(key instanceof TypedValue v))
                throw new TPLPositionedException("Expected constant value in tuple index!", getPosition());
            GenericType generic = (GenericType) type;
            List<Type> generics = generic.getGenerics();
            int val = Integer.parseInt(v.getValue());
            if(val < 0 || generics.size() <= val)
                throw new TPLPositionedException("Expected key between 0 and " + generics.size() + "!", getPosition());
            return new TypedSelection(generics.get(val), source, "_" + val);//(generics.get(val), source, key);
        }
        else
            throw new TPLPositionedException("Unable to index non array / tuple types (" + source.getType() + ")", getPosition());
    }

    @Override
    public TypedSegment checkAssignment(TypeEnvironment env, TypedSegment value)
    {
        TypedSegment source = this.source.checkType(env);
        Type type = source.getType().substitute(env.getCurrentSubstituteMapping());
        TypedSegment key = this.key.checkType(env);

        if(type.getName().equals("Tuple"))
        {
            if(!(key instanceof TypedValue v))
                throw new TPLPositionedException("Expected constant value in tuple index!", getPosition());

            GenericType generic = (GenericType) type;
            List<Type> generics = generic.getGenerics();
            int val = Integer.parseInt(v.getValue());

            if(val < 0 || generics.size() <= val)
                throw new TPLPositionedException("Expected key between 0 and " + generics.size() + "!", getPosition());

            return new TypedSelectionAssignment(generics.get(val), source, "_" + val, value);
        }
        else if(type.getName().equals("Array"))
        {
            if(!type.getName().equals("Array"))
                throw new TPLPositionedException("Unable to index non array types (" + source.getType() + ")", getPosition());

            if(!INTEGER_TYPE.matches(key.getType()))
                throw new TPLPositionedException("Expected type " + INTEGER_TYPE + " as key but got " + key.getType() + "!", getPosition());

            return new TypedIndexAssignment(((ArrayType) type).getContentType(), source, key, value);
        }
        else
        {
            throw new TPLPositionedException("Unable to index non array / tuple types (" + source.getType() + ")", getPosition());
        }
    }
}
