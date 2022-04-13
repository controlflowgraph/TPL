type <T> List (var int length, var T[] elements);

function <T> void add(const List<T> list, const T element)
{
    // relocate if too small
    const int size = list.elements.length;
    if(list.length == size)
    {
        const T[] arr = new T[](size * 2);
        for(var int i = 0; i < size; i += 1)
        {
            arr[i] = list.elements[i];
        }
        list.elements = arr;
    }
    list.elements[list.length] = element;
    list.length += 1;
}

function <T> void remove(const List<T> list, const int index)
{
    for(var int i = index; i < list.length - 1; i += 1)
    {
        list.elements[i] = list.elements[i + 1];
    }
    list.length -= 1;
}

function <T> String str1(const List<T> list)
{
    var String acc = "[ ";
    const String delimiter = ", ";
    for(var int i = 0; i < list.length; i += 1)
    {
        acc = con(acc, str(list.elements[i]));
        if(i + 1 < list.length)
        {
            acc = con(acc, delimiter);
        }
    }

    return con(acc, " ]");
}

function <T> String str2(const List<T> list)
{
    const StringBuilder builder = new StringBuilder(0, new char[](16));
    append(builder, "[ ");
    const String delimiter = ", ";
    for(var int i = 0; i < list.length; i += 1)
    {
        append(builder, str(list.elements[i]));
        if(i + 1 < list.length)
        {
            append(builder, delimiter);
        }
    }
    append(builder, " ]");

    return str(builder);
}

function <T> String str(const List<T> list)
{
    const int le = list.length;
    const String[] strings = new String[](le);

    const String delimiter = ", ";
    var int length = 0;
    for(var int i = 0; i < le; i += 1)
    {
        const String s = str(list.elements[i]);
        strings[i] = s;
        length += s.length;
    }
    length += le * 2 + 2;

    const char[] chars = new char[](length);
    chars[0] = '[';
    chars[1] = ' ';
    var int pos = 2;
    var int seg = 0;
    while(seg < le)
    {
        const char[] cs = strings[seg].chars;
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
    chars[pos - 1] = ']';

    return new String(length, chars);
}

function <T> boolean equal(const List<T> l1, const List<T> l2)
{
    if(l1 == l2) return true;
    if(l1 == null) return false;
    if(l2 == null) return false;
    if(l1.length != l2.length) return false;
    for(var int i = 0; i < l1.length; i += 1)
    {
        if(!equal(l1.elements[i], l2.elements[i])) return false;
    }
    return true;
}