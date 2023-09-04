package net.sergeych.raysearch

import net.sergeych.mp_logger.LogTag
import net.sergeych.mp_logger.debug
import net.sergeych.mp_logger.info
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.odftoolkit.odfdom.doc.OdfDocument
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument
import org.odftoolkit.odfdom.doc.OdfTextDocument
import org.odftoolkit.odfdom.incubator.doc.text.OdfEditableTextExtractor
import java.io.BufferedInputStream
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.readText


interface TextExtractor {
    val name: String
    fun extractTextFrom(file: Path): String

    /**
     * Check that the file appers to be valid for the rule
     */
    fun isValid(path: Path): Boolean
}

/**
 * USe the object to not allocate many instances of the text extractor
 */
class PlainTextExtractor(val cs: SupportedCharset) : TextExtractor, LogTag("PTEX") {
    override val name = "plain text"
    override fun extractTextFrom(file: Path): String = file.readText(cs.charset)

    /**
     * if the encoding was detected (and the instance would not be created otherwise)
     * the file is ok.
     */
    override fun isValid(path: Path): Boolean {
        return true
    }

    companion object : LogTag("PTEX") {

        fun detectCharset(file: Path): SupportedCharset? = when {
            isUTF8(file) -> SupportedCharset.UTF8
            isAscii(file) -> SupportedCharset.ASCII
            else -> {
                debug { "can't detect plain text: $file" }
                null
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

        fun isAscii(file: Path): Boolean {
            BufferedInputStream(file.inputStream()).use { ins ->
                for (b in ins) {
                    when (b.toUByte().toInt()) {
                        9, 10, 12, 13 -> continue // spaces: HT, LF, FF, CR
                        in 32..126, in 145..156, in 158..255 -> continue
                        else -> {
                            debug { "invalid latin-1 character code ${b.toUByte().toString(16)}" }
                            return false
                        }
                    }
                }
                return true
            }
        }
    }
}

class OdtExtractor : TextExtractor, LogTag("ODFX") {
    override val name: String
        get() = "ODF text document"

    override fun extractTextFrom(file: Path): String {
        val odt: OdfDocument = OdfTextDocument.loadDocument(file.pathString)
        val text = OdfEditableTextExtractor.newOdfEditableTextExtractor(odt).text.replace('\r', '\n')
        return text
    }

    override fun isValid(path: Path): Boolean = true

}

class OdsExtractor : TextExtractor, LogTag("ODSX") {
    override val name: String
        get() = "ODF spreadsheet"

    override fun extractTextFrom(file: Path): String {
        val ods = OdfSpreadsheetDocument.loadDocument(file.toFile())
        val text = OdfEditableTextExtractor.newOdfEditableTextExtractor(ods)
            .text.replace('\r', '\n')
        return text
    }

    override fun isValid(path: Path): Boolean = true

}

class PdfExtractor : TextExtractor, LogTag("PDFX") {
    override val name: String
        get() = "PDF"

    override fun extractTextFrom(file: Path): String {
        val parser = PDFParser(RandomAccessBufferedFileInputStream(file.toFile()))
        parser.parse()
        parser.getDocument().use { doc ->
            val stripper = PDFTextStripper()
            val pdDoc = PDDocument(doc)
            return stripper.getText(pdDoc).also { info { "extracted pdf: $file\n$it" }}
        }
    }

    override fun isValid(path: Path): Boolean = true
}

