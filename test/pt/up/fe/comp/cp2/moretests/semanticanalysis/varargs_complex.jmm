import B;

class A {
    int foo(int... a) {
        return a[0];
    }

    int bar(int... b) {
        return this.foo(b);
    }

    int[] baz(int... c) {
        int[] hello;
        int world;
        hello = [1, 2, 3];
        world = this.bar(hello);
        world = this.foo([1, 2, 3]);
        world = this.foo(1, 2, 3);

        return [1, 2, 3];
    }
}
