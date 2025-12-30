package be.endevops

import kotlin.reflect.full.memberProperties

/**
 * Very small, reflection-based serializer to XML for simple data classes.
 * - Supports primitive types and Strings
 * - Supports nested data classes
 * - Supports Iterables (as repeated child elements)
 * This is intentionally lightweight to avoid pulling extra XML libs.
 */
object XmlConverter {
    fun toXml(
        obj: Any,
        rootName: String,
        namespace: String? = null,
    ): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        appendElement(sb, rootName, obj, namespace, 0)
        return sb.toString()
    }

    // produce only the element (no XML declaration) for embedding inside SOAP body
    fun toElementXml(
        obj: Any,
        rootName: String,
        namespace: String? = null,
    ): String {
        val sb = StringBuilder()
        appendElement(sb, rootName, obj, namespace, 0)
        return sb.toString()
    }

    private fun appendElement(
        sb: StringBuilder,
        name: String,
        value: Any?,
        namespace: String?,
        indent: Int,
    ) {
        val pad = "  ".repeat(indent)
        if (value == null) {
            sb.append("$pad<$name/>")
            return
        }

        when (value) {
            is String, is Number, is Boolean -> {
                val text = escapeXml(value.toString())
                if (namespace != null) {
                    sb.append("$pad<$name xmlns=\"$namespace\">$text</$name>\n")
                } else {
                    sb.append("$pad<$name>$text</$name>\n")
                }
            }

            is Iterable<*> -> {
                for (item in value) {
                    appendElement(sb, name, item, namespace, indent)
                }
            }

            else -> {
                // assume data class / object with properties
                if (namespace != null) {
                    sb.append("$pad<$name xmlns=\"$namespace\">\n")
                } else {
                    sb.append("$pad<$name>\n")
                }
                val kClass = value::class
                val props = kClass.memberProperties
                for (p in props) {
                    val propName = p.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    val v = p.getter.call(value)
                    when (v) {
                        null -> {
                            sb.append("$pad  <$propName/>\n")
                        }

                        is String, is Number, is Boolean -> {
                            sb.append("$pad  <$propName>${escapeXml(v.toString())}</$propName>\n")
                        }

                        is Iterable<*> -> {
                            for (item in v) {
                                appendElement(sb, propName, item, null, indent + 2)
                            }
                        }

                        else -> {
                            appendElement(sb, propName, v, null, indent + 1)
                        }
                    }
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
