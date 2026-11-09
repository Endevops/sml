package be.endevops.xml

import nl.adaptivity.xmlutil.NamespaceContext
import nl.adaptivity.xmlutil.XMLConstants
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.XmlEvent
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.core.impl.multiplatform.assert
import nl.adaptivity.xmlutil.core.impl.toIndentSequence
import nl.adaptivity.xmlutil.core.impl.toIndentString
import nl.adaptivity.xmlutil.dom.NodeConsts
import nl.adaptivity.xmlutil.dom.PlatformNode
import nl.adaptivity.xmlutil.dom.adoptNode
import nl.adaptivity.xmlutil.dom2.Attr
import nl.adaptivity.xmlutil.dom2.Document
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.NamedNodeMap
import nl.adaptivity.xmlutil.dom2.Node
import nl.adaptivity.xmlutil.dom2.attributes
import nl.adaptivity.xmlutil.dom2.childNodes
import nl.adaptivity.xmlutil.dom2.createElementNS
import nl.adaptivity.xmlutil.dom2.documentElement
import nl.adaptivity.xmlutil.dom2.implementation
import nl.adaptivity.xmlutil.dom2.nodeType
import nl.adaptivity.xmlutil.dom2.ownerDocument
import nl.adaptivity.xmlutil.dom2.parentNode
import nl.adaptivity.xmlutil.qname
import nl.adaptivity.xmlutil.util.impl.createDocument

/** Implement forEach with a built-in cast to attr. */
internal inline fun NamedNodeMap<Attr>.forEachAttr(body: (Attr) -> Unit) {
    val l = size
    for (idx in 0 until l) {
        body(item(idx) as Attr)
    }
}

/** A filter function on a [NamedNodeMap] that returns a list of all
 * (attributes)[Attr] that meet the [predicate].
 */
internal inline fun NamedNodeMap<Attr>.filterTyped(predicate: (Attr) -> Boolean): List<Attr> {
    val result = mutableListOf<Attr>()
    forEachAttr { attr ->
        if (predicate(attr)) result.add(attr)
    }
    return result
}

internal fun Node.myLookupPrefix(namespaceUri: String): String? = (this as? Element)?.myLookupPrefixImpl(namespaceUri, mutableSetOf())

private fun Element.myLookupPrefixImpl(
    namespaceUri: String,
    seenPrefixes: MutableSet<String>,
): String? {
    getAttributes().forEachAttr { attr ->
        when {
            attr.getPrefix() == XMLConstants.XMLNS_ATTRIBUTE -> {
                if (attr.getValue() == namespaceUri && attr.getLocalName() !in seenPrefixes) {
                    return attr.getLocalName()
                } else {
                    seenPrefixes.add(attr.getLocalName() ?: attr.getName())
                }
            }

            attr
                .getPrefix()
                .isNullOrBlank() && attr.getLocalName() == XMLConstants.XMLNS_ATTRIBUTE -> {
                if (attr.getValue() == namespaceUri && attr.getLocalName() !in seenPrefixes) {
                    return ""
                } else {
                    seenPrefixes.add("")
                }
            }
        }
    }
    return (getParentNode() as? Element)?.myLookupPrefixImpl(namespaceUri, seenPrefixes)
}

internal fun Node.myLookupNamespaceURI(prefix: String): String? =
    when (this) {
        is Element -> {
            getAttributes()
                .filterTyped {
                    (prefix == "" && it.getLocalName() == "xmlns") ||
                        (it.getPrefix() == "xmlns" && it.getLocalName() == prefix)
                }.firstOrNull()
                ?.getValue() ?: getParentNode()?.myLookupNamespaceURI(prefix)
        }

        else -> {
            null
        }
    }

/**
 * Writer that uses the DOM for the underlying storage (rather than writing to some string).
 * @property isAppend Should the writing append new children (`true`) or replace the existing
 *         children (`false`)
 * @property xmlDeclMode Determine whether an XML declaration is written
 */
