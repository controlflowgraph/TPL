package tpl.interpreter;

import java.util.Map;

public class OptimizedProgram
{
    private final Map<Integer, OptimizedFragment> procedures;
    private final OptimizedFragment base;
    private final Pool<String> strings;
    private final Pool<Integer> integers;
    private final Pool<Double> doubles;
    private final Pool<Boolean> booleans;
    private final Pool<Character> characters;
    private final Pool<Object> nulls;

    public OptimizedProgram(Map<Integer, OptimizedFragment> procedures, OptimizedFragment base, Pool<String> strings, Pool<Integer> integers, Pool<Double> doubles, Pool<Boolean> booleans, Pool<Character> characters, Pool<Object> nulls)
    {
        this.procedures = procedures;
        this.base = base;
        this.strings = strings;
        this.integers = integers;
        this.doubles = doubles;
        this.booleans = booleans;
        this.characters = characters;
        this.nulls = nulls;
    }

    public Map<Integer, OptimizedFragment> getProcedures()
    {
        return this.procedures;
    }

    public OptimizedFragment getBase()
    {
        return this.base;
    }

    public Pool<String> getStrings()
    {
        return this.strings;
    }

    public Pool<Integer> getIntegers()
    {
        return this.integers;
    }

    public Pool<Double> getDoubles()
    {
        return this.doubles;
    }

    public Pool<Boolean> getBooleans()
    {
        return this.booleans;
    }

    public Pool<Character> getCharacters()
    {
        return this.characters;
    }

    public OptimizedFragment getFragment(int i)
    {
        return this.procedures.get(i);
    }

    public Pool<Object> getNulls()
    {
        return this.nulls;
    }
}
