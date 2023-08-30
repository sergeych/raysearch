package net.sergeych.raysearch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.sergeych.kotyara.db.DbJson

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
    object TextDocument : DocDef() {
        override val textExtractor: TextExtractor get() = PlainTextExtractor
        override val typeName: String = "plain text"
    }

    @Serializable
    @DbJson
    @SerialName("src")
    object ProgramSource : DocDef() {
        override val textExtractor: TextExtractor get() = PlainTextExtractor
        override val typeName: String = "source program"
    }
}