@OptIn(XmlUtilInternal::class)
class DomWriter(
    current: Node?,
    val isAppend: Boolean = false,
    val xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
    xmlVersion: XmlVersion? = null,
    dummy: Boolean = false,
) : XmlWriter {
    /**
     * The sequence of events to use for indentation
     */
    var indentSequence: List<XmlEvent.TextEvent> = emptyList()

    /**
     * Shortcut string to use for indentation. This uses [indentSequence] as storage.
     */
    @OptIn(XmlUtilInternal::class)
    override var indentString: String
        get() = indentSequence.toIndentString()
        set(value) {
            indentSequence = value.toIndentSequence()
        }

    private var docDelegate: Document?

    /** The document that is used to create nodes */
    @XmlUtilInternal
    val target: Document get() = docDelegate ?: throw XmlException("Document not created yet")

    /**
     * The requested version of the document. This is set through [startDocument] and `null` if left default.
     */
    var requestedVersion: String? = xmlVersion?.versionString
        private set

    /**
     * The requested encoding for the document. This is set through [startDocument] and `null` if left default.
     */
    var requestedEncoding: String? = null
        private set

    /**
     * Whether this document is requested to be standalone. This is set through [startDocument] and `null` if left default.
     */
    var requestedStandalone: Boolean? = null
        private set

    /** The current node that events would need to be written to */
    @XmlUtilInternal
    var currentNode: Node?
        private set

    private val pendingOperations: List<(Document) -> Unit> = mutableListOf()

    private var lastTagDepth = TAG_DEPTH_NOT_TAG

    //    @Suppress("OverridingDeprecatedMember")
    override val namespaceContext: NamespaceContext =
        object : NamespaceContext {
            override fun getNamespaceURI(prefix: String): String? = currentNode?.lookupNamespaceURI(prefix)

            override fun getPrefix(namespaceURI: String): String? = currentNode?.lookupPrefix(namespaceURI)

            private fun Element.collectDeclaredPrefixes(
                namespaceUri: String,
                result: MutableSet<String>,
                redeclared: MutableCollection<String>,
            ) {
                for (attr in attributes) {
                    val prefix =
                        when {
                            attr.getPrefix() == "xmlns" -> attr.getLocalName()

                            attr
                                .getPrefix()
                                .isNullOrEmpty() && attr.getLocalName() == "xmlns" -> ""

                            else -> null
                        }
                    if (prefix != null) {
                        if (prefix in redeclared) {
                            if (attr.getValue() == namespaceUri) result.add(prefix)
                            redeclared.add(prefix)
                        }
                    }
                }
                getParentElement()?.collectDeclaredPrefixes(namespaceUri, result, redeclared)
            }

            override fun getPrefixes(namespaceURI: String): Iterator<String> =
                buildSet {
                    (currentNode as Element?)?.collectDeclaredPrefixes(namespaceURI, this, mutableListOf())
                }.toList().iterator()
        }

    override var depth: Int = 0
        private set

    private val requireCurrent get() = (currentNode ?: throw IllegalStateException("No current element")) as Element

    init {
        when (current) {
            null -> {
                docDelegate = null
                currentNode = null
            }

            is Document -> {
                docDelegate = current
                currentNode = current
            }

            else -> {
                docDelegate = current.ownerDocument
                currentNode = current
            }
        }
    }

    /**
     * Create a new writer.
     * @param current The node that will receive the new nodes as children
     * @param isAppend Should the writing append new children (`true`) or replace the existing
     *   children (`false`)
     * @param xmlDeclMode Determine whether an XML declaration is written
     */
    constructor(
        current: PlatformNode,
        isAppend: Boolean = false,
        xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired,
        xmlVersion: XmlVersion = XmlVersion.XML10,
    ) : this(
        current as? Node ?: createDocument(
            nl.adaptivity.xmlutil
                .QName("x"),
        ).adoptNode(current),
        isAppend,
        xmlDeclMode,
        xmlVersion,
        false,
    )

    /**
     * Create a new writer.
     * @param xmlDeclMode Determine whether an XML declaration is written
     */
    constructor(xmlDeclMode: XmlDeclMode = XmlDeclMode.IfRequired) : this(null, xmlDeclMode = xmlDeclMode)

    private fun writeIndent(newDepth: Int = depth) {
        val indentSeq = indentSequence
        if (lastTagDepth >= 0 && indentSeq.isNotEmpty() && lastTagDepth != depth) {
            ignorableWhitespace("\n")
            try {
                indentSequence = emptyList()
                repeat(depth) { indentSeq.forEach { it.writeTo(this) } }
            } finally {
                indentSequence = indentSeq
            }
        }
        lastTagDepth = newDepth
    }

    private fun addToPending(operation: (Document) -> Unit) {
        if (docDelegate == null) {
            (pendingOperations as MutableList).add(operation)
        } else {
            error("Use of pending list when there is a document already")
        }
    }

    private fun requireCurrent(error: String) = currentNode as? Element ?: throw XmlException("The current node is not an element: $error")

    override fun namespaceAttr(
        namespacePrefix: String,
        namespaceUri: String,
    ) {
        val cur = requireCurrent("Namespace attribute")
        when {
            namespacePrefix.isEmpty() -> {
                // Also ignore setting the namespace to empty if it is set.
                if (!(namespaceUri.isEmpty() && (cur.lookupNamespaceURI("") == ""))) {
                    cur.setAttributeNS(
                        XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                        XMLConstants.XMLNS_ATTRIBUTE,
                        namespaceUri,
                    )
                }
            }

            else -> {
                cur.setAttributeNS(
                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                    "${XMLConstants.XMLNS_ATTRIBUTE}:$namespacePrefix",
                    namespaceUri,
                )
            }
        }
    }

    override fun startTag(
        namespace: String?,
        localName: String,
        prefix: String?,
    ) {
        writeIndent()
        depth++
        when {
            currentNode == null && docDelegate == null -> {
                val doc =
                    createDocument(
                        qname(namespace ?: "", localName, prefix),
                    )
                docDelegate = doc
                currentNode = docDelegate

                val e = doc.documentElement!!
                doc.removeChild(e) // remove to allow for pending operations

                for (pending in pendingOperations) {
                    pending(doc)
                }
                doc.appendChild(e)

                (pendingOperations as MutableList).clear()
                lastTagDepth = 0
                currentNode = doc.documentElement
                return
            }

            currentNode == null && !isAppend -> {
                if (target.childNodes
                        .iterator()
                        .asSequence()
                        .count { it.nodeType == NodeConsts.ELEMENT_NODE } > 0
                ) {
                    for (e in target.childNodes
                        .filterIsInstance<Element>()) { // use filter/list to have temporary list
                        target.removeChild(e)
                    }
                }
            }
        }

        target.createElementNS(qname(namespace, localName, prefix)).let { elem: Element ->
            currentNode!!.appendChild(elem)
            currentNode = elem
        }
    }

    override fun comment(text: String) {
        writeIndent()
        val ce = currentNode
        if (ce == null) {
            addToPending { comment(text) }
        } else {
            target.createComment(text).let { comment ->
                ce.appendChild(comment)
            }
        }
    }

    override fun text(text: String) {
        lastTagDepth = TAG_DEPTH_NOT_TAG
        val ce = currentNode
        if (ce == null) {
            if (text.isBlank()) addToPending { ignorableWhitespace(text) } else throw XmlException("Not in an element -- text")
        } else {
            target.createTextNode(text).let { textNode ->
                ce.appendChild(textNode)
            }
        }
    }

    override fun cdsect(text: String) {
        lastTagDepth = TAG_DEPTH_NOT_TAG
        target.createCDATASection(text).let { cdataSection ->
            currentNode?.appendChild(cdataSection) ?: throw XmlException("Not in an element -- cdsect")
        }
    }

    override fun entityRef(text: String) {
        lastTagDepth = TAG_DEPTH_NOT_TAG
        when (text) { // handle the built in entities as regular text (which will escape where needed)
            "amp" -> {
                text("&")
            }

            "quot" -> {
                text("\"")
            }

            "lt" -> {
                text("<")
            }

            "gt" -> {
                text(">")
            }

            "apos" -> {
                text("'")
            }

            else -> {
                val a = text.toIntOrNull(10)
                if (a != null) {
                    text("${Char(a)}")
                } else {
                    throw UnsupportedOperationException(
                        "Creating entity references is not supported (or incorrect) in most browsers: $text",
                    )
                }
            }
        }
    }

    override fun processingInstruction(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        if (currentNode?.nodeType != NodeConsts.ELEMENT_NODE) throw XmlException("Document already started")
        if (docDelegate == null) {
            addToPending { processingInstruction(text) }
        } else {
            val split = text.indexOf(' ')
            val (target, data) =
                when {
                    split < 0 -> text to ""
                    else -> text.substring(0, split) to text.substring(split + 1)
                }
            this.target.createProcessingInstruction(target, data).let { processInstr ->
                this.target
                    .appendChild(processInstr)
            }
        }
    }

    override fun processingInstruction(
        target: String,
        data: String,
    ) {
        val ce = currentNode
//        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        if (ce == null) {
            addToPending { processingInstruction(target, data) }
        } else {
            val processInstr =
                this.target
                    .createProcessingInstruction(target, data)

            ce.appendChild(processInstr)
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun ignorableWhitespace(text: String) {
        val ce = currentNode
        if (ce == null) {
            addToPending { ignorableWhitespace(text) }
        } else if (ce.nodeType != NodeConsts.DOCUMENT_NODE) { // There is no way to specify whitespace on a document element
            target.createTextNode(text).let { textNode ->
                ce.appendChild(textNode)
            }
        }
        lastTagDepth = TAG_DEPTH_NOT_TAG
    }

    override fun attribute(
        namespace: String?,
        name: String,
        prefix: String?,
        value: String,
    ) {
        val cur = requireCurrent("attribute")
        when {
            namespace.isNullOrEmpty() && prefix.isNullOrEmpty() -> {
                cur.setAttribute(name, value)
            }

            prefix.isNullOrEmpty() -> {
                cur.setAttributeNS(namespace, name, value)
            }

            else -> {
                cur.setAttributeNS(
                    namespace ?: XMLConstants.NULL_NS_URI,
                    "$prefix:$name",
                    value,
                )
            }
        }
    }

    override fun docdecl(text: String) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        val target = docDelegate
        if (target == null) {
            addToPending { docdecl(text) }
        } else {
            val textElems = text.split(" ", limit = 3)
            val qualifiedName = textElems[0]
            val publicId = if (textElems.size > 1) textElems[1] else ""
            val systemId = if (textElems.size > 2) textElems[2] else ""
            target.implementation.createDocumentType(qualifiedName, publicId, systemId).let { docType ->
                target.appendChild(docType)
            }
        }
    }

    override fun startDocument(
        version: String?,
        encoding: String?,
        standalone: Boolean?,
    ) {
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)
        // Ignore everything for now as this cannot be set on a dom tree
        requestedVersion = version
        requestedEncoding = encoding
        requestedStandalone = standalone
    }

    override fun endDocument() {
        currentNode = null
    }

    override fun endTag(
        namespace: String?,
        localName: String,
        prefix: String?,
    ) {
        depth--
        writeIndent(TAG_DEPTH_FORCE_INDENT_NEXT)

        currentNode = requireCurrent("No current element or no parent element").parentNode
    }

    override fun getNamespaceUri(prefix: String): String? = currentNode?.myLookupNamespaceURI(prefix)

    override fun getPrefix(namespaceUri: String?): String? = currentNode?.myLookupPrefix(namespaceUri ?: "")

    override fun setPrefix(
        prefix: String,
        namespaceUri: String,
    ) {
        val docDelegate = docDelegate
        if (docDelegate == null) {
            addToPending { setPrefix(prefix, namespaceUri) }
        } else {
            if (docDelegate.lookupNamespaceURI(prefix) != namespaceUri) {
                val qname = if (prefix.isEmpty()) "xmlns" else "xmlns:$prefix"
                (currentNode as? Element)?.setAttribute(qname, namespaceUri)
            }
        }
    }

    override fun close() {
        assert(depth == 0) { "Closing a dom writer but not all elements were closed (depth:$depth)" }
        currentNode = null
    }

    override fun flush() = Unit

    private companion object {
        private const val TAG_DEPTH_NOT_TAG = -1
        private const val TAG_DEPTH_FORCE_INDENT_NEXT = Int.MAX_VALUE
    }
}
