package be.endevops

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object JsonXmlConverter {
    private val json =
        Json {
            encodeDefaults = true
            prettyPrint = true
        }

    fun <T> toXml(
        obj: T,
        rootName: String,
        namespace: String? = null,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): String {
        val el = json.encodeToJsonElement(serializer, obj)
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        appendElement(sb, rootName, el, namespace, 0)
        return sb.toString()
    }

    fun <T> toElementXml(
        obj: T,
        rootName: String,
        namespace: String? = null,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): String {
        val el = json.encodeToJsonElement(serializer, obj)
        val sb = StringBuilder()
        appendElement(sb, rootName, el, namespace, 0)
        return sb.toString()
    }

    // decode XML string into kotlinx.serialization JsonElement using Jackson XmlMapper
    private val xmlMapper: XmlMapper by lazy {
        XmlMapper
            .builder()
            .findAndAddModules()
            .build()
    }

    fun fromXmlString(xml: String): JsonElement? =
        try {
            val node: JsonNode = xmlMapper.readTree(xml)
            jsonNodeToKotlinx(node)
        } catch (e: Exception) {
            null
        }

    // Convert Jackson JsonNode tree into kotlinx.serialization JsonElement
    private fun jsonNodeToKotlinx(node: JsonNode): JsonElement =
        when {
            node.isObject -> {
                val builder = JsonObjectBuilder()
                val it = node.fields()
                while (it.hasNext()) {
                    val e = it.next()
                    val rawKey = e.key
                    // normalize: strip namespace prefix (ns:Name -> Name), map text key to _text, titlecase first char
                    val key = normalizeKey(rawKey)
                    val value = jsonNodeToKotlinx(e.value)
                    // merge repeated keys into arrays (if needed)
                    val existing = builder.map()[key]
                    if (existing == null) {
                        builder.put(key, value)
                    } else {
                        val newArr =
                            when (existing) {
                                is JsonArray -> {
                                    val items = existing.toMutableList()
                                    items.add(value)
                                    JsonArray(items)
                                }

                                else -> {
                                    JsonArray(listOf(existing, value))
                                }
                            }
                        builder.put(key, newArr)
                    }
                }
                builder.build()
            }

            node.isArray -> {
                val arr = mutableListOf<JsonElement>()
                node.forEach { arr.add(jsonNodeToKotlinx(it)) }
                JsonArray(arr)
            }

            node.isTextual -> {
                JsonPrimitive(node.asText())
            }

            node.isNumber -> {
                JsonPrimitive(node.numberValue().toString())
            }

            node.isBoolean -> {
                JsonPrimitive(node.booleanValue())
            }

            node.isNull -> {
                JsonNull
            }

            else -> {
                JsonPrimitive(node.toString())
            }
        }

    // small helper to build JsonObject programmatically
    private class JsonObjectBuilder {
        private val map = mutableMapOf<String, JsonElement>()

        fun put(
            k: String,
            v: JsonElement,
        ) {
            map[k] = v
        }

        fun build(): JsonObject = JsonObject(map)

        fun map(): MutableMap<String, JsonElement> = map
    }

    private fun normalizeKey(k: String): String {
        var key = k
        val idx = key.indexOf(':')
        if (idx >= 0 && idx < key.length - 1) key = key.substring(idx + 1)
        if (key.isEmpty()) return key
        return key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    // legacy DOM-based methods kept for XML output generation
    private fun elementToJson(el: Element): JsonElement {
        // Helper: normalize element/attribute names to PascalCase to match Kotlin serializer fields
        fun normalizeName(n: String): String {
            if (n.isEmpty()) return n
            return n.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // Collect element child nodes and text nodes
        val childElements = mutableListOf<Element>()
        val nodeList = el.childNodes
        var textContent: String? = null
        for (i in 0 until nodeList.length) {
            val n = nodeList.item(i)
            when (n) {
                is Element -> {
                    childElements.add(n)
                }

                else -> {
                    val t = n.nodeValue?.trim() ?: ""
                    if (t.isNotBlank()) textContent = (textContent?.let { it + " " + t } ?: t)
                }
            }
        }

        // Attributes: include under keys prefixed with '@'
        val attrs = mutableMapOf<String, String>()
        val atts = el.attributes
        if (atts != null && atts.length > 0) {
            for (i in 0 until atts.length) {
                val a = atts.item(i)
                attrs[normalizeName(a.nodeName)] = a.nodeValue
            }
        }

        // If no child elements, treat as primitive (prefer textContent or attribute if present)
        if (childElements.isEmpty()) {
            val primary = textContent ?: attrs.values.firstOrNull() ?: ""
            return JsonPrimitive(primary)
        }

        // Group children by normalized name
        val grouped = mutableMapOf<String, MutableList<JsonElement>>()
        for (c in childElements) {
            val name = normalizeName(c.localName ?: c.nodeName)
            val je = elementToJson(c)
            grouped.computeIfAbsent(name) { mutableListOf() }.add(je)
        }

        val obj =
            buildJsonObject {
                // include attributes first
                for ((k, v) in attrs) {
                    put("@$k", JsonPrimitive(v))
                }

                // include grouped children
                for ((k, v) in grouped) {
                    if (v.size == 1) put(k, v[0]) else put(k, JsonArray(v))
                }

                // include mixed text content under _text key if present
                if (!textContent.isNullOrBlank()) {
                    put("_text", JsonPrimitive(textContent))
                }
            }
        return obj
    }

    private fun appendElement(
        sb: StringBuilder,
        name: String,
        el: JsonElement,
        namespace: String?,
        indent: Int,
    ) {
        val pad = "  ".repeat(indent)
        when (el) {
            is JsonNull -> {
                sb.append("$pad<$name/>\n")
            }

            is JsonPrimitive -> {
                val txt = escapeXml(el.contentOrNull ?: "")
                if (namespace != null) {
                    sb.append("$pad<$name xmlns=\"$namespace\">$txt</$name>\n")
                } else {
                    sb.append("$pad<$name>$txt</$name>\n")
                }
            }

            is JsonArray -> {
                for (it in el) appendElement(sb, name, it, namespace, indent)
            }

            is JsonObject -> {
                if (namespace != null) {
                    sb.append("$pad<$name xmlns=\"$namespace\">\n")
                } else {
                    sb.append("$pad<$name>\n")
                }
                for ((k, v) in el.entries) {
                    appendElement(sb, k.replaceFirstChar { it.titlecase() }, v, null, indent + 1)
                }
                sb.append("$pad</$name>\n")
            }
        }
    }

    private fun escapeXml(s: String): String =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
