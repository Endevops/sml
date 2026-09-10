package be.endevops.xml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.SerializableQName
import nl.adaptivity.xmlutil.serialization.XmlAfter
import nl.adaptivity.xmlutil.serialization.XmlBefore
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlOtherAttributes
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import nl.adaptivity.xmlutil.util.CompactFragment

const val SOAP11_NS = "http://schemas.xmlsoap.org/soap/envelope/"
const val SOAP11_PREFIX = "S11"

@Serializable
@XmlSerialName(value = "Body", namespace = SOAP11_NS, prefix = SOAP11_PREFIX)
data class Soap11Body(
    @XmlValue
    val any: List<CompactFragment> = emptyList(),
    @XmlOtherAttributes
    val otherAttributes: Map<SerializableQName, String> = emptyMap(),
)

@Serializable
@XmlSerialName(value = "detail", namespace = "")
data class Soap11Detail(
    @XmlValue
    val any: List<CompactFragment> = emptyList(),
    @XmlOtherAttributes
    val otherAttributes: Map<SerializableQName, String> = emptyMap(),
)

@Serializable
@XmlSerialName(value = "Envelope", namespace = SOAP11_NS, prefix = SOAP11_PREFIX)
data class Soap11Envelope(
    @XmlSerialName(value = "Header", namespace = SOAP11_NS, prefix = SOAP11_PREFIX)
    @XmlBefore("body")
    val header: Soap11Header? = null,
    @XmlSerialName(value = "Body", namespace = SOAP11_NS, prefix = SOAP11_PREFIX)
    val body: Soap11Body,
    @XmlOtherAttributes
    val otherAttributes: Map<SerializableQName, String> = emptyMap(),
)

@Serializable
@XmlSerialName(value = "Fault", namespace = SOAP11_NS, prefix = SOAP11_PREFIX)
data class Soap11Fault(
    @XmlSerialName(value = "faultcode", namespace = "")
    @XmlElement(value = true)
    val faultcode: SerializableQName? = null,
    @XmlSerialName(value = "faultstring", namespace = "")
    @XmlElement(value = true)
    @XmlAfter("faultcode")
    val faultstring: String? = null,
    @XmlSerialName(value = "faultactor", namespace = "")
    @XmlElement(value = true)
    @XmlAfter("faultstring")
    val faultactor: String? = null,
    @XmlSerialName(value = "detail", namespace = "")
    @XmlElement(value = true)
    @XmlAfter("faultactor")
    val detail: Soap11Detail? = null,
)

@Serializable
@XmlSerialName(value = "Header", namespace = SOAP11_NS, prefix = SOAP11_PREFIX)
data class Soap11Header(
    @XmlValue
    val any: List<CompactFragment> = emptyList(),
)
