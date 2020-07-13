interface A {
    fun foo(a: String = "OK"): String
}

interface A2 : A

interface B {
    fun foo(a: String = "Fail"): String
}

class Impl : A2, B {
    override fun foo(a: String) = a
}

fun box(): String = Impl().foo()