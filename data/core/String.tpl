type String (const int length, const char[] chars);

function String con(const String a, const String b)
{
    const char[] arr = new char[](a.length + b.length);
    for(var int i = 0; i < a.length; i += 1)
    {
        arr[i] = a.chars[i];
    }
    for(var int i = 0; i < b.length; i += 1)
    {
        arr[i + a.length] = b.chars[i];
    }
    return new String(arr.length, arr);
}

function boolean equal(const String a, const String b)
{
    if(a == b) return true;
    if(a == null) return false;
    if(b == null) return false;
    if(a.length != b.length) return false;
    for(var int i = 0; i < a.length; i += 1)
    {
        if(a.chars[i] != b.chars[i]) return false;
    }
    return true;
}

function String sub(const String s, const int start)
{
    return sub(s, start, s.length);
}

function String sub(const String s, const int start, const int end)
{
    const char[] chars = new char[](end - start);
    for(var int i = 0; i < chars.length; i += 1)
    {
        chars[i] = s.chars[start + i];
    }
    return new String(chars.length, chars);
}

native function void print(const String str);

function String str(const String s) { return s; }
native function String str(const boolean v);
native function String str(const char v);
native function String str(const byte v);
native function String str(const short v);
native function String str(const int v);
native function String str(const long v);
native function String str(const float v);
native function String str(const double v);