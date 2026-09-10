package be.endevops.xml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName(value = "BadRequestFault", namespace = SERVICE_METADATA_LOCATOR_NS)
data class BadRequestFaultPojo(
    @XmlSerialName(value = "FaultMessage", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val faultMessage: String? = null,
)

@Serializable
@XmlSerialName(value = "NotFoundFault", namespace = SERVICE_METADATA_LOCATOR_NS)
data class NotFoundFaultPojo(
    @XmlSerialName(value = "FaultMessage", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val faultMessage: String? = null,
)

@Serializable
@XmlSerialName(value = "InternalErrorFault", namespace = SERVICE_METADATA_LOCATOR_NS)
data class InternalErrorFaultPojo(
    @XmlSerialName(value = "FaultMessage", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val faultMessage: String? = null,
)
