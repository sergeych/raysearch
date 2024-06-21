package net.sergeych.raysearch

import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.debug
import net.sergeych.mp_logger.info
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.tika.Tika
import org.odftoolkit.odfdom.doc.OdfDocument
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.incubator.doc.text.OdfEditableTextExtractor
import java.io.BufferedInputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.readText

enum class DocType(val extractTextFrom: (Path) -> String) {
    @Suppress("unused")
    Unknown({ "" }),
    ISO8859_1({ it.readText(Charsets.ISO_8859_1) }),
    UTF8({ it.readText(Charsets.UTF_8) }),
    PDF({ extractFromPdf(it) }),
    ODT({ extractFromOdt(it) }),
    ODS({ extractFromOds(it) }),
    Other({extractWithTika(it)})
    ;

    companion object : LogTag("DocType") {
        fun detectPlainText(x: Path): DocType? = when {
            isUTF8(x) -> UTF8
            isAscii(x) -> ISO8859_1
            else -> {
                info { "can't detect encoding of the text file: $x" }
                null
            }
        }
    }
}

fun extractFromOdt(file: Path): String {
    val odt: OdfDocument = OdfTextDocument.loadDocument(file.pathString)
    val text = OdfEditableTextExtractor.newOdfEditableTextExtractor(odt).text.replace('\r', '\n')
    return text
}

fun extractFromOds(file: Path): String {
    // Odfspreadsheet package is a little too old and do not extract some
    // data, so back to Tika:
    return extractWithTika(file)
//    val ods = OdfSpreadsheetDocument.loadDocument(file.toFile())
//    val text = OdfEditableTextExtractor.newOdfEditableTextExtractor(ods)
//        .text.replace('\r', '\n')
//    return text
}

fun extractFromPdf(file: Path): String {
    val parser = PDFParser(RandomAccessBufferedFileInputStream(file.toFile()))
    parser.parse()
    parser.getDocument().use { doc ->
        val stripper = PDFTextStripper()
        val pdDoc = PDDocument(doc)
        return stripper.getText(pdDoc)
    }
}

fun isUTF8(file: Path): Boolean {
    // todo: reimplement with sequential file access
    val pText = file.readBytes()
    var expectedLength: Long
    var i = 0
    while (i < pText.size) {
        expectedLength = if (pText[i].toInt() and 128 == 0) {
            1
        } else if (pText[i].toInt() and 224 == 192) {
            2
        } else if (pText[i].toInt() and 240 == 224) {
            3
        } else if (pText[i].toInt() and 248 == 240) {
            4
        } else if (pText[i].toInt() and 252 == 248) {
            5
        } else if (pText[i].toInt() and 254 == 252) {
            6
        } else {
            return false
        }
        while (--expectedLength > 0) {
            if (++i >= pText.size) {
                return false
            }
            if (pText[i].toInt() and 192 != 128) {
                return false
            }
        }
        i++
    }
    return true
}

val log = LogTag("TENC")
fun isAscii(file: Path): Boolean {
    BufferedInputStream(file.inputStream()).use { ins ->
        for (b in ins) {
            when (b.toUByte().toInt()) {
                9, 10, 12, 13 -> continue // spaces: HT, LF, FF, CR
                in 32..126, in 145..156, in 158..255 -> continue
                else -> {
                    log.debug { "invalid latin-1 character code ${b.toUByte().toString(16)}" }
                    return false
                }
            }
        }
        return true
    }
}

fun extractWithTika(file: Path): String = Tika().parseToString(file)


