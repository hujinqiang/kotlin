open class SuperBase

open class Base : SuperBase() {

}

open class Derived : Base() {
    protected fun <T : Base> T.transformChildren() {

    }

    fun genericTest(derived: Derived) {
        derived.transformChildren()
    }

}


fun test() {
}

// JVM_TEMPLATES:
// 0 CHECKCAST

// JVM_IR_TEMPLATES:
// 0 CHECKCAST
