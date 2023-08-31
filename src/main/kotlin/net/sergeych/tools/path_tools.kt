package net.sergeych.tools

import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun Path.stringFromHome():String {
    val home = System.getProperty("user.home")
    val x = this.absolutePathString()
    return if (x.startsWith(home))
        "~/" + x.substring(home.length)
    else x
}