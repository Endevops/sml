package be.endevops

import org.intellij.lang.annotations.Language
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class FaultError(
    @Language("xml")
    message: String
) : Throwable(message)

fun firstElementInSoapBody(xml: String): Element? {
    return runCatching {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(InputSource(StringReader(xml)))
        val nodeList = doc.getElementsByTagNameNS("*", "Body")
        if (nodeList != null && nodeList.length > 0) {
            val body = nodeList.item(0) as Element
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val n = childNodes.item(i)
                if (n is Element) {
                    return n
                }
            }
        }
        null
    }.getOrNull()
}

fun extractOperationNameFromSoap(xml: String): String? {
    return runCatching {
        val dbf = DocumentBuilderFactory.newInstance()
        dbf.isNamespaceAware = true
        val db = dbf.newDocumentBuilder()
        val doc = db.parse(InputSource(StringReader(xml)))
        val nodeList = doc.getElementsByTagNameNS("*", "Body")
        if (nodeList != null && nodeList.length > 0) {
            val body = nodeList.item(0) as Element
            val childNodes = body.childNodes
            for (i in 0 until childNodes.length) {
                val n = childNodes.item(i)
                if (n is Element) {
                    return n.localName ?: n.nodeName
                }
            }
        }
        null
    }.getOrNull()
}

fun extractOperationFromSoapAction(soapAction: String?): String? {
    if (soapAction == null) return null
    val idx = soapAction.lastIndexOf(':')
    if (idx >= 0 && idx < soapAction.length - 1) {
        return soapAction
            .substring(idx + 1)
            .replace("In", "")
            .replace("In\"", "")
            .trim()
    }
    return soapAction
}

fun nodeToString(node: Element): String {
    val sw = java.io.StringWriter()
    val transformer =
        javax.xml.transform.TransformerFactory
            .newInstance()
            .newTransformer()
    transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes")
    val src =
        javax.xml.transform.dom
            .DOMSource(node)
    val out =
        javax.xml.transform.stream
            .StreamResult(sw)
    transformer.transform(src, out)
    return sw.toString()
}

fun wrapInSoapEnvelope(innerXml: String): String =
    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
            "  <soap:Body>\n" +
            indentXml(innerXml) + "\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>"

fun indentXml(
    xml: String,
    spaces: Int = 4,
): String {
    val pad = " ".repeat(spaces)
    return xml.lines().joinToString("\n") { if (it.isBlank()) it else pad + it }
}

fun escapeXml(s: String): String =
    s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
