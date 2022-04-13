native function double random();

function double random(const double min, const double max)
{
    return random() * (max - min) + min;
}

function double random(const double max)
{
    return random(0.0, max);
}

function int random(const int min, const int max)
{
    return random(min to double, max to double) to int;
}

function int random(const int max)
{
    return random(0.0, max to double) to int;
}