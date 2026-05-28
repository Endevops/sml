package be.endevops.xml

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlAfter
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName(value = "PublisherEndpoint", namespace = SERVICE_METADATA_LOCATOR_NS)
data class PublisherEndpointType(
    @XmlSerialName(value = "LogicalAddress", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val logicalAddress: String,
    @XmlAfter("logicalAddress")
    @XmlSerialName(value = "PhysicalAddress", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val physicalAddress: String,
)

// Create request
@Serializable
@XmlSerialName(value = "CreateServiceMetadataPublisherService", namespace = SERVICE_METADATA_LOCATOR_NS)
data class CreateServiceMetadataPublisherServiceType(
    @XmlSerialName(value = "PublisherEndpoint", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val publisherEndpoint: PublisherEndpointType,
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    @XmlAfter("publisherEndpoint")
    val serviceMetadataPublisherID: String,
)

// Create response
@Serializable
@XmlSerialName(value = "CreateServiceMetadataPublisherServiceResponse", namespace = SERVICE_METADATA_LOCATOR_NS)
data class CreateServiceMetadataPublisherServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlSerialName(value = "DatabaseId")
    @XmlElement(value = true)
    val databaseId: Int? = null,
    @XmlSerialName(value = "FaultMessage")
    @XmlElement(value = true)
    val faultMessage: String? = null,
)

// Read request
@Serializable
@XmlSerialName(value = "ReadServiceMetadataPublisherService", namespace = SERVICE_METADATA_LOCATOR_NS)
data class ReadServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String,
)

// Read response
@Serializable
@XmlSerialName(value = "ReadServiceMetadataPublisherServiceResponse", namespace = SERVICE_METADATA_LOCATOR_NS)
data class ReadServiceMetadataPublisherServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String? = null,
    @XmlSerialName(value = "PublisherEndpoint", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val publisherEndpoint: PublisherEndpointType? = null,
    @XmlSerialName(value = "FaultMessage")
    @XmlElement(value = true)
    val faultMessage: String? = null,
)

// Update request (same structure as Create)
@Serializable
@XmlSerialName(value = "UpdateServiceMetadataPublisherService", namespace = SERVICE_METADATA_LOCATOR_NS)
data class UpdateServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "PublisherEndpoint")
    @XmlElement(value = true)
    val publisherEndpoint: PublisherEndpointType,
    @XmlElement(value = true)
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    val serviceMetadataPublisherID: String,
)

// Update response
@Serializable
@XmlSerialName(value = "UpdateServiceMetadataPublisherServiceResponse", namespace = SERVICE_METADATA_LOCATOR_NS)
data class UpdateServiceMetadataPublisherServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlElement(value = true)
    @XmlSerialName(value = "FaultMessage")
    val faultMessage: String? = null,
)

// Delete request (same as Read request shape)
@Serializable
@XmlSerialName(value = "DeleteServiceMetadataPublisherService", namespace = SERVICE_METADATA_LOCATOR_NS)
data class DeleteServiceMetadataPublisherServiceRequestPojo(
    @XmlSerialName(value = "ServiceMetadataPublisherID", namespace = SERVICE_METADATA_LOCATOR_NS)
    @XmlElement(value = true)
    val serviceMetadataPublisherID: String,
)

// Shared Update/Delete response type
@Serializable
@XmlSerialName(value = "UpdateDeleteServiceResponse", namespace = SERVICE_METADATA_LOCATOR_NS)
data class UpdateDeleteServiceResponsePojo(
    @XmlSerialName(value = "Result")
    @XmlElement(value = true)
    val result: String? = null,
    @XmlSerialName(value = "FaultMessage")
    @XmlElement(value = true)
    val faultMessage: String? = null,
)
