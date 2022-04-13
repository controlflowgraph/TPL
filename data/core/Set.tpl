type <T> Set(var int size, var T[] elements);

function <T> void add(const Set<T> set, const T element)
{
    if(contains(set, element))
    {
        return;
    }

    if(set.size == set.elements.length)
    {
        const T[] elements = new T[](set.size * 2);
        for(var int i = 0; i < set.size; i += 1)
        {
            elements[i] = set.elements[i];
        }
        set.elements = elements;
    }
    set.elements[set.size] = element;
    set.size += 1;
}

function <T> boolean contains(const Set<T> set, const T element)
{
    for(var int i = 0; i < set.size; i += 1)
    {
        if(equal(set.elements[i], element))
        {
            return true;
        }
    }
    return false;
}

function <T> String str(const Set<T> set)
{
    if(set == null) return "null";

    const int le = set.size;
    const String[] strs = new String[](le);

    const String delimiter = ", ";
    var int length = 0;
    for(var int i = 0; i < le; i += 1)
    {
        const String s = str(set.elements[i]);
        strs[i] = s;
        length += s.length;
    }
    length += le * 2 + 2;

    const char[] chars = new char[](length);
    chars[0] = '{';
    chars[1] = ' ';
    var int pos = 2;
    var int seg = 0;
    while(seg < le)
    {
        const char[] cs = strs[seg].chars;
        const int len = cs.length;
        for(var int i = 0; i < len; i += 1)
        {
            chars[pos] = cs[i];
            pos += 1;
        }
        chars[pos] = ',';
        chars[pos + 1] = ' ';
        pos += 2;
        seg += 1;
    }

    chars[pos - 2] = ' ';
    chars[pos - 1] = '}';

    return new String(length, chars);
}

function <T> boolean equal(const Set<T> s1, const Set<T> s2)
{
    if(s1 == s2) return true;
    if(s1 == null) return false;
    if(s2 == null) return false;
    if(s1.size != s2.size) return false;
    for(var int i = 0; i < s1.size; i += 1)
    {
        if(!contains(s2, s1.elements[i])) return false;
    }

    for(var int i = 0; i < s2.size; i += 1)
    {
        if(!contains(s1, s2.elements[i])) return false;
    }
    return true;
}