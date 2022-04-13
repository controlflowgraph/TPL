type StringBuilder (var int length, var char[] chars);

function StringBuilder append(const StringBuilder builder, const String str)
{
    ensureCapacity(builder, str.length);
    for(var int i = 0; i < str.length; i += 1)
    {
        builder.chars[i + builder.length] = str.chars[i];
    }
    builder.length += str.length;
    return builder;
}

function StringBuilder append(const StringBuilder builder, const char c)
{
    ensureCapacity(builder, 1);
    builder.chars[builder.length] = c;
    builder.length += 1;
    return builder;
}

function void ensureCapacity(const StringBuilder builder, const int required)
{
    if(builder.length + required >= builder.chars.length)
    {
        // rework this unsafe assumption
        const char[] resize = new char[](builder.chars.length * 2);
        for(var int i = 0; i < builder.chars.length; i += 1)
        {
            resize[i] = builder.chars[i];
        }
        builder.chars = resize;
    }
}

function String str(const StringBuilder builder)
{
    const char[] chars = new char[](builder.length);
    for(var int i = 0; i < builder.length; i += 1)
    {
        chars[i] = builder.chars[i];
    }
    return new String(chars.length, chars);
}