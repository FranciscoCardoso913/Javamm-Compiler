import io;

class ArrayVarargs {

    public static void main(String[] args) {
        ArrayVarargs a;
        a = new ArrayVarargs();

        a.bar();
    }

    int foo(int... a) {
        return a[0];
    }

    int bar() {
        int res;

        res = this.foo(1, 2, 3);
        io.println(res);

        res = this.foo(4);
        io.println(res);

        return res;
    }

}