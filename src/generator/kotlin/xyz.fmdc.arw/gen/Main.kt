package xyz.fmdc.arw.gen

import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val base = File(args[0])
        Vec2XGen.generates(base)
    }
}
