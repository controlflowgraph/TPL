include "core/Include.tpl";

const String m1 = "Hello  ";
const String m2 = "World!";
const String msg = con(m1, m2);
print(msg);

var double sum = 0.0;
for(var int i = 0; i < 1; i += 1)
{
    print(str(random(20)));
}
print(str(sum));

var int an = 10.0 to int;
print(str(an));

print(str('A'));

const List<double> numbers = new List<>(0, new double[](16));

for(var int i = 0; i < 100; i += 1)
{
    add(numbers, random());
}
print(str(numbers));

const List<int> a = new List<>(0, new int[](10));
add(a, 10);
const List<int> b = new List<>(0, new int[](10));
add(b, 10);
print(str(null));
print(str(equal(a, b)));

const Map<String, int> map = new Map<>(0, new String[](10), new int[](10), -1);

for(var int i = 0; i < 1000; i += 1)
{
    put(map, str(i), i);
}

const StringBuilder builder = new StringBuilder(0, new char[](20));
append(builder, "Hello ");
append(builder, "World!");
print(str(builder));

const Set<String> set1 = new Set<>(0, new String[](2));
for(var int i = 0; i < 10; i += 1)
{
    add(set1, str(i));
}

print(str(contains(set1, "9")));