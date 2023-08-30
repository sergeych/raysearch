package net.sergeych.raysearch

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertIs

class DocDefTest {
    @Test
    fun testSerialization() {
        val dd1 = DocDef.ProgramSource as DocDef
        val s = Json.encodeToString(dd1)
        println(s)
        val dd2 = Json.decodeFromString<DocDef>(s)
        println(dd2)
        assertIs<DocDef.ProgramSource>(dd2)
    }
}