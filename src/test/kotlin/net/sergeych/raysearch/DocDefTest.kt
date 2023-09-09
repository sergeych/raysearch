package net.sergeych.raysearch

import java.nio.file.Paths
import kotlin.test.Test


class classDocDefTest {
    @Test
    fun testOdT() {
        val txt = DocType.ODS.extractTextFrom(
            Paths.get("/home/sergeych/Documents/indigo/M1.ods")
        )
        println("------------- text is --- [${txt}]")
    }
}

//    @Test
//    fun testTikaFacade() {
//        val tika = Tika()
//        Paths.get("/home/sergeych/dev/raysearch/src/main/kotlin/net/sergeych/raysearch/database.kt")
//            .inputStream().use {
//                val text = tika.parseToString(it)
//                println(text)
//            }
//        Paths.get("/home/sergeych/Documents/indigo/M1.ods").inputStream().use {
//            println("ods:\n${tika.parseToString(it)}")
//        }
//        println(
//            "ods:\n${
//                tika.parseToString(
//                    Paths.get("/home/sergeych/Documents/indigo/SD 44k Black Water invoice 8Rays 2023 08 30.pdf")
//                )
//            }"
//        )
//        println(
//            "ods:\n${
//                tika.parseToString(
//                    Paths.get("/home/sergeych/Documents/indigo/SD 44k Black Water invoice 8Rays 2023 08 30.pdf")
//                )
//            }"
//        )
//        println(
//                "ods:\n${
//                    Tika().parseToString(
//                        Paths.get("/home/sergeych/Documents/8rays/5G-анализ.odt")
//                    )
//                }"
//            )
//        }
//        println(Tika().parseToString(Paths.get("/home/sergeych/Documents/glompertest.ods")))
//    }
//}


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
//                }
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
