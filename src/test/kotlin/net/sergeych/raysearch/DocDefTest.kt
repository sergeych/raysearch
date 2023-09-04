package net.sergeych.raysearch

import java.nio.file.Paths
import kotlin.test.Test

class classDocDefTest {
    @Test
    fun testOdt() {
        val txt = OdtExtractor().extractTextFrom(
            Paths.get("/home/sergeych/Documents/testing/souper.odt")
        )
        println(txt)
    }

//    @Test
//    fun testPdf() {
//        val txt = PdfExtractor().extractTextFrom(
//            Paths.get("/home/sergeych/Documents/indigo/SD 44k Black Water invoice 8Rays 2023 08 30.pdf")
//        )
//        println(txt)
//    }

//    @Test
//    fun testRuntime() {
//        val x = Runtime.getRuntime().exec(
//           arrayOf("gio","open", "/home/sergeych/Downloads/8rays software contract template.odt")
//        )
//        println(x.errorReader().readLine())
//    }
}
//    fun testSerialization() {
//        val dd1 = DocDef.ProgramSource as DocDef
//        val s = Json.encodeToString(dd1)
//        val dd2 = Json.decodeFromString<DocDef>(s)
//        assertIs<DocDef.ProgramSource>(dd2)
//    }
//}


///
/// glomper
///
