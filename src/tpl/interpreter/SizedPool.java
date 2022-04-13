package tpl.interpreter;

import java.util.function.Function;

public class SizedPool <T>
{
    private final T[] arr;

    public<S> SizedPool(Pool<S> pool, Function<S, T> mapper, Function<Integer, T[]> creator)
    {
        this.arr = creator.apply(pool.size());
        for(int i = 0; i < pool.size(); i++)
        {
            this.arr[i] = mapper.apply(pool.get(i));
        }
    }

    public T get(int i)
    {
        return this.arr[i];
    }
}
