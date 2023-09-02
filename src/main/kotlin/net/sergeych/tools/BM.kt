package net.sergeych.tools

@Suppress("unused")
object BM {
    operator fun invoke(f:()->Unit): Long {
        val x = System.currentTimeMillis()
        f()
        val t = System.currentTimeMillis() - x
        println("executed time: $t ms")
        return t
    }
}