package net.sergeych.raysearch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.sergeych.kotyara.db.DbJson
import java.nio.charset.Charset

enum class SupportedCharset(val charset: Charset) {
    UTF8(Charsets.UTF_8),
    ASCII(Charsets.ISO_8859_1)
}

/**
 * Doc definition allows us to store how the document should be treated
 * and filter by the document types whilch are all DocDef descendants
 * (could use in searches)
 */
@Serializable
@DbJson
sealed class DocDef {
    abstract val textExtractor: TextExtractor
    abstract val typeName: String

    @Serializable
    @DbJson
    @SerialName("txt")
    class TextDocument(val cs: SupportedCharset) : DocDef() {
        override val textExtractor: TextExtractor get() = PlainTextExtractor(cs)
        override val typeName: String = "plain text"
    }

    @Serializable
    @DbJson
    @SerialName("src")
    class ProgramSource(val cs: SupportedCharset) : DocDef() {
        override val textExtractor: TextExtractor get() = PlainTextExtractor(cs)
        override val typeName: String = "source program"
    }

    @Serializable
    @DbJson
    @SerialName("bad")
    object Invalid : DocDef() {
        override val textExtractor: TextExtractor
            get() = throw IllegalArgumentException("can't extract text on the invalid file")
        override val typeName = "invalid"
    }

    @Serializable
    @DbJson
    @SerialName("odt")
    object OdfDef : DocDef() {
        override val textExtractor: TextExtractor = OdtExtractor()
        override val typeName: String = "ODF text document"
    }
    @Serializable
    @DbJson
    @SerialName("ods")
    object OdsDef : DocDef() {
        override val textExtractor: TextExtractor = OdsExtractor()
        override val typeName: String = "ODF spreadsheet"
    }
    @Serializable
    @DbJson
    @SerialName("pdf")
    object PdfDef : DocDef() {
        override val textExtractor: TextExtractor = PdfExtractor()
        override val typeName: String = "PDF"
    }
}