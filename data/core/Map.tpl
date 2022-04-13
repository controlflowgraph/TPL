type <T, V> Map (var int size, var T[] keys, var V[] values, const V default);

function <T, V> boolean containsKey(var Map<T, V> map, var T key)
{
    for(var int i = 0; i < map.size; i += 1)
    {
        if(equal(map.keys[i], key)) return true;
    }
    return false;
}


function <T, V> void put(var Map<T, V> map, var T key, var V value)
{
    const boolean notSet = setIfKeyExists(map, key, value);
    if(notSet)
    {
        const int size = map.size;

        if(size == map.keys.length)
        {
            const T[] keys = new T[](map.size * 2);
            const V[] values = new V[](map.size * 2);
            const T[] ks = map.keys;
            const V[] vs = map.values;
            for(var int i = 0; i < ks.length; i += 1)
            {
                keys[i] = ks[i];
                values[i] = vs[i];
            }
            map.keys = keys;
            map.values = values;
        }
        map.keys[size] = key;
        map.values[size] = value;
        map.size += 1;
    }
}

function <T, V> boolean setIfKeyExists(const Map<T, V> map, const T key, const V value)
{
    const int size = map.size;
    for(var int i = 0; i < size; i += 1)
    {
        if(equal(map.keys[i], key))
        {
            map.values[i] = value;
            return false;
        }
    }
    return true;
}

function <T, V> V get(const Map<T, V> map, const T key)
{
    for(var int i = 0; i < map.size; i += 1)
    {
        if(equal(map.keys[i], key))
        {
            return map.values[i];
        }
    }
    return map.default;
}