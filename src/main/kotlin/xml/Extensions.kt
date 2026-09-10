package be.endevops.xml

import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.dom.PlatformDocument
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.util.CompactFragment
import nl.adaptivity.xmlutil.xmlStreaming

@OptIn(ExperimentalXmlUtilApi::class)
fun PlatformNode.toCompactFragment(): CompactFragment =
    xmlStreaming.newReader(this).use { reader ->
        CompactFragment.deserialize(reader)
    }

/**
 * Convert an object to a correct native [org.w3c.dom.Document] using [XML] and kotlinx.serialization
 */
@OptIn(XmlUtilInternal::class, ExperimentalXmlUtilApi::class)
inline fun <reified T : Any> XML.toPlatformDocument(value: T): PlatformDocument {
    val root =
        xmlStreaming.genericDomImplementation
            .createDocument(null, null, null)

    return (
        DomWriter(root).use {
            it.indentString = ""
            this.encodeToWriter(it, value)
            it.target
        } as PlatformDocument
    )
}